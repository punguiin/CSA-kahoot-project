package kahoot.db;

import kahoot.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDAO {

    int insert(User user);

    Optional<User> findById(int id);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndPassword(String username, String password);

    List<User> findAll();

    int updateStatus(int id, String status);

    int deleteAll();
}
