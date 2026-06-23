package kahoot.model;

import java.util.Objects;

public class User {

    public static final String ACTIVE = "ACTIVE";
    public static final String BLOCKED = "BLOCKED";

    private Integer id;
    private String username;
    private String passwordHash;
    private String role;
    private String status;

    public User(String username, String passwordHash, String role) {
        this(null, username, passwordHash, role, ACTIVE);
    }

    public User(Integer id, String username, String passwordHash, String role) {
        this(id, username, passwordHash, role, ACTIVE);
    }

    public User(Integer id, String username, String passwordHash, String role, String status) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status == null ? ACTIVE : status;
    }

    public String getStatus() {
        return status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(username, user.username) &&
                Objects.equals(role, user.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, role);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }
}
