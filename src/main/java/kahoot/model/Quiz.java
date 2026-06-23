package kahoot.model;

import java.util.List;
import java.util.Objects;

public class Quiz {

    private Integer id;
    private String title;
    private String description;
    private int creatorId;
    private List<Question> questions;

    public Quiz(String title, String description, int creatorId) {
        this(null, title, description, creatorId, List.of());
    }

    public Quiz(Integer id, String title, String description, int creatorId, List<Question> questions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.creatorId = creatorId;
        this.questions = questions;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Quiz quiz = (Quiz) o;
        return creatorId == quiz.creatorId &&
                Objects.equals(id, quiz.id) &&
                Objects.equals(title, quiz.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, creatorId);
    }

    @Override
    public String toString() {
        return "Quiz{id=" + id + ", title='" + title + "', creatorId=" + creatorId + "}";
    }
}
