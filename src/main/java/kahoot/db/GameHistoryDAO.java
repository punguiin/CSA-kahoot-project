package kahoot.db;

public interface GameHistoryDAO {

    int insert(int quizId, String winnerNickname, int playersCount);

    int countByQuizId(int quizId);

    int deleteAll();
}