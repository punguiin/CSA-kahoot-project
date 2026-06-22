package kahoot.db;

import kahoot.model.Answer;
import kahoot.model.Question;
import kahoot.model.Quiz;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuizDAOImpl implements QuizDAO {

    private final Connection connection;

    public QuizDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int insert(Quiz quiz) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO quizzes(title, description, creator_id) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, quiz.getTitle());
            ps.setString(2, quiz.getDescription());
            ps.setInt(3, quiz.getCreatorId());
            int inserted = ps.executeUpdate();
            if (inserted < 1) throw new RuntimeException("Insert failed");
            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) throw new RuntimeException("Insert failed");
            int quizId = keys.getInt(1);
            for (Question q : quiz.getQuestions()) {
                insertQuestion(q, quizId);
            }
            return quizId;
        } catch (SQLException e) {
            throw new RuntimeException("Can't insert quiz: " + quiz, e);
        }
    }

    @Override
    public Optional<Quiz> findById(int id) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM quizzes WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Quiz quiz = mapQuiz(rs);
                    quiz.setQuestions(loadQuestions(id));
                    return Optional.of(quiz);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Can't find quiz by id: " + id, e);
        }
    }

    @Override
    public List<Quiz> findByCreatorId(int creatorId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM quizzes WHERE creator_id = ?")) {
            ps.setInt(1, creatorId);
            List<Quiz> quizzes = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    quizzes.add(mapQuiz(rs));
                }
            }
            return quizzes;
        } catch (SQLException e) {
            throw new RuntimeException("Can't find quizzes by creator: " + creatorId, e);
        }
    }

    @Override
    public int deleteById(int id) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM quizzes WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete quiz: " + id, e);
        }
    }

    @Override
    public int deleteAll() {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quizzes")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete quizzes", e);
        }
    }

    private void insertQuestion(Question question, int quizId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO questions(quiz_id, text, time_limit) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, quizId);
            ps.setString(2, question.getText());
            ps.setInt(3, question.getTimeLimit());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) throw new RuntimeException("Insert question failed");
            int questionId = keys.getInt(1);
            for (Answer a : question.getAnswers()) {
                insertAnswer(a, questionId);
            }
        }
    }

    private void insertAnswer(Answer answer, int questionId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO answers(question_id, text, is_correct) VALUES (?, ?, ?)")) {
            ps.setInt(1, questionId);
            ps.setString(2, answer.getText());
            ps.setInt(3, answer.isCorrect() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    private List<Question> loadQuestions(int quizId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM questions WHERE quiz_id = ?")) {
            ps.setInt(1, quizId);
            List<Question> questions = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int questionId = rs.getInt("id");
                    questions.add(new Question(
                            questionId,
                            quizId,
                            rs.getString("text"),
                            rs.getInt("time_limit"),
                            loadAnswers(questionId)
                    ));
                }
            }
            return questions;
        }
    }

    private List<Answer> loadAnswers(int questionId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM answers WHERE question_id = ?")) {
            ps.setInt(1, questionId);
            List<Answer> answers = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(new Answer(
                            rs.getInt("id"),
                            questionId,
                            rs.getString("text"),
                            rs.getInt("is_correct") == 1
                    ));
                }
            }
            return answers;
        }
    }

    private Quiz mapQuiz(ResultSet rs) throws SQLException {
        return new Quiz(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("creator_id"),
                List.of()
        );
    }
}