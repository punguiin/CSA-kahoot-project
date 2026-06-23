package kahoot.web;

import kahoot.db.DatabaseConnection;
import kahoot.db.GameHistoryDAO;
import kahoot.db.GameHistoryDAOImpl;
import kahoot.db.QuizDAO;
import kahoot.db.QuizDAOImpl;
import kahoot.db.UserDAO;
import kahoot.db.UserDAOImpl;
import kahoot.game.GameSession;
import kahoot.game.GameStateManager;
import kahoot.model.Answer;
import kahoot.model.Question;
import kahoot.model.Quiz;
import kahoot.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class HttpApiServerTest {

    private HttpApiServer server;
    private QuizDAO quizDAO;
    private UserDAO userDAO;
    private GameHistoryDAO historyDAO;
    private GameStateManager gsm;
    private final List<String> endedRooms = new CopyOnWriteArrayList<>();
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() throws Exception {
        var connection = new DatabaseConnection(":memory:").getConnection();
        quizDAO = new QuizDAOImpl(connection);
        userDAO = new UserDAOImpl(connection);
        historyDAO = new GameHistoryDAOImpl(connection);
        gsm = new GameStateManager();

        Question q1 = new Question(0, "Q1", 10);
        q1.setAnswers(List.of(new Answer(0, "a", true), new Answer(0, "b", false)));
        Question q2 = new Question(0, "Q2", 10);
        q2.setAnswers(List.of(new Answer(0, "c", true), new Answer(0, "d", false)));
        Quiz quiz = new Quiz("Networks", "About networks", 1);
        quiz.setQuestions(List.of(q1, q2));
        quizDAO.insert(quiz);

        userDAO.insert(new User("admin", "admin", "ADMIN"));

        server = new HttpApiServer(0, quizDAO, userDAO, historyDAO, gsm, endedRooms::add);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher pub = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path))
                .header("Content-Type", "application/json")
                .method(method, pub)
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void listsQuizzesWithRealQuestionCount() throws Exception {
        HttpResponse<String> r = send("GET", "/quizzes", null);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("Access-Control-Allow-Origin")).contains("*");
        assertThat(r.body()).contains("\"title\":\"Networks\"").contains("\"questionCount\":2");
    }

    @Test
    void registerThenLoginWorks() throws Exception {
        assertThat(send("POST", "/register", "{\"username\":\"sam\",\"password\":\"pw\"}").statusCode()).isEqualTo(200);
        HttpResponse<String> login = send("POST", "/login", "{\"username\":\"sam\",\"password\":\"pw\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("\"role\":\"PLAYER\"");
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        assertThat(send("POST", "/login", "{\"username\":\"admin\",\"password\":\"nope\"}").statusCode()).isEqualTo(401);
    }

    @Test
    void blockedUserCannotLogin() throws Exception {
        int id = userDAO.findByUsername("admin").orElseThrow().getId();
        assertThat(send("POST", "/users/" + id + "/status", "{\"status\":\"BLOCKED\"}").statusCode()).isEqualTo(200);
        assertThat(send("POST", "/login", "{\"username\":\"admin\",\"password\":\"admin\"}").statusCode()).isEqualTo(403);
    }

    @Test
    void listsUsersWithStatus() throws Exception {
        HttpResponse<String> r = send("GET", "/users", null);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("\"username\":\"admin\"").contains("\"status\":\"ACTIVE\"");
    }

    @Test
    void deletesQuiz() throws Exception {
        assertThat(send("DELETE", "/quizzes/1", null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/quizzes", null).body()).doesNotContain("\"title\":\"Networks\"");
    }

    @Test
    void createsFetchesAndUpdatesQuiz() throws Exception {
        String payload = "{\"title\":\"Math\",\"description\":\"d\",\"questions\":["
                + "{\"text\":\"1+1\",\"timeLimit\":5,\"answers\":[{\"text\":\"2\",\"isCorrect\":true},{\"text\":\"3\",\"isCorrect\":false}]}]}";
        HttpResponse<String> created = send("POST", "/quizzes", payload);
        assertThat(created.statusCode()).isEqualTo(200);

        assertThat(send("GET", "/quizzes", null).body()).contains("\"title\":\"Math\"");

        int id = quizDAO.findAll().stream().filter(q -> q.getTitle().equals("Math")).findFirst().orElseThrow().getId();
        HttpResponse<String> full = send("GET", "/quizzes/" + id, null);
        assertThat(full.body()).contains("\"text\":\"1+1\"").contains("\"isCorrect\":true");

        String updated = "{\"title\":\"Math v2\",\"description\":\"d\",\"questions\":["
                + "{\"text\":\"2+2\",\"timeLimit\":5,\"answers\":[{\"text\":\"4\",\"isCorrect\":true}]}]}";
        assertThat(send("PUT", "/quizzes/" + id, updated).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/quizzes/" + id, null).body()).contains("Math v2").contains("2+2");
    }

    @Test
    void deletingQuizRemovesItsQuestions() throws Exception {
        send("DELETE", "/quizzes/1", null);

        assertThat(send("GET", "/quizzes", null).body()).doesNotContain("Networks");
    }

    @Test
    void listsGameHistory() throws Exception {
        historyDAO.insert(1, "alice", 3);
        HttpResponse<String> r = send("GET", "/history", null);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("\"winner\":\"alice\"").contains("\"quizTitle\":\"Networks\"");
    }

    @Test
    void listsAndForceEndsSessions() throws Exception {
        GameSession session = gsm.createSession(1);
        HttpResponse<String> list = send("GET", "/sessions", null);
        assertThat(list.body()).contains("\"pin\":\"" + session.getPin() + "\"").contains("\"quizTitle\":\"Networks\"");

        assertThat(send("POST", "/sessions/" + session.getPin() + "/end", null).statusCode()).isEqualTo(200);
        assertThat(endedRooms).containsExactly(session.getPin());
    }
}
