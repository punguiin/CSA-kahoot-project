package kahoot.game;

import kahoot.model.Answer;
import kahoot.model.Question;
import kahoot.model.Quiz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GameSessionScoringTest {

    private GameSession sut;
    private Answer correctAnswer;
    private Answer wrongAnswer;

    @BeforeEach
    void setUp() {
        correctAnswer = new Answer(1, 1, "Paris", true);
        wrongAnswer = new Answer(2, 1, "Berlin", false);

        Question question = new Question(1, 1, "Capital of France?", 10, List.of(correctAnswer, wrongAnswer));
        Quiz quiz = new Quiz(1, "Geography", "desc", 1, List.of(question));

        sut = new GameSession("123456", 1);
        sut.setQuiz(quiz);
        sut.addPlayer("alice");
        sut.addPlayer("bob");
    }

    @Test
    void shouldExposeCurrentQuestionAfterStart() {
        sut.startQuiz();

        assertThat(sut.getCurrentQuestion())
                .isPresent()
                .get()
                .extracting(Question::getText)
                .isEqualTo("Capital of France?");
    }

    @Test
    void shouldAwardPointsForCorrectAnswer() {
        sut.startQuiz();

        AnswerResult result = sut.submitAnswer("alice", correctAnswer.getId());

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.isCorrect()).isTrue();
        assertThat(result.getPointsAwarded()).isGreaterThan(0);
    }

    @Test
    void shouldAwardNoPointsForWrongAnswer() {
        sut.startQuiz();

        AnswerResult result = sut.submitAnswer("bob", wrongAnswer.getId());

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.isCorrect()).isFalse();
        assertThat(result.getPointsAwarded()).isZero();
    }

    @Test
    void shouldRejectAnswerFromUnknownPlayer() {
        sut.startQuiz();

        AnswerResult result = sut.submitAnswer("ghost", correctAnswer.getId());

        assertThat(result.isAccepted()).isFalse();
    }

    @Test
    void shouldRejectAnswerWhenGameIsNotInQuestionState() {
        AnswerResult result = sut.submitAnswer("alice", correctAnswer.getId());

        assertThat(result.isAccepted()).isFalse();
    }

    @Test
    void shouldRejectUnknownAnswerId() {
        sut.startQuiz();

        AnswerResult result = sut.submitAnswer("alice", 999);

        assertThat(result.isAccepted()).isFalse();
    }

    @Test
    void shouldIncreaseAnsweredCountOnEachSubmission() {
        sut.startQuiz();

        sut.submitAnswer("alice", correctAnswer.getId());
        sut.submitAnswer("bob", wrongAnswer.getId());

        assertThat(sut.getAnsweredCount()).isEqualTo(2);
    }

    @Test
    void shouldResetAnsweredCountWhenNewQuestionBegins() {
        Question q1 = new Question(1, 1, "Q1", 10, List.of(correctAnswer, wrongAnswer));
        Question q2 = new Question(2, 1, "Q2", 10, List.of(correctAnswer, wrongAnswer));
        Quiz quiz = new Quiz(1, "Geography", "desc", 1, List.of(q1, q2));
        sut.setQuiz(quiz);

        sut.startQuiz();
        sut.submitAnswer("alice", correctAnswer.getId());
        sut.endRound();
        sut.nextQuestion();

        assertThat(sut.getAnsweredCount()).isZero();
    }

    @Test
    void shouldMoveToFinishedAfterLastQuestion() {
        sut.startQuiz();
        sut.endRound();
        sut.nextQuestion();

        assertThat(sut.getState()).isEqualTo(GameState.FINISHED);
    }

    @Test
    void leaderboardShouldReflectAccumulatedScoresAfterRound() {
        sut.startQuiz();

        sut.submitAnswer("alice", correctAnswer.getId());
        sut.submitAnswer("bob", wrongAnswer.getId());

        List<Player> leaderboard = sut.getLeaderboard();

        assertThat(leaderboard.get(0).getNickname()).isEqualTo("alice");
        assertThat(leaderboard.get(0).getScore()).isGreaterThan(0);
        assertThat(leaderboard.get(1).getNickname()).isEqualTo("bob");
        assertThat(leaderboard.get(1).getScore()).isZero();
    }

    @Test
    void shouldHandleConcurrentAnswerSubmissionsWithoutLosingPoints() throws InterruptedException {
        Question question = new Question(1, 1, "Q1", 30, List.of(correctAnswer, wrongAnswer));
        Quiz quiz = new Quiz(1, "Geography", "desc", 1, List.of(question));

        GameSession session = new GameSession("654321", 1);
        session.setQuiz(quiz);

        int playerCount = 30;
        for (int i = 0; i < playerCount; i++) {
            session.addPlayer("player_" + i);
        }
        session.startQuiz();

        ExecutorService pool = Executors.newFixedThreadPool(playerCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(playerCount);

        for (int i = 0; i < playerCount; i++) {
            final String nickname = "player_" + i;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    session.submitAnswer(nickname, correctAnswer.getId());
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

        assertThat(session.getAnsweredCount()).isEqualTo(playerCount);
        assertThat(session.getPlayers())
                .allSatisfy(player -> assertThat(player.getScore()).isGreaterThan(0));
    }
}