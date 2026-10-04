import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// --------------------------------------------------------
// 1. Main Application Class (UI & Controllers)
// --------------------------------------------------------
public class HomeTrackApp extends JFrame {
    private JPanel dashboardPanel;
    private DashboardDAO dao;

    public HomeTrackApp() {
        setTitle("HomeTrack: Household Asset Manager (CRUD Version)");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize DB and DAO
        DBManager.initializeDatabase();
        dao = new DashboardDAO();

        // Top Toolbar for CREATE operations
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddWarranty = new JButton("Add Warranty");
        JButton btnAddEmi = new JButton("Add EMI");
        toolbar.add(btnAddWarranty);
        toolbar.add(btnAddEmi);
        add(toolbar, BorderLayout.NORTH);

        // Dashboard Panel for READ operations
        dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JScrollPane(dashboardPanel), BorderLayout.CENTER);

        // Event Listeners for CREATE
        btnAddWarranty.addActionListener(e -> showAddWarrantyDialog());
        btnAddEmi.addActionListener(e -> showAddEmiDialog());

        // Initial Load
        refreshDashboard();
    }

    // --- READ (Refresh UI) ---
    private void refreshDashboard() {
        dashboardPanel.removeAll();
        
        JLabel title = new JLabel("Your Upcoming Due Dates");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        dashboardPanel.add(title);
        dashboardPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Fetch from Database
        List<Reminder> reminders = dao.getAllReminders();

        for (Reminder reminder : reminders) {
            JPanel card = createReminderCard(reminder);
            dashboardPanel.add(card);
            dashboardPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        dashboardPanel.revalidate();
        dashboardPanel.repaint();
    }

    // Creates individual UI cards with UPDATE and DELETE buttons
    private JPanel createReminderCard(Reminder reminder) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        
        JLabel nameLabel = new JLabel("  " + reminder.getItemName() + " (" + reminder.getCategory() + ")");
        JLabel detailsLabel = new JLabel(reminder.getDetails() + "  ");
        
        // Color Logic
        String status = reminder.getStatusColor();
        if (status.equals("RED")) { card.setBackground(new Color(255, 200, 200)); nameLabel.setForeground(Color.RED); } 
        else if (status.equals("ORANGE")) { card.setBackground(new Color(255, 235, 153)); } 
        else { card.setBackground(new Color(200, 255, 200)); }

        // Action Panel (Update / Delete)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);
        JButton btnEdit = new JButton("Edit");
        JButton btnDelete = new JButton("Delete");

        // --- UPDATE ---
        btnEdit.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog(this, "Enter new name:", reminder.getItemName());
            if (newName != null && !newName.trim().isEmpty()) {
                dao.updateAssetName(reminder.getAssetId(), newName);
                refreshDashboard();
            }
        });

        // --- DELETE ---
        btnDelete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete " + reminder.getItemName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dao.deleteAsset(reminder.getAssetId(), reminder.getCategory());
                refreshDashboard();
            }
        });

        actionPanel.add(btnEdit);
        actionPanel.add(btnDelete);

        card.add(nameLabel, BorderLayout.WEST);
        card.add(detailsLabel, BorderLayout.CENTER);
        card.add(actionPanel, BorderLayout.EAST);
        card.setMaximumSize(new Dimension(800, 45));

        return card;
    }

    // --- CREATE Dialogs ---
    private void showAddWarrantyDialog() {
        JTextField nameField = new JTextField();
        JTextField dateField = new JTextField(LocalDate.now().toString()); // Format: YYYY-MM-DD
        JTextField monthsField = new JTextField();
        Object[] message = { "Product Name:", nameField, "Purchase Date (YYYY-MM-DD):", dateField, "Warranty Duration (Months):", monthsField };

        int option = JOptionPane.showConfirmDialog(this, message, "Add New Warranty", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                dao.addWarranty(nameField.getText(), LocalDate.parse(dateField.getText()), Integer.parseInt(monthsField.getText()));
                refreshDashboard();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: Check date format or number inputs.\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAddEmiDialog() {
        JTextField nameField = new JTextField();
        JTextField amountField = new JTextField();
        JTextField dayField = new JTextField();
        Object[] message = { "Loan/EMI Name:", nameField, "EMI Amount (Rs):", amountField, "Due Day of Month (1-31):", dayField };

        int option = JOptionPane.showConfirmDialog(this, message, "Add New EMI", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                dao.addEmi(nameField.getText(), Double.parseDouble(amountField.getText()), Integer.parseInt(dayField.getText()));
                refreshDashboard();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input. Amount and Day must be numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HomeTrackApp().setVisible(true));
    }
}

// --------------------------------------------------------
// 2. Database Manager
// --------------------------------------------------------
class DBManager {
    private static final String URL = "jdbc:sqlite:hometrack.db";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Assets Table (Parent)
            stmt.execute("CREATE TABLE IF NOT EXISTS assets (" +
                    "asset_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "category TEXT, name TEXT, purchase_date TEXT)");

            // Warranties Table (Child)
            stmt.execute("CREATE TABLE IF NOT EXISTS warranties (" +
                    "warranty_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "asset_id INTEGER, warranty_months INTEGER, " +
                    "FOREIGN KEY(asset_id) REFERENCES assets(asset_id))");

            // EMI Table (Child)
            stmt.execute("CREATE TABLE IF NOT EXISTS emi (" +
                    "emi_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "asset_id INTEGER, emi_amount REAL, due_day INTEGER, " +
                    "FOREIGN KEY(asset_id) REFERENCES assets(asset_id))");

            System.out.println("SQLite DB Ready.");
        } catch (Exception e) { e.printStackTrace(); }
    }
}

// --------------------------------------------------------
// 3. Data Access Object (DAO) - CRUD Operations
// --------------------------------------------------------
class DashboardDAO {

    // --- CREATE ---
    public void addWarranty(String name, LocalDate purchaseDate, int months) {
        String insertAsset = "INSERT INTO assets (category, name, purchase_date) VALUES ('Warranty', ?, ?)";
        String insertWarranty = "INSERT INTO warranties (asset_id, warranty_months) VALUES (?, ?)";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement pstmtAsset = conn.prepareStatement(insertAsset, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtWarranty = conn.prepareStatement(insertWarranty)) {
            
            pstmtAsset.setString(1, name);
            pstmtAsset.setString(2, purchaseDate.toString());
            pstmtAsset.executeUpdate();

            ResultSet rs = pstmtAsset.getGeneratedKeys();
            if (rs.next()) {
                int assetId = rs.getInt(1);
                pstmtWarranty.setInt(1, assetId);
                pstmtWarranty.setInt(2, months);
                pstmtWarranty.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void addEmi(String name, double amount, int dueDay) {
        String insertAsset = "INSERT INTO assets (category, name, purchase_date) VALUES ('EMI', ?, ?)";
        String insertEmi = "INSERT INTO emi (asset_id, emi_amount, due_day) VALUES (?, ?, ?)";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement pstmtAsset = conn.prepareStatement(insertAsset, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtEmi = conn.prepareStatement(insertEmi)) {
            
            pstmtAsset.setString(1, name);
            pstmtAsset.setString(2, LocalDate.now().toString()); // fallback date
            pstmtAsset.executeUpdate();

            ResultSet rs = pstmtAsset.getGeneratedKeys();
            if (rs.next()) {
                int assetId = rs.getInt(1);
                pstmtEmi.setInt(1, assetId);
                pstmtEmi.setDouble(2, amount);
                pstmtEmi.setInt(3, dueDay);
                pstmtEmi.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- READ ---
    public List<Reminder> getAllReminders() {
        List<Reminder> reminders = new ArrayList<>();
        
        // Read Warranties
        String sqlWarranties = "SELECT a.asset_id, a.name, a.purchase_date, w.warranty_months " +
                               "FROM assets a JOIN warranties w ON a.asset_id = w.asset_id";
        try (Connection conn = DBManager.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlWarranties)) {
            while (rs.next()) {
                reminders.add(new Warranty(rs.getInt("asset_id"), rs.getString("name"), 
                        LocalDate.parse(rs.getString("purchase_date")), rs.getInt("warranty_months")));
            }
        } catch (Exception e) { e.printStackTrace(); }

        // Read EMIs
        String sqlEmis = "SELECT a.asset_id, a.name, e.emi_amount, e.due_day " +
                         "FROM assets a JOIN emi e ON a.asset_id = e.asset_id";
        try (Connection conn = DBManager.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlEmis)) {
            while (rs.next()) {
                reminders.add(new Emi(rs.getInt("asset_id"), rs.getString("name"), 
                        rs.getInt("due_day"), rs.getDouble("emi_amount")));
            }
        } catch (Exception e) { e.printStackTrace(); }

        // Sort by nearest due date
        reminders.sort(Comparator.comparing(Reminder::getDueDate));
        return reminders;
    }

    // --- UPDATE ---
    public void updateAssetName(int assetId, String newName) {
        String sql = "UPDATE assets SET name = ? WHERE asset_id = ?";
        try (Connection conn = DBManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setInt(2, assetId);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- DELETE ---
    public void deleteAsset(int assetId, String category) {
        String delChild = category.equals("Warranty") ? 
                "DELETE FROM warranties WHERE asset_id = ?" : "DELETE FROM emi WHERE asset_id = ?";
        String delParent = "DELETE FROM assets WHERE asset_id = ?";

        try (Connection conn = DBManager.getConnection(); 
             PreparedStatement pstmtChild = conn.prepareStatement(delChild);
             PreparedStatement pstmtParent = conn.prepareStatement(delParent)) {
            
            // Delete from child table (EMI/Warranty)
            pstmtChild.setInt(1, assetId);
            pstmtChild.executeUpdate();

            // Delete from parent table (Assets)
            pstmtParent.setInt(1, assetId);
            pstmtParent.executeUpdate();
            
        } catch (Exception e) { e.printStackTrace(); }
    }
}

// --------------------------------------------------------
// 4. OOP Models (Interfaces, Inheritance, Polymorphism)
// --------------------------------------------------------
interface Reminderable {
    LocalDate getDueDate();
    long getDaysRemaining();
    String getStatusColor();
}

abstract class Reminder implements Reminderable {
    protected int assetId;
    protected String itemName;
    protected String category;

    public Reminder(int assetId, String itemName, String category) {
        this.assetId = assetId;
        this.itemName = itemName;
        this.category = category;
    }

    public int getAssetId() { return assetId; }
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

    public Warranty(int assetId, String itemName, LocalDate purchaseDate, int warrantyMonths) {
        super(assetId, itemName, "Warranty");
        this.purchaseDate = purchaseDate;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public LocalDate getDueDate() { return purchaseDate.plusMonths(warrantyMonths); }

    @Override
    public String getDetails() { return "Expires: " + getDueDate() + " (" + getDaysRemaining() + " days left)"; }
}

class Emi extends Reminder {
    private int dueDayOfMonth;
    private double emiAmount;

    public Emi(int assetId, String itemName, int dueDayOfMonth, double emiAmount) {
        super(assetId, itemName, "EMI");
        this.dueDayOfMonth = dueDayOfMonth;
        this.emiAmount = emiAmount;
    }

    @Override
    public LocalDate getDueDate() {
        LocalDate today = LocalDate.now();
        LocalDate dueDateThisMonth = today.withDayOfMonth(dueDayOfMonth);
        return today.isAfter(dueDateThisMonth) ? dueDateThisMonth.plusMonths(1) : dueDateThisMonth;
    }

    @Override
    public String getDetails() { return "Amount: Rs." + emiAmount + " | Due: " + getDueDate(); }
}