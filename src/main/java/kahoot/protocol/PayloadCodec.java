package kahoot.protocol;

import kahoot.game.AnswerResult;
import kahoot.game.GameAction;
import kahoot.game.GameState;
import kahoot.game.Player;
import kahoot.model.Answer;
import kahoot.model.Question;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PayloadCodec {

    private PayloadCodec() {
    }

    public static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    public static Map<String, Object> read(byte[] payload) {
        return Json.parseObject(new String(payload, StandardCharsets.UTF_8));
    }

    public static GameAction toAction(MessageType type, Map<String, Object> json,
                                      String boundPin, String boundNickname) {
        return switch (type) {
            case REQ_CREATE_ROOM -> GameAction.createRoom(reqInt(json, "quizId"));
            case REQ_JOIN_ROOM -> GameAction.joinRoom(reqStr(json, "pin"), reqStr(json, "nickname"));
            case REQ_START_QUIZ -> GameAction.startQuiz(boundPin);
            case REQ_SUBMIT_ANSWER -> GameAction.submitAnswer(boundPin, boundNickname, reqInt(json, "answerId"));
            case REQ_NEXT_QUESTION -> GameAction.nextQuestion(boundPin);
            case REQ_END_ROUND -> GameAction.endRound(boundPin);
            case REQ_GET_LEADERBOARD -> GameAction.getLeaderboard(boundPin);
            default -> throw new IllegalArgumentException("Not a request type: " + type);
        };
    }

    public static String roomCreated(String pin) {
        return Json.Writer.object().str("pin", pin).end();
    }

    public static String joinAccepted(String pin, String nickname, GameState state) {
        return Json.Writer.object()
                .str("pin", pin)
                .str("nickname", nickname)
                .str("state", state.name())
                .end();
    }

    public static String playerJoined(String pin, List<Player> players, String joined) {
        return Json.Writer.object()
                .str("pin", pin)
                .raw("players", players(players))
                .str("joined", joined)
                .end();
    }

    public static String playerLeft(String pin, List<Player> players, String left) {
        return Json.Writer.object()
                .str("pin", pin)
                .raw("players", players(players))
                .str("left", left)
                .end();
    }

    public static String question(String pin, int index, Question q) {
        List<String> answers = new ArrayList<>();
        for (Answer a : q.getAnswers()) {
            Json.Writer w = Json.Writer.object();
            if (a.getId() == null) {
                w.str("id", null);
            } else {
                w.num("id", a.getId());
            }
            answers.add(w.str("text", a.getText()).end());
        }
        return Json.Writer.object()
                .str("pin", pin)
                .num("index", index)
                .str("text", q.getText())
                .num("timeLimit", q.getTimeLimit())
                .raw("answers", Json.array(answers))
                .end();
    }

    public static String answerResult(AnswerResult result) {
        return Json.Writer.object()
                .bool("accepted", result.isAccepted())
                .bool("correct", result.isCorrect())
                .num("points", result.getPointsAwarded())
                .end();
    }

    public static String leaderboard(String pin, GameState state, List<Player> players) {
        return Json.Writer.object()
                .str("pin", pin)
                .str("state", state.name())
                .raw("leaderboard", players(players))
                .end();
    }

    public static String error(String message) {
        return Json.Writer.object().str("message", message).end();
    }

    private static String players(List<Player> players) {
        List<String> items = new ArrayList<>();
        for (Player p : players) {
            items.add(Json.Writer.object()
                    .str("nickname", p.getNickname())
                    .num("score", p.getScore())
                    .end());
        }
        return Json.array(items);
    }

    private static String reqStr(Map<String, Object> json, String key) {
        Object v = json.get(key);
        if (!(v instanceof String s)) {
            throw new IllegalArgumentException("Missing or non-string field: " + key);
        }
        return s;
    }

    private static int reqInt(Map<String, Object> json, String key) {
        Object v = json.get(key);
        if (!(v instanceof Long l)) {
            throw new IllegalArgumentException("Missing or non-integer field: " + key);
        }
        return l.intValue();
    }
}
