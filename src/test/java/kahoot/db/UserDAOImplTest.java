package kahoot.db;

import kahoot.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDAOImplTest {

    private UserDAO sut;

    @BeforeEach
    void setUp() {
        DatabaseConnection db = new DatabaseConnection(":memory:");
        sut = new UserDAOImpl(db.getConnection());
    }

    @Test
    void shouldInsertAndFindById() {
        int id = sut.insert(new User("nikita", "password123", "PLAYER"));

        assertThat(sut.findById(id))
                .isPresent()
                .get()
                .returns("nikita", User::getUsername)
                .returns("PLAYER", User::getRole);
    }

    @Test
    void shouldFindByUsername() {
        sut.insert(new User("nikita", "password123", "PLAYER"));

        assertThat(sut.findByUsername("nikita")).isPresent();
        assertThat(sut.findByUsername("unknown")).isEmpty();
    }

    @Test
    void shouldAuthenticateWithCorrectPassword() {
        sut.insert(new User("nikita", "password123", "PLAYER"));

        assertThat(sut.findByUsernameAndPassword("nikita", "password123")).isPresent();
    }

    @Test
    void shouldNotAuthenticateWithWrongPassword() {
        sut.insert(new User("nikita", "password123", "PLAYER"));

        assertThat(sut.findByUsernameAndPassword("nikita", "wrongpass")).isEmpty();
    }

    @Test
    void shouldDeleteAll() {
        sut.insert(new User("user1", "pass1", "PLAYER"));
        sut.insert(new User("user2", "pass2", "ADMIN"));

        sut.deleteAll();

        assertThat(sut.findByUsername("user1")).isEmpty();
        assertThat(sut.findByUsername("user2")).isEmpty();
    }
}