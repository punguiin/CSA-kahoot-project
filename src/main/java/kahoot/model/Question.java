package kahoot.model;

import java.util.List;
import java.util.Objects;

public class Question {

    private Integer id;
    private int quizId;
    private String text;
    private int timeLimit;
    private List<Answer> answers;

    public Question(int quizId, String text, int timeLimit) {
        this(null, quizId, text, timeLimit, List.of());
    }

    public Question(Integer id, int quizId, String text, int timeLimit, List<Answer> answers) {
        this.id = id;
        this.quizId = quizId;
        this.text = text;
        this.timeLimit = timeLimit;
        this.answers = answers;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getQuizId() {
        return quizId;
    }

    public String getText() {
        return text;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return quizId == question.quizId &&
                Objects.equals(id, question.id) &&
                Objects.equals(text, question.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, quizId, text);
    }

    @Override
    public String toString() {
        return "Question{id=" + id + ", quizId=" + quizId + ", text='" + text + "'}";
    }
}
