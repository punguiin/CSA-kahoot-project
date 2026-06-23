package kahoot.game;

public class GameAction {

    private final GameActionType type;
    private final String pin;
    private final String nickname;
    private final int quizId;
    private final int answerId;

    private GameAction(GameActionType type, String pin, String nickname, int quizId, int answerId) {
        this.type = type;
        this.pin = pin;
        this.nickname = nickname;
        this.quizId = quizId;
        this.answerId = answerId;
    }

    public static GameAction createRoom(int quizId) {
        return new GameAction(GameActionType.CREATE_ROOM, null, null, quizId, 0);
    }

    public static GameAction joinRoom(String pin, String nickname) {
        return new GameAction(GameActionType.JOIN_ROOM, pin, nickname, 0, 0);
    }

    public static GameAction startQuiz(String pin) {
        return new GameAction(GameActionType.START_QUIZ, pin, null, 0, 0);
    }

    public static GameAction submitAnswer(String pin, String nickname, int answerId) {
        return new GameAction(GameActionType.SUBMIT_ANSWER, pin, nickname, 0, answerId);
    }

    public static GameAction nextQuestion(String pin) {
        return new GameAction(GameActionType.NEXT_QUESTION, pin, null, 0, 0);
    }

    public static GameAction endRound(String pin) {
        return new GameAction(GameActionType.END_ROUND, pin, null, 0, 0);
    }

    public static GameAction getLeaderboard(String pin) {
        return new GameAction(GameActionType.GET_LEADERBOARD, pin, null, 0, 0);
    }

    public GameActionType getType() {
        return type;
    }

    public String getPin() {
        return pin;
    }

    public String getNickname() {
        return nickname;
    }

    public int getQuizId() {
        return quizId;
    }

    public int getAnswerId() {
        return answerId;
    }

    @Override
    public String toString() {
        return "GameAction{type=" + type + ", pin='" + pin + "', nickname='" + nickname +
                "', quizId=" + quizId + ", answerId=" + answerId + "}";
    }
}
