package kahoot.db;

import java.util.List;

public interface GameHistoryDAO {

    record Entry(int quizId, String quizTitle, String playedAt, String winnerNickname, int playersCount) {
    }

    int insert(int quizId, String winnerNickname, int playersCount);

    int countByQuizId(int quizId);

    List<Entry> findRecent();

    int deleteAll();
}
