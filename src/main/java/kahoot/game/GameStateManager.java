package kahoot.game;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GameStateManager {

    private final ConcurrentHashMap<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public GameSession createSession(int quizId) {
        while (true) {
            String pin = generatePin();
            GameSession session = new GameSession(pin, quizId);
            if (sessions.putIfAbsent(pin, session) == null) {
                return session;
            }
        }
    }

    public Optional<GameSession> getSession(String pin) {
        return Optional.ofNullable(sessions.get(pin));
    }

    public java.util.List<GameSession> all() {
        return new java.util.ArrayList<>(sessions.values());
    }

    public boolean joinSession(String pin, String nickname) {
        GameSession session = sessions.get(pin);
        if (session == null) {
            return false;
        }
        return session.addPlayer(nickname);
    }

    public boolean removeSession(String pin) {
        return sessions.remove(pin) != null;
    }

    public int activeSessionCount() {
        return sessions.size();
    }

    public boolean sessionExists(String pin) {
        return sessions.containsKey(pin);
    }

    private String generatePin() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
