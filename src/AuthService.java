import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AuthService {
    private AuthService() {
    }

    static String authenticate(String username, String password) {
        String sql = "SELECT role FROM users WHERE username=? AND password=?";
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet result = statement.executeQuery();
            return result.next() ? result.getString("role") : null;
        } catch (SQLException e) {
            Database.showDatabaseError(e);
            return null;
        }
    }
}
