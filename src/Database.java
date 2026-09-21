import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    static final String DB_URL = "jdbc:mysql://localhost:3306/healthfirst_pims?useSSL=false&serverTimezone=Africa/Johannesburg";
    static final String DB_USER = "Kyle";
    static final String DB_PASSWORD = "402306600Richield.ac.za";

    private Database() {
    }

    static Connection connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver is missing. Put mysql-connector-j-9.4.0.jar in the lib folder.", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    static void showDatabaseError(SQLException e) {
        JOptionPane.showMessageDialog(null,
            "Database connection failed.\n\n" + e.getMessage() +
            "\n\nCheck MySQL, the database credentials, and the JDBC driver.",
            "MySQL Error", JOptionPane.ERROR_MESSAGE);
    }

    static void styleButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBackground(new Color(0, 102, 204));
        button.setForeground(Color.WHITE);
    }
}
