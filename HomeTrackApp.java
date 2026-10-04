import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Main Application Class
public class HomeTrackApp extends JFrame {

    public HomeTrackApp() {
        setTitle("HomeTrack: Household Asset Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize Database
        DBManager.initializeDatabase();

        // Fetch and sort data via DAO
        DashboardDAO dao = new DashboardDAO();
        List<Reminder> reminders = dao.getAllRemindersForUser(1);

        // Build UI
        JPanel dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Your Upcoming Due Dates");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        dashboardPanel.add(title);
        dashboardPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Iterate through sorted reminders and display them
        for (Reminder reminder : reminders) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            
            JLabel nameLabel = new JLabel(" " + reminder.getItemName() + " (" + reminder.getCategory() + ")");
            JLabel detailsLabel = new JLabel(reminder.getDetails() + " ");
            
            // Apply Red/Orange/Green Logic
            String status = reminder.getStatusColor();
            if (status.equals("RED")) {
                card.setBackground(new Color(255, 200, 200)); // Overdue
                nameLabel.setForeground(Color.RED);
            } else if (status.equals("ORANGE")) {
                card.setBackground(new Color(255, 235, 153)); // Due within 30 days
            } else {
                card.setBackground(new Color(200, 255, 200)); // Safe
            }

            card.add(nameLabel, BorderLayout.WEST);
            card.add(detailsLabel, BorderLayout.EAST);
            card.setPreferredSize(new Dimension(550, 40));
            card.setMaximumSize(new Dimension(550, 40));

            dashboardPanel.add(card);
            dashboardPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        add(new JScrollPane(dashboardPanel), BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HomeTrackApp().setVisible(true);
        });
    }
}

// --------------------------------------------------------
// Database Manager
// --------------------------------------------------------
class DBManager {
    private static final String URL = "jdbc:sqlite:hometrack.db";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            
            // Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, email TEXT UNIQUE, password TEXT)");

            // Assets Table
            stmt.execute("CREATE TABLE IF NOT EXISTS assets (" +
                    "asset_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, category TEXT, name TEXT, " +
                    "purchase_date TEXT, price REAL, shop TEXT, bill_path TEXT)");

            // Warranties Table
            stmt.execute("CREATE TABLE IF NOT EXISTS warranties (" +
                    "warranty_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "asset_id INTEGER, warranty_months INTEGER, end_date TEXT, " +
                    "free_services_total INTEGER, free_services_used INTEGER)");

            // EMI Table
            stmt.execute("CREATE TABLE IF NOT EXISTS emi (" +
                    "emi_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "asset_id INTEGER, loan_name TEXT, emi_amount REAL, " +
                    "due_day INTEGER, end_date TEXT)");

            System.out.println("SQLite Database Initialized Successfully.");
        } catch (Exception e) {
            System.err.println("Could not initialize DB. Is sqlite-jdbc.jar in your classpath?");
            e.printStackTrace();
        }
    }
}

// --------------------------------------------------------
// OOP Models (Interfaces, Inheritance, Polymorphism)
// --------------------------------------------------------
interface Reminderable {
    LocalDate getDueDate();
    long getDaysRemaining();
    String getStatusColor();
}

abstract class Reminder implements Reminderable {
    protected String itemName;
    protected String category;

    public Reminder(String itemName, String category) {
        this.itemName = itemName;
        this.category = category;
    }

    public String getItemName() { return itemName; }
    public String getCategory() { return category; }

    public abstract String getDetails();

    @Override
    public long getDaysRemaining() {
        return ChronoUnit.DAYS.between(LocalDate.now(), getDueDate());
    }

    @Override
    public String getStatusColor() {
        long days = getDaysRemaining();
        if (days < 0) return "RED"; 
        if (days <= 30) return "ORANGE"; 
        return "GREEN"; 
    }
}

class Warranty extends Reminder {
    private LocalDate purchaseDate;
    private int warrantyMonths;

    public Warranty(String itemName, LocalDate purchaseDate, int warrantyMonths) {
        super(itemName, "Warranty");
        this.purchaseDate = purchaseDate;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public LocalDate getDueDate() {
        return purchaseDate.plusMonths(warrantyMonths);
    }

    @Override
    public String getDetails() {
        return "Expires: " + getDueDate() + " (" + getDaysRemaining() + " days left)";
    }
}

class Emi extends Reminder {
    private int dueDayOfMonth;
    private double emiAmount;

    public Emi(String itemName, int dueDayOfMonth, double emiAmount) {
        super(itemName, "EMI");
        this.dueDayOfMonth = dueDayOfMonth;
        this.emiAmount = emiAmount;
    }

    @Override
    public LocalDate getDueDate() {
        LocalDate today = LocalDate.now();
        LocalDate dueDateThisMonth = today.withDayOfMonth(dueDayOfMonth);
        
        if (today.isAfter(dueDateThisMonth)) {
            return dueDateThisMonth.plusMonths(1);
        }
        return dueDateThisMonth;
    }

    @Override
    public String getDetails() {
        return "Amount: Rs." + emiAmount + " | Due: " + getDueDate();
    }
}

// --------------------------------------------------------
// Data Access Object (DAO)
// --------------------------------------------------------
class DashboardDAO {
    public List<Reminder> getAllRemindersForUser(int userId) {
        List<Reminder> reminders = new ArrayList<>();

        // Simulated database fetch - replace with ResultSet logic for full project
        reminders.add(new Warranty("Samsung TV", LocalDate.now().minusMonths(11), 12)); 
        reminders.add(new Warranty("LG Refrigerator", LocalDate.now().minusYears(2), 60)); 
        reminders.add(new Emi("Car Loan", 5, 12500.0));
        reminders.add(new Warranty("Sony Headphones", LocalDate.now().minusMonths(13), 12)); // Overdue example

        // Sort items by nearest due date 
        reminders.sort(Comparator.comparing(Reminder::getDueDate));

        return reminders;
    }
}