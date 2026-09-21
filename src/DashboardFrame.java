import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class DashboardFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final DefaultTableModel model;
    private final JTable table;

    DashboardFrame(String username, String role) {
        this.username = username;
        setTitle("Richfield Pharmacy - " + role);
        setSize(1050, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(0, 102, 204));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(0, 102, 204));
        JLabel title = new JLabel("  RICHFIELD PHARMACY  |  " + role + ": " + username);
        title.setForeground(Color.WHITE);
        top.add(title, BorderLayout.WEST);

        model = new DefaultTableModel(new String[]{"ID", "Medicine", "Company", "Type", "Price (R)", "Stock", "Reorder", "Expiry"}, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(28);
        table.setBackground(new Color(0, 102, 204));
        table.setForeground(Color.WHITE);
        table.setGridColor(Color.WHITE);
        table.getTableHeader().setBackground(new Color(0, 82, 164));
        table.getTableHeader().setForeground(Color.WHITE);
        refresh();

        JPanel bottom = new JPanel();
        bottom.setBackground(new Color(0, 102, 204));
        addButton(bottom, "Refresh Stock", event -> refresh());
        if (role.equalsIgnoreCase("Admin")) {
            addButton(bottom, "Add Medicine", event -> addMedicineDialog());
            addButton(bottom, "Edit Medicine", event -> editMedicineDialog());
            addButton(bottom, "Delete Medicine", event -> deleteMedicine());
            addButton(bottom, "Add Supplier", event -> addSupplierDialog());
            addButton(bottom, "Add User", event -> addUserDialog());
            addButton(bottom, "Reports", event -> reportDialog());
        } else {
            addButton(bottom, "Stock Check", event -> stockCheckDialog());
            addButton(bottom, "POS / Billing", event -> saleDialog());
        }
        addButton(bottom, "Logout", event -> logout());

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void addButton(JPanel panel, String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        Database.styleButton(button);
        button.addActionListener(action);
        panel.add(button);
    }

    private void refresh() {
        model.setRowCount(0);
        for (Object[] row : MedicineStorage.medicines()) model.addRow(row);
    }

    private int selectedMedicineId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a medicine first.");
            return -1;
        }
        return (Integer) model.getValueAt(row, 0);
    }

    private void addMedicineDialog() {
        JTextField name = new JTextField();
        JTextField company = new JTextField();
        JTextField type = new JTextField();
        JTextField price = new JTextField();
        JTextField quantity = new JTextField();
        JTextField reorder = new JTextField();
        JTextField expiry = new JTextField("2027-12-31");
        if (showMedicineForm("Add Medicine", name, company, type, price, quantity, reorder, expiry)) {
            try {
                MedicineStorage.addMedicine(name.getText(), company.getText(), type.getText(), Double.parseDouble(price.getText()), Integer.parseInt(quantity.getText()), Integer.parseInt(reorder.getText()), expiry.getText());
                refresh();
            } catch (IllegalArgumentException e) { showInvalidDetails(); }
        }
    }

    private void editMedicineDialog() {
        int id = selectedMedicineId();
        if (id < 0) return;
        int row = table.getSelectedRow();
        JTextField name = new JTextField(model.getValueAt(row, 1).toString());
        JTextField company = new JTextField(model.getValueAt(row, 2).toString());
        JTextField type = new JTextField(model.getValueAt(row, 3).toString());
        JTextField price = new JTextField(model.getValueAt(row, 4).toString());
        JTextField quantity = new JTextField(model.getValueAt(row, 5).toString());
        JTextField reorder = new JTextField(model.getValueAt(row, 6).toString());
        JTextField expiry = new JTextField(model.getValueAt(row, 7).toString());
        if (showMedicineForm("Edit Medicine", name, company, type, price, quantity, reorder, expiry)) {
            try {
                MedicineStorage.updateMedicine(id, name.getText(), company.getText(), type.getText(), Double.parseDouble(price.getText()), Integer.parseInt(quantity.getText()), Integer.parseInt(reorder.getText()), expiry.getText());
                refresh();
            } catch (IllegalArgumentException e) { showInvalidDetails(); }
        }
    }

    private boolean showMedicineForm(String title, JTextField name, JTextField company, JTextField type, JTextField price, JTextField quantity, JTextField reorder, JTextField expiry) {
        JPanel panel = new JPanel(new GridLayout(7, 2, 6, 6));
        addField(panel, "Medicine:", name); addField(panel, "Company:", company); addField(panel, "Type:", type);
        addField(panel, "Price (R):", price); addField(panel, "Quantity:", quantity); addField(panel, "Reorder level:", reorder); addField(panel, "Expiry (YYYY-MM-DD):", expiry);
        return JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    private void addField(JPanel panel, String label, Component field) { panel.add(new JLabel(label)); panel.add(field); }
    private void showInvalidDetails() { JOptionPane.showMessageDialog(this, "Please enter valid medicine details."); }

    private void deleteMedicine() {
        int id = selectedMedicineId();
        if (id >= 0 && JOptionPane.showConfirmDialog(this, "Delete selected medicine?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            MedicineStorage.deleteMedicine(id);
            refresh();
        }
    }

    private void stockCheckDialog() {
        String search = JOptionPane.showInputDialog(this, "Search medicine name (leave blank for all):");
        if (search == null) return;
        StringBuilder result = new StringBuilder("STOCK CHECK\n\n");
        for (Object[] medicine : MedicineStorage.medicines()) {
            if (medicine[1].toString().toLowerCase().contains(search.toLowerCase())) {
                result.append(medicine[1]).append(" | Stock: ").append(medicine[5]).append(" | Price: R").append(medicine[4]).append("\n");
            }
        }
        JOptionPane.showMessageDialog(this, result.toString());
    }

    private void saleDialog() {
        List<Object[]> medicines = MedicineStorage.medicines();
        JComboBox<String> choices = new JComboBox<>();
        for (Object[] medicine : medicines) choices.addItem(medicine[0] + " - " + medicine[1] + " (Stock: " + medicine[5] + ")");
        JTextField quantity = new JTextField("1");
        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        addField(panel, "Medicine:", choices); addField(panel, "Quantity:", quantity);
        if (medicines.isEmpty() || JOptionPane.showConfirmDialog(this, panel, "POS / Billing", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            String bill = MedicineStorage.completeSale((Integer) medicines.get(choices.getSelectedIndex())[0], Integer.parseInt(quantity.getText()), username);
            JOptionPane.showMessageDialog(this, bill, "Bill", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        } catch (IllegalArgumentException | SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Sale Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void addSupplierDialog() {
        JTextField name = new JTextField(); JTextField contact = new JTextField(); JTextField phone = new JTextField(); JTextField email = new JTextField(); JTextField address = new JTextField();
        JPanel panel = new JPanel(new GridLayout(5, 2, 6, 6));
        addField(panel, "Name:", name); addField(panel, "Contact person:", contact); addField(panel, "Phone:", phone); addField(panel, "Email:", email); addField(panel, "Address:", address);
        if (JOptionPane.showConfirmDialog(this, panel, "Add Supplier", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) MedicineStorage.addSupplier(name.getText(), contact.getText(), phone.getText(), email.getText(), address.getText());
    }

    private void addUserDialog() {
        JTextField user = new JTextField(); JPasswordField password = new JPasswordField(); JComboBox<String> roleBox = new JComboBox<>(new String[]{"Admin", "Cashier"}); JTextField fullName = new JTextField();
        JPanel panel = new JPanel(new GridLayout(4, 2, 6, 6));
        addField(panel, "Username:", user); addField(panel, "Password:", password); addField(panel, "Role:", roleBox); addField(panel, "Full name:", fullName);
        if (JOptionPane.showConfirmDialog(this, panel, "Add User", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) MedicineStorage.addUser(user.getText(), new String(password.getPassword()), roleBox.getSelectedItem().toString(), fullName.getText());
    }

    private void reportDialog() {
        String sql = "SELECT COUNT(*) AS medicines, COALESCE(SUM(quantity_in_stock),0) AS stock, (SELECT COUNT(*) FROM suppliers) AS suppliers, (SELECT COUNT(*) FROM users) AS users, (SELECT COUNT(*) FROM sales) AS sales FROM medicines";
        try (Connection connection = Database.connect(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            if (result.next()) JOptionPane.showMessageDialog(this, "ADMIN REPORT\n\nMedicines: " + result.getInt("medicines") + "\nItems in stock: " + result.getInt("stock") + "\nSuppliers: " + result.getInt("suppliers") + "\nUsers: " + result.getInt("users") + "\nSales: " + result.getInt("sales"));
        } catch (SQLException e) { Database.showDatabaseError(e); }
    }

    private void logout() {
        dispose();
        new LoginFrame().setVisible(true);
    }
}
