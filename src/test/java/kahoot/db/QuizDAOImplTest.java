package kahoot.db;

import kahoot.model.Answer;
import kahoot.model.Question;
import kahoot.model.Quiz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizDAOImplTest {

    private QuizDAO sut;

    @BeforeEach
    void setUp() {
        DatabaseConnection db = new DatabaseConnection(":memory:");
        sut = new QuizDAOImpl(db.getConnection());
    }

    private Quiz buildQuiz(int creatorId) {
        Question q = new Question(0, "Який протокол безпечний?", 15);
        q.setAnswers(List.of(
                new Answer(0, "HTTPS", true),
                new Answer(0, "HTTP", false),
                new Answer(0, "FTP", false),
                new Answer(0, "TCP", false)
        ));
        Quiz quiz = new Quiz("Мережі", "Тест з мереж", creatorId);
        quiz.setQuestions(List.of(q));
        return quiz;
    }

    @Test
    void shouldInsertAndFindById() {
        int id = sut.insert(buildQuiz(1));

        assertThat(sut.findById(id))
                .isPresent()
                .get()
                .returns("Мережі", Quiz::getTitle)
                .returns(1, Quiz::getCreatorId);
    }

    @Test
    void shouldLoadQuestionsAndAnswersOnFindById() {
        int id = sut.insert(buildQuiz(1));

        Quiz quiz = sut.findById(id).get();

        assertThat(quiz.getQuestions()).hasSize(1);
        assertThat(quiz.getQuestions().get(0).getAnswers()).hasSize(4);
        assertThat(quiz.getQuestions().get(0).getAnswers())
                .filteredOn(Answer::isCorrect)
                .hasSize(1);
    }

    @Test
    void shouldFindByCreatorId() {
        sut.insert(buildQuiz(1));
        sut.insert(buildQuiz(1));
        sut.insert(buildQuiz(2));

        assertThat(sut.findByCreatorId(1)).hasSize(2);
        assertThat(sut.findByCreatorId(2)).hasSize(1);
        assertThat(sut.findByCreatorId(99)).isEmpty();
    }

    @Test
    void shouldReturnEmptyForUnknownId() {
        assertThat(sut.findById(999)).isEmpty();
    }

    @Test
    void shouldDeleteById() {
        int id = sut.insert(buildQuiz(1));

        sut.deleteById(id);

        assertThat(sut.findById(id)).isEmpty();
    }
}
