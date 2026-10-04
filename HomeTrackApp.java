import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
        setTitle("HomeTrack: Household Asset Manager (MySQL Version)");
        setSize(750, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Initialize DB and DAO
        DBManager.initializeDatabase();
        dao = new DashboardDAO();

        // Top Toolbar for CREATE operations
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        toolbar.setBackground(new Color(240, 248, 255));
        
        JButton btnAddWarranty = new JButton("➕ Add Warranty");
        JButton btnAddEmi = new JButton("➕ Add EMI");
        
        styleButton(btnAddWarranty, new Color(70, 130, 180));
        styleButton(btnAddEmi, new Color(70, 130, 180));

        toolbar.add(btnAddWarranty);
        toolbar.add(btnAddEmi);
        add(toolbar, BorderLayout.NORTH);

        // Dashboard Panel for READ operations
        dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
        dashboardPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        dashboardPanel.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(dashboardPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Event Listeners for CREATE
        btnAddWarranty.addActionListener(e -> showAddWarrantyDialog());
        btnAddEmi.addActionListener(e -> showAddEmiDialog());

        refreshDashboard();
    }
    
    private void styleButton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private void refreshDashboard() {
        dashboardPanel.removeAll();
        
        JLabel title = new JLabel("Your Upcoming Due Dates");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(title);
        titlePanel.setMaximumSize(new Dimension(800, 50));
        dashboardPanel.add(titlePanel);

        List<Reminder> reminders = dao.getAllReminders();

        if (reminders.isEmpty()) {
            JLabel emptyLabel = new JLabel("🎉 No reminders found! Add a Warranty or EMI to get started.");
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            emptyLabel.setForeground(Color.GRAY);
            dashboardPanel.add(emptyLabel);
        } else {
            for (Reminder reminder : reminders) {
                dashboardPanel.add(createReminderCard(reminder));
                dashboardPanel.add(Box.createRigidArea(new Dimension(0, 10))); 
            }
        }

        dashboardPanel.revalidate();
        dashboardPanel.repaint();
    }

    private JPanel createReminderCard(Reminder reminder) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(reminder.getItemName() + " (" + reminder.getCategory() + ")");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JLabel detailsLabel = new JLabel(reminder.getDetails());
        detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        String status = reminder.getStatusColor();
        if (status.equals("RED")) { 
            card.setBackground(new Color(255, 230, 230)); 
            nameLabel.setForeground(new Color(180, 0, 0)); 
        } 
        else if (status.equals("ORANGE")) { 
            card.setBackground(new Color(255, 245, 210)); 
            nameLabel.setForeground(new Color(180, 100, 0));
        } 
        else { 
            card.setBackground(new Color(230, 255, 230)); 
            nameLabel.setForeground(new Color(0, 120, 0));
        }

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(detailsLabel);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);
        JButton btnEdit = new JButton("✏️ Edit");
        JButton btnDelete = new JButton("🗑️ Delete");

        btnEdit.addActionListener(e -> {
            if (reminder instanceof Warranty) showEditWarrantyDialog((Warranty) reminder);
            else if (reminder instanceof Emi) showEditEmiDialog((Emi) reminder);
        });

        btnDelete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                    "Are you sure you want to delete '" + reminder.getItemName() + "'?", 
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                dao.deleteAsset(reminder.getAssetId(), reminder.getCategory());
                refreshDashboard();
            }
        });

        actionPanel.add(btnEdit);
        actionPanel.add(btnDelete);
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(actionPanel, BorderLayout.EAST);
        card.setMaximumSize(new Dimension(800, 75));

        return card;
    }

    private void showAddWarrantyDialog() {
        JTextField nameField = new JTextField();
        JTextField dateField = new JTextField(LocalDate.now().toString());
        JTextField monthsField = new JTextField();
        Object[] message = { "Product Name:", nameField, "Purchase Date (YYYY-MM-DD):", dateField, "Warranty Duration (Months):", monthsField };

        int option = JOptionPane.showConfirmDialog(this, message, "Add New Warranty", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            try {
                dao.addWarranty(nameField.getText(), LocalDate.parse(dateField.getText()), Integer.parseInt(monthsField.getText()));
                refreshDashboard();
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Date Format. Please use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Warranty Duration must be a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAddEmiDialog() {
        JTextField nameField = new JTextField();
        JTextField amountField = new JTextField();
        JTextField dayField = new JTextField();
        Object[] message = { "Loan/EMI Name:", nameField, "EMI Amount (Rs):", amountField, "Due Day of Month (1-31):", dayField };

        int option = JOptionPane.showConfirmDialog(this, message, "Add New EMI", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            try {
                dao.addEmi(nameField.getText(), Double.parseDouble(amountField.getText()), Integer.parseInt(dayField.getText()));
                refreshDashboard();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Amount and Day must be valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditWarrantyDialog(Warranty w) {
        JTextField nameField = new JTextField(w.getItemName());
        JTextField dateField = new JTextField(w.getPurchaseDate().toString());
        JTextField monthsField = new JTextField(String.valueOf(w.getWarrantyMonths()));
        Object[] message = { "Product Name:", nameField, "Purchase Date (YYYY-MM-DD):", dateField, "Warranty Duration (Months):", monthsField };

        if (JOptionPane.showConfirmDialog(this, message, "Edit Warranty", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                dao.updateWarranty(w.getAssetId(), nameField.getText(), LocalDate.parse(dateField.getText()), Integer.parseInt(monthsField.getText()));
                refreshDashboard();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input data.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditEmiDialog(Emi e) {
        JTextField nameField = new JTextField(e.getItemName());
        JTextField amountField = new JTextField(String.valueOf(e.getEmiAmount()));
        JTextField dayField = new JTextField(String.valueOf(e.getDueDayOfMonth()));
        Object[] message = { "Loan/EMI Name:", nameField, "EMI Amount (Rs):", amountField, "Due Day of Month (1-31):", dayField };

        if (JOptionPane.showConfirmDialog(this, message, "Edit EMI", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                dao.updateEmi(e.getAssetId(), nameField.getText(), Double.parseDouble(amountField.getText()), Integer.parseInt(dayField.getText()));
                refreshDashboard();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input data.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new HomeTrackApp().setVisible(true));
    }
}

// --------------------------------------------------------
// 2. Database Manager (MySQL VERSION)
// --------------------------------------------------------
class DBManager {
    // TODO: Update these to match your MySQL Workbench credentials
    private static final String BASE_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "hometrack_db";
    private static final String USER = "root"; 
    private static final String PASS = "ReplaceWithYourPassword"; // Replace with your MySQL password

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(BASE_URL + DB_NAME, USER, PASS);
    }

    public static void initializeDatabase() {
        try {
            // 1. Connect to MySQL server and create the database if it doesn't exist
            try (Connection conn = DriverManager.getConnection(BASE_URL, USER, PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            }

            // 2. Connect to the specific database and create tables
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                // MySQL uses AUTO_INCREMENT and VARCHAR/DATE
                stmt.execute("CREATE TABLE IF NOT EXISTS assets (" +
                        "asset_id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "category VARCHAR(50), name VARCHAR(255), purchase_date DATE)");

                // Added ON DELETE CASCADE to automatically clean up child rows if parent is deleted
                stmt.execute("CREATE TABLE IF NOT EXISTS warranties (" +
                        "warranty_id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "asset_id INT, warranty_months INT, " +
                        "FOREIGN KEY(asset_id) REFERENCES assets(asset_id) ON DELETE CASCADE)");

                stmt.execute("CREATE TABLE IF NOT EXISTS emi (" +
                        "emi_id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "asset_id INT, emi_amount DOUBLE, due_day INT, " +
                        "FOREIGN KEY(asset_id) REFERENCES assets(asset_id) ON DELETE CASCADE)");
                
                System.out.println("MySQL Database Connected & Ready.");
            }
        } catch (Exception e) { 
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Database connection failed!\nMake sure MySQL is running and credentials are correct.\n" + e.getMessage(), 
                "MySQL Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

// --------------------------------------------------------
// 3. Data Access Object (DAO) 
// --------------------------------------------------------
class DashboardDAO {

    public void addWarranty(String name, LocalDate purchaseDate, int months) {
        String insertAsset = "INSERT INTO assets (category, name, purchase_date) VALUES ('Warranty', ?, ?)";
        String insertWarranty = "INSERT INTO warranties (asset_id, warranty_months) VALUES (?, ?)";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement pstmtAsset = conn.prepareStatement(insertAsset, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtWarranty = conn.prepareStatement(insertWarranty)) {
            
            pstmtAsset.setString(1, name);
            pstmtAsset.setDate(2, java.sql.Date.valueOf(purchaseDate)); // MySQL Date Handling
            pstmtAsset.executeUpdate();

            ResultSet rs = pstmtAsset.getGeneratedKeys();
            if (rs.next()) {
                pstmtWarranty.setInt(1, rs.getInt(1));
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
            pstmtAsset.setDate(2, java.sql.Date.valueOf(LocalDate.now())); 
            pstmtAsset.executeUpdate();

            ResultSet rs = pstmtAsset.getGeneratedKeys();
            if (rs.next()) {
                pstmtEmi.setInt(1, rs.getInt(1));
                pstmtEmi.setDouble(2, amount);
                pstmtEmi.setInt(3, dueDay);
                pstmtEmi.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public List<Reminder> getAllReminders() {
        List<Reminder> reminders = new ArrayList<>();
        
        String sqlWarranties = "SELECT a.asset_id, a.name, a.purchase_date, w.warranty_months " +
                               "FROM assets a JOIN warranties w ON a.asset_id = w.asset_id";
        try (Connection conn = DBManager.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlWarranties)) {
            while (rs.next()) {
                reminders.add(new Warranty(rs.getInt("asset_id"), rs.getString("name"), 
                        rs.getDate("purchase_date").toLocalDate(), rs.getInt("warranty_months")));
            }
        } catch (Exception e) { e.printStackTrace(); }

        String sqlEmis = "SELECT a.asset_id, a.name, e.emi_amount, e.due_day " +
                         "FROM assets a JOIN emi e ON a.asset_id = e.asset_id";
        try (Connection conn = DBManager.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlEmis)) {
            while (rs.next()) {
                reminders.add(new Emi(rs.getInt("asset_id"), rs.getString("name"), 
                        rs.getInt("due_day"), rs.getDouble("emi_amount")));
            }
        } catch (Exception e) { e.printStackTrace(); }

        reminders.sort(Comparator.comparing(Reminder::getDueDate));
        return reminders;
    }

    public void updateWarranty(int assetId, String newName, LocalDate newDate, int newMonths) {
        String updateAsset = "UPDATE assets SET name = ?, purchase_date = ? WHERE asset_id = ?";
        String updateWarranty = "UPDATE warranties SET warranty_months = ? WHERE asset_id = ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement pstmtAsset = conn.prepareStatement(updateAsset);
             PreparedStatement pstmtWarranty = conn.prepareStatement(updateWarranty)) {
            
            pstmtAsset.setString(1, newName);
            pstmtAsset.setDate(2, java.sql.Date.valueOf(newDate));
            pstmtAsset.setInt(3, assetId);
            pstmtAsset.executeUpdate();

            pstmtWarranty.setInt(1, newMonths);
            pstmtWarranty.setInt(2, assetId);
            pstmtWarranty.executeUpdate();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public void updateEmi(int assetId, String newName, double newAmount, int newDay) {
        String updateAsset = "UPDATE assets SET name = ? WHERE asset_id = ?";
        String updateEmi = "UPDATE emi SET emi_amount = ?, due_day = ? WHERE asset_id = ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement pstmtAsset = conn.prepareStatement(updateAsset);
             PreparedStatement pstmtEmi = conn.prepareStatement(updateEmi)) {
            
            pstmtAsset.setString(1, newName);
            pstmtAsset.setInt(2, assetId);
            pstmtAsset.executeUpdate();

            pstmtEmi.setDouble(1, newAmount);
            pstmtEmi.setInt(2, newDay);
            pstmtEmi.setInt(3, assetId);
            pstmtEmi.executeUpdate();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public void deleteAsset(int assetId, String category) {
        // Since we added ON DELETE CASCADE in MySQL, deleting the parent removes the children automatically.
        String delParent = "DELETE FROM assets WHERE asset_id = ?";
        try (Connection conn = DBManager.getConnection(); 
             PreparedStatement pstmtParent = conn.prepareStatement(delParent)) {
            pstmtParent.setInt(1, assetId);
            pstmtParent.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}

// --------------------------------------------------------
// 4. OOP Models 
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
    public long getDaysRemaining() { return ChronoUnit.DAYS.between(LocalDate.now(), getDueDate()); }

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
    
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public int getWarrantyMonths() { return warrantyMonths; }
    @Override public LocalDate getDueDate() { return purchaseDate.plusMonths(warrantyMonths); }
    @Override public String getDetails() { return "Expires: " + getDueDate() + " (" + getDaysRemaining() + " days left)"; }
}

class Emi extends Reminder {
    private int dueDayOfMonth;
    private double emiAmount;

    public Emi(int assetId, String itemName, int dueDayOfMonth, double emiAmount) {
        super(assetId, itemName, "EMI");
        this.dueDayOfMonth = dueDayOfMonth;
        this.emiAmount = emiAmount;
    }
    
    public int getDueDayOfMonth() { return dueDayOfMonth; }
    public double getEmiAmount() { return emiAmount; }
    @Override public LocalDate getDueDate() {
        LocalDate today = LocalDate.now();
        LocalDate dueDateThisMonth = today.withDayOfMonth(dueDayOfMonth);
        return today.isAfter(dueDateThisMonth) ? dueDateThisMonth.plusMonths(1) : dueDateThisMonth;
    }
    @Override public String getDetails() { return "Amount: Rs." + emiAmount + " | Next Due: " + getDueDate(); }
}