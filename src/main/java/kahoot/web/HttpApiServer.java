package kahoot.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import kahoot.db.GameHistoryDAO;
import kahoot.db.QuizDAO;
import kahoot.db.UserDAO;
import kahoot.game.GameSession;
import kahoot.game.GameStateManager;
import kahoot.model.Answer;
import kahoot.model.Question;
import kahoot.model.Quiz;
import kahoot.model.User;
import kahoot.protocol.Json;
import kahoot.protocol.PayloadCodec;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class HttpApiServer {

    private final int port;
    private final QuizDAO quizDAO;
    private final UserDAO userDAO;
    private final GameHistoryDAO historyDAO;
    private final GameStateManager gameStateManager;
    private final Consumer<String> endRoom;
    private HttpServer server;

    public HttpApiServer(int port, QuizDAO quizDAO, UserDAO userDAO, GameHistoryDAO historyDAO,
                         GameStateManager gameStateManager, Consumer<String> endRoom) {
        this.port = port;
        this.quizDAO = quizDAO;
        this.userDAO = userDAO;
        this.historyDAO = historyDAO;
        this.gameStateManager = gameStateManager;
        this.endRoom = endRoom;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/quizzes", guard(this::handleQuizzes));
        server.createContext("/history", guard(this::handleHistory));
        server.createContext("/users", guard(this::handleUsers));
        server.createContext("/sessions", guard(this::handleSessions));
        server.createContext("/login", guard(this::handleLogin));
        server.createContext("/register", guard(this::handleRegister));
        server.start();
        System.out.println("HttpApiServer: listening on " + server.getAddress().getPort());
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public int port() {
        return server != null ? server.getAddress().getPort() : port;
    }

    private void handleQuizzes(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] seg = segments(ex);
        if (method.equals("GET") && seg.length == 2) {
            List<String> items = new ArrayList<>();
            for (Quiz quiz : quizDAO.findAll()) {
                items.add(Json.Writer.object()
                        .num("id", quiz.getId())
                        .str("title", quiz.getTitle())
                        .str("description", quiz.getDescription())
                        .num("questionCount", quiz.getQuestions().size())
                        .end());
            }
            send(ex, 200, Json.array(items));
        } else if (method.equals("GET") && seg.length == 3) {
            Quiz quiz = quizDAO.findById(Integer.parseInt(seg[2])).orElse(null);
            if (quiz == null) {
                send(ex, 404, PayloadCodec.error("Quiz not found"));
            } else {
                send(ex, 200, fullQuizJson(quiz));
            }
        } else if (method.equals("POST") && seg.length == 2) {
            int id = quizDAO.insert(quizFromBody(readBody(ex), null));
            send(ex, 200, "{\"id\":" + id + "}");
        } else if (method.equals("PUT") && seg.length == 3) {
            quizDAO.update(quizFromBody(readBody(ex), Integer.parseInt(seg[2])));
            send(ex, 200, "{\"updated\":true}");
        } else if (method.equals("DELETE") && seg.length == 3) {
            quizDAO.deleteById(Integer.parseInt(seg[2]));
            send(ex, 200, "{\"deleted\":true}");
        } else {
            send(ex, 405, PayloadCodec.error("Unsupported"));
        }
    }

    private void handleHistory(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) {
            send(ex, 405, PayloadCodec.error("Unsupported"));
            return;
        }
        List<String> items = new ArrayList<>();
        for (GameHistoryDAO.Entry e : historyDAO.findRecent()) {
            items.add(Json.Writer.object()
                    .str("quizTitle", e.quizTitle())
                    .str("playedAt", e.playedAt())
                    .str("winner", e.winnerNickname())
                    .num("players", e.playersCount())
                    .end());
        }
        send(ex, 200, Json.array(items));
    }

    private void handleUsers(HttpExchange ex) throws IOException {
        String[] seg = segments(ex);
        if (ex.getRequestMethod().equals("GET") && seg.length == 2) {
            List<String> items = new ArrayList<>();
            for (User user : userDAO.findAll()) {
                items.add(Json.Writer.object()
                        .num("id", user.getId())
                        .str("username", user.getUsername())
                        .str("role", user.getRole())
                        .str("status", user.getStatus())
                        .end());
            }
            send(ex, 200, Json.array(items));
        } else if (ex.getRequestMethod().equals("POST") && seg.length == 4 && seg[3].equals("status")) {
            Map<String, Object> body = readBody(ex);
            String status = String.valueOf(body.get("status"));
            userDAO.updateStatus(Integer.parseInt(seg[2]), status);
            send(ex, 200, "{\"updated\":true}");
        } else {
            send(ex, 405, PayloadCodec.error("Unsupported"));
        }
    }

    private void handleSessions(HttpExchange ex) throws IOException {
        String[] seg = segments(ex);
        if (ex.getRequestMethod().equals("GET") && seg.length == 2) {
            List<String> items = new ArrayList<>();
            for (GameSession s : gameStateManager.all()) {
                String title = quizDAO.findById(s.getQuizId()).map(Quiz::getTitle).orElse("—");
                items.add(Json.Writer.object()
                        .str("pin", s.getPin())
                        .str("quizTitle", title)
                        .num("players", s.getPlayerCount())
                        .str("state", s.getState().name())
                        .end());
            }
            send(ex, 200, Json.array(items));
        } else if (ex.getRequestMethod().equals("POST") && seg.length == 4 && seg[3].equals("end")) {
            endRoom.accept(seg[2]);
            send(ex, 200, "{\"ended\":true}");
        } else {
            send(ex, 405, PayloadCodec.error("Unsupported"));
        }
    }

    private void handleLogin(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) {
            send(ex, 405, PayloadCodec.error("Unsupported"));
            return;
        }
        Map<String, Object> body = readBody(ex);
        String username = String.valueOf(body.get("username"));
        String password = String.valueOf(body.get("password"));
        User user = userDAO.findByUsernameAndPassword(username, password).orElse(null);
        if (user == null) {
            send(ex, 401, PayloadCodec.error("Неправильний логін або пароль"));
            return;
        }
        if (User.BLOCKED.equals(user.getStatus())) {
            send(ex, 403, PayloadCodec.error("Обліковий запис заблоковано"));
            return;
        }
        send(ex, 200, userJson(user));
    }

    private void handleRegister(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) {
            send(ex, 405, PayloadCodec.error("Unsupported"));
            return;
        }
        Map<String, Object> body = readBody(ex);
        String username = String.valueOf(body.get("username"));
        String password = String.valueOf(body.get("password"));
        if (username.isBlank() || password.isBlank()) {
            send(ex, 400, PayloadCodec.error("Потрібні логін і пароль"));
            return;
        }
        if (userDAO.findByUsername(username).isPresent()) {
            send(ex, 409, PayloadCodec.error("Користувач вже існує"));
            return;
        }
        int id = userDAO.insert(new User(username, password, "PLAYER"));
        send(ex, 200, userJson(userDAO.findById(id).orElseThrow()));
    }

    @SuppressWarnings("unchecked")
    private static Quiz quizFromBody(Map<String, Object> body, Integer id) {
        String title = String.valueOf(body.getOrDefault("title", ""));
        String description = body.get("description") == null ? "" : String.valueOf(body.get("description"));
        int creatorId = body.get("creatorId") instanceof Long l ? l.intValue() : 1;

        List<Question> questions = new ArrayList<>();
        for (Object qo : (List<Object>) body.getOrDefault("questions", List.of())) {
            Map<String, Object> qm = (Map<String, Object>) qo;
            int timeLimit = qm.get("timeLimit") instanceof Long l ? l.intValue() : 15;
            List<Answer> answers = new ArrayList<>();
            for (Object ao : (List<Object>) qm.getOrDefault("answers", List.of())) {
                Map<String, Object> am = (Map<String, Object>) ao;
                answers.add(new Answer(0, String.valueOf(am.get("text")), Boolean.TRUE.equals(am.get("isCorrect"))));
            }
            Question question = new Question(0, String.valueOf(qm.get("text")), timeLimit);
            question.setAnswers(answers);
            questions.add(question);
        }

        if (id == null) {
            Quiz quiz = new Quiz(title, description, creatorId);
            quiz.setQuestions(questions);
            return quiz;
        }
        return new Quiz(id, title, description, creatorId, questions);
    }

    private static String fullQuizJson(Quiz quiz) {
        List<String> questions = new ArrayList<>();
        for (Question q : quiz.getQuestions()) {
            List<String> answers = new ArrayList<>();
            for (Answer a : q.getAnswers()) {
                answers.add(Json.Writer.object().str("text", a.getText()).bool("isCorrect", a.isCorrect()).end());
            }
            questions.add(Json.Writer.object()
                    .str("text", q.getText())
                    .num("timeLimit", q.getTimeLimit())
                    .raw("answers", Json.array(answers))
                    .end());
        }
        return Json.Writer.object()
                .num("id", quiz.getId())
                .str("title", quiz.getTitle())
                .str("description", quiz.getDescription())
                .raw("questions", Json.array(questions))
                .end();
    }

    private static String userJson(User user) {
        return Json.Writer.object()
                .num("id", user.getId())
                .str("username", user.getUsername())
                .str("role", user.getRole())
                .str("status", user.getStatus())
                .end();
    }

    private HttpHandler guard(ThrowingHandler handler) {
        return ex -> {
            var headers = ex.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            if (ex.getRequestMethod().equals("OPTIONS")) {
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }
            try {
                handler.handle(ex);
            } catch (RuntimeException e) {
                send(ex, 400, PayloadCodec.error(e.getMessage() == null ? "Bad request" : e.getMessage()));
            }
        };
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange ex) throws IOException;
    }

    private static String[] segments(HttpExchange ex) {
        return ex.getRequestURI().getPath().split("/");
    }

    private Map<String, Object> readBody(HttpExchange ex) throws IOException {
        byte[] body = ex.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return Map.of();
        }
        return Json.parseObject(new String(body, StandardCharsets.UTF_8));
    }

    private void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}