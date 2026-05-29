package com.hotel.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;
import com.hotel.auth.AuthService;
import com.hotel.auth.UserAccount;
import com.hotel.auth.UserRole;
import com.hotel.auth.UserSession;
import com.hotel.config.AppBrand;
import com.hotel.i18n.I18n;
import com.hotel.i18n.Language;
import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.Guest;
import com.hotel.model.HotelStats;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.model.RoomTypeStats;
import com.hotel.service.BookingService;
import com.hotel.service.GuestService;
import com.hotel.service.RoomService;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class MainFrame extends JFrame {
    private final UserSession session;
    private final AuthService authService = new AuthService();
    private final RoomService roomService = new RoomService();
    private final GuestService guestService = new GuestService();
    private final BookingService bookingService = new BookingService();
    private final CardLayout pages = new CardLayout();
    private final AnimatedPageHost pageHost = new AnimatedPageHost(pages);
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Refreshable> refreshables = new LinkedHashMap<>();
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final JLabel pageSubtitle = new JLabel("Executive overview");
    private final JLabel clockLabel = new JLabel();
    private final JTextField globalSearch = new JTextField();
    private final JComboBox<Language> languageSwitcher = new JComboBox<>(Language.values());
    private boolean darkMode = true;
    private boolean collapsedSidebar;
    private String currentPage;
    private RoundedPanel sidebar;
    private JPanel navList;

    public MainFrame(UserSession session) {
        super(I18n.t("app.name"));
        this.session = session;
        darkMode = Theme.dark;
        configureMacWindow();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1440, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.bg());

        sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);
        add(buildWorkspace(), BorderLayout.CENTER);
        installPages();
        startClock();
        showPage(session.isAdmin() ? "Dashboard" : "Bookings");
    }

    private void configureMacWindow() {
        getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
        getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
        getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        getRootPane().putClientProperty("JRootPane.titleBarBackground", Theme.bg());
        getRootPane().putClientProperty("JRootPane.titleBarForeground", Theme.text());
    }

    private RoundedPanel buildSidebar() {
        RoundedPanel panel = new RoundedPanel(0, Theme.sidebar(), Theme.border());
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(262, 10));
        panel.setBorder(new EmptyBorder(28, 16, 18, 14));

        JPanel brand = transparentPanel(new BorderLayout(12, 0));
        BrandMark mark = new BrandMark();
        JLabel name = label(AppBrand.NAME, 22, Font.BOLD, Theme.text());
        JLabel caption = label(session.user().getRole() == UserRole.ADMIN ? "Executive Suite" : "Front Desk Suite", 12, Font.PLAIN, Theme.muted());
        JPanel text = transparentPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(name);
        text.add(caption);
        brand.add(mark, BorderLayout.WEST);
        brand.add(text, BorderLayout.CENTER);

        JButton collapse = iconButton("Collapse", IconGlyph.COLLAPSE);
        collapse.addActionListener(event -> toggleSidebar());
        brand.add(collapse, BorderLayout.EAST);

        navList = transparentPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        if (session.isAdmin()) {
            addNav("Dashboard", "nav.dashboard", IconGlyph.DASHBOARD, "nav.dashboard.sub");
            addNav("Rooms", "nav.rooms", IconGlyph.ROOMS, "nav.rooms.sub");
        }
        addNav("Bookings", "nav.bookings", IconGlyph.BOOKINGS, "nav.bookings.sub");
        addNav("Guests", "nav.guests", IconGlyph.GUESTS, "nav.guests.sub");
        if (session.isAdmin()) {
            addNav("Payments", "nav.payments", IconGlyph.PAYMENTS, "nav.payments.sub");
            addNav("Workers", "nav.workers", IconGlyph.STAFF, "nav.workers.sub");
            addNav("Analytics", "nav.analytics", IconGlyph.ANALYTICS, "nav.analytics.sub");
        }
        addNav("Settings", "nav.settings", IconGlyph.SETTINGS, "nav.settings.sub");

        RoundedPanel plan = new RoundedPanel(Theme.RADIUS_CARD, Theme.cardAlt(), Theme.border()).elevated(2f);
        plan.setLayout(new BorderLayout(10, 8));
        plan.setBorder(new EmptyBorder(16, 16, 16, 16));
        plan.add(label(session.user().getName(), 13, Font.BOLD, Theme.text()), BorderLayout.NORTH);
        plan.add(label("<html>" + session.user().getRole() + "<br>" + session.user().getEmail() + "</html>", 12, Font.PLAIN, Theme.muted()), BorderLayout.CENTER);

        panel.add(brand, BorderLayout.NORTH);
        panel.add(navList, BorderLayout.CENTER);
        panel.add(plan, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildWorkspace() {
        JPanel shell = transparentPanel(new BorderLayout());
        shell.setBorder(new EmptyBorder(0, 0, 0, 0));
        shell.add(buildTopBar(), BorderLayout.NORTH);

        pageHost.setOpaque(false);
        JScrollPane scroller = new JScrollPane(pageHost);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setOpaque(false);
        scroller.setOpaque(false);
        shell.add(scroller, BorderLayout.CENTER);
        return shell;
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout(18, 0)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = graphics2(graphics);
                g.setPaint(new GradientPaint(0, 0, Theme.topStart(), getWidth(), 0, Theme.topEnd()));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.border());
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g.dispose();
            }
        };
        top.setOpaque(false);
        top.setPreferredSize(new Dimension(10, 92));
        top.setBorder(new EmptyBorder(18, 28, 14, 28));

        JPanel titleBox = transparentPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        pageTitle.setFont(Theme.font(24, Font.BOLD));
        pageTitle.setForeground(Theme.text());
        pageSubtitle.setFont(Theme.font(12, Font.PLAIN));
        pageSubtitle.setForeground(Theme.muted());
        titleBox.add(pageTitle);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(pageSubtitle);

        globalSearch.putClientProperty("JTextField.placeholderText", I18n.t("top.search"));
        globalSearch.putClientProperty("JTextField.leadingIcon", new LineIcon(IconGlyph.SEARCH, 16, Theme.muted()));
        globalSearch.setPreferredSize(new Dimension(320, 42));
        globalSearch.addActionListener(event -> showToast("Use the search field inside Bookings or Guests.", false));

        JPanel actions = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        clockLabel.setForeground(Theme.muted());
        clockLabel.setFont(Theme.font(12, Font.BOLD));
        JButton quickBook = primaryButton(I18n.t("top.quick"));
        quickBook.addActionListener(event -> showPage("Bookings"));
        JButton bell = iconButton(I18n.t("top.notifications"), IconGlyph.BELL);
        bell.addActionListener(event -> showToast("No new notifications", false));
        JButton theme = iconButton("Switch theme", IconGlyph.THEME);
        theme.addActionListener(event -> toggleTheme());
        JButton logout = ghostButton(I18n.t("top.logout"));
        logout.addActionListener(event -> logout());
        languageSwitcher.setSelectedItem(I18n.language());
        languageSwitcher.addActionListener(event -> switchLanguage((Language) languageSwitcher.getSelectedItem()));
        Avatar avatar = new Avatar(session.initials());
        actions.add(clockLabel);
        actions.add(globalSearch);
        actions.add(quickBook);
        actions.add(bell);
        actions.add(theme);
        actions.add(languageSwitcher);
        actions.add(logout);
        actions.add(avatar);

        top.add(titleBox, BorderLayout.WEST);
        top.add(actions, BorderLayout.CENTER);
        return top;
    }

    private void installPages() {
        if (session.isAdmin()) {
            addPage("Dashboard", new DashboardPage());
            addPage("Rooms", new RoomsPage());
        }
        addPage("Bookings", new BookingsPage());
        addPage("Guests", new GuestsPage());
        if (session.isAdmin()) {
            addPage("Payments", new PaymentsPage());
            addPage("Workers", new WorkersPage());
            addPage("Analytics", new AnalyticsPage());
        }
        addPage("Settings", new SettingsPage());
    }

    private void addPage(String name, JComponent component) {
        pageHost.add(component, name);
        if (component instanceof Refreshable refreshable) {
            refreshables.put(name, refreshable);
        }
    }

    private void addNav(String name, String labelKey, IconGlyph glyph, String subtitleKey) {
        NavButton button = new NavButton(labelKey, glyph, subtitleKey);
        button.addActionListener(event -> showPage(name));
        navButtons.put(name, button);
        navList.add(Box.createVerticalStrut(7));
        navList.add(button);
    }

    private void showPage(String name) {
        if (!navButtons.containsKey(name)) {
            return;
        }
        currentPage = name;
        pageHost.beginTransition();
        pages.show(pageHost, name);
        pageTitle.setText(I18n.t(navButtons.get(name).labelKey));
        pageSubtitle.setText(I18n.t(navButtons.get(name).subtitleKey));
        navButtons.forEach((key, button) -> button.setActive(key.equals(name)));
        refreshPage(name);
        pageHost.playIn();
    }

    private void refreshAll() {
        if (currentPage != null) {
            refreshPage(currentPage);
        }
    }

    private void refreshPage(String name) {
        Refreshable refreshable = refreshables.get(name);
        if (refreshable != null) {
            refreshable.refresh();
        }
    }

    private <T> void runInBackground(Callable<T> task, Consumer<T> onSuccess, String errorPrefix) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showToast(errorPrefix + ": " + cause.getMessage(), true);
                }
            }
        }.execute();
    }

    private void runInBackground(Callable<Void> task, Runnable onSuccess, String errorPrefix) {
        runInBackground(task, ignored -> onSuccess.run(), errorPrefix);
    }

    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM d  HH:mm");
        Timer timer = new Timer(1000, event -> clockLabel.setText(LocalDateTime.now().format(formatter)));
        timer.start();
        clockLabel.setText(LocalDateTime.now().format(formatter));
    }

    private void toggleSidebar() {
        collapsedSidebar = !collapsedSidebar;
        sidebar.setPreferredSize(new Dimension(collapsedSidebar ? 86 : 262, 10));
        for (NavButton button : navButtons.values()) {
            button.setCollapsed(collapsedSidebar);
        }
        revalidate();
        repaint();
    }

    private void toggleTheme() {
        Theme.dark = !Theme.dark;
        darkMode = Theme.dark;
        if (darkMode) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        FlatLaf.updateUI();
        Dimension size = getSize();
        java.awt.Point location = getLocation();
        dispose();
        MainFrame frame = new MainFrame(session);
        frame.setSize(size);
        frame.setLocation(location);
        frame.setVisible(true);
    }

    private void switchLanguage(Language language) {
        if (language == null || language == I18n.language()) {
            return;
        }
        I18n.setLanguage(language);
        Dimension size = getSize();
        java.awt.Point location = getLocation();
        dispose();
        MainFrame frame = new MainFrame(session);
        frame.setSize(size);
        frame.setLocation(location);
        frame.setVisible(true);
    }

    private void logout() {
        dispose();
        new LoginFrame().setVisible(true);
    }

    private class DashboardPage extends PagePanel implements Refreshable {
        private final MetricCard totalRooms = new MetricCard(I18n.t("metric.totalRooms"), "0", "+0%", IconGlyph.ROOMS, Theme.blue());
        private final MetricCard occupiedRooms = new MetricCard(I18n.t("metric.occupied"), "0", "+0%", IconGlyph.BOOKINGS, Theme.blue());
        private final MetricCard availableRooms = new MetricCard(I18n.t("metric.available"), "0", "+0%", IconGlyph.CHECK, Theme.green());
        private final MetricCard guests = new MetricCard(I18n.t("metric.activeGuests"), "0", "+0%", IconGlyph.GUESTS, Theme.muted());
        private final MetricCard revenue = new MetricCard(I18n.t("metric.revenue"), "$0", "+0%", IconGlyph.PAYMENTS, Theme.green());
        private final MetricCard reservations = new MetricCard(I18n.t("metric.today"), "0", "+0%", IconGlyph.CALENDAR, Theme.blue());
        private final MetricCard checkIns = new MetricCard(I18n.t("metric.checkIns"), "0", "+0%", IconGlyph.LOGIN, Theme.blue());
        private final MetricCard checkOuts = new MetricCard(I18n.t("metric.checkOuts"), "0", "+0%", IconGlyph.LOGOUT, Theme.muted());
        private final ProgressRing ring = new ProgressRing();
        private final ActivityFeed activityFeed = new ActivityFeed();
        private final CalendarWidget calendar = new CalendarWidget();
        private final BookingsTableModel tableModel = new BookingsTableModel();
        private final JTable table = styledTable(tableModel);

        DashboardPage() {
            JPanel hero = new GradientHero();
            hero.setLayout(new BorderLayout());
            hero.setBorder(new EmptyBorder(26, 28, 26, 28));
            JPanel heroText = transparentPanel();
            heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
            heroText.add(label(String.format(I18n.t("dashboard.welcome"), session.user().getName()), 28, Font.BOLD, Theme.text()));
            heroText.add(Box.createVerticalStrut(8));
            heroText.add(label(I18n.t("dashboard.sync"), 14, Font.PLAIN, Theme.muted()));
            JPanel heroActions = transparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 18));
            JButton addBooking = primaryButton(I18n.t("dashboard.create"));
            addBooking.addActionListener(event -> showPage("Bookings"));
            JButton roomsAction = ghostButton(I18n.t("dashboard.reviewRooms"));
            roomsAction.addActionListener(event -> showPage("Rooms"));
            heroActions.add(addBooking);
            heroActions.add(roomsAction);
            heroText.add(heroActions);
            hero.add(heroText, BorderLayout.CENTER);
            hero.add(ring, BorderLayout.EAST);

            JPanel metrics = grid(4, 14);
            metrics.add(totalRooms);
            metrics.add(occupiedRooms);
            metrics.add(availableRooms);
            metrics.add(guests);
            metrics.add(revenue);
            metrics.add(reservations);
            metrics.add(checkIns);
            metrics.add(checkOuts);

            JPanel lower = new JPanel(new GridBagLayout());
            lower.setOpaque(false);
            GridBagConstraints c = constraints();
            c.gridx = 0;
            c.weightx = 0.62;
            c.fill = GridBagConstraints.BOTH;
            lower.add(card(I18n.t("table.activity"), tableWithToolbar(table, null)), c);
            c.gridx = 1;
            c.weightx = 0.38;
            JPanel side = transparentPanel();
            side.setLayout(new GridLayout(2, 1, 0, 14));
            side.add(activityFeed);
            side.add(calendar);
            lower.add(side, c);

            add(hero);
            add(metrics);
            add(lower);
        }

        @Override
        public void refresh() {
            runInBackground(() -> {
                HotelStats stats = roomService.getStats();
                List<Guest> allGuests = guestService.findAll();
                List<Booking> bookings = bookingService.findAll();
                BigDecimal estimatedRevenue = estimateRevenue();
                return new DashboardData(stats, allGuests.size(), bookings, estimatedRevenue);
            }, data -> {
                HotelStats stats = data.stats();
                List<Booking> bookings = data.bookings();
                long active = bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.ACTIVE).count();
                long todayIn = bookings.stream().filter(booking -> LocalDate.now().equals(booking.getCheckIn())).count();
                long todayOut = bookings.stream().filter(booking -> LocalDate.now().equals(booking.getCheckOut())).count();
                totalRooms.setValue(String.valueOf(stats.totalRooms()), "+12%");
                occupiedRooms.setValue(String.valueOf(stats.occupiedRooms()), active > 0 ? "+8%" : "+0%");
                availableRooms.setValue(String.valueOf(stats.availableRooms()), "+4%");
                guests.setValue(String.valueOf(data.guestCount()), "+18%");
                revenue.setValue(currency(data.estimatedRevenue()), "+24%");
                reservations.setValue(String.valueOf(todayIn), "+6%");
                checkIns.setValue(String.valueOf(todayIn), "+3%");
                checkOuts.setValue(String.valueOf(todayOut), "-2%");
                ring.setProgress(stats.totalRooms() == 0 ? 0 : stats.occupiedRooms() / (double) stats.totalRooms());
                activityFeed.setBookings(bookings);
                calendar.setBookings(bookings);
                tableModel.setRows(bookings);
            }, "Unable to refresh dashboard");
        }
    }

    private class AnalyticsPage extends PagePanel implements Refreshable {
        private final JPanel chartGrid = grid(2, 14);
        private final MetricCard adr = new MetricCard("Average daily rate", "$0", "+0%", IconGlyph.PAYMENTS, Theme.green());
        private final MetricCard revPar = new MetricCard("RevPAR", "$0", "+0%", IconGlyph.ANALYTICS, Theme.blue());
        private final MetricCard occupancy = new MetricCard("Occupancy", "0%", "+0%", IconGlyph.ROOMS, Theme.blue());
        private final MetricCard retention = new MetricCard("Guest activity", "0", "+0%", IconGlyph.GUESTS, Theme.muted());

        AnalyticsPage() {
            JPanel metrics = grid(4, 14);
            metrics.add(adr);
            metrics.add(revPar);
            metrics.add(occupancy);
            metrics.add(retention);
            add(metrics);
            add(chartGrid);
        }

        @Override
        public void refresh() {
            runInBackground(() -> {
                HotelStats stats = roomService.getStats();
                List<Booking> bookings = bookingService.findAll();
                BigDecimal revenue = estimateRevenue();
                return new AnalyticsData(stats, bookings, revenue);
            }, data -> {
                HotelStats stats = data.stats();
                List<Booking> bookings = data.bookings();
                BigDecimal revenue = data.revenue();
                int total = Math.max(stats.totalRooms(), 1);
                double occupancyRate = stats.occupiedRooms() / (double) total;
                adr.setValue(currency(bookings.isEmpty() ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(bookings.size()), 2, java.math.RoundingMode.HALF_UP)), "+11%");
                revPar.setValue(currency(revenue.divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP)), "+17%");
                occupancy.setValue(Math.round(occupancyRate * 100) + "%", "+9%");
                retention.setValue(String.valueOf(bookings.size()), "+21%");
                rebuildCharts(stats, bookings);
            }, "Unable to refresh analytics");
        }

        private void rebuildCharts(HotelStats stats, List<Booking> bookings) {
            chartGrid.removeAll();
            chartGrid.add(card("Booking trends", chartPanel(lineChart(bookings))));
            chartGrid.add(card("Revenue analytics", chartPanel(barChart())));
            chartGrid.add(card("Occupancy mix", chartPanel(pieChart(stats))));
            chartGrid.add(card("Guest activity", chartPanel(activityChart(bookings))));
            chartGrid.revalidate();
            chartGrid.repaint();
        }
    }

    private class RoomsPage extends PagePanel implements Refreshable {
        private final RoomTableModel tableModel = new RoomTableModel();
        private final JTable table = styledTable(tableModel);
        private final JComboBox<String> typeFilter = new JComboBox<>();
        private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{I18n.t("rooms.allStatus"), I18n.t("rooms.available"), I18n.t("rooms.occupied")});
        private final Map<RoomType, JLabel> typeStats = new EnumMap<>(RoomType.class);

        RoomsPage() {
            typeFilter.addItem(I18n.t("rooms.allTypes"));
            for (RoomType type : RoomType.values()) {
                typeFilter.addItem(type.toString());
            }

            JPanel summary = grid(RoomType.values().length, 14);
            for (RoomType type : RoomType.values()) {
                RoundedPanel panel = new RoundedPanel(Theme.RADIUS_CARD, Theme.card(), Theme.border()).elevated(2f);
                panel.setLayout(new BorderLayout());
                panel.setBorder(new EmptyBorder(18, 18, 18, 18));
                JLabel value = label("0 / 0", 24, Font.BOLD, Theme.text());
                typeStats.put(type, value);
                panel.add(label(type.toString(), 13, Font.BOLD, Theme.muted()), BorderLayout.NORTH);
                panel.add(value, BorderLayout.CENTER);
                panel.add(label(I18n.t("rooms.availableOccupied"), 11, Font.PLAIN, Theme.muted()), BorderLayout.SOUTH);
                summary.add(panel);
            }

            JPanel toolbar = transparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            JButton add = primaryButton(I18n.t("button.addRoom"));
            JButton bulk = ghostButton(I18n.t("button.bulkAdd"));
            JButton refresh = ghostButton(I18n.t("button.refresh"));
            add.addActionListener(event -> openRoomDialog(null));
            bulk.addActionListener(event -> openBulkRoomDialog());
            refresh.addActionListener(event -> refresh());
            typeFilter.addActionListener(event -> refresh());
            statusFilter.addActionListener(event -> refresh());
            toolbar.add(add);
            toolbar.add(bulk);
            toolbar.add(typeFilter);
            toolbar.add(statusFilter);
            toolbar.add(refresh);

            add(summary);
            add(card(I18n.t("rooms.inventory"), tableWithToolbar(table, toolbar)));
        }

        @Override
        public void refresh() {
            RoomType selectedType = selectedRoomType();
            Boolean available = selectedAvailability();
            runInBackground(() -> new RoomsData(
                    roomService.search(selectedType, available),
                    roomService.getStatsByType()
            ), data -> {
                tableModel.setRows(data.rooms());
                for (RoomType type : RoomType.values()) {
                    typeStats.get(type).setText("0 / 0");
                }
                for (RoomTypeStats stats : data.stats()) {
                    typeStats.get(stats.type()).setText(stats.availableRooms() + " / " + stats.occupiedRooms());
                }
            }, "Unable to refresh rooms");
        }

        private RoomType selectedRoomType() {
            int index = typeFilter.getSelectedIndex();
            return index <= 0 ? null : RoomType.values()[index - 1];
        }

        private Boolean selectedAvailability() {
            return switch ((String) statusFilter.getSelectedItem()) {
                case "Available", "E lirë", "Dostupno" -> true;
                case "Occupied", "E zënë", "Zauzeto" -> false;
                default -> null;
            };
        }

        private void openRoomDialog(Room room) {
            JTextField numberField = new JTextField(room == null ? "" : room.getRoomNumber());
            JComboBox<RoomType> typeField = new JComboBox<>(RoomType.values());
            if (room != null) {
                typeField.setSelectedItem(room.getType());
            }
            JTextField priceField = new JTextField(room == null ? "" : room.getPrice().toString());
            JCheckBox availableField = new JCheckBox(I18n.t("rooms.available"), room == null || room.isAvailable());

            JPanel form = formPanel();
            addFormRow(form, 0, I18n.t("field.roomNumber"), numberField);
            addFormRow(form, 1, I18n.t("field.type"), typeField);
            addFormRow(form, 2, I18n.t("field.nightlyRate"), priceField);
            addFormRow(form, 3, I18n.t("field.status"), availableField);
            if (!modernConfirm(form, room == null ? I18n.t("rooms.addTitle") : I18n.t("rooms.updateTitle"))) {
                return;
            }

            try {
                Room saved = room == null ? new Room() : room;
                saved.setRoomNumber(numberField.getText().trim());
                saved.setType((RoomType) typeField.getSelectedItem());
                saved.setPrice(new BigDecimal(priceField.getText().trim()));
                saved.setAvailable(availableField.isSelected());
                runInBackground(() -> {
                    roomService.save(saved);
                    return null;
                }, () -> {
                    showToast("Room saved", false);
                    refresh();
                }, "Unable to save room");
            } catch (Exception ex) {
                UiUtils.showError(this, ex);
            }
        }

        private void openBulkRoomDialog() {
            JTextField startField = new JTextField("101");
            JTextField countField = new JTextField("12");
            JComboBox<RoomType> typeField = new JComboBox<>(RoomType.values());
            JTextField priceField = new JTextField("120.00");
            JCheckBox availableField = new JCheckBox(I18n.t("rooms.available"), true);
            JPanel form = formPanel();
            addFormRow(form, 0, I18n.t("field.startingRoom"), startField);
            addFormRow(form, 1, I18n.t("field.roomCount"), countField);
            addFormRow(form, 2, I18n.t("field.type"), typeField);
            addFormRow(form, 3, I18n.t("field.nightlyRate"), priceField);
            addFormRow(form, 4, I18n.t("field.initialStatus"), availableField);
            if (!modernConfirm(form, I18n.t("rooms.bulkTitle"))) {
                return;
            }

            try {
                String start = startField.getText().trim();
                int count = Integer.parseInt(countField.getText().trim());
                RoomType type = (RoomType) typeField.getSelectedItem();
                BigDecimal price = new BigDecimal(priceField.getText().trim());
                boolean available = availableField.isSelected();
                runInBackground(() -> {
                    roomService.createRoomRange(start, count, type, price, available);
                    return null;
                }, () -> {
                    showToast("Rooms created", false);
                    refresh();
                }, "Unable to create rooms");
            } catch (Exception ex) {
                UiUtils.showError(this, ex);
            }
        }
    }

    private class BookingsPage extends PagePanel implements Refreshable {
        private final BookingsTableModel tableModel = new BookingsTableModel();
        private final JTable table = styledTable(tableModel);
        private final TableRowSorter<BookingsTableModel> sorter = new TableRowSorter<>(tableModel);
        private final JTextField search = new JTextField();
        private final JComboBox<String> status = new JComboBox<>(new String[]{I18n.t("bookings.all"), I18n.t("status.inStay"), I18n.t("status.upcoming"), I18n.t("status.completed")});
        private final JLabel pageLabel = label("Page 1", 12, Font.BOLD, Theme.muted());
        private final int pageSize = 20;
        private List<Booking> allRows = new ArrayList<>();
        private int page = 1;

        BookingsPage() {
            table.setRowSorter(sorter);
            search.putClientProperty("JTextField.placeholderText", I18n.t("bookings.search"));
            search.getDocument().addDocumentListener((SimpleDocumentListener) event -> applyFilters());
            status.addActionListener(event -> applyFilters());

            JPanel toolbar = transparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            JButton book = primaryButton(I18n.t("button.bookRoom"));
            JButton checkout = ghostButton(I18n.t("button.checkout"));
            JButton prev = ghostButton(I18n.t("button.previous"));
            JButton next = ghostButton(I18n.t("button.next"));
            JButton refresh = ghostButton(I18n.t("button.refresh"));
            book.addActionListener(event -> openBookingDialog());
            checkout.addActionListener(event -> completeSelectedBooking());
            refresh.addActionListener(event -> refresh());
            prev.addActionListener(event -> {
                page = Math.max(1, page - 1);
                updatePage();
            });
            next.addActionListener(event -> {
                int maxPage = Math.max(1, (int) Math.ceil(allRows.size() / (double) pageSize));
                page = Math.min(maxPage, page + 1);
                updatePage();
            });
            toolbar.add(book);
            toolbar.add(checkout);
            toolbar.add(search);
            toolbar.add(status);
            toolbar.add(prev);
            toolbar.add(pageLabel);
            toolbar.add(next);
            toolbar.add(refresh);
            add(card(I18n.t("bookings.title"), tableWithToolbar(table, toolbar)));
        }

        @Override
        public void refresh() {
            runInBackground(() -> bookingService.findAll(), bookings -> {
                allRows = bookings;
                page = Math.min(page, Math.max(1, (int) Math.ceil(allRows.size() / (double) pageSize)));
                updatePage();
                applyFilters();
            }, "Unable to refresh bookings");
        }

        private void updatePage() {
            int maxPage = Math.max(1, (int) Math.ceil(allRows.size() / (double) pageSize));
            page = Math.max(1, Math.min(page, maxPage));
            int from = Math.min((page - 1) * pageSize, allRows.size());
            int to = Math.min(from + pageSize, allRows.size());
            tableModel.setRows(new ArrayList<>(allRows.subList(from, to)));
            pageLabel.setText("Page " + page + " / " + maxPage);
            applyFilters();
        }

        private void applyFilters() {
            List<RowFilter<BookingsTableModel, Integer>> filters = new ArrayList<>();
            String query = search.getText().trim();
            if (!query.isEmpty()) {
                filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
            }
            String selected = (String) status.getSelectedItem();
            if (I18n.t("status.inStay").equals(selected)) {
                filters.add(RowFilter.regexFilter(java.util.regex.Pattern.quote(I18n.t("status.inStay")), 5));
            } else if (I18n.t("status.upcoming").equals(selected)) {
                filters.add(RowFilter.regexFilter(java.util.regex.Pattern.quote(I18n.t("status.upcoming")), 5));
            } else if (I18n.t("status.completed").equals(selected)) {
                filters.add(RowFilter.regexFilter(java.util.regex.Pattern.quote(I18n.t("status.completed")), 5));
            }
            sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        }

        private void completeSelectedBooking() {
            int row = table.getSelectedRow();
            if (row >= 0) {
                row = table.convertRowIndexToModel(row);
            }
            Booking booking = tableModel.getAt(row);
            if (booking == null || !UiUtils.confirm(this, "Mark selected booking as checked out?")) {
                return;
            }
            runInBackground(() -> {
                bookingService.completeBooking(booking.getId());
                return null;
            }, () -> {
                showToast("Booking checked out", false);
                refresh();
            }, "Unable to check out booking");
        }

        private void openBookingDialog() {
            runInBackground(() -> roomService.findAll(), rooms -> {
                if (rooms.isEmpty()) {
                    showToast("No rooms have been added yet", true);
                    return;
                }
                openBookingDialog(rooms);
            }, "Unable to load rooms");
        }

        private void openBookingDialog(List<Room> rooms) {
            try {
                Set<RoomType> availableTypes = new LinkedHashSet<>();
                for (Room room : rooms) {
                    availableTypes.add(room.getType());
                }
                JComboBox<RoomType> roomTypeBox = new JComboBox<>(availableTypes.toArray(new RoomType[0]));
                JTextField guestSearchField = new JTextField();
                DefaultListModel<Guest> guestListModel = new DefaultListModel<>();
                JList<Guest> guestList = new JList<>(guestListModel);
                guestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                guestList.setVisibleRowCount(4);
                JButton searchGuestButton = ghostButton(I18n.t("button.search"));
                JButton newGuestButton = ghostButton(I18n.t("button.newGuest"));
                JTextField guestNameField = new JTextField();
                JTextField guestPhoneField = new JTextField();
                JTextField guestEmailField = new JTextField();
                DefaultListModel<Guest> additionalGuestModel = new DefaultListModel<>();
                JList<Guest> additionalGuestList = new JList<>(additionalGuestModel);
                additionalGuestList.setVisibleRowCount(3);
                JTextField additionalGuestNameField = new JTextField();
                JTextField additionalGuestPhoneField = new JTextField();
                JTextField additionalGuestEmailField = new JTextField();
                JButton addAdditionalGuestButton = ghostButton(I18n.t("button.addGuest"));
                JButton removeAdditionalGuestButton = ghostButton(I18n.t("button.remove"));
                JTextField checkInField = new JTextField(LocalDate.now().toString());
                JTextField checkOutField = new JTextField(LocalDate.now().plusDays(1).toString());

                Runnable loadGuests = () -> {
                    String query = guestSearchField.getText();
                    runInBackground(() -> guestService.search(query), guests -> {
                        guestListModel.clear();
                        for (Guest guest : guests) {
                            guestListModel.addElement(guest);
                        }
                    }, "Unable to search guests");
                };
                loadGuests.run();

                searchGuestButton.addActionListener(event -> loadGuests.run());
                newGuestButton.addActionListener(event -> {
                    guestList.clearSelection();
                    guestNameField.setText("");
                    guestPhoneField.setText("");
                    guestEmailField.setText("");
                });
                guestList.addListSelectionListener(event -> {
                    if (!event.getValueIsAdjusting() && guestList.getSelectedValue() != null) {
                        Guest guest = guestList.getSelectedValue();
                        guestNameField.setText(guest.getName());
                        guestPhoneField.setText(guest.getPhone());
                        guestEmailField.setText(guest.getEmail());
                    }
                });
                addAdditionalGuestButton.addActionListener(event -> {
                    Guest guest = new Guest();
                    guest.setName(additionalGuestNameField.getText().trim());
                    guest.setPhone(additionalGuestPhoneField.getText().trim());
                    guest.setEmail(additionalGuestEmailField.getText().trim());
                    if (guest.getName().isBlank() || guest.getPhone().isBlank() || guest.getEmail().isBlank()) {
                        showToast("Additional guest fields are required", true);
                        return;
                    }
                    additionalGuestModel.addElement(guest);
                    additionalGuestNameField.setText("");
                    additionalGuestPhoneField.setText("");
                    additionalGuestEmailField.setText("");
                });
                removeAdditionalGuestButton.addActionListener(event -> {
                    int selectedIndex = additionalGuestList.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        additionalGuestModel.remove(selectedIndex);
                    }
                });

                JPanel guestSearchPanel = transparentPanel(new BorderLayout(8, 8));
                JPanel guestSearchActions = transparentPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                guestSearchActions.add(searchGuestButton);
                guestSearchActions.add(newGuestButton);
                guestSearchPanel.add(guestSearchField, BorderLayout.CENTER);
                guestSearchPanel.add(guestSearchActions, BorderLayout.EAST);

                JScrollPane guestListPane = new JScrollPane(guestList);
                guestListPane.setPreferredSize(new Dimension(380, 100));
                JScrollPane additionalGuestPane = new JScrollPane(additionalGuestList);
                additionalGuestPane.setPreferredSize(new Dimension(380, 78));
                JPanel additionalGuestFields = formPanel();
                addFormRow(additionalGuestFields, 0, I18n.t("field.name"), additionalGuestNameField);
                addFormRow(additionalGuestFields, 1, I18n.t("field.phone"), additionalGuestPhoneField);
                addFormRow(additionalGuestFields, 2, I18n.t("field.email"), additionalGuestEmailField);
                JPanel additionalGuestPanel = transparentPanel(new BorderLayout(8, 8));
                JPanel additionalActions = transparentPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                additionalActions.add(addAdditionalGuestButton);
                additionalActions.add(removeAdditionalGuestButton);
                additionalGuestPanel.add(additionalActions, BorderLayout.NORTH);
                additionalGuestPanel.add(additionalGuestFields, BorderLayout.CENTER);
                additionalGuestPanel.add(additionalGuestPane, BorderLayout.SOUTH);

                JPanel form = formPanel();
                addFormRow(form, 0, I18n.t("field.type"), roomTypeBox);
                addFormRow(form, 1, I18n.t("bookings.findGuest"), guestSearchPanel);
                addFormRow(form, 2, I18n.t("bookings.guestMatches"), guestListPane);
                addFormRow(form, 3, I18n.t("bookings.mainGuestName"), guestNameField);
                addFormRow(form, 4, I18n.t("field.phone"), guestPhoneField);
                addFormRow(form, 5, I18n.t("field.email"), guestEmailField);
                addFormRow(form, 6, I18n.t("bookings.additionalGuests"), additionalGuestPanel);
                addFormRow(form, 7, I18n.t("field.checkIn"), checkInField);
                addFormRow(form, 8, I18n.t("field.checkOut"), checkOutField);
                if (!modernConfirm(form, I18n.t("bookings.createTitle"))) {
                    return;
                }

                RoomType selectedRoomType = (RoomType) roomTypeBox.getSelectedItem();
                if (selectedRoomType == null) {
                    throw new IllegalArgumentException("Room type is required.");
                }
                LocalDate checkIn = LocalDate.parse(checkInField.getText().trim());
                LocalDate checkOut = LocalDate.parse(checkOutField.getText().trim());
                Booking booking = new Booking();
                booking.setCheckIn(checkIn);
                booking.setCheckOut(checkOut);
                Guest selectedGuest = guestList.getSelectedValue();
                Guest bookingGuest = selectedGuest == null
                        ? new Guest()
                        : new Guest(selectedGuest.getId(), selectedGuest.getName(), selectedGuest.getPhone(), selectedGuest.getEmail());
                bookingGuest.setName(guestNameField.getText().trim());
                bookingGuest.setPhone(guestPhoneField.getText().trim());
                bookingGuest.setEmail(guestEmailField.getText().trim());
                List<Guest> additionalGuests = guestsFromModel(additionalGuestModel);
                runInBackground(() -> {
                    Room selectedRoom = roomService.findAvailableRooms(checkIn, checkOut).stream()
                            .filter(room -> room.getType() == selectedRoomType)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("No " + selectedRoomType + " rooms are available for those dates."));
                    booking.setRoomId(selectedRoom.getId());
                    bookingService.bookRoomWithGuests(booking, bookingGuest, additionalGuests);
                    return null;
                }, () -> {
                    showToast("Reservation created", false);
                    refresh();
                }, "Unable to create reservation");
            } catch (DateTimeParseException ex) {
                UiUtils.showError(this, new IllegalArgumentException("Dates must use yyyy-mm-dd format."));
            } catch (Exception ex) {
                UiUtils.showError(this, ex);
            }
        }
    }

    private class GuestsPage extends PagePanel implements Refreshable {
        private final GuestTableModel tableModel = new GuestTableModel();
        private final JTable table = styledTable(tableModel);
        private final JTextField search = new JTextField();
        private final TableRowSorter<GuestTableModel> sorter = new TableRowSorter<>(tableModel);

        GuestsPage() {
            table.setRowSorter(sorter);
            search.putClientProperty("JTextField.placeholderText", I18n.t("guests.search"));
            search.getDocument().addDocumentListener((SimpleDocumentListener) event -> sorter.setRowFilter(
                    search.getText().isBlank() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(search.getText()))
            ));
            JPanel toolbar = transparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            JButton add = primaryButton(I18n.t("button.addGuest"));
            JButton edit = ghostButton(I18n.t("button.update"));
            JButton delete = ghostButton(I18n.t("button.delete"));
            JButton refresh = ghostButton(I18n.t("button.refresh"));
            add.addActionListener(event -> openGuestDialog(null));
            edit.addActionListener(event -> editSelectedGuest());
            delete.addActionListener(event -> deleteSelectedGuest());
            refresh.addActionListener(event -> refresh());
            toolbar.add(add);
            toolbar.add(edit);
            toolbar.add(delete);
            toolbar.add(search);
            toolbar.add(refresh);
            add(card(I18n.t("guests.title"), tableWithToolbar(table, toolbar)));
        }

        @Override
        public void refresh() {
            runInBackground(() -> guestService.findAll(), tableModel::setRows, "Unable to refresh guests");
        }

        private void editSelectedGuest() {
            int row = table.getSelectedRow();
            if (row >= 0) {
                row = table.convertRowIndexToModel(row);
            }
            Guest guest = tableModel.getAt(row);
            if (guest != null) {
                openGuestDialog(guest);
            }
        }

        private void deleteSelectedGuest() {
            int row = table.getSelectedRow();
            if (row >= 0) {
                row = table.convertRowIndexToModel(row);
            }
            Guest guest = tableModel.getAt(row);
            if (guest == null || !UiUtils.confirm(this, "Delete selected guest?")) {
                return;
            }
            runInBackground(() -> {
                guestService.delete(guest.getId());
                return null;
            }, () -> {
                showToast("Guest deleted", false);
                refresh();
            }, "Unable to delete guest");
        }

        private void openGuestDialog(Guest guest) {
            JTextField nameField = new JTextField(guest == null ? "" : guest.getName());
            JTextField phoneField = new JTextField(guest == null ? "" : guest.getPhone());
            JTextField emailField = new JTextField(guest == null ? "" : guest.getEmail());
            JPanel form = formPanel();
            addFormRow(form, 0, I18n.t("field.name"), nameField);
            addFormRow(form, 1, I18n.t("field.phone"), phoneField);
            addFormRow(form, 2, I18n.t("field.email"), emailField);
            if (!modernConfirm(form, guest == null ? I18n.t("button.addGuest") : I18n.t("button.update"))) {
                return;
            }
            try {
                Guest saved = guest == null ? new Guest() : guest;
                saved.setName(nameField.getText().trim());
                saved.setPhone(phoneField.getText().trim());
                saved.setEmail(emailField.getText().trim());
                runInBackground(() -> {
                    guestService.save(saved);
                    return null;
                }, () -> {
                    showToast("Guest saved", false);
                    refresh();
                }, "Unable to save guest");
            } catch (Exception ex) {
                UiUtils.showError(this, ex);
            }
        }
    }

    private class PaymentsPage extends PagePanel implements Refreshable {
        private final MetricCard gross = new MetricCard(I18n.t("payments.projectedGross"), "$0", "+0%", IconGlyph.PAYMENTS, Theme.green());
        private final MetricCard pending = new MetricCard(I18n.t("payments.pending"), "$0", "-4%", IconGlyph.CLOCK, Theme.muted());
        private final MetricCard settled = new MetricCard(I18n.t("payments.settled"), "$0", "+7%", IconGlyph.CHECK, Theme.blue());
        private final MetricCard refunds = new MetricCard(I18n.t("payments.refundRisk"), "$0", "-1%", IconGlyph.ALERT, Theme.blue());

        PaymentsPage() {
            JPanel metrics = grid(4, 14);
            metrics.add(gross);
            metrics.add(pending);
            metrics.add(settled);
            metrics.add(refunds);
            add(metrics);
            add(card(I18n.t("payments.workflow"), new EmptyState(I18n.t("payments.emptyTitle"), I18n.t("payments.emptyMessage"))));
        }

        @Override
        public void refresh() {
            runInBackground(() -> estimateRevenue(), value -> {
                gross.setValue(currency(value), "+24%");
                pending.setValue(currency(value.multiply(BigDecimal.valueOf(0.18))), "-4%");
                settled.setValue(currency(value.multiply(BigDecimal.valueOf(0.42))), "+7%");
                refunds.setValue(currency(value.multiply(BigDecimal.valueOf(0.02))), "-1%");
            }, "Unable to refresh payments");
        }
    }

    private class WorkersPage extends PagePanel implements Refreshable {
        private final WorkerTableModel tableModel = new WorkerTableModel();
        private final JTable table = styledTable(tableModel);

        WorkersPage() {
            JPanel grid = grid(3, 14);
            grid.add(new TeamCard(I18n.t("workers.frontDesk"), "7 members", "92% response SLA"));
            grid.add(new TeamCard(I18n.t("workers.housekeeping"), "14 members", "38 rooms queued"));
            grid.add(new TeamCard(I18n.t("workers.management"), "4 members", "3 approvals"));

            JPanel toolbar = transparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            JButton add = primaryButton(I18n.t("button.addWorker"));
            JButton edit = ghostButton(I18n.t("button.update"));
            JButton delete = ghostButton(I18n.t("button.delete"));
            JButton reset = ghostButton(I18n.t("button.resetPassword"));
            add.addActionListener(event -> openWorkerDialog(null));
            edit.addActionListener(event -> editSelectedWorker());
            delete.addActionListener(event -> removeSelectedWorker());
            reset.addActionListener(event -> resetSelectedPassword());
            toolbar.add(add);
            toolbar.add(edit);
            toolbar.add(delete);
            toolbar.add(reset);

            add(grid);
            add(card(I18n.t("workers.title"), tableWithToolbar(table, toolbar)));
        }

        @Override
        public void refresh() {
            runInBackground(() -> authService.users(), tableModel::setRows, "Unable to refresh workers");
        }

        private void editSelectedWorker() {
            UserAccount worker = selectedWorker();
            if (worker != null) {
                openWorkerDialog(worker);
            }
        }

        private void removeSelectedWorker() {
            UserAccount worker = selectedWorker();
            if (worker == null || worker.getRole() == UserRole.ADMIN || !UiUtils.confirm(this, "Remove selected worker?")) {
                return;
            }
            runInBackground(() -> {
                authService.removeWorker(worker.getId());
                return null;
            }, () -> {
                showToast("Worker removed", false);
                refresh();
            }, "Unable to remove worker");
        }

        private void resetSelectedPassword() {
            UserAccount worker = selectedWorker();
            if (worker == null) {
                return;
            }
            UserAccount saved = new UserAccount(worker.getId(), worker.getName(), worker.getEmail(), "changeme123",
                    worker.getRole(), worker.isActive(), worker.getLastActive());
            runInBackground(() -> {
                authService.saveWorker(saved);
                return null;
            }, () -> {
                showToast("Password reset to changeme123", false);
                refresh();
            }, "Unable to reset password");
        }

        private UserAccount selectedWorker() {
            int row = table.getSelectedRow();
            if (row >= 0) {
                row = table.convertRowIndexToModel(row);
            }
            return tableModel.getAt(row);
        }

        private void openWorkerDialog(UserAccount worker) {
            JTextField name = new JTextField(worker == null ? "" : worker.getName());
            JTextField email = new JTextField(worker == null ? "" : worker.getEmail());
            JTextField password = new JTextField(worker == null ? "changeme123" : worker.getPassword());
            JComboBox<UserRole> role = new JComboBox<>(UserRole.values());
            if (worker != null) {
                role.setSelectedItem(worker.getRole());
            }
            JCheckBox active = new JCheckBox("Active", worker == null || worker.isActive());
            JPanel form = formPanel();
            addFormRow(form, 0, I18n.t("field.name"), name);
            addFormRow(form, 1, I18n.t("field.email"), email);
            addFormRow(form, 2, I18n.t("field.password"), password);
            addFormRow(form, 3, I18n.t("field.role"), role);
            addFormRow(form, 4, I18n.t("field.status"), active);
            if (!modernConfirm(form, worker == null ? I18n.t("button.addWorker") : I18n.t("button.update"))) {
                return;
            }
            try {
                UserAccount saved = new UserAccount(
                        worker == null ? "" : worker.getId(),
                        name.getText().trim(),
                        email.getText().trim(),
                        password.getText().trim(),
                        (UserRole) role.getSelectedItem(),
                        active.isSelected(),
                        worker == null ? LocalDateTime.now() : worker.getLastActive()
                );
                runInBackground(() -> {
                    authService.saveWorker(saved);
                    return null;
                }, () -> {
                    showToast("Worker saved", false);
                    refresh();
                }, "Unable to save worker");
            } catch (Exception ex) {
                UiUtils.showError(this, ex);
            }
        }
    }

    private class SettingsPage extends PagePanel {
        SettingsPage() {
            RoundedPanel settings = new RoundedPanel(Theme.RADIUS_CARD, Theme.card(), Theme.border()).elevated(2f);
            settings.setLayout(new GridLayout(0, 2, 18, 18));
            settings.setBorder(new EmptyBorder(22, 22, 22, 22));
            JButton theme = primaryButton(I18n.t("theme.switch"));
            theme.addActionListener(event -> toggleTheme());
            settings.add(label(I18n.t("settings.appearance"), 18, Font.BOLD, Theme.text()));
            settings.add(theme);
            settings.add(label(I18n.t("settings.language"), 18, Font.BOLD, Theme.text()));
            JComboBox<Language> language = new JComboBox<>(Language.values());
            language.setSelectedItem(I18n.language());
            language.addActionListener(event -> switchLanguage((Language) language.getSelectedItem()));
            settings.add(language);
            settings.add(label(I18n.t("settings.sidebar"), 18, Font.BOLD, Theme.text()));
            JButton collapse = ghostButton(I18n.t("sidebar.toggle"));
            collapse.addActionListener(event -> toggleSidebar());
            settings.add(collapse);
            settings.add(label(I18n.t("settings.hotel"), 18, Font.BOLD, Theme.text()));
            settings.add(label(AppBrand.HOTEL_NAME, 14, Font.PLAIN, Theme.muted()));
            add(card(I18n.t("settings.title"), settings));
        }
    }

    private interface Refreshable {
        void refresh();
    }

    private record DashboardData(HotelStats stats, int guestCount, List<Booking> bookings, BigDecimal estimatedRevenue) {
    }

    private record AnalyticsData(HotelStats stats, List<Booking> bookings, BigDecimal revenue) {
    }

    private record RoomsData(List<Room> rooms, List<RoomTypeStats> stats) {
    }

    @FunctionalInterface
    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent event);

        @Override
        default void insertUpdate(javax.swing.event.DocumentEvent event) {
            update(event);
        }

        @Override
        default void removeUpdate(javax.swing.event.DocumentEvent event) {
            update(event);
        }

        @Override
        default void changedUpdate(javax.swing.event.DocumentEvent event) {
            update(event);
        }
    }

    private static class PagePanel extends JPanel {
        PagePanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(24, 28, 32, 28));
        }

        @Override
        public Component add(Component component) {
            if (getComponentCount() > 0) {
                super.add(Box.createVerticalStrut(14));
            }
            return super.add(component);
        }
    }

    private static class Theme {
        static final int RADIUS_SMALL = 10;
        static final int RADIUS_BUTTON = 14;
        static final int RADIUS_CARD = 18;
        static final int RADIUS_FLOATING = 24;
        static final int MOTION_FAST = 140;
        static final int MOTION_BASE = 220;
        static final int MOTION_SLOW = 320;
        static final int SPACE_1 = 6;
        static final int SPACE_2 = 10;
        static final int SPACE_3 = 14;
        static final int SPACE_4 = 20;
        static final int SPACE_5 = 28;
        private static boolean dark = true;

        static Color bg() {
            return dark ? new Color(18, 18, 20) : new Color(246, 246, 248);
        }

        static Color sidebar() {
            return dark ? new Color(24, 24, 27) : new Color(255, 255, 255);
        }

        static Color card() {
            return dark ? new Color(31, 31, 35) : Color.WHITE;
        }

        static Color cardAlt() {
            return dark ? new Color(40, 40, 44) : new Color(241, 242, 245);
        }

        static Color text() {
            return dark ? new Color(235, 235, 238) : new Color(28, 28, 30);
        }

        static Color muted() {
            return dark ? new Color(156, 156, 162) : new Color(102, 102, 109);
        }

        static Color border() {
            return dark ? new Color(58, 58, 63) : new Color(224, 224, 228);
        }

        static Color topStart() {
            return dark ? new Color(22, 22, 24) : new Color(255, 255, 255);
        }

        static Color topEnd() {
            return dark ? new Color(26, 26, 29) : new Color(247, 247, 249);
        }

        static Color blue() {
            return new Color(82, 145, 255);
        }

        static Color purple() {
            return blue();
        }

        static Color green() {
            return new Color(77, 181, 126);
        }

        static Color gold() {
            return muted();
        }

        static Color danger() {
            return new Color(232, 92, 106);
        }

        static int radius() {
            return RADIUS_CARD;
        }

        static int arc(int radius) {
            return radius * 2;
        }

        static int arc() {
            return arc(radius());
        }

        static boolean reduceMotion() {
            return Boolean.getBoolean("hotel.reduceMotion")
                    || "1".equals(System.getenv("REDUCE_MOTION"))
                    || "true".equalsIgnoreCase(System.getenv("REDUCE_MOTION"));
        }

        static float easeOutCubic(float t) {
            float p = 1f - Math.max(0f, Math.min(1f, t));
            return 1f - p * p * p;
        }

        static Shape continuousRect(float x, float y, float width, float height, float radius) {
            float r = Math.min(radius, Math.min(width, height) / 2f);
            return new RoundRectangle2D.Float(x, y, width, height, r * 2f, r * 2f);
        }

        static Font font(float size, int style) {
            Font inter = new Font("Inter", style, Math.round(size));
            if (!"Dialog".equals(inter.getFamily())) {
                return inter;
            }
            return new Font("-apple-system", style, Math.round(size));
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private final Color border;
        private float elevation;

        RoundedPanel(int radius, Color fill, Color border) {
            this.radius = radius;
            this.fill = fill;
            this.border = border;
            setOpaque(false);
        }

        RoundedPanel elevated(float elevation) {
            this.elevation = elevation;
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            Shape shape = radius == 0
                    ? new java.awt.Rectangle(0, 0, getWidth(), getHeight())
                    : Theme.continuousRect(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, radius);
            if (elevation > 0 && radius > 0) {
                g.setColor(new Color(0, 0, 0, Theme.dark ? 80 : 24));
                g.translate(0, Math.round(elevation));
                g.fill(shape);
                g.translate(0, -Math.round(elevation));
            }
            g.setColor(fill);
            g.fill(shape);
            g.setColor(border);
            g.draw(shape);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class HoverPanel extends RoundedPanel {
        private boolean hovered;
        private float hoverProgress;
        private Timer hoverTimer;

        HoverPanel(int radius, Color fill, Color border) {
            super(radius, fill, border);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    animateHover(1f);
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    animateHover(0f);
                }
            });
        }

        private void animateHover(float target) {
            if (Theme.reduceMotion()) {
                hoverProgress = target;
                repaint();
                return;
            }
            if (hoverTimer != null && hoverTimer.isRunning()) {
                hoverTimer.stop();
            }
            float start = hoverProgress;
            long started = System.currentTimeMillis();
            hoverTimer = new Timer(16, event -> {
                float elapsed = (System.currentTimeMillis() - started) / (float) Theme.MOTION_BASE;
                float eased = Theme.easeOutCubic(elapsed);
                hoverProgress = start + (target - start) * eased;
                repaint();
                if (elapsed >= 1f) {
                    hoverProgress = target;
                    hoverTimer.stop();
                }
            });
            hoverTimer.start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (hovered || hoverProgress > 0.01f) {
                Graphics2D g = graphics2(graphics);
                int alpha = Math.round((Theme.dark ? 22 : 96) * hoverProgress);
                g.setColor(new Color(255, 255, 255, alpha));
                g.draw(Theme.continuousRect(1, 1, getWidth() - 3, getHeight() - 3, Theme.RADIUS_CARD));
                g.dispose();
            }
        }
    }

    private static class AnimatedPageHost extends JPanel {
        private float alpha = 1f;
        private Timer timer;

        AnimatedPageHost(CardLayout layout) {
            super(layout);
        }

        void beginTransition() {
            alpha = Theme.reduceMotion() ? 1f : 0f;
        }

        void playIn() {
            if (Theme.reduceMotion()) {
                alpha = 1f;
                repaint();
                return;
            }
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            long started = System.currentTimeMillis();
            timer = new Timer(16, event -> {
                float elapsed = (System.currentTimeMillis() - started) / (float) Theme.MOTION_SLOW;
                alpha = Theme.easeOutCubic(elapsed);
                repaint();
                if (elapsed >= 1f) {
                    alpha = 1f;
                    timer.stop();
                }
            });
            timer.start();
        }

        @Override
        protected void paintChildren(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
            super.paintChildren(g);
            g.dispose();
        }
    }

    private static class MetricCard extends HoverPanel {
        private final JLabel value = label("0", 28, Font.BOLD, Theme.text());
        private final JLabel trend = label("+0%", 12, Font.BOLD, Theme.green());
        private final Sparkline sparkline;

        MetricCard(String title, String initialValue, String initialTrend, IconGlyph icon, Color accent) {
            super(Theme.RADIUS_CARD, Theme.card(), Theme.border());
            setLayout(new BorderLayout(12, 12));
            setBorder(new EmptyBorder(18, 18, 16, 18));
            setPreferredSize(new Dimension(210, 150));
            JLabel titleLabel = label(title, 12, Font.BOLD, Theme.muted());
            JLabel iconLabel = new JLabel(new LineIcon(icon, 22, accent));
            JPanel head = transparentPanel(new BorderLayout());
            head.add(titleLabel, BorderLayout.CENTER);
            head.add(iconLabel, BorderLayout.EAST);
            sparkline = new Sparkline(accent);
            JPanel bottom = transparentPanel(new BorderLayout());
            bottom.add(trend, BorderLayout.WEST);
            bottom.add(sparkline, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
            setValue(initialValue, initialTrend);
        }

        void setValue(String text, String trendText) {
            value.setText(text);
            trend.setText(trendText);
            trend.setForeground(trendText.startsWith("-") ? Theme.muted() : Theme.blue());
        }
    }

    private static class AppleButton extends JButton {
        private final boolean primary;
        private float pressProgress;
        private float hoverProgress;
        private Timer motionTimer;

        AppleButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            installStyle();
        }

        AppleButton(javax.swing.Icon icon) {
            super(icon);
            this.primary = false;
            installStyle();
        }

        private void installStyle() {
            setFont(Theme.font(12, Font.BOLD));
            setForeground(primary ? new Color(245, 247, 250) : Theme.text());
            setBackground(primary ? Theme.blue() : Theme.cardAlt());
            setBorder(new EmptyBorder(10, 16, 10, 16));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    animate(hoverProgress, 1f, false);
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    animate(hoverProgress, 0f, false);
                }

                @Override
                public void mousePressed(MouseEvent event) {
                    animate(pressProgress, 1f, true);
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    animate(pressProgress, 0f, true);
                }
            });
        }

        private void animate(float start, float target, boolean press) {
            if (Theme.reduceMotion()) {
                if (press) {
                    pressProgress = target;
                } else {
                    hoverProgress = target;
                }
                repaint();
                return;
            }
            if (motionTimer != null && motionTimer.isRunning()) {
                motionTimer.stop();
            }
            long started = System.currentTimeMillis();
            int duration = press ? Theme.MOTION_FAST : Theme.MOTION_BASE;
            motionTimer = new Timer(16, event -> {
                float elapsed = (System.currentTimeMillis() - started) / (float) duration;
                float eased = Theme.easeOutCubic(elapsed);
                float value = start + (target - start) * eased;
                if (press) {
                    pressProgress = value;
                } else {
                    hoverProgress = value;
                }
                repaint();
                if (elapsed >= 1f) {
                    if (press) {
                        pressProgress = target;
                    } else {
                        hoverProgress = target;
                    }
                    motionTimer.stop();
                }
            });
            motionTimer.start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            float scale = 1f - 0.025f * pressProgress;
            AffineTransform original = g.getTransform();
            g.translate(getWidth() * (1f - scale) / 2f, getHeight() * (1f - scale) / 2f);
            g.scale(scale, scale);
            Color base = primary ? Theme.blue() : Theme.cardAlt();
            int lift = Math.round((primary ? 10 : 8) * hoverProgress);
            Color fill = new Color(
                    Math.min(255, base.getRed() + lift),
                    Math.min(255, base.getGreen() + lift),
                    Math.min(255, base.getBlue() + lift)
            );
            g.setColor(fill);
            g.fill(Theme.continuousRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_BUTTON));
            g.setColor(new Color(255, 255, 255, primary ? 26 : 12));
            g.draw(Theme.continuousRect(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, Theme.RADIUS_BUTTON));
            g.setTransform(original);
            super.paintComponent(graphics);
            g.dispose();
        }
    }

    private static class Sparkline extends JComponent {
        private final Color color;

        Sparkline(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(70, 28));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, Theme.dark ? 10 : 55));
            g.fill(Theme.continuousRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_SMALL));
            g.setColor(Theme.blue());
            Path2D path = new Path2D.Double();
            path.moveTo(8, getHeight() - 8);
            path.curveTo(18, 10, 26, 24, 38, 12);
            path.curveTo(48, 2, 54, 18, 64, 8);
            g.draw(path);
            g.dispose();
        }
    }

    private static class ProgressRing extends JComponent {
        private double progress;

        ProgressRing() {
            setPreferredSize(new Dimension(210, 170));
        }

        void setProgress(double progress) {
            this.progress = Math.max(0, Math.min(1, progress));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            int size = Math.min(getWidth(), getHeight()) - 30;
            int x = (getWidth() - size) / 2;
            int y = 14;
            g.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 50));
            g.draw(new Arc2D.Double(x, y, size, size, 0, 360, Arc2D.OPEN));
            g.setColor(Theme.blue());
            g.draw(new Arc2D.Double(x, y, size, size, 90, -360 * progress, Arc2D.OPEN));
            g.setFont(Theme.font(26, Font.BOLD));
            g.setColor(Theme.text());
            String value = Math.round(progress * 100) + "%";
            int width = g.getFontMetrics().stringWidth(value);
            g.drawString(value, getWidth() / 2 - width / 2, y + size / 2 + 8);
            g.setFont(Theme.font(11, Font.BOLD));
            g.setColor(Theme.muted());
            String label = "OCCUPANCY";
            int labelWidth = g.getFontMetrics().stringWidth(label);
            g.drawString(label, getWidth() / 2 - labelWidth / 2, y + size + 22);
            g.dispose();
        }
    }

    private class ActivityFeed extends RoundedPanel {
        private final JPanel list = transparentPanel();

        ActivityFeed() {
            super(Theme.RADIUS_FLOATING, Theme.card(), Theme.border());
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(18, 18, 18, 18));
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            add(label("Recent activity", 16, Font.BOLD, Theme.text()), BorderLayout.NORTH);
            add(list, BorderLayout.CENTER);
        }

        void setBookings(List<Booking> bookings) {
            list.removeAll();
            bookings.stream().limit(5).forEach(booking -> {
                JLabel row = label("Room " + booking.getRoomNumber() + " - " + booking.getGuestName() + " - " + displayStatus(booking), 12, Font.PLAIN, Theme.muted());
                row.setBorder(new EmptyBorder(8, 0, 8, 0));
                list.add(row);
            });
            if (bookings.isEmpty()) {
                list.add(label("No activity yet", 12, Font.PLAIN, Theme.muted()));
            }
            revalidate();
            repaint();
        }
    }

    private class CalendarWidget extends RoundedPanel {
        private final JPanel days = transparentPanel(new GridLayout(2, 7, 6, 6));

        CalendarWidget() {
            super(Theme.RADIUS_FLOATING, Theme.card(), Theme.border());
            setLayout(new BorderLayout(0, 14));
            setBorder(new EmptyBorder(18, 18, 18, 18));
            add(label("Reservation calendar", 16, Font.BOLD, Theme.text()), BorderLayout.NORTH);
            add(days, BorderLayout.CENTER);
        }

        void setBookings(List<Booking> bookings) {
            days.removeAll();
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 14; i++) {
                LocalDate day = today.plusDays(i);
                long count = bookings.stream().filter(booking -> day.equals(booking.getCheckIn())).count();
                JLabel label = new DayCell("<html><b>" + day.getDayOfMonth() + "</b><br>" + count + " in</html>",
                        count > 0 ? Theme.blue() : Theme.cardAlt());
                label.setForeground(count > 0 ? new Color(245, 247, 250) : Theme.muted());
                label.setBorder(new EmptyBorder(7, 4, 7, 4));
                days.add(label);
            }
            revalidate();
            repaint();
        }
    }

    private static class DayCell extends JLabel {
        private final Color fill;

        DayCell(String text, Color fill) {
            super(text, SwingConstants.CENTER);
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            g.setColor(fill);
            g.fill(Theme.continuousRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_SMALL));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class GradientHero extends JPanel {
        GradientHero() {
            setOpaque(false);
            setPreferredSize(new Dimension(10, 210));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            Shape shape = Theme.continuousRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS_FLOATING);
            g.setPaint(new GradientPaint(0, 0, Theme.cardAlt(), getWidth(), getHeight(), Theme.card()));
            g.fill(shape);
            g.setComposite(AlphaComposite.SrcOver.derive(0.10f));
            g.setColor(Theme.blue());
            g.fill(new Ellipse2D.Double(getWidth() - 250, -90, 310, 310));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class BrandMark extends JComponent {
        BrandMark() {
            setPreferredSize(new Dimension(42, 42));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            g.setPaint(new GradientPaint(0, 0, Theme.blue(), getWidth(), getHeight(), Theme.blue().darker()));
            g.fill(Theme.continuousRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_BUTTON));
            g.setColor(new Color(245, 247, 250));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(13, 28, 13, 15);
            g.drawLine(13, 15, 28, 15);
            g.drawLine(28, 15, 28, 28);
            g.drawLine(13, 22, 28, 22);
            g.dispose();
        }
    }

    private class NavButton extends JButton {
        private final String labelKey;
        private final IconGlyph glyph;
        private final String subtitleKey;
        private boolean active;
        private boolean collapsed;

        NavButton(String labelKey, IconGlyph glyph, String subtitleKey) {
            this.labelKey = labelKey;
            this.glyph = glyph;
            this.subtitleKey = subtitleKey;
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setHorizontalAlignment(SwingConstants.LEFT);
            setPreferredSize(new Dimension(230, 50));
            setMaximumSize(new Dimension(240, 50));
            setBorder(new EmptyBorder(0, 14, 0, 12));
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
            setPreferredSize(new Dimension(collapsed ? 54 : 230, 50));
            setMaximumSize(new Dimension(collapsed ? 54 : 240, 50));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            if (active || getModel().isRollover()) {
                g.setColor(active ? new Color(86, 154, 255, 36) : new Color(255, 255, 255, Theme.dark ? 14 : 110));
                g.fill(Theme.continuousRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_BUTTON));
            }
            if (active) {
                g.setColor(Theme.blue());
                g.fill(Theme.continuousRect(0, 12, 4, getHeight() - 24, 2));
            }
            new LineIcon(glyph, 19, active ? Theme.blue() : Theme.muted()).paintIcon(this, g, 16, 15);
            if (!collapsed) {
                g.setFont(Theme.font(13, Font.BOLD));
                g.setColor(active ? Theme.text() : Theme.muted());
                g.drawString(I18n.t(labelKey), 48, 22);
                g.setFont(Theme.font(10, Font.PLAIN));
                g.setColor(Theme.muted());
                g.drawString(I18n.t(subtitleKey), 48, 37);
            }
            g.dispose();
        }
    }

    private static class Avatar extends JComponent {
        private final String initials;

        Avatar(String initials) {
            this.initials = initials;
            setPreferredSize(new Dimension(42, 42));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            g.setColor(Theme.cardAlt());
            g.fill(Theme.continuousRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_FLOATING));
            g.setColor(Theme.border());
            g.draw(Theme.continuousRect(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, Theme.RADIUS_FLOATING));
            g.setFont(Theme.font(13, Font.BOLD));
            g.setColor(Theme.text());
            int width = g.getFontMetrics().stringWidth(initials);
            g.drawString(initials, getWidth() / 2 - width / 2, getHeight() / 2 + 5);
            g.dispose();
        }
    }

    private enum IconGlyph {
        DASHBOARD, ROOMS, BOOKINGS, GUESTS, PAYMENTS, STAFF, ANALYTICS, SETTINGS, SEARCH,
        BELL, THEME, COLLAPSE, CHECK, CALENDAR, LOGIN, LOGOUT, CLOCK, ALERT
    }

    private static class LineIcon implements javax.swing.Icon {
        private final IconGlyph glyph;
        private final int size;
        private final Color color;

        LineIcon(IconGlyph glyph, int size, Color color) {
            this.glyph = glyph;
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = graphics2(graphics);
            g.translate(x, y);
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.7f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int s = size;
            switch (glyph) {
                case DASHBOARD -> {
                    g.drawRoundRect(1, 1, s / 2 - 3, s / 2 - 3, 4, 4);
                    g.drawRoundRect(s / 2 + 2, 1, s / 2 - 3, s / 2 - 3, 4, 4);
                    g.drawRoundRect(1, s / 2 + 2, s / 2 - 3, s / 2 - 3, 4, 4);
                    g.drawRoundRect(s / 2 + 2, s / 2 + 2, s / 2 - 3, s / 2 - 3, 4, 4);
                }
                case ROOMS -> {
                    g.drawRoundRect(2, 4, s - 4, s - 6, 4, 4);
                    g.drawLine(2, s / 2, s - 2, s / 2);
                    g.drawLine(s / 2, 4, s / 2, s - 2);
                }
                case BOOKINGS, CALENDAR -> {
                    g.drawRoundRect(2, 4, s - 4, s - 5, 4, 4);
                    g.drawLine(5, 2, 5, 7);
                    g.drawLine(s - 5, 2, s - 5, 7);
                    g.drawLine(2, 9, s - 2, 9);
                }
                case GUESTS, STAFF -> {
                    g.drawOval(5, 2, s - 10, s - 10);
                    g.drawArc(3, s / 2, s - 6, s - 5, 20, 140);
                }
                case PAYMENTS -> {
                    g.drawRoundRect(2, 5, s - 4, s - 8, 5, 5);
                    g.drawLine(4, 10, s - 4, 10);
                    g.drawLine(6, s - 6, s / 2, s - 6);
                }
                case ANALYTICS -> {
                    g.drawLine(3, s - 4, 3, s - 9);
                    g.drawLine(s / 2, s - 4, s / 2, 5);
                    g.drawLine(s - 3, s - 4, s - 3, s / 2);
                }
                case SETTINGS -> {
                    g.drawOval(4, 4, s - 8, s - 8);
                    g.drawOval(s / 2 - 2, s / 2 - 2, 4, 4);
                    g.drawLine(s / 2, 1, s / 2, 5);
                    g.drawLine(s / 2, s - 5, s / 2, s - 1);
                }
                case SEARCH -> {
                    g.drawOval(2, 2, s - 8, s - 8);
                    g.drawLine(s - 6, s - 6, s - 1, s - 1);
                }
                case BELL -> {
                    g.drawArc(4, 3, s - 8, s - 6, 0, 180);
                    g.drawLine(4, s / 2, 4, s - 5);
                    g.drawLine(s - 4, s / 2, s - 4, s - 5);
                    g.drawLine(3, s - 5, s - 3, s - 5);
                    g.drawLine(s / 2 - 2, s - 2, s / 2 + 2, s - 2);
                }
                case THEME -> {
                    Area moon = new Area(new Ellipse2D.Double(2, 2, s - 4, s - 4));
                    moon.subtract(new Area(new Ellipse2D.Double(s / 2, 0, s - 5, s - 5)));
                    g.fill(moon);
                }
                case COLLAPSE -> {
                    g.drawLine(5, 3, 5, s - 3);
                    g.drawLine(s - 5, 4, s / 2, s / 2);
                    g.drawLine(s / 2, s / 2, s - 5, s - 4);
                }
                case CHECK -> {
                    g.drawLine(3, s / 2, s / 2 - 2, s - 4);
                    g.drawLine(s / 2 - 2, s - 4, s - 3, 4);
                }
                case LOGIN -> {
                    g.drawLine(3, s / 2, s - 5, s / 2);
                    g.drawLine(s - 8, s / 2 - 4, s - 4, s / 2);
                    g.drawLine(s - 8, s / 2 + 4, s - 4, s / 2);
                }
                case LOGOUT -> {
                    g.drawLine(s - 3, s / 2, 5, s / 2);
                    g.drawLine(8, s / 2 - 4, 4, s / 2);
                    g.drawLine(8, s / 2 + 4, 4, s / 2);
                }
                case CLOCK -> {
                    g.drawOval(2, 2, s - 4, s - 4);
                    g.drawLine(s / 2, s / 2, s / 2, 5);
                    g.drawLine(s / 2, s / 2, s - 5, s / 2);
                }
                case ALERT -> {
                    g.drawOval(2, 2, s - 4, s - 4);
                    g.drawLine(s / 2, 5, s / 2, s - 8);
                    g.fillOval(s / 2 - 1, s - 5, 2, 2);
                }
            }
            g.translate(-x, -y);
            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private static class EmptyState extends JPanel {
        EmptyState(String title, String message) {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(48, 24, 48, 24));
            JLabel titleLabel = label(title, 20, Font.BOLD, Theme.text());
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            JLabel messageLabel = label("<html><div style='text-align:center'>" + message + "</div></html>", 13, Font.PLAIN, Theme.muted());
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(titleLabel, BorderLayout.NORTH);
            add(messageLabel, BorderLayout.CENTER);
        }
    }

    private static class TeamCard extends RoundedPanel {
        TeamCard(String name, String count, String detail) {
            super(Theme.RADIUS_CARD, Theme.card(), Theme.border());
            setLayout(new BorderLayout(8, 8));
            setBorder(new EmptyBorder(20, 20, 20, 20));
            add(label(name, 18, Font.BOLD, Theme.text()), BorderLayout.NORTH);
            add(label(count, 28, Font.BOLD, Theme.text()), BorderLayout.CENTER);
            add(label(detail, 12, Font.PLAIN, Theme.muted()), BorderLayout.SOUTH);
        }
    }

    private static class StatusBadge extends JLabel {
        StatusBadge(Object value) {
            super(String.valueOf(value), SwingConstants.CENTER);
            setOpaque(false);
            setBorder(new EmptyBorder(6, 10, 6, 10));
            setFont(Theme.font(11, Font.BOLD));
            String text = String.valueOf(value);
            if (text.contains(I18n.t("status.inStay")) || text.contains(I18n.t("status.upcoming")) || text.equalsIgnoreCase(I18n.t("rooms.available"))) {
                setForeground(Theme.green());
            } else if (text.contains(I18n.t("status.completed"))) {
                setForeground(Theme.muted());
            } else {
                setForeground(Theme.muted());
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = graphics2(graphics);
            Color color = getForeground();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
            g.fill(Theme.continuousRect(4, 6, getWidth() - 8, getHeight() - 12, Theme.RADIUS_SMALL));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class BookingsTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Room", "Guest", "Check-in", "Check-out", "Status"};
        private List<Booking> rows = new ArrayList<>();

        void setRows(List<Booking> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        Booking getAt(int row) {
            return row < 0 || row >= rows.size() ? null : rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Booking booking = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> booking.getId();
                case 1 -> booking.getRoomNumber();
                case 2 -> booking.getGuestName();
                case 3 -> booking.getCheckIn();
                case 4 -> booking.getCheckOut();
                case 5 -> displayStatus(booking);
                default -> "";
            };
        }
    }

    private static class RoomTableModel extends AbstractTableModel {
        private final String[] columns = {"Room", "Type", "Nightly rate", "Status"};
        private List<Room> rows = new ArrayList<>();

        void setRows(List<Room> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Room room = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> room.getRoomNumber();
                case 1 -> room.getType();
                case 2 -> currency(room.getPrice());
                case 3 -> room.isAvailable() ? I18n.t("rooms.available") : I18n.t("rooms.occupied");
                default -> "";
            };
        }
    }

    private static class GuestTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Name", "Phone", "Email"};
        private List<Guest> rows = new ArrayList<>();

        void setRows(List<Guest> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        Guest getAt(int row) {
            return row < 0 || row >= rows.size() ? null : rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Guest guest = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> guest.getId();
                case 1 -> guest.getName();
                case 2 -> guest.getPhone();
                case 3 -> guest.getEmail();
                default -> "";
            };
        }
    }

    private static class WorkerTableModel extends AbstractTableModel {
        private final String[] columns = {"Name", "Email", "Role", "Status", "Last active"};
        private List<UserAccount> rows = new ArrayList<>();

        void setRows(List<UserAccount> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        UserAccount getAt(int row) {
            return row < 0 || row >= rows.size() ? null : rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            UserAccount worker = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> worker.getName();
                case 1 -> worker.getEmail();
                case 2 -> worker.getRole();
                case 3 -> worker.isActive() ? "Active" : "Inactive";
                case 4 -> worker.getLastActive().format(DateTimeFormatter.ofPattern("MMM d, HH:mm"));
                default -> "";
            };
        }
    }

    private JFreeChart lineChart(List<Booking> bookings) {
        XYSeries series = new XYSeries("Bookings");
        for (int i = 0; i < 10; i++) {
            series.add(i + 1, Math.max(1, bookings.size() / 2.0 + Math.sin(i) * 3 + i));
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(null, "Day", "Reservations", dataset, PlotOrientation.VERTICAL, false, false, false);
        XYPlot plot = chart.getXYPlot();
        plot.setRenderer(new XYLineAndShapeRenderer(true, false));
        styleChart(chart);
        return chart;
    }

    private JFreeChart barChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(6200, "Revenue", "Mon");
        dataset.addValue(8100, "Revenue", "Tue");
        dataset.addValue(7300, "Revenue", "Wed");
        dataset.addValue(9600, "Revenue", "Thu");
        dataset.addValue(11200, "Revenue", "Fri");
        JFreeChart chart = ChartFactory.createBarChart(null, "Day", "Revenue", dataset, PlotOrientation.VERTICAL, false, false, false);
        BarRenderer renderer = (BarRenderer) chart.getCategoryPlot().getRenderer();
        renderer.setSeriesPaint(0, Theme.blue());
        styleChart(chart);
        return chart;
    }

    @SuppressWarnings("unchecked")
    private JFreeChart pieChart(HotelStats stats) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        dataset.setValue("Occupied", stats.occupiedRooms());
        dataset.setValue("Available", stats.availableRooms());
        JFreeChart chart = ChartFactory.createPieChart(null, dataset, false, false, false);
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setSectionPaint("Occupied", Theme.blue());
        plot.setSectionPaint("Available", Theme.green());
        plot.setBackgroundPaint(Theme.card());
        plot.setOutlineVisible(false);
        chart.setBackgroundPaint(Theme.card());
        return chart;
    }

    private JFreeChart activityChart(List<Booking> bookings) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < 7; i++) {
            dataset.addValue(Math.max(1, bookings.size() + i * 2), "Guests", LocalDate.now().minusDays(6 - i).getDayOfWeek().toString().substring(0, 3));
        }
        JFreeChart chart = ChartFactory.createLineChart(null, "Day", "Guests", dataset, PlotOrientation.VERTICAL, false, false, false);
        styleChart(chart);
        return chart;
    }

    private static void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(Theme.card());
        if (chart.getPlot() instanceof XYPlot plot) {
            plot.setBackgroundPaint(Theme.card());
            plot.setDomainGridlinePaint(Theme.border());
            plot.setRangeGridlinePaint(Theme.border());
            plot.getDomainAxis().setLabelPaint(Theme.muted());
            plot.getRangeAxis().setLabelPaint(Theme.muted());
            plot.getDomainAxis().setTickLabelPaint(Theme.muted());
            plot.getRangeAxis().setTickLabelPaint(Theme.muted());
        } else if (chart.getPlot() instanceof org.jfree.chart.plot.CategoryPlot plot) {
            plot.setBackgroundPaint(Theme.card());
            plot.setDomainGridlinePaint(Theme.border());
            plot.setRangeGridlinePaint(Theme.border());
            plot.getDomainAxis().setLabelPaint(Theme.muted());
            plot.getRangeAxis().setLabelPaint(Theme.muted());
            plot.getDomainAxis().setTickLabelPaint(Theme.muted());
            plot.getRangeAxis().setTickLabelPaint(Theme.muted());
        }
    }

    private static ChartPanel chartPanel(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.setPreferredSize(new Dimension(460, 300));
        return panel;
    }

    private BigDecimal estimateRevenue() throws SQLException {
        List<Booking> bookings = bookingService.findAll();
        List<Room> rooms = roomService.findAll();
        Map<Integer, BigDecimal> roomRates = new LinkedHashMap<>();
        for (Room room : rooms) {
            roomRates.put(room.getId(), room.getPrice());
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Booking booking : bookings) {
            long nights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut()));
            total = total.add(roomRates.getOrDefault(booking.getRoomId(), BigDecimal.ZERO).multiply(BigDecimal.valueOf(nights)));
        }
        return total;
    }

    private static String currency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(value);
    }

    private static String displayStatus(Booking booking) {
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return I18n.t("status.completed");
        }
        LocalDate today = LocalDate.now();
        if (booking.getCheckIn().isAfter(today)) {
            return I18n.t("status.upcoming");
        }
        if (!booking.getCheckOut().isAfter(today)) {
            return I18n.t("status.completed");
        }
        return I18n.t("status.inStay");
    }

    private static List<Guest> guestsFromModel(DefaultListModel<Guest> model) {
        List<Guest> guests = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            guests.add(model.getElementAt(i));
        }
        return guests;
    }

    private static JTable styledTable(AbstractTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(48);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(86, 154, 255, 45));
        table.setSelectionForeground(Theme.text());
        table.setBackground(Theme.card());
        table.setForeground(Theme.text());
        table.setFont(Theme.font(13, Font.PLAIN));
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.font(12, Font.BOLD));
        header.setForeground(Theme.muted());
        header.setBackground(Theme.card());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                if (column == table.getColumnCount() - 1) {
                    return new StatusBadge(value);
                }
                Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                component.setFont(Theme.font(13, column == 1 || column == 2 ? Font.BOLD : Font.PLAIN));
                component.setForeground(selected ? Theme.text() : Theme.text());
                component.setBackground(selected ? table.getSelectionBackground() : Theme.card());
                setBorder(new EmptyBorder(0, 14, 0, 14));
                return component;
            }
        });
        return table;
    }

    private static JPanel tableWithToolbar(JTable table, JPanel toolbar) {
        JPanel panel = transparentPanel(new BorderLayout(0, 14));
        if (toolbar != null) {
            panel.add(toolbar, BorderLayout.NORTH);
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.card());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static RoundedPanel card(String title, JComponent content) {
        RoundedPanel card = new RoundedPanel(Theme.RADIUS_CARD, Theme.card(), Theme.border()).elevated(Theme.dark ? 3f : 1f);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.add(label(title, 18, Font.BOLD, Theme.text()), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static JPanel grid(int columns, int gap) {
        JPanel panel = transparentPanel(new GridLayout(0, columns, gap, gap));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private static GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 14);
        return c;
    }

    private static JPanel formPanel() {
        JPanel form = transparentPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        return form;
    }

    private static void addFormRow(JPanel form, int row, String labelText, Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(7, 0, 7, 14);
        JLabel label = label(labelText, 12, Font.BOLD, Theme.muted());
        form.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(7, 0, 7, 0);
        form.add(field, fieldConstraints);
    }

    private boolean modernConfirm(JPanel form, String title) {
        final boolean[] confirmed = {false};
        JDialog dialog = new JDialog(this, title, true);
        dialog.setUndecorated(true);
        RoundedPanel sheet = new RoundedPanel(Theme.RADIUS_FLOATING, Theme.card(), Theme.border()).elevated(6f);
        sheet.setLayout(new BorderLayout(0, Theme.SPACE_4));
        sheet.setBorder(new EmptyBorder(Theme.SPACE_5, Theme.SPACE_5, Theme.SPACE_4, Theme.SPACE_5));
        JLabel titleLabel = label(title, 18, Font.BOLD, Theme.text());
        sheet.add(titleLabel, BorderLayout.NORTH);
        sheet.add(form, BorderLayout.CENTER);
        JPanel actions = transparentPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_2, 0));
        JButton cancel = ghostButton(I18n.t("button.cancel"));
        JButton ok = primaryButton(I18n.t("button.ok"));
        cancel.addActionListener(event -> animateDialogOpacity(dialog, 1f, 0f, Theme.MOTION_FAST, dialog::dispose));
        ok.addActionListener(event -> {
            confirmed[0] = true;
            animateDialogOpacity(dialog, 1f, 0f, Theme.MOTION_FAST, dialog::dispose);
        });
        actions.add(cancel);
        actions.add(ok);
        sheet.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(sheet);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        if (!Theme.reduceMotion()) {
            try {
                dialog.setOpacity(0f);
            } catch (UnsupportedOperationException ignored) {
                // Some window managers do not support per-window opacity.
            }
            animateDialogOpacity(dialog, 0f, 1f, Theme.MOTION_BASE, null);
        }
        dialog.setVisible(true);
        return confirmed[0];
    }

    private void showToast(String message, boolean error) {
        JDialog toast = new JDialog(this);
        toast.setUndecorated(true);
        RoundedPanel panel = new RoundedPanel(Theme.RADIUS_FLOATING, Theme.cardAlt(), error ? Theme.danger() : Theme.green()).elevated(5f);
        panel.setBorder(new EmptyBorder(14, 18, 14, 18));
        panel.add(label(message, 13, Font.BOLD, Theme.text()));
        toast.setContentPane(panel);
        toast.pack();
        int x = getX() + getWidth() - toast.getWidth() - 32;
        int y = getY() + 104;
        toast.setLocation(x, y);
        if (!Theme.reduceMotion()) {
            try {
                toast.setOpacity(0f);
            } catch (UnsupportedOperationException ignored) {
                // Opacity is not available on every desktop configuration.
            }
        }
        toast.setVisible(true);
        if (!Theme.reduceMotion()) {
            animateDialogOpacity(toast, 0f, 1f, Theme.MOTION_BASE, null);
        }
        Timer timer = new Timer(2600, event -> animateDialogOpacity(toast, 1f, 0f, Theme.MOTION_BASE, toast::dispose));
        timer.setRepeats(false);
        timer.start();
    }

    private static void animateDialogOpacity(JDialog dialog, float from, float to, int duration, Runnable after) {
        if (Theme.reduceMotion()) {
            if (after != null) {
                after.run();
            }
            return;
        }
        long started = System.currentTimeMillis();
        Timer opacityTimer = new Timer(16, null);
        opacityTimer.addActionListener(event -> {
            float elapsed = (System.currentTimeMillis() - started) / (float) duration;
            float value = from + (to - from) * Theme.easeOutCubic(elapsed);
            try {
                dialog.setOpacity(Math.max(0f, Math.min(1f, value)));
            } catch (UnsupportedOperationException ignored) {
                opacityTimer.stop();
                if (after != null) {
                    after.run();
                }
            }
            if (elapsed >= 1f) {
                opacityTimer.stop();
                if (after != null) {
                    after.run();
                }
            }
        });
        opacityTimer.start();
    }

    private static JButton primaryButton(String text) {
        return new AppleButton(text, true);
    }

    private static JButton ghostButton(String text) {
        return new AppleButton(text, false);
    }

    private static JButton iconButton(String tooltip, IconGlyph glyph) {
        JButton button = new AppleButton(new LineIcon(glyph, 18, Theme.muted()));
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(42, 42));
        button.setBorder(new EmptyBorder(10, 10, 10, 10));
        return button;
    }

    private static JLabel label(String text, float size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(size, style));
        label.setForeground(color);
        return label;
    }

    private static JPanel transparentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel transparentPanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static Graphics2D graphics2(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        return g;
    }
}
