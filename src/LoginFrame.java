import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;

public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    LoginFrame() {
        setTitle("Richfield Pharmacy");
        setSize(430, 430);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints grid = new GridBagConstraints();
        grid.insets = new Insets(7, 10, 7, 10);
        grid.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("RICHFIELD PHARMACY", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setForeground(new Color(0, 102, 204));
        ImageIcon source = new ImageIcon("assets/user.png");
        Image image = source.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
        JLabel userIcon = new JLabel(new ImageIcon(image), SwingConstants.CENTER);

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JButton toggle = new JButton("Show");
        JButton login = new JButton("LOGIN");
        char echo = password.getEchoChar();
        toggle.setFocusPainted(false);
        toggle.addActionListener(event -> {
            boolean visible = password.getEchoChar() != 0;
            password.setEchoChar(visible ? (char) 0 : echo);
            toggle.setText(visible ? "Hide" : "Show");
        });
        Database.styleButton(login);

        grid.gridx = 0; grid.gridy = 0; grid.gridwidth = 2; panel.add(title, grid);
        grid.gridy++; panel.add(userIcon, grid);
        grid.gridwidth = 1; grid.gridy++;
        panel.add(new JLabel("Username:"), grid); grid.gridx = 1; panel.add(username, grid);
        grid.gridx = 0; grid.gridy++; panel.add(new JLabel("Password:"), grid); grid.gridx = 1;
        JPanel passwordPanel = new JPanel(new BorderLayout(5, 0));
        passwordPanel.setBackground(Color.WHITE);
        passwordPanel.add(password, BorderLayout.CENTER);
        passwordPanel.add(toggle, BorderLayout.EAST);
        panel.add(passwordPanel, grid);
        grid.gridx = 0; grid.gridy++; grid.gridwidth = 2; panel.add(login, grid);

        JLabel hint = new JLabel("<html><center><b>Login details</b><br>Admin: admin / admin123<br>Cashier: cashier / cash123</center></html>", SwingConstants.CENTER);
        grid.gridy++; panel.add(hint, grid);
        login.addActionListener(event -> authenticate(username, password));
        password.addActionListener(event -> authenticate(username, password));
        add(panel);
    }

    private void authenticate(JTextField username, JPasswordField password) {
        String role = AuthService.authenticate(username.getText(), new String(password.getPassword()));
        if (role != null) {
            dispose();
            DashboardFrame dashboard = new DashboardFrame(username.getText(), role);
            dashboard.setVisible(true);
            return;
        }
        JOptionPane.showMessageDialog(this,
            "Incorrect username or password.\n\nIf you forgot your login details, use the credentials shown on the login screen or contact the system administrator.",
            "Login Failed", JOptionPane.WARNING_MESSAGE);
    }
}
