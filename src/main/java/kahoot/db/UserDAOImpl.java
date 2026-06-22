package kahoot.db;

import kahoot.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Optional;

public class UserDAOImpl implements UserDAO {

    private final Connection connection;

    public UserDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int insert(User user) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users(username, password_hash, role) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, hash(user.getPasswordHash()));
            ps.setString(3, user.getRole());
            int inserted = ps.executeUpdate();
            if (inserted < 1) throw new RuntimeException("Insert failed");
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
            throw new RuntimeException("Insert failed");
        } catch (SQLException e) {
            throw new RuntimeException("Can't insert user: " + user, e);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Can't find user by id: " + id, e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Can't find user by username: " + username, e);
        }
    }

    @Override
    public Optional<User> findByUsernameAndPassword(String username, String password) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM users WHERE username = ? AND password_hash = ?")) {
            ps.setString(1, username);
            ps.setString(2, hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Can't authenticate user: " + username, e);
        }
    }

    @Override
    public int deleteAll() {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM users")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete users", e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role")
        );
    }

    private static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}