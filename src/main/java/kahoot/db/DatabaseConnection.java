package kahoot.db;

import java.sql.*;

public class DatabaseConnection {

    private final Connection connection;

    public DatabaseConnection(String dbName) {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        } catch (SQLException e) {
            throw new RuntimeException("Can't create SQLite DB", e);
        }
        init();
    }

    public Connection getConnection() {
        return connection;
    }

    private void init() {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      VARCHAR(50)  NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    role          VARCHAR(20)  NOT NULL
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS quizzes (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    title       VARCHAR(100) NOT NULL,
                    description TEXT,
                    creator_id  INTEGER      NOT NULL,
                    FOREIGN KEY (creator_id) REFERENCES users(id)
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS questions (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    quiz_id    INTEGER NOT NULL,
                    text       TEXT    NOT NULL,
                    time_limit INTEGER NOT NULL DEFAULT 15,
                    FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS answers (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    question_id INTEGER NOT NULL,
                    text        TEXT    NOT NULL,
                    is_correct  INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (question_id) REFERENCES questions(id)
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS game_history (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    quiz_id         INTEGER   NOT NULL,
                    played_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    winner_nickname VARCHAR(50),
                    players_count   INTEGER,
                    FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException("Exception while DB init", e);
        }
    }
}