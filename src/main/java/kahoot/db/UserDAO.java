package kahoot.db;

import kahoot.model.User;

import java.util.Optional;

public interface UserDAO {

    int insert(User user);

    Optional<User> findById(int id);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndPassword(String username, String password);

    int deleteAll();
}