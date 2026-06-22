package kahoot;

import kahoot.db.DatabaseConnection;

public class Main {

    public static void main(String[] args) {
        DatabaseConnection db = new DatabaseConnection("kahoot.db");
        System.out.println("DB initialized: " + db.getConnection());
    }
}