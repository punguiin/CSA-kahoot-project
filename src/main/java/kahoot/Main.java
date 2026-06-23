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
import kahoot.web.WebSocketServer;

public class Main {

    public static void main(String[] args) throws Exception {
        int tcpPort = port("TCP_PORT", 9090);
        int wsPort = port("WS_PORT", 9092);

        DatabaseConnection db = new DatabaseConnection("kahoot.db");
        QuizDAO quizDao = new QuizDAOImpl(db.getConnection());
        GameHistoryDAO historyDao = new GameHistoryDAOImpl(db.getConnection());
        DemoData.seedIfEmpty(quizDao);

        GameStateManager gameStateManager = new GameStateManager();
        GameService gameService = new GameService(gameStateManager, quizDao, historyDao);
        MessageCipher cipher = new IdentityCipher();

        ConnectionRegistry tcpRegistry = new ConnectionRegistry();
        SessionDispatcher tcpDispatcher = new SessionDispatcher(gameService, gameStateManager, tcpRegistry);
        KahootServer tcpServer = new KahootServer(tcpPort, cipher, tcpRegistry, tcpDispatcher);

        ConnectionRegistry wsRegistry = new ConnectionRegistry();
        SessionDispatcher wsDispatcher = new SessionDispatcher(gameService, gameStateManager, wsRegistry);
        WebSocketServer wsServer = new WebSocketServer(
                wsPort, cipher, wsRegistry::register, wsDispatcher::onPacket, wsDispatcher::onDisconnect);

        tcpServer.start();
        wsServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Main: shutting down");
            tcpServer.stop();
            wsServer.stop();
        }));
        System.out.println("Main: Kahoot up — TCP/" + tcpServer.port() + " (custom client), WS/" + wsServer.port() + " (browser)");
        wsServer.join();
    }

    private static int port(String envVar, int fallback) {
        String value = System.getenv(envVar);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }
}
