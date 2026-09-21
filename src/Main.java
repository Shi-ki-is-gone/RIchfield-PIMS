import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static final String DB_URL = "jdbc:mysql://localhost:3306/healthfirst_pims?useSSL=false&serverTimezone=Africa/Johannesburg";
    static final String DB_USER = "Kyle";
    static final String DB_PASSWORD = "402306600Richield.ac.za";

    static Connection connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "MySQL JDBC Driver is missing. Put mysql-connector-j-9.4.0.jar in the lib folder.", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    static boolean login(String username, String password) {
        String sql = "SELECT role FROM users WHERE username=? AND password=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            p.setString(2, password);
            ResultSet r = p.executeQuery();
            if (r.next()) {
                Dashboard dashboard = new Dashboard(username, r.getString("role"));
                dashboard.setVisible(true);
                return true;
            }
            JOptionPane.showMessageDialog(null,
                "Incorrect username or password.\n\n" +
                "If you forgot your login details, use the credentials shown on the login screen " +
                "or contact the system administrator.",
                "Login Failed", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            showDatabaseError(e);
        }
        return false;
    }

    static List<Object[]> medicines() {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT medicine_id,name,company,medicine_type,price,quantity_in_stock,reorder_level,expiry_date FROM medicines ORDER BY name";
        try (Connection c = connect(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                rows.add(new Object[]{
                    r.getInt(1), r.getString(2), r.getString(3), r.getString(4),
                    r.getDouble(5), r.getInt(6), r.getInt(7), r.getDate(8)
                });
            }
        } catch (SQLException e) {
            showDatabaseError(e);
        }
        return rows;
    }

    static void addMedicine(String name, String company, String type, double price, int qty, int reorder, String expiry) {
        String sql = "INSERT INTO medicines(name,company,medicine_type,price,quantity_in_stock,reorder_level,expiry_date,supplier_id) VALUES(?,?,?,?,?,?,?,NULL)";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, name); p.setString(2, company); p.setString(3, type);
            p.setDouble(4, price); p.setInt(5, qty); p.setInt(6, reorder);
            p.setDate(7, Date.valueOf(expiry));
            p.executeUpdate();
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    static void updateMedicine(int id, String name, String company, String type,
                               double price, int qty, int reorder, String expiry) {
        String sql = "UPDATE medicines SET name=?,company=?,medicine_type=?,price=?,quantity_in_stock=?,reorder_level=?,expiry_date=? WHERE medicine_id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, name); p.setString(2, company); p.setString(3, type);
            p.setDouble(4, price); p.setInt(5, qty); p.setInt(6, reorder);
            p.setDate(7, Date.valueOf(expiry)); p.setInt(8, id);
            p.executeUpdate();
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    static void deleteMedicine(int id) {
        String sql = "DELETE FROM medicines WHERE medicine_id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            p.executeUpdate();
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    static void addSupplier(String name, String contact, String phone, String email, String address) {
        String sql = "INSERT INTO suppliers(name,contact_person,phone,email,address) VALUES(?,?,?,?,?)";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, name); p.setString(2, contact); p.setString(3, phone);
            p.setString(4, email); p.setString(5, address); p.executeUpdate();
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    static void addUser(String username, String password, String role, String fullName) {
        String sql = "INSERT INTO users(username,password,role,full_name) VALUES(?,?,?,?)";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username); p.setString(2, password); p.setString(3, role);
            p.setString(4, fullName); p.executeUpdate();
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    static String completeSale(int medicineId, int quantity, String username) throws SQLException {
        String select = "SELECT name,price,quantity_in_stock FROM medicines WHERE medicine_id=?";
        String user = "SELECT user_id FROM users WHERE username=?";
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try (PreparedStatement medicine = c.prepareStatement(select);
                 PreparedStatement account = c.prepareStatement(user)) {
                medicine.setInt(1, medicineId);
                ResultSet row = medicine.executeQuery();
                if (!row.next()) throw new SQLException("Medicine not found.");
                String name = row.getString("name");
                double price = row.getDouble("price");
                int stock = row.getInt("quantity_in_stock");
                if (quantity <= 0 || quantity > stock) {
                    throw new SQLException("Only " + stock + " item(s) are available in stock.");
                }

                account.setString(1, username);
                ResultSet userRow = account.executeQuery();
                if (!userRow.next()) throw new SQLException("Cashier account not found.");
                int userId = userRow.getInt("user_id");
                double total = price * quantity;

                try (PreparedStatement sale = c.prepareStatement(
                        "INSERT INTO sales(total_amount,user_id) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement item = c.prepareStatement(
                        "INSERT INTO sale_items(sale_id,medicine_id,quantity_sold,price_at_sale) VALUES(?,?,?,?)");
                     PreparedStatement update = c.prepareStatement(
                        "UPDATE medicines SET quantity_in_stock=quantity_in_stock-? WHERE medicine_id=?")) {
                    sale.setDouble(1, total); sale.setInt(2, userId); sale.executeUpdate();
                    ResultSet keys = sale.getGeneratedKeys();
                    keys.next();
                    item.setInt(1, keys.getInt(1)); item.setInt(2, medicineId);
                    item.setInt(3, quantity); item.setDouble(4, price); item.executeUpdate();
                    update.setInt(1, quantity); update.setInt(2, medicineId); update.executeUpdate();
                    c.commit();
                    return String.format("SALE COMPLETE\n\nMedicine: %s\nQuantity: %d\nUnit price: R%.2f\nTotal: R%.2f",
                            name, quantity, price, total);
                }
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    static void showDatabaseError(SQLException e) {
        JOptionPane.showMessageDialog(null,
            "Database connection failed.\n\n" +
            e.getMessage() +
            "\n\nCheck:\n1. MySQL Server is running.\n2. Database 'healthfirst_pims' exists.\n3. Username/password in Main.java are correct.\n4. mysql-connector-j-9.4.0.jar is inside the lib folder.",
            "MySQL Error", JOptionPane.ERROR_MESSAGE);
    }

    static void styleButton(JButton b) {
        b.setFont(new Font("Arial", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBackground(new Color(0, 102, 204));
        b.setForeground(Color.WHITE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login login = new Login();
            login.setVisible(true);
        });
    }

    static class Login extends JFrame {
        private static final long serialVersionUID = 1L;

        Login() {
            setTitle("Richfield Pharmacy");
            setSize(430, 430);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(EXIT_ON_CLOSE);

            JPanel p = new JPanel(new GridBagLayout());
            p.setBackground(Color.WHITE);
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(7, 10, 7, 10);
            g.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel("RICHFIELD PHARMACY", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 30));
            title.setForeground(new Color(0,102,204));

            ImageIcon userImage = new ImageIcon("assets/user.png");
            Image scaledUserImage = userImage.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
            JLabel userIcon = new JLabel(new ImageIcon(scaledUserImage), SwingConstants.CENTER);

            JTextField user = new JTextField();
            JPasswordField pass = new JPasswordField();
            JButton togglePassword = new JButton("Show");
            JButton login = new JButton("LOGIN");
            char passwordEchoChar = pass.getEchoChar();
            togglePassword.setFocusPainted(false);
            togglePassword.addActionListener(e -> {
                boolean showPassword = pass.getEchoChar() != 0;
                pass.setEchoChar(showPassword ? (char) 0 : passwordEchoChar);
                togglePassword.setText(showPassword ? "Hide" : "Show");
            });
            styleButton(login);

            g.gridx=0; g.gridy=0; g.gridwidth=2; p.add(title,g);
            g.gridy++; p.add(userIcon,g);
            g.gridwidth=1; g.gridy++;
            p.add(new JLabel("Username:"),g); g.gridx=1; p.add(user,g);
            g.gridx=0; g.gridy++;
            p.add(new JLabel("Password:"),g); g.gridx=1;
            JPanel passwordPanel = new JPanel(new BorderLayout(5, 0));
            passwordPanel.add(pass, BorderLayout.CENTER);
            passwordPanel.add(togglePassword, BorderLayout.EAST);
            p.add(passwordPanel,g);
            g.gridx=0; g.gridy++; g.gridwidth=2; p.add(login,g);

            JLabel hint = new JLabel("<html><center><b>Login details</b><br>Admin: admin / admin123<br>Cashier: cashier / cash123</center></html>",
                    SwingConstants.CENTER);
            g.gridy++; p.add(hint,g);

            login.addActionListener(e -> login(user.getText(), new String(pass.getPassword())));
            pass.addActionListener(e -> login(user.getText(), new String(pass.getPassword())));

            add(p);
        }
    }

    static class Dashboard extends JFrame {
        private static final long serialVersionUID = 1L;

        JTable table;
        DefaultTableModel model;

        Dashboard(String username, String role) {
            setTitle("Richfield Pharmacy - " + role);
            setSize(1050, 620);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            getContentPane().setBackground(new Color(0, 102, 204));

            JPanel top = new JPanel(new BorderLayout());
            top.setBackground(new Color(0,102,204));
            JLabel title = new JLabel("  RICHFIELD PHARMACY  |  " + role + ": " + username);
            title.setFont(new Font("Arial", Font.BOLD, 20));
            title.setForeground(Color.WHITE);
            top.add(title, BorderLayout.WEST);

            model = new DefaultTableModel(
                new String[]{"ID","Medicine","Company","Type","Price (R)","Stock","Reorder","Expiry"}, 0) {
                    @Override
                    public boolean isCellEditable(int r, int c) { return false; }
                };
            table = new JTable(model);
            table.setRowHeight(28);
            table.setBackground(new Color(0, 102, 204));
            table.setForeground(Color.WHITE);
            table.setGridColor(Color.WHITE);
            table.getTableHeader().setBackground(new Color(0, 82, 164));
            table.getTableHeader().setForeground(Color.WHITE);
            refresh();

            JButton refresh = new JButton("Refresh Stock");
            styleButton(refresh);
            refresh.addActionListener(e -> refresh());

            JPanel bottom = new JPanel();
            bottom.setBackground(new Color(0, 102, 204));
            bottom.add(refresh);

            if (role.equalsIgnoreCase("Admin")) {
                JButton add = new JButton("Add Medicine");
                styleButton(add);
                add.addActionListener(e -> addMedicineDialog());
                bottom.add(add);

                JButton edit = new JButton("Edit Medicine");
                styleButton(edit);
                edit.addActionListener(e -> editMedicineDialog());
                bottom.add(edit);

                JButton delete = new JButton("Delete Medicine");
                styleButton(delete);
                delete.addActionListener(e -> deleteMedicine());
                bottom.add(delete);

                JButton supplier = new JButton("Add Supplier");
                styleButton(supplier);
                supplier.addActionListener(e -> addSupplierDialog());
                bottom.add(supplier);

                JButton user = new JButton("Add User");
                styleButton(user);
                user.addActionListener(e -> addUserDialog());
                bottom.add(user);

                JButton reports = new JButton("Reports");
                styleButton(reports);
                reports.addActionListener(e -> reportDialog());
                bottom.add(reports);
            } else {
                JButton stock = new JButton("Stock Check");
                styleButton(stock);
                stock.addActionListener(e -> stockCheckDialog());
                bottom.add(stock);

                JButton sale = new JButton("POS / Billing");
                styleButton(sale);
                sale.addActionListener(e -> saleDialog(username));
                bottom.add(sale);
            }

            JButton logout = new JButton("Logout");
            styleButton(logout);
            logout.addActionListener(e -> {
                dispose();
                Login login = new Login();
                login.setVisible(true);
            });
            bottom.add(logout);

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }

        private void refresh() {
            model.setRowCount(0);
            for (Object[] row : medicines()) model.addRow(row);
        }

        private int selectedMedicineId() {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a medicine first.");
                return -1;
            }
            return (Integer) model.getValueAt(row, 0);
        }

        private void editMedicineDialog() {
            int id = selectedMedicineId();
            if (id < 0) return;
            int row = table.getSelectedRow();
            JTextField name = new JTextField(model.getValueAt(row, 1).toString());
            JTextField company = new JTextField(model.getValueAt(row, 2).toString());
            JTextField type = new JTextField(model.getValueAt(row, 3).toString());
            JTextField price = new JTextField(model.getValueAt(row, 4).toString());
            JTextField qty = new JTextField(model.getValueAt(row, 5).toString());
            JTextField reorder = new JTextField(model.getValueAt(row, 6).toString());
            JTextField expiry = new JTextField(model.getValueAt(row, 7).toString());
            JPanel panel = medicineForm(name, company, type, price, qty, reorder, expiry);
            if (JOptionPane.showConfirmDialog(this, panel, "Edit Medicine", JOptionPane.OK_CANCEL_OPTION)
                    == JOptionPane.OK_OPTION) {
                try {
                    updateMedicine(id, name.getText(), company.getText(), type.getText(),
                        Double.parseDouble(price.getText()), Integer.parseInt(qty.getText()),
                        Integer.parseInt(reorder.getText()), expiry.getText());
                    refresh();
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter valid medicine details.");
                }
            }
        }

        private void deleteMedicine() {
            int id = selectedMedicineId();
            if (id >= 0 && JOptionPane.showConfirmDialog(this, "Delete selected medicine?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                Main.deleteMedicine(id);
                refresh();
            }
        }

        private JPanel medicineForm(JTextField name, JTextField company, JTextField type,
                                     JTextField price, JTextField qty, JTextField reorder, JTextField expiry) {
            JPanel panel = new JPanel(new GridLayout(7, 2, 6, 6));
            panel.add(new JLabel("Medicine:")); panel.add(name);
            panel.add(new JLabel("Company:")); panel.add(company);
            panel.add(new JLabel("Type:")); panel.add(type);
            panel.add(new JLabel("Price (R):")); panel.add(price);
            panel.add(new JLabel("Quantity:")); panel.add(qty);
            panel.add(new JLabel("Reorder level:")); panel.add(reorder);
            panel.add(new JLabel("Expiry (YYYY-MM-DD):")); panel.add(expiry);
            return panel;
        }

        private void stockCheckDialog() {
            String search = JOptionPane.showInputDialog(this, "Search medicine name (leave blank for all):");
            if (search == null) return;
            StringBuilder result = new StringBuilder("STOCK CHECK\n\n");
            for (Object[] medicine : medicines()) {
                if (medicine[1].toString().toLowerCase().contains(search.toLowerCase())) {
                    result.append(medicine[1]).append(" | Stock: ").append(medicine[5])
                        .append(" | Price: R").append(medicine[4]).append("\n");
                }
            }
            JOptionPane.showMessageDialog(this, result.toString());
        }

        private void saleDialog(String username) {
            List<Object[]> available = medicines();
            JComboBox<String> medicineBox = new JComboBox<>();
            for (Object[] medicine : available) {
                medicineBox.addItem(medicine[0] + " - " + medicine[1] + " (Stock: " + medicine[5] + ")");
            }
            JTextField quantity = new JTextField("1");
            JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
            panel.add(new JLabel("Medicine:")); panel.add(medicineBox);
            panel.add(new JLabel("Quantity:")); panel.add(quantity);
            if (available.isEmpty() || JOptionPane.showConfirmDialog(this, panel, "POS / Billing",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            try {
                int selected = medicineBox.getSelectedIndex();
                String receipt = completeSale((Integer) available.get(selected)[0],
                    Integer.parseInt(quantity.getText()), username);
                JOptionPane.showMessageDialog(this, receipt, "Bill", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            } catch (IllegalArgumentException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Sale Failed", JOptionPane.WARNING_MESSAGE);
            }
        }

        private void addSupplierDialog() {
            JTextField name = new JTextField();
            JTextField contact = new JTextField();
            JTextField phone = new JTextField();
            JTextField email = new JTextField();
            JTextField address = new JTextField();
            JPanel panel = new JPanel(new GridLayout(5, 2, 6, 6));
            panel.add(new JLabel("Name:")); panel.add(name);
            panel.add(new JLabel("Contact person:")); panel.add(contact);
            panel.add(new JLabel("Phone:")); panel.add(phone);
            panel.add(new JLabel("Email:")); panel.add(email);
            panel.add(new JLabel("Address:")); panel.add(address);
            if (JOptionPane.showConfirmDialog(this, panel, "Add Supplier", JOptionPane.OK_CANCEL_OPTION)
                    == JOptionPane.OK_OPTION) {
                addSupplier(name.getText(), contact.getText(), phone.getText(), email.getText(), address.getText());
            }
        }

        private void addUserDialog() {
            JTextField username = new JTextField();
            JPasswordField password = new JPasswordField();
            JComboBox<String> role = new JComboBox<>(new String[]{"Admin", "Cashier"});
            JTextField fullName = new JTextField();
            JPanel panel = new JPanel(new GridLayout(4, 2, 6, 6));
            panel.add(new JLabel("Username:")); panel.add(username);
            panel.add(new JLabel("Password:")); panel.add(password);
            panel.add(new JLabel("Role:")); panel.add(role);
            panel.add(new JLabel("Full name:")); panel.add(fullName);
            if (JOptionPane.showConfirmDialog(this, panel, "Add User", JOptionPane.OK_CANCEL_OPTION)
                    == JOptionPane.OK_OPTION) {
                addUser(username.getText(), new String(password.getPassword()), role.getSelectedItem().toString(), fullName.getText());
            }
        }

        private void reportDialog() {
            String sql = "SELECT COUNT(*) AS medicines, COALESCE(SUM(quantity_in_stock),0) AS stock, " +
                "(SELECT COUNT(*) FROM suppliers) AS suppliers, (SELECT COUNT(*) FROM users) AS users, " +
                "(SELECT COUNT(*) FROM sales) AS sales FROM medicines";
            try (Connection c = connect(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
                if (r.next()) {
                    JOptionPane.showMessageDialog(this,
                        "ADMIN REPORT\n\nMedicines: " + r.getInt("medicines") +
                        "\nItems in stock: " + r.getInt("stock") +
                        "\nSuppliers: " + r.getInt("suppliers") +
                        "\nUsers: " + r.getInt("users") + "\nSales: " + r.getInt("sales"));
                }
            } catch (SQLException e) {
                showDatabaseError(e);
            }
        }

        void addMedicineDialog() {
            JTextField name = new JTextField();
            JTextField company = new JTextField();
            JTextField type = new JTextField();
            JTextField price = new JTextField();
            JTextField qty = new JTextField();
            JTextField reorder = new JTextField();
            JTextField expiry = new JTextField("2027-12-31");

            JPanel p = new JPanel(new GridLayout(7,2,6,6));
            p.add(new JLabel("Medicine:")); p.add(name);
            p.add(new JLabel("Company:")); p.add(company);
            p.add(new JLabel("Type:")); p.add(type);
            p.add(new JLabel("Price (R):")); p.add(price);
            p.add(new JLabel("Quantity:")); p.add(qty);
            p.add(new JLabel("Reorder level:")); p.add(reorder);
            p.add(new JLabel("Expiry (YYYY-MM-DD):")); p.add(expiry);

            int result = JOptionPane.showConfirmDialog(this, p, "Add Medicine",
                    JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    addMedicine(name.getText(), company.getText(), type.getText(),
                        Double.parseDouble(price.getText()), Integer.parseInt(qty.getText()),
                        Integer.parseInt(reorder.getText()), expiry.getText());
                    refresh();
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter valid medicine details.");
                }
            }
        }
    }
}
