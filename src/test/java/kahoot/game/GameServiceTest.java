package kahoot.game;

import kahoot.db.DatabaseConnection;
import kahoot.db.GameHistoryDAO;
import kahoot.db.GameHistoryDAOImpl;
import kahoot.db.QuizDAO;
import kahoot.db.QuizDAOImpl;
import kahoot.model.Answer;
import kahoot.model.Question;
import kahoot.model.Quiz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceTest {

    private GameService sut;
    private QuizDAO quizDAO;
    private GameHistoryDAO gameHistoryDAO;
    private int quizId;

    @BeforeEach
    void setUp() {
        Connection connection = new DatabaseConnection(":memory:").getConnection();
        quizDAO = new QuizDAOImpl(connection);
        gameHistoryDAO = new GameHistoryDAOImpl(connection);

        sut = new GameService(new GameStateManager(), quizDAO, gameHistoryDAO);

        quizId = quizDAO.insert(buildQuiz());
    }

    private Quiz buildQuiz() {
        Question question = new Question(0, "Capital of France?", 10);
        question.setAnswers(List.of(
                new Answer(0, "Paris", true),
                new Answer(0, "Berlin", false),
                new Answer(0, "Madrid", false)
        ));
        Quiz quiz = new Quiz("Geography", "Quick geography quiz", 1);
        quiz.setQuestions(List.of(question));
        return quiz;
    }

    @Test
    void shouldCreateRoomForExistingQuiz() {
        GameResult result = sut.executeAction(GameAction.createRoom(quizId));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPin()).hasSize(6);
    }

    @Test
    void shouldFailToCreateRoomForUnknownQuiz() {
        GameResult result = sut.executeAction(GameAction.createRoom(999_999));

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void shouldAllowPlayerToJoinCreatedRoom() {
        String pin = sut.executeAction(GameAction.createRoom(quizId)).getPin();

        GameResult result = sut.executeAction(GameAction.joinRoom(pin, "alice"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getState()).isEqualTo(GameState.LOBBY);
    }

    @Test
    void shouldRejectJoinForUnknownRoom() {
        GameResult result = sut.executeAction(GameAction.joinRoom("000000", "ghost"));

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void shouldStartQuizAndExposeFirstQuestion() {
        String pin = sut.executeAction(GameAction.createRoom(quizId)).getPin();
        sut.executeAction(GameAction.joinRoom(pin, "alice"));

        GameResult result = sut.executeAction(GameAction.startQuiz(pin));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getState()).isEqualTo(GameState.QUESTION);
        assertThat(result.getCurrentQuestion().getText()).isEqualTo("Capital of France?");
    }

    @Test
    void shouldProcessSubmittedAnswer() {
        String pin = sut.executeAction(GameAction.createRoom(quizId)).getPin();
        sut.executeAction(GameAction.joinRoom(pin, "alice"));
        sut.executeAction(GameAction.startQuiz(pin));

        int correctAnswerId = quizDAO.findById(quizId).get()
                .getQuestions().get(0).getAnswers().stream()
                .filter(Answer::isCorrect)
                .findFirst().get().getId();

        GameResult result = sut.executeAction(GameAction.submitAnswer(pin, "alice", correctAnswerId));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswerResult().isCorrect()).isTrue();
        assertThat(result.getAnswerResult().getPointsAwarded()).isGreaterThan(0);
    }

    @Test
    void shouldRecordGameHistoryWhenQuizFinishes() {
        String pin = sut.executeAction(GameAction.createRoom(quizId)).getPin();
        sut.executeAction(GameAction.joinRoom(pin, "alice"));
        sut.executeAction(GameAction.startQuiz(pin));

        sut.executeAction(GameAction.endRound(pin));
        GameResult result = sut.executeAction(GameAction.nextQuestion(pin));

        assertThat(result.getState()).isEqualTo(GameState.FINISHED);
        assertThat(gameHistoryDAO.countByQuizId(quizId)).isEqualTo(1);
    }

    @Test
    void shouldSimulateFullGameWithThreeConcurrentPlayers() throws InterruptedException {
        String pin = sut.executeAction(GameAction.createRoom(quizId)).getPin();

        sut.executeAction(GameAction.joinRoom(pin, "alice"));
        sut.executeAction(GameAction.joinRoom(pin, "bob"));
        sut.executeAction(GameAction.joinRoom(pin, "carol"));

        sut.executeAction(GameAction.startQuiz(pin));

        int correctAnswerId = quizDAO.findById(quizId).get()
                .getQuestions().get(0).getAnswers().stream()
                .filter(Answer::isCorrect)
                .findFirst().get().getId();
        int wrongAnswerId = quizDAO.findById(quizId).get()
                .getQuestions().get(0).getAnswers().stream()
                .filter(a -> !a.isCorrect())
                .findFirst().get().getId();

        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);

        pool.submit(() -> runPlayerAnswer(pin, "alice", correctAnswerId, startLatch, doneLatch));
        pool.submit(() -> runPlayerAnswer(pin, "bob", correctAnswerId, startLatch, doneLatch));
        pool.submit(() -> runPlayerAnswer(pin, "carol", wrongAnswerId, startLatch, doneLatch));

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        GameResult endRoundResult = sut.executeAction(GameAction.endRound(pin));
        assertThat(endRoundResult.getState()).isEqualTo(GameState.LEADERBOARD);

        List<Player> leaderboard = endRoundResult.getLeaderboard();
        assertThat(leaderboard).hasSize(3);
        assertThat(leaderboard)
                .filteredOn(p -> p.getNickname().equals("carol"))
                .first()
                .extracting(Player::getScore)
                .isEqualTo(0);
        assertThat(leaderboard)
                .filteredOn(p -> p.getNickname().equals("alice") || p.getNickname().equals("bob"))
                .allSatisfy(p -> assertThat(p.getScore()).isGreaterThan(0));

        GameResult finishResult = sut.executeAction(GameAction.nextQuestion(pin));
        assertThat(finishResult.getState()).isEqualTo(GameState.FINISHED);

        assertThat(gameHistoryDAO.countByQuizId(quizId)).isEqualTo(1);
    }

    private void runPlayerAnswer(String pin, String nickname, int answerId,
                                 CountDownLatch startLatch, CountDownLatch doneLatch) {
        try {
            startLatch.await();
            sut.executeAction(GameAction.submitAnswer(pin, nickname, answerId));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            doneLatch.countDown();
        }
    }

    @Test
    void selfPacedPlayerAdvancesQuestionByQuestionThenFinishes() {
        int twoQuizId = quizDAO.insert(twoQuestionQuiz());
        String pin = sut.executeAction(GameAction.createRoom(twoQuizId)).getPin();
        sut.executeAction(GameAction.joinRoom(pin, "alice"));
        sut.startGame(pin);

        Quiz quiz = quizDAO.findById(twoQuizId).get();
        int firstCorrect = correctAnswerId(quiz, 0);
        int secondCorrect = correctAnswerId(quiz, 1);

        GameResult afterFirst = sut.submitAndAdvance(pin, "alice", firstCorrect);
        assertThat(afterFirst.getState()).isEqualTo(GameState.QUESTION);
        assertThat(afterFirst.getAnswerResult().isCorrect()).isTrue();
        assertThat(afterFirst.getCurrentQuestion()).isNotNull();

        GameResult afterSecond = sut.submitAndAdvance(pin, "alice", secondCorrect);
        assertThat(afterSecond.getState()).isEqualTo(GameState.FINISHED);
        assertThat(afterSecond.getCurrentQuestion()).isNull();
        assertThat(afterSecond.getLeaderboard()).isNotEmpty();
        assertThat(gameHistoryDAO.countByQuizId(twoQuizId)).isEqualTo(1);
    }

    private int correctAnswerId(Quiz quiz, int questionIndex) {
        return quiz.getQuestions().get(questionIndex).getAnswers().stream()
                .filter(Answer::isCorrect).findFirst().get().getId();
    }

    private Quiz twoQuestionQuiz() {
        Question q1 = new Question(0, "1 + 1 = ?", 10);
        q1.setAnswers(List.of(new Answer(0, "2", true), new Answer(0, "3", false)));
        Question q2 = new Question(0, "2 + 2 = ?", 10);
        q2.setAnswers(List.of(new Answer(0, "4", true), new Answer(0, "5", false)));
        Quiz quiz = new Quiz("Math", "Two questions", 1);
        quiz.setQuestions(List.of(q1, q2));
        return quiz;
    }
}
