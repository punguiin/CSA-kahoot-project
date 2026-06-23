package kahoot.model;

import java.util.Objects;

public class Answer {

    private Integer id;
    private int questionId;
    private String text;
    private boolean isCorrect;

    public Answer(int questionId, String text, boolean isCorrect) {
        this(null, questionId, text, isCorrect);
    }

    public Answer(Integer id, int questionId, String text, boolean isCorrect) {
        this.id = id;
        this.questionId = questionId;
        this.text = text;
        this.isCorrect = isCorrect;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Answer answer = (Answer) o;
        return questionId == answer.questionId &&
                isCorrect == answer.isCorrect &&
                Objects.equals(id, answer.id) &&
                Objects.equals(text, answer.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, questionId, text, isCorrect);
    }

    @Override
    public String toString() {
        return "Answer{id=" + id + ", questionId=" + questionId +
                ", text='" + text + "', isCorrect=" + isCorrect + "}";
    }
}
