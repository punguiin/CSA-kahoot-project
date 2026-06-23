package kahoot.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameSessionTest {

    private GameSession sut;

    @BeforeEach
    void setUp() {
        sut = new GameSession("123456", 1);
    }

    @Test
    void shouldStartInLobbyState() {
        assertThat(sut.getState()).isEqualTo(GameState.LOBBY);
    }

    @Test
    void shouldHaveNoPlayersInitially() {
        assertThat(sut.getPlayerCount()).isZero();
    }

    @Test
    void shouldAllowPlayerToJoinLobby() {
        boolean joined = sut.addPlayer("nikita");

        assertThat(joined).isTrue();
        assertThat(sut.getPlayerCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateNickname() {
        sut.addPlayer("nikita");

        boolean joinedAgain = sut.addPlayer("nikita");

        assertThat(joinedAgain).isFalse();
        assertThat(sut.getPlayerCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateNicknameCaseInsensitive() {
        sut.addPlayer("Nikita");

        assertThat(sut.addPlayer("NIKITA")).isFalse();
        assertThat(sut.addPlayer("nikita")).isFalse();
    }

    @Test
    void shouldRejectPlayerAfterGameStarts() {
        sut.startQuiz();

        boolean joined = sut.addPlayer("latePlayer");

        assertThat(joined).isFalse();
    }

    @Test
    void shouldTransitionLobbyToQuestion() {
        sut.startQuiz();

        assertThat(sut.getState()).isEqualTo(GameState.QUESTION);
    }

    @Test
    void shouldTransitionQuestionToLeaderboard() {
        sut.startQuiz();
        sut.endRound();

        assertThat(sut.getState()).isEqualTo(GameState.LEADERBOARD);
    }

    @Test
    void shouldTransitionLeaderboardToNextQuestion() {
        sut.startQuiz();
        sut.endRound();
        sut.nextQuestion(true);

        assertThat(sut.getState()).isEqualTo(GameState.QUESTION);
    }

    @Test
    void shouldTransitionLeaderboardToFinishedOnLastQuestion() {
        sut.startQuiz();
        sut.endRound();
        sut.nextQuestion(false);

        assertThat(sut.getState()).isEqualTo(GameState.FINISHED);
    }

    @Test
    void shouldThrowWhenStartingAlreadyRunningGame() {
        sut.startQuiz();

        assertThatThrownBy(sut::startQuiz)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowWhenEndingRoundFromLobby() {
        assertThatThrownBy(sut::endRound)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldReturnPlayersOrderedByScoreDescending() {
        sut.addPlayer("alice");
        sut.addPlayer("bob");
        sut.addPlayer("carol");

        List<Player> players = sut.getPlayers();
        players.get(0).addScore(300);
        players.get(1).addScore(500);
        players.get(2).addScore(100);

        List<Player> leaderboard = sut.getLeaderboard();

        assertThat(leaderboard)
                .extracting(Player::getNickname)
                .containsExactly("bob", "alice", "carol");
    }

    @Test
    void shouldHandleConcurrentJoinsWithoutDuplicates() throws InterruptedException {
        int threadCount = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            final String nick = "player_" + i;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    sut.addPlayer(nick);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(sut.getPlayerCount()).isEqualTo(threadCount);
    }

    @Test
    void shouldAllowOnlyOneWinnerWhenMultipleThreadsTryTheSameNickname()
            throws InterruptedException {
        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    sut.addPlayer("sharedNickname");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(sut.getPlayerCount()).isEqualTo(1);
    }
}
