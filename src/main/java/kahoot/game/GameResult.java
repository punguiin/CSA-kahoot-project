package kahoot.game;

import kahoot.model.Question;

import java.util.List;

public class GameResult {

    private final boolean success;
    private final String message;
    private final String pin;
    private final GameState state;
    private final Question currentQuestion;
    private final AnswerResult answerResult;
    private final List<Player> leaderboard;

    private GameResult(boolean success, String message, String pin, GameState state,
                       Question currentQuestion, AnswerResult answerResult, List<Player> leaderboard) {
        this.success = success;
        this.message = message;
        this.pin = pin;
        this.state = state;
        this.currentQuestion = currentQuestion;
        this.answerResult = answerResult;
        this.leaderboard = leaderboard;
    }

    public static GameResult error(String message) {
        return new GameResult(false, message, null, null, null, null, null);
    }

    public static GameResult roomCreated(String pin) {
        return new GameResult(true, "Room created", pin, GameState.LOBBY, null, null, null);
    }

    public static GameResult playerJoined(String pin, GameState state) {
        return new GameResult(true, "Joined room", pin, state, null, null, null);
    }

    public static GameResult questionStarted(String pin, GameState state, Question question) {
        return new GameResult(true, "Question started", pin, state, question, null, null);
    }

    public static GameResult answerSubmitted(String pin, AnswerResult answerResult) {
        return new GameResult(true, "Answer processed", pin, GameState.QUESTION, null, answerResult, null);
    }

    public static GameResult leaderboard(String pin, GameState state, List<Player> leaderboard) {
        return new GameResult(true, "Leaderboard", pin, state, null, null, leaderboard);
    }

    public static GameResult selfPacedNext(String pin, AnswerResult answerResult, Question nextQuestion) {
        return new GameResult(true, "Next question", pin, GameState.QUESTION, nextQuestion, answerResult, null);
    }

    public static GameResult selfPacedFinished(String pin, AnswerResult answerResult, List<Player> leaderboard) {
        return new GameResult(true, "Finished", pin, GameState.FINISHED, null, answerResult, leaderboard);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getPin() {
        return pin;
    }

    public GameState getState() {
        return state;
    }

    public Question getCurrentQuestion() {
        return currentQuestion;
    }

    public AnswerResult getAnswerResult() {
        return answerResult;
    }

    public List<Player> getLeaderboard() {
        return leaderboard;
    }

    @Override
    public String toString() {
        return "GameResult{success=" + success + ", message='" + message + "', pin='" + pin +
                "', state=" + state + "}";
    }
}
