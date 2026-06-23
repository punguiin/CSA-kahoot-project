package kahoot.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateManagerTest {

    private GameStateManager sut;

    @BeforeEach
    void setUp() {
        sut = new GameStateManager();
    }

    @Test
    void shouldCreateSessionWithValidPin() {
        GameSession session = sut.createSession(1);

        assertThat(session.getPin())
                .hasSize(6)
                .matches("\\d{6}");
    }

    @Test
    void shouldBeAbleToRetrieveSessionByPin() {
        GameSession created = sut.createSession(1);

        assertThat(sut.getSession(created.getPin()))
                .isPresent()
                .get()
                .isEqualTo(created);
    }

    @Test
    void shouldReturnEmptyForUnknownPin() {
        assertThat(sut.getSession("000000")).isEmpty();
    }

    @Test
    void shouldCreateMultipleSessionsWithUniquePins() {
        GameSession s1 = sut.createSession(1);
        GameSession s2 = sut.createSession(2);
        GameSession s3 = sut.createSession(3);

        assertThat(Set.of(s1.getPin(), s2.getPin(), s3.getPin()))
                .hasSize(3);
    }

    @Test
    void shouldCountActiveSessions() {
        sut.createSession(1);
        sut.createSession(2);

        assertThat(sut.activeSessionCount()).isEqualTo(2);
    }

    @Test
    void shouldAllowPlayerToJoinExistingSession() {
        GameSession session = sut.createSession(1);

        boolean joined = sut.joinSession(session.getPin(), "nikita");

        assertThat(joined).isTrue();
        assertThat(session.getPlayerCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectJoinForUnknownPin() {
        boolean joined = sut.joinSession("999999", "ghost");

        assertThat(joined).isFalse();
    }

    @Test
    void shouldRejectDuplicateNicknameViaManager() {
        GameSession session = sut.createSession(1);
        sut.joinSession(session.getPin(), "nikita");

        boolean joinedAgain = sut.joinSession(session.getPin(), "nikita");

        assertThat(joinedAgain).isFalse();
        assertThat(session.getPlayerCount()).isEqualTo(1);
    }

    @Test
    void shouldRemoveSessionByPin() {
        GameSession session = sut.createSession(1);
        String pin = session.getPin();

        boolean removed = sut.removeSession(pin);

        assertThat(removed).isTrue();
        assertThat(sut.sessionExists(pin)).isFalse();
        assertThat(sut.activeSessionCount()).isZero();
    }

    @Test
    void shouldReturnFalseWhenRemovingNonExistentSession() {
        assertThat(sut.removeSession("000000")).isFalse();
    }

    @Test
    void shouldCreateUniqueSessionsUnderConcurrentLoad() throws InterruptedException {
        int threadCount = 30;
        Set<String> collectedPins = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int quizId = i;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    GameSession s = sut.createSession(quizId);
                    collectedPins.add(s.getPin());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(collectedPins).hasSize(threadCount);
        assertThat(sut.activeSessionCount()).isEqualTo(threadCount);
    }

    @Test
    void shouldHandleConcurrentJoinsAcrossDifferentSessions() throws InterruptedException {
        int sessionCount = 5;
        int playersPerSession = 10;

        GameSession[] sessions = new GameSession[sessionCount];
        for (int i = 0; i < sessionCount; i++) {
            sessions[i] = sut.createSession(i);
        }

        ExecutorService pool = Executors.newFixedThreadPool(sessionCount * playersPerSession);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(sessionCount * playersPerSession);

        for (int s = 0; s < sessionCount; s++) {
            final String pin = sessions[s].getPin();
            for (int p = 0; p < playersPerSession; p++) {
                final String nickname = "player_" + p;
                pool.submit(() -> {
                    try {
                        startLatch.await();
                        sut.joinSession(pin, nickname);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        for (GameSession session : sessions) {
            assertThat(session.getPlayerCount()).isEqualTo(playersPerSession);
        }
    }
}
