package kahoot.db;

import java.sql.*;

public class GameHistoryDAOImpl implements GameHistoryDAO {

    private final Connection connection;

    public GameHistoryDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int insert(int quizId, String winnerNickname, int playersCount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO game_history(quiz_id, winner_nickname, players_count) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, quizId);
            ps.setString(2, winnerNickname);
            ps.setInt(3, playersCount);
            int inserted = ps.executeUpdate();
            if (inserted < 1) throw new RuntimeException("Insert failed");
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
            throw new RuntimeException("Insert failed");
        } catch (SQLException e) {
            throw new RuntimeException("Can't insert game history for quiz: " + quizId, e);
        }
    }

    @Override
    public int countByQuizId(int quizId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM game_history WHERE quiz_id = ?")) {
            ps.setInt(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Can't count game history for quiz: " + quizId, e);
        }
    }

    @Override
    public int deleteAll() {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM game_history")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete game history", e);
        }
    }
}