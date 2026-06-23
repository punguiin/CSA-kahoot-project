package kahoot.game;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameSession {

    private final String pin;
    private final int quizId;

    private volatile GameState state;

    private final CopyOnWriteArrayList<Player> players;

    public GameSession(String pin, int quizId) {
        this.pin = pin;
        this.quizId = quizId;
        this.state = GameState.LOBBY;
        this.players = new CopyOnWriteArrayList<>();
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

    public synchronized void startQuiz() {
        requireState(GameState.LOBBY);
        this.state = GameState.QUESTION;
    }

    public synchronized void endRound() {
        requireState(GameState.QUESTION);
        this.state = GameState.LEADERBOARD;
    }

    public synchronized void nextQuestion(boolean hasMoreQuestions) {
        requireState(GameState.LEADERBOARD);
        this.state = hasMoreQuestions ? GameState.QUESTION : GameState.FINISHED;
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