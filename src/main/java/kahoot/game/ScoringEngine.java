package kahoot.game;

public final class ScoringEngine {

    public static final int BASE_POINTS = 1000;
    public static final int MAX_SPEED_BONUS = 500;

    private ScoringEngine() {
    }

    public static int calculatePoints(boolean correct, long questionStartedAt, long answeredAt, int timeLimitSeconds) {
        if (!correct) {
            return 0;
        }

        long timeLimitMillis = timeLimitSeconds * 1000L;
        long elapsed = answeredAt - questionStartedAt;

        if (elapsed < 0) {
            elapsed = 0;
        }
        if (elapsed > timeLimitMillis || timeLimitMillis <= 0) {
            return BASE_POINTS;
        }

        double remainingFraction = 1.0 - ((double) elapsed / timeLimitMillis);
        int speedBonus = (int) Math.round(MAX_SPEED_BONUS * remainingFraction);

        return BASE_POINTS + speedBonus;
    }
}
