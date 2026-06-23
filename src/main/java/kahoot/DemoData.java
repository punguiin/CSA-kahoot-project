package kahoot;

import kahoot.db.QuizDAO;
import kahoot.db.UserDAO;
import kahoot.model.Answer;
import kahoot.model.Question;
import kahoot.model.Quiz;
import kahoot.model.User;

import java.util.List;

public final class DemoData {

    private DemoData() {
    }

    public static void seedIfEmpty(QuizDAO quizDAO, UserDAO userDAO) {
        if (userDAO.findByUsername("admin").isEmpty()) {
            userDAO.insert(new User("admin", "admin", "ADMIN"));
            userDAO.insert(new User("player", "player", "PLAYER"));
            System.out.println("DemoData: seeded users (admin/admin, player/player)");
        }
        if (quizDAO.findById(1).isEmpty()) {
            for (Quiz quiz : demoQuizzes()) {
                quizDAO.insert(quiz);
            }
            System.out.println("DemoData: seeded demo quizzes");
        }
    }

    private static List<Quiz> demoQuizzes() {
        Quiz networks = new Quiz("Мережеві протоколи", "Основи комп'ютерних мереж", 1);
        networks.setQuestions(List.of(
                question("Який протокол використовується для безпечного передавання гіпертексту?", 15,
                        answer("HTTPS", true), answer("HTTP", false), answer("FTP", false), answer("SMTP", false)),
                question("Скільки біт у IPv4-адресі?", 15,
                        answer("32", true), answer("64", false), answer("128", false), answer("16", false)),
                question("Який протокол транспортного рівня є з'єднання-орієнтованим?", 15,
                        answer("TCP", true), answer("UDP", false), answer("IP", false), answer("ICMP", false))));

        Quiz programming = new Quiz("Основи програмування", "Java та загальні поняття", 1);
        programming.setQuestions(List.of(
                question("Що з переліченого НЕ є мовою програмування?", 10,
                        answer("HTML", true), answer("Java", false), answer("Python", false), answer("C++", false)),
                question("Яка структура даних працює за принципом LIFO?", 10,
                        answer("Стек", true), answer("Черга", false), answer("Список", false), answer("Дерево", false))));

        Quiz databases = new Quiz("Бази даних", "SQL та реляційна модель", 1);
        databases.setQuestions(List.of(
                question("Яка команда SQL вибирає дані з таблиці?", 20,
                        answer("SELECT", true), answer("INSERT", false), answer("UPDATE", false), answer("DELETE", false)),
                question("Що гарантує первинний ключ (PRIMARY KEY)?", 20,
                        answer("Унікальність рядка", true), answer("Швидкий інтернет", false),
                        answer("Шифрування", false), answer("Резервну копію", false))));

        return List.of(networks, programming, databases);
    }

    private static Question question(String text, int timeLimit, Answer... answers) {
        Question q = new Question(0, text, timeLimit);
        q.setAnswers(List.of(answers));
        return q;
    }

    private static Answer answer(String text, boolean correct) {
        return new Answer(0, text, correct);
    }
}
