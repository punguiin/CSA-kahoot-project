package kahoot.game;

public class AnswerResult {

    private final boolean accepted;
    private final boolean correct;
    private final int pointsAwarded;
    private final String message;

    private AnswerResult(boolean accepted, boolean correct, int pointsAwarded, String message) {
        this.accepted = accepted;
        this.correct = correct;
        this.pointsAwarded = pointsAwarded;
        this.message = message;
    }

    public static AnswerResult accepted(boolean correct, int pointsAwarded) {
        return new AnswerResult(true, correct, pointsAwarded, null);
    }

    public static AnswerResult rejected(String message) {
        return new AnswerResult(false, false, 0, message);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "AnswerResult{accepted=" + accepted + ", correct=" + correct +
                ", pointsAwarded=" + pointsAwarded + ", message='" + message + "'}";
    }
}