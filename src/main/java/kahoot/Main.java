package kahoot;

import kahoot.db.DatabaseConnection;
import kahoot.db.GameHistoryDAO;
import kahoot.db.GameHistoryDAOImpl;
import kahoot.db.QuizDAO;
import kahoot.db.QuizDAOImpl;
import kahoot.game.GameService;
import kahoot.game.GameStateManager;
import kahoot.net.ConnectionRegistry;
import kahoot.net.KahootServer;
import kahoot.net.SessionDispatcher;
import kahoot.protocol.IdentityCipher;
import kahoot.protocol.MessageCipher;

public class Main {

    public static void main(String[] args) throws Exception {
        int tcpPort = port("TCP_PORT", 9090);

        DatabaseConnection db = new DatabaseConnection("kahoot.db");
        QuizDAO quizDao = new QuizDAOImpl(db.getConnection());
        GameHistoryDAO historyDao = new GameHistoryDAOImpl(db.getConnection());

        GameStateManager gameStateManager = new GameStateManager();
        GameService gameService = new GameService(gameStateManager, quizDao, historyDao);

        ConnectionRegistry registry = new ConnectionRegistry();
        SessionDispatcher dispatcher = new SessionDispatcher(gameService, gameStateManager, registry);
        MessageCipher cipher = new IdentityCipher();
        KahootServer server = new KahootServer(tcpPort, cipher, registry, dispatcher);

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Main: shutting down");
            server.stop();
        }));
        System.out.println("Main: Kahoot TCP server up on port " + server.port());
        server.join();
    }

    private static int port(String envVar, int fallback) {
        String value = System.getenv(envVar);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }
}
