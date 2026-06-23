package kahoot.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateManagerReapTest {

    private static final long FINISHED_GRACE = 5 * 60_000L;
    private static final long IDLE_TIMEOUT = 60 * 60_000L;

    @Test
    void keepsFreshSessions() {
        GameStateManager manager = new GameStateManager();
        GameSession session = manager.createSession(1);

        long now = session.getCreatedAt() + 1_000;
        List<String> notified = manager.reapStale(now, FINISHED_GRACE, IDLE_TIMEOUT);

        assertThat(notified).isEmpty();
        assertThat(manager.sessionExists(session.getPin())).isTrue();
    }

    @Test
    void reapsIdleNonFinishedSessionAndReportsIt() {
        GameStateManager manager = new GameStateManager();
        GameSession session = manager.createSession(1);

        long now = session.getLastActivityAt() + IDLE_TIMEOUT + 1;
        List<String> notified = manager.reapStale(now, FINISHED_GRACE, IDLE_TIMEOUT);

        assertThat(notified).containsExactly(session.getPin());
        assertThat(manager.sessionExists(session.getPin())).isFalse();
    }

    @Test
    void reapsFinishedSessionSilentlyAfterGrace() {
        GameStateManager manager = new GameStateManager();
        GameSession session = manager.createSession(1);
        finishImmediately(session);

        long now = session.getFinishedAt() + FINISHED_GRACE + 1;
        List<String> notified = manager.reapStale(now, FINISHED_GRACE, IDLE_TIMEOUT);

        assertThat(notified).isEmpty();
        assertThat(manager.sessionExists(session.getPin())).isFalse();
    }

    @Test
    void keepsFinishedSessionDuringGrace() {
        GameStateManager manager = new GameStateManager();
        GameSession session = manager.createSession(1);
        finishImmediately(session);

        long now = session.getFinishedAt() + 1_000;
        List<String> notified = manager.reapStale(now, FINISHED_GRACE, IDLE_TIMEOUT);

        assertThat(notified).isEmpty();
        assertThat(manager.sessionExists(session.getPin())).isTrue();
    }

    private static void finishImmediately(GameSession session) {
        session.setQuiz(emptyQuiz());
        session.addPlayer("solo");
        session.startSelfPaced();
        session.markFinishedOnce();
        assertThat(session.getState()).isEqualTo(GameState.FINISHED);
    }

    private static kahoot.model.Quiz emptyQuiz() {
        kahoot.model.Quiz quiz = new kahoot.model.Quiz("t", "d", 1);
        quiz.setQuestions(List.of());
        return quiz;
    }
}
