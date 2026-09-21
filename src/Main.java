import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static final String DB_URL = "jdbc:mysql://localhost:3306/healthfirst_pims?useSSL=false&serverTimezone=Africa/Johannesburg";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "root";

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
