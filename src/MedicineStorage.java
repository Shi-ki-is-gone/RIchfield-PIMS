import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class MedicineStorage {
    private MedicineStorage() {
    }

    static List<Object[]> medicines() {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT medicine_id,name,company,medicine_type,price,quantity_in_stock,reorder_level,expiry_date FROM medicines ORDER BY name";
        try (Connection connection = Database.connect(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                rows.add(new Object[]{result.getInt(1), result.getString(2), result.getString(3),
                    result.getString(4), result.getDouble(5), result.getInt(6), result.getInt(7), result.getDate(8)});
            }
        } catch (SQLException e) {
            Database.showDatabaseError(e);
        }
        return rows;
    }

    static void addMedicine(String name, String company, String type, double price, int quantity,
                            int reorder, String expiry) {
        String sql = "INSERT INTO medicines(name,company,medicine_type,price,quantity_in_stock,reorder_level,expiry_date,supplier_id) VALUES(?,?,?,?,?,?,?,NULL)";
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name); statement.setString(2, company); statement.setString(3, type);
            statement.setDouble(4, price); statement.setInt(5, quantity); statement.setInt(6, reorder);
            statement.setDate(7, Date.valueOf(expiry)); statement.executeUpdate();
        } catch (SQLException e) {
            Database.showDatabaseError(e);
        }
    }

    static void updateMedicine(int id, String name, String company, String type, double price,
                               int quantity, int reorder, String expiry) {
        String sql = "UPDATE medicines SET name=?,company=?,medicine_type=?,price=?,quantity_in_stock=?,reorder_level=?,expiry_date=? WHERE medicine_id=?";
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name); statement.setString(2, company); statement.setString(3, type);
            statement.setDouble(4, price); statement.setInt(5, quantity); statement.setInt(6, reorder);
            statement.setDate(7, Date.valueOf(expiry)); statement.setInt(8, id); statement.executeUpdate();
        } catch (SQLException e) {
            Database.showDatabaseError(e);
        }
    }

    static void deleteMedicine(int id) {
        try (Connection connection = Database.connect(); PreparedStatement statement =
                connection.prepareStatement("DELETE FROM medicines WHERE medicine_id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            Database.showDatabaseError(e);
        }
    }

    static void addSupplier(String name, String contact, String phone, String email, String address) {
        String sql = "INSERT INTO suppliers(name,contact_person,phone,email,address) VALUES(?,?,?,?,?)";
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name); statement.setString(2, contact); statement.setString(3, phone);
            statement.setString(4, email); statement.setString(5, address); statement.executeUpdate();
        } catch (SQLException e) {
            Database.showDatabaseError(e);
        }
    }

    static void addUser(String username, String password, String role, String fullName) {
        String sql = "INSERT INTO users(username,password,role,full_name) VALUES(?,?,?,?)";
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username); statement.setString(2, password);
            statement.setString(3, role); statement.setString(4, fullName); statement.executeUpdate();
        } catch (SQLException e) {
            Database.showDatabaseError(e);
        }
    }

    static String completeSale(int medicineId, int quantity, String username) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement medicine = connection.prepareStatement(
                    "SELECT name,price,quantity_in_stock FROM medicines WHERE medicine_id=?")) {
                medicine.setInt(1, medicineId);
                ResultSet row = medicine.executeQuery();
                if (!row.next()) throw new SQLException("Medicine not found.");
                String name = row.getString("name");
                double price = row.getDouble("price");
                int stock = row.getInt("quantity_in_stock");
                if (quantity <= 0 || quantity > stock) throw new SQLException("Only " + stock + " item(s) are available in stock.");

                int userId;
                try (PreparedStatement account = connection.prepareStatement("SELECT user_id FROM users WHERE username=?")) {
                    account.setString(1, username);
                    ResultSet user = account.executeQuery();
                    if (!user.next()) throw new SQLException("Cashier account not found.");
                    userId = user.getInt("user_id");
                }
                double total = price * quantity;
                try (PreparedStatement sale = connection.prepareStatement("INSERT INTO sales(total_amount,user_id) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement item = connection.prepareStatement("INSERT INTO sale_items(sale_id,medicine_id,quantity_sold,price_at_sale) VALUES(?,?,?,?)");
                     PreparedStatement update = connection.prepareStatement("UPDATE medicines SET quantity_in_stock=quantity_in_stock-? WHERE medicine_id=?")) {
                    sale.setDouble(1, total); sale.setInt(2, userId); sale.executeUpdate();
                    ResultSet keys = sale.getGeneratedKeys(); keys.next();
                    item.setInt(1, keys.getInt(1)); item.setInt(2, medicineId); item.setInt(3, quantity); item.setDouble(4, price); item.executeUpdate();
                    update.setInt(1, quantity); update.setInt(2, medicineId); update.executeUpdate();
                    connection.commit();
                    return String.format("SALE COMPLETE\n\nMedicine: %s\nQuantity: %d\nUnit price: R%.2f\nTotal: R%.2f", name, quantity, price, total);
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
