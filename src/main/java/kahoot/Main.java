package kahoot;

import kahoot.db.DatabaseConnection;
import kahoot.db.GameHistoryDAO;
import kahoot.db.GameHistoryDAOImpl;
import kahoot.db.QuizDAO;
import kahoot.db.QuizDAOImpl;
import kahoot.db.UserDAO;
import kahoot.db.UserDAOImpl;
import kahoot.game.GameService;
import kahoot.game.GameStateManager;
import kahoot.net.ConnectionRegistry;
import kahoot.net.KahootServer;
import kahoot.net.SessionDispatcher;
import kahoot.protocol.IdentityCipher;
import kahoot.protocol.MessageCipher;
import kahoot.web.HttpApiServer;
import kahoot.web.WebSocketServer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final long REAP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long FINISHED_GRACE_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long IDLE_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1);

    public static void main(String[] args) throws Exception {
        int tcpPort = port("TCP_PORT", 9090);
        int wsPort = port("WS_PORT", 9092);
        int httpPort = port("HTTP_PORT", 8090);

        DatabaseConnection db = new DatabaseConnection("kahoot.db");
        QuizDAO quizDao = new QuizDAOImpl(db.getConnection());
        GameHistoryDAO historyDao = new GameHistoryDAOImpl(db.getConnection());
        UserDAO userDao = new UserDAOImpl(db.getConnection());
        DemoData.seedIfEmpty(quizDao, userDao);

        GameStateManager gameStateManager = new GameStateManager();
        GameService gameService = new GameService(gameStateManager, quizDao, historyDao);
        MessageCipher cipher = new IdentityCipher();

        ConnectionRegistry tcpRegistry = new ConnectionRegistry();
        SessionDispatcher tcpDispatcher = new SessionDispatcher(gameService, gameStateManager, tcpRegistry);
        KahootServer tcpServer = new KahootServer(tcpPort, cipher, tcpRegistry, tcpDispatcher);

        ConnectionRegistry wsRegistry = new ConnectionRegistry();
        SessionDispatcher wsDispatcher = new SessionDispatcher(gameService, gameStateManager, wsRegistry,
                id -> userDao.findById(id).map(u -> kahoot.model.User.BLOCKED.equals(u.getStatus())).orElse(false));
        WebSocketServer wsServer = new WebSocketServer(
                wsPort, cipher, wsRegistry::register, wsDispatcher::onPacket, wsDispatcher::onDisconnect);

        HttpApiServer httpServer = new HttpApiServer(
                httpPort, quizDao, userDao, historyDao, gameStateManager, wsDispatcher::endRoom);

        ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-reaper");
            t.setDaemon(true);
            return t;
        });
        reaper.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                for (String pin : gameStateManager.reapStale(now, FINISHED_GRACE_MS, IDLE_TIMEOUT_MS)) {
                    wsDispatcher.notifyClosed(pin);
                }
            } catch (RuntimeException e) {
                System.err.println("session-reaper: " + e.getMessage());
            }
        }, REAP_INTERVAL_MS, REAP_INTERVAL_MS, TimeUnit.MILLISECONDS);

        tcpServer.start();
        wsServer.start();
        httpServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Main: shutting down");
            reaper.shutdownNow();
            tcpServer.stop();
            wsServer.stop();
            httpServer.stop();
        }));
        System.out.println("Main: Kahoot up — TCP/" + tcpServer.port() + " (custom client), WS/"
                + wsServer.port() + " (browser), HTTP/" + httpServer.port() + " (api)");
        wsServer.join();
    }

    private static int port(String envVar, int fallback) {
        String value = System.getenv(envVar);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }
}
