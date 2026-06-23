package kahoot.db;

import kahoot.model.Quiz;

import java.util.List;
import java.util.Optional;

public interface QuizDAO {

    int insert(Quiz quiz);

    void update(Quiz quiz);

    List<Quiz> findAll();

    Optional<Quiz> findById(int id);

    List<Quiz> findByCreatorId(int creatorId);

    int deleteById(int id);

    int deleteAll();
}
