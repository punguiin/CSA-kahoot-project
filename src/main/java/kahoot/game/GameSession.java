package kahoot.game;

import kahoot.model.Answer;
import kahoot.model.Question;
import kahoot.model.Quiz;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GameSession {

    private final String pin;
    private final int quizId;

    private volatile GameState state;
    private volatile Quiz quiz;
    private volatile int currentQuestionIndex;
    private volatile long questionStartedAt;

    private final CopyOnWriteArrayList<Player> players;
    private final AtomicInteger answeredCount = new AtomicInteger(0);

    public GameSession(String pin, int quizId) {
        this.pin = pin;
        this.quizId = quizId;
        this.state = GameState.LOBBY;
        this.players = new CopyOnWriteArrayList<>();
        this.currentQuestionIndex = -1;
    }

    public synchronized boolean addPlayer(String nickname) {
        if (state != GameState.LOBBY) {
            return false;
        }
        boolean taken = players.stream()
                .anyMatch(p -> p.getNickname().equalsIgnoreCase(nickname));
        if (taken) {
            return false;
        }
        players.add(new Player(nickname));
        return true;
    }

    public List<Player> getLeaderboard() {
        return players.stream()
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .toList();
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public synchronized void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public synchronized void startQuiz() {
        requireState(GameState.LOBBY);
        this.currentQuestionIndex = 0;
        beginQuestion();
    }

    public synchronized void endRound() {
        requireState(GameState.QUESTION);
        this.state = GameState.LEADERBOARD;
    }

    public synchronized void nextQuestion(boolean hasMoreQuestions) {
        requireState(GameState.LEADERBOARD);
        if (hasMoreQuestions) {
            this.currentQuestionIndex++;
            beginQuestion();
        } else {
            this.state = GameState.FINISHED;
        }
    }

    public synchronized void nextQuestion() {
        requireState(GameState.LEADERBOARD);
        if (hasMoreQuestions()) {
            this.currentQuestionIndex++;
            beginQuestion();
        } else {
            this.state = GameState.FINISHED;
        }
    }

    private void beginQuestion() {
        this.state = GameState.QUESTION;
        this.questionStartedAt = System.currentTimeMillis();
        this.answeredCount.set(0);
    }

    private boolean hasMoreQuestions() {
        return quiz != null && currentQuestionIndex < quiz.getQuestions().size() - 1;
    }

    public Optional<Question> getCurrentQuestion() {
        if (quiz == null || currentQuestionIndex < 0 || currentQuestionIndex >= quiz.getQuestions().size()) {
            return Optional.empty();
        }
        return Optional.of(quiz.getQuestions().get(currentQuestionIndex));
    }

    public AnswerResult submitAnswer(String playerNickname, int answerId) {
        if (state != GameState.QUESTION) {
            return AnswerResult.rejected("Game is not accepting answers right now");
        }

        Optional<Question> currentQuestion = getCurrentQuestion();
        if (currentQuestion.isEmpty()) {
            return AnswerResult.rejected("No active question");
        }

        Optional<Player> player = findPlayer(playerNickname);
        if (player.isEmpty()) {
            return AnswerResult.rejected("Unknown player: " + playerNickname);
        }

        Optional<Answer> selected = currentQuestion.get().getAnswers().stream()
                .filter(a -> a.getId() != null && a.getId() == answerId)
                .findFirst();
        if (selected.isEmpty()) {
            return AnswerResult.rejected("Unknown answer id: " + answerId);
        }

        long answeredAt = System.currentTimeMillis();
        answeredCount.incrementAndGet();

        boolean correct = selected.get().isCorrect();
        int points = ScoringEngine.calculatePoints(
                correct,
                questionStartedAt,
                answeredAt,
                currentQuestion.get().getTimeLimit()
        );

        if (points > 0) {
            player.get().addScore(points);
        }

        return AnswerResult.accepted(correct, points);
    }

    public int getAnsweredCount() {
        return answeredCount.get();
    }

    public synchronized void startSelfPaced() {
        requireState(GameState.LOBBY);
        this.currentQuestionIndex = 0;
        this.state = GameState.QUESTION;
        long now = System.currentTimeMillis();
        for (Player p : players) {
            p.setProgressIndex(0);
            p.setQuestionStartedAt(now);
        }
    }

    public int questionCount() {
        return quiz == null ? 0 : quiz.getQuestions().size();
    }

    public Optional<Question> questionAt(int index) {
        if (quiz == null || index < 0 || index >= quiz.getQuestions().size()) {
            return Optional.empty();
        }
        return Optional.of(quiz.getQuestions().get(index));
    }

    public synchronized AnswerResult submitSelfPaced(String playerNickname, int answerId) {
        if (state != GameState.QUESTION) {
            return AnswerResult.rejected("Game is not running");
        }
        Optional<Player> player = findPlayer(playerNickname);
        if (player.isEmpty()) {
            return AnswerResult.rejected("Unknown player: " + playerNickname);
        }
        Player p = player.get();
        Optional<Question> question = questionAt(p.getProgressIndex());
        if (question.isEmpty()) {
            return AnswerResult.rejected("You have finished the quiz");
        }
        Optional<Answer> selected = question.get().getAnswers().stream()
                .filter(a -> a.getId() != null && a.getId() == answerId)
                .findFirst();
        if (selected.isEmpty()) {
            return AnswerResult.rejected("Unknown answer id: " + answerId);
        }

        long answeredAt = System.currentTimeMillis();
        boolean correct = selected.get().isCorrect();
        int points = ScoringEngine.calculatePoints(
                correct, p.getQuestionStartedAt(), answeredAt, question.get().getTimeLimit());
        if (points > 0) {
            p.addScore(points);
        }

        p.setProgressIndex(p.getProgressIndex() + 1);
        p.setQuestionStartedAt(answeredAt);
        return AnswerResult.accepted(correct, points);
    }

    public Optional<Question> currentQuestionForPlayer(String nickname) {
        return findPlayer(nickname).flatMap(p -> questionAt(p.getProgressIndex()));
    }

    public int progressOf(String nickname) {
        return findPlayer(nickname).map(Player::getProgressIndex).orElse(0);
    }

    public boolean isPlayerDone(String nickname) {
        return findPlayer(nickname).map(p -> p.getProgressIndex() >= questionCount()).orElse(false);
    }

    public synchronized boolean markFinishedOnce() {
        if (state == GameState.FINISHED || players.isEmpty()) {
            return false;
        }
        boolean allDone = players.stream().allMatch(p -> p.getProgressIndex() >= questionCount());
        if (!allDone) {
            return false;
        }
        this.state = GameState.FINISHED;
        return true;
    }

    private Optional<Player> findPlayer(String nickname) {
        return players.stream()
                .filter(p -> p.getNickname().equalsIgnoreCase(nickname))
                .findFirst();
    }

    public String getPin() {
        return pin;
    }

    public int getQuizId() {
        return quizId;
    }

    public GameState getState() {
        return state;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    private void requireState(GameState expected) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Expected state " + expected + " but was " + state);
        }
    }

    @Override
    public String toString() {
        return "GameSession{pin='" + pin + "', quizId=" + quizId +
                ", state=" + state + ", players=" + players.size() + "}";
    }
}
