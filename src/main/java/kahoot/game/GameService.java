package kahoot.game;

import kahoot.db.GameHistoryDAO;
import kahoot.db.QuizDAO;
import kahoot.model.Question;
import kahoot.model.Quiz;

import java.util.List;
import java.util.Optional;

public class GameService {

    private final GameStateManager gameStateManager;
    private final QuizDAO quizDAO;
    private final GameHistoryDAO gameHistoryDAO;

    public GameService(GameStateManager gameStateManager, QuizDAO quizDAO, GameHistoryDAO gameHistoryDAO) {
        this.gameStateManager = gameStateManager;
        this.quizDAO = quizDAO;
        this.gameHistoryDAO = gameHistoryDAO;
    }

    public GameResult executeAction(GameAction action) {
        return switch (action.getType()) {
            case CREATE_ROOM -> createRoom(action);
            case JOIN_ROOM -> joinRoom(action);
            case START_QUIZ -> startQuiz(action);
            case SUBMIT_ANSWER -> submitAnswer(action);
            case NEXT_QUESTION -> nextQuestion(action);
            case END_ROUND -> endRound(action);
            case GET_LEADERBOARD -> getLeaderboard(action);
        };
    }

    private GameResult createRoom(GameAction action) {
        Optional<Quiz> quiz = quizDAO.findById(action.getQuizId());
        if (quiz.isEmpty()) {
            return GameResult.error("Quiz not found: " + action.getQuizId());
        }

        GameSession session = gameStateManager.createSession(action.getQuizId());
        session.setQuiz(quiz.get());

        return GameResult.roomCreated(session.getPin());
    }

    private GameResult joinRoom(GameAction action) {
        Optional<GameSession> session = gameStateManager.getSession(action.getPin());
        if (session.isEmpty()) {
            return GameResult.error("Room not found: " + action.getPin());
        }

        boolean joined = gameStateManager.joinSession(action.getPin(), action.getNickname());
        if (!joined) {
            return GameResult.error("Could not join room, nickname taken or game already started");
        }

        return GameResult.playerJoined(action.getPin(), session.get().getState());
    }

    private GameResult startQuiz(GameAction action) {
        Optional<GameSession> session = gameStateManager.getSession(action.getPin());
        if (session.isEmpty()) {
            return GameResult.error("Room not found: " + action.getPin());
        }

        try {
            session.get().startQuiz();
        } catch (IllegalStateException e) {
            return GameResult.error(e.getMessage());
        }

        Optional<Question> question = session.get().getCurrentQuestion();
        return GameResult.questionStarted(action.getPin(), session.get().getState(), question.orElse(null));
    }

    private GameResult submitAnswer(GameAction action) {
        Optional<GameSession> session = gameStateManager.getSession(action.getPin());
        if (session.isEmpty()) {
            return GameResult.error("Room not found: " + action.getPin());
        }

        AnswerResult result = session.get().submitAnswer(action.getNickname(), action.getAnswerId());
        return GameResult.answerSubmitted(action.getPin(), result);
    }

    private GameResult nextQuestion(GameAction action) {
        Optional<GameSession> session = gameStateManager.getSession(action.getPin());
        if (session.isEmpty()) {
            return GameResult.error("Room not found: " + action.getPin());
        }

        GameSession gameSession = session.get();
        try {
            gameSession.nextQuestion();
        } catch (IllegalStateException e) {
            return GameResult.error(e.getMessage());
        }

        if (gameSession.getState() == GameState.FINISHED) {
            recordGameHistory(gameSession);
            return GameResult.leaderboard(action.getPin(), gameSession.getState(), gameSession.getLeaderboard());
        }

        return GameResult.questionStarted(action.getPin(), gameSession.getState(),
                gameSession.getCurrentQuestion().orElse(null));
    }

    private GameResult endRound(GameAction action) {
        Optional<GameSession> session = gameStateManager.getSession(action.getPin());
        if (session.isEmpty()) {
            return GameResult.error("Room not found: " + action.getPin());
        }

        try {
            session.get().endRound();
        } catch (IllegalStateException e) {
            return GameResult.error(e.getMessage());
        }

        return GameResult.leaderboard(action.getPin(), session.get().getState(), session.get().getLeaderboard());
    }

    private GameResult getLeaderboard(GameAction action) {
        Optional<GameSession> session = gameStateManager.getSession(action.getPin());
        if (session.isEmpty()) {
            return GameResult.error("Room not found: " + action.getPin());
        }

        return GameResult.leaderboard(action.getPin(), session.get().getState(), session.get().getLeaderboard());
    }

    private void recordGameHistory(GameSession session) {
        List<Player> leaderboard = session.getLeaderboard();
        String winner = leaderboard.isEmpty() ? null : leaderboard.get(0).getNickname();
        gameHistoryDAO.insert(session.getQuizId(), winner, session.getPlayerCount());
    }
}