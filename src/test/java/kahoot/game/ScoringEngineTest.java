package kahoot.game;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringEngineTest {

    @Test
    void shouldAwardZeroPointsForIncorrectAnswer() {
        int points = ScoringEngine.calculatePoints(false, 0L, 5_000L, 10);

        assertThat(points).isZero();
    }

    @Test
    void shouldAwardBasePointsPlusFullBonusForInstantCorrectAnswer() {
        int points = ScoringEngine.calculatePoints(true, 1_000L, 1_000L, 10);

        assertThat(points).isEqualTo(ScoringEngine.BASE_POINTS + ScoringEngine.MAX_SPEED_BONUS);
    }

    @Test
    void shouldAwardOnlyBasePointsWhenAnsweredExactlyAtTimeLimit() {
        long start = 0L;
        long timeLimitSeconds = 10;
        long answeredAt = start + timeLimitSeconds * 1000L;

        int points = ScoringEngine.calculatePoints(true, start, answeredAt, (int) timeLimitSeconds);

        assertThat(points).isEqualTo(ScoringEngine.BASE_POINTS);
    }

    @Test
    void shouldAwardOnlyBasePointsWhenAnsweredAfterTimeLimit() {
        int points = ScoringEngine.calculatePoints(true, 0L, 20_000L, 10);

        assertThat(points).isEqualTo(ScoringEngine.BASE_POINTS);
    }

    @Test
    void shouldAwardPartialBonusForAnswerInTheMiddleOfTimeWindow() {
        long start = 0L;
        long answeredAt = 5_000L;

        int points = ScoringEngine.calculatePoints(true, start, answeredAt, 10);

        int expectedBonus = (int) Math.round(ScoringEngine.MAX_SPEED_BONUS * 0.5);
        assertThat(points).isEqualTo(ScoringEngine.BASE_POINTS + expectedBonus);
    }

    @Test
    void fasterAnswerShouldScoreHigherThanSlowerAnswer() {
        int fastPoints = ScoringEngine.calculatePoints(true, 0L, 1_000L, 10);
        int slowPoints = ScoringEngine.calculatePoints(true, 0L, 8_000L, 10);

        assertThat(fastPoints).isGreaterThan(slowPoints);
    }

    @Test
    void shouldTreatNegativeElapsedTimeAsZero() {
        int points = ScoringEngine.calculatePoints(true, 5_000L, 1_000L, 10);

        assertThat(points).isEqualTo(ScoringEngine.BASE_POINTS + ScoringEngine.MAX_SPEED_BONUS);
    }

    @Test
    void shouldFallBackToBasePointsWhenTimeLimitIsZero() {
        int points = ScoringEngine.calculatePoints(true, 0L, 100L, 0);

        assertThat(points).isEqualTo(ScoringEngine.BASE_POINTS);
    }
}