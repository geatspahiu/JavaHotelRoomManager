package com.hotel.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;
import com.hotel.auth.AuthService;
import com.hotel.auth.UserSession;
import com.hotel.config.AppBrand;
import com.hotel.i18n.I18n;
import com.hotel.i18n.Language;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.Optional;

public class LoginFrame extends JFrame {
    private static final int RADIUS_BUTTON = 14;
    private static final int RADIUS_FLOATING = 24;
    private static final Color DARK_BG = new Color(18, 18, 20);
    private static final Color DARK_BG_ALT = new Color(32, 32, 36);
    private static final Color SURFACE = new Color(32, 32, 36, 226);
    private static final Color SURFACE_BORDER = new Color(255, 255, 255, 38);
    private static final Color TEXT = new Color(235, 235, 238);
    private static final Color MUTED = new Color(166, 166, 172);
    private static final Color ACCENT = new Color(82, 145, 255);
    private final AuthService authService = new AuthService();
    private final JTextField emailField = new JTextField(AppBrand.ADMIN_EMAIL);
    private final JPasswordField passwordField = new JPasswordField("admin123");
    private final JCheckBox remember = new JCheckBox();
    private final JLabel errorLabel = new JLabel(" ");
    private final JComboBox<Language> language = new JComboBox<>(Language.values());
    private boolean passwordVisible;
    private boolean dark = true;

    public LoginFrame() {
        super(I18n.t("app.name"));
        configureWindow();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setSize(1120, 760);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
    }

    private void configureWindow() {
        getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
        getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
        getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        getRootPane().putClientProperty("JRootPane.titleBarBackground", DARK_BG);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = graphics2(graphics);
                g.setPaint(new GradientPaint(0, 0, DARK_BG, getWidth(), getHeight(), DARK_BG_ALT));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(82, 145, 255, 18));
                g.fillOval(getWidth() - 360, -120, 520, 520);
                g.setColor(new Color(255, 255, 255, 10));
                g.fillOval(-180, getHeight() - 260, 420, 420);
                g.dispose();
            }
        };
        root.setBorder(new EmptyBorder(60, 60, 60, 60));

        JPanel card = new GlassCard();
        card.setLayout(new BorderLayout(0, 24));
        card.setBorder(new EmptyBorder(36, 38, 34, 38));
        card.setPreferredSize(new Dimension(460, 560));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel(AppBrand.NAME, SwingConstants.CENTER);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        logo.setFont(font(34, Font.BOLD));
        logo.setForeground(TEXT);
        JLabel title = new JLabel(I18n.t("login.title"), SwingConstants.CENTER);
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setFont(font(22, Font.BOLD));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel(I18n.t("login.subtitle"), SwingConstants.CENTER);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setFont(font(13, Font.PLAIN));
        subtitle.setForeground(MUTED);
        header.add(logo);
        header.add(Box.createVerticalStrut(16));
        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(subtitle);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addRow(form, 0, I18n.t("login.email"), emailField);
        addRow(form, 1, I18n.t("login.password"), passwordField);
        JButton toggle = secondaryButton(I18n.t("button.showHide"));
        toggle.addActionListener(event -> togglePassword());
        addRow(form, 2, "", toggle);
        remember.setText(I18n.t("login.remember"));
        remember.setForeground(TEXT);
        remember.setOpaque(false);
        addRow(form, 3, "", remember);
        JButton submit = primaryButton(I18n.t("login.button"));
        submit.addActionListener(event -> login());
        addRow(form, 4, "", submit);
        errorLabel.setForeground(new Color(255, 137, 153));
        errorLabel.setFont(font(12, Font.BOLD));
        addRow(form, 5, "", errorLabel);

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        JLabel demo = new JLabel("<html>" + I18n.t("login.demo") + "</html>");
        demo.setFont(font(11, Font.PLAIN));
        demo.setForeground(MUTED);
        language.setSelectedItem(I18n.language());
        language.addActionListener(event -> {
            I18n.setLanguage((Language) language.getSelectedItem());
            dispose();
            new LoginFrame().setVisible(true);
        });
        JButton theme = secondaryButton(I18n.t("button.theme"));
        theme.addActionListener(event -> toggleTheme());
        footer.add(demo, BorderLayout.CENTER);
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(language);
        actions.add(theme);
        footer.add(actions, BorderLayout.SOUTH);

        card.add(header, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        root.add(card);
        return root;
    }

    private void login() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        boolean rememberMe = remember.isSelected();
        errorLabel.setText(" ");
        new SwingWorker<Optional<UserSession>, Void>() {
            @Override
            protected Optional<UserSession> doInBackground() {
                return authService.login(email, password, rememberMe);
            }

            @Override
            protected void done() {
                try {
                    Optional<UserSession> session = get();
                    if (session.isEmpty()) {
                        errorLabel.setText(I18n.t("login.error"));
                        return;
                    }
                    dispose();
                    MainFrame frame = new MainFrame(session.get());
                    frame.setVisible(true);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    errorLabel.setText(cause.getMessage());
                }
            }
        }.execute();
    }

    private void togglePassword() {
        passwordVisible = !passwordVisible;
        passwordField.setEchoChar(passwordVisible ? (char) 0 : '*');
    }

    private void toggleTheme() {
        dark = !dark;
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        FlatLaf.updateUI();
        SwingUtilities.updateComponentTreeUI(this);
    }

    private static void addRow(JPanel form, int row, String labelText, java.awt.Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(8, 0, 8, 14);
        JLabel label = new JLabel(labelText);
        label.setFont(font(12, Font.BOLD));
        label.setForeground(TEXT);
        form.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        fieldConstraints.insets = new Insets(8, 0, 8, 0);
        field.setPreferredSize(new Dimension(260, 42));
        form.add(field, fieldConstraints);
    }

    private static JButton primaryButton(String text) {
        JButton button = new SoftButton(text, ACCENT, TEXT);
        button.setFont(font(13, Font.BOLD));
        button.setBorder(new EmptyBorder(11, 18, 11, 18));
        return button;
    }

    private static JButton secondaryButton(String text) {
        JButton button = new SoftButton(text, new Color(255, 255, 255, 28), TEXT);
        button.setFont(font(12, Font.BOLD));
        button.setBorder(new EmptyBorder(9, 14, 9, 14));
        return button;
    }

    private static Font font(float size, int style) {
        return new Font("-apple-system", style, Math.round(size));
    }

    private static Graphics2D graphics2(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        return g;
    }

    private static Shape continuousRect(float x, float y, float width, float height, float radius) {
        float r = Math.min(radius, Math.min(width, height) / 2f);
        return new RoundRectangle2D.Float(x, y, width, height, r * 2f, r * 2f);
    }

    private static class SoftButton extends JButton {
        private final Color fill;

        SoftButton(String text, Color fill, Color foreground) {
            super(text);
            this.fill = fill;
            setForeground(foreground);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            g.setColor(fill);
            g.fill(continuousRect(0, 0, getWidth(), getHeight(), RADIUS_BUTTON));
            g.setColor(new Color(255, 255, 255, 34));
            g.draw(continuousRect(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, RADIUS_BUTTON));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class GlassCard extends JPanel {
        GlassCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            Shape shape = continuousRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS_FLOATING);
            g.setColor(SURFACE);
            g.fill(shape);
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(SURFACE_BORDER);
            g.draw(shape);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
