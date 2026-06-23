package kahoot.game;

import java.util.Objects;

public class Player {

    private final String nickname;
    private volatile int score;

    public Player(String nickname) {
        this.nickname = Objects.requireNonNull(nickname, "nickname must not be null");
        this.score = 0;
    }

    public String getNickname() {
        return nickname;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        if (points > 0) {
            this.score += points;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return nickname.equalsIgnoreCase(player.nickname);
    }

    @Override
    public int hashCode() {
        return nickname.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return "Player{nickname='" + nickname + "', score=" + score + "}";
    }
}