/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package driveingschool;

import java.awt.CardLayout;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author chiki
 */
public class Dashbord extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Dashbord.class.getName());
    private String currentUsername;
    private String currentRole;
    private Connection con;

    /**
     * Creates new form Dashbord
     */
    public Dashbord(String username, String role) {
        this.currentUsername = (username != null) ? username : "";
        this.currentRole = (role != null) ? role : "";

        initComponents();
        btnhide();
        if (role.equals("Admin")) {
            btnInstructors.setVisible(true);
            btnVehicle.setVisible(true);
            btnUserManagement.setVisible(true);
        } else {
            btnInstructors.setVisible(false);
            btnVehicle.setVisible(false);
            btnUserManagement.setVisible(false);
        }
        lblUsername.setText(this.currentUsername);
        lblRole.setText(this.currentRole);
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        setLocationRelativeTo(null);

        // Show dashboard card by default
        switchCard("cardDashboard");

        // Load users from DB into table
        loadUsers();

        // Initialize and load Students from DB
        initStudentComponents();
        loadStudents();

        // Initialize and load Instructors from DB
        initInstructorComponents();
        loadInstructors();

        // Initialize and load Vehicles from DB
        initVehicleComponents();
        loadVehicles();
    }

    private void switchCard(String cardName) {
        CardLayout cl = (CardLayout) Contructor.getLayout();
        cl.show(Contructor, cardName);
    }

    private Connection getConnection() {
        try {
            if (con == null || con.isClosed()) {
                con = DBConnection.connect();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            con = DBConnection.connect();
        }
        return con;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, null, ex);
            return "";
        }
    }

    public String getDefaultPassword() {
        Connection conn = getConnection();
        if (conn != null) {
            String sql = "SELECT default_password FROM settings LIMIT 1";
            try (PreparedStatement pst = conn.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String def = rs.getString("default_password");
                    if (def != null && !def.trim().isEmpty()) {
                        return def.trim();
                    }
                }
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "Failed to load default password from settings", ex);
            }
        }
        return "1234";
    }

    private void loadUsers() {
        DefaultTableModel dtm = (DefaultTableModel) jTable1.getModel();
        dtm.setRowCount(0);
        Connection conn = getConnection();
        if (conn == null) {
            return;
        }
        String sql = "SELECT user_id, username, nic, role FROM users ORDER BY user_id ASC";
        try (PreparedStatement pst = conn.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("user_id"));
                row.add(rs.getString("username"));
                row.add(rs.getString("nic"));
                row.add(rs.getString("role"));
                dtm.addRow(row);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to load users: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        lblUserID.setText("");
        jTextField2.setText("");
        jTextField4.setText("");
        jPasswordField1.setText("");
        jPasswordField2.setText("");
        jComboBox1.setSelectedIndex(0);
        CheckFirstTimeLog.setSelected(false);
        jTable1.clearSelection();
    }

    private void ensureStudentTableSchema() {
        Connection conn = getConnection();
        if (conn != null) {
            try (java.sql.Statement stmt = conn.createStatement()) {
                try {
                    stmt.executeUpdate("ALTER TABLE students ADD COLUMN vehicle_class VARCHAR(50) DEFAULT 'Class B (Dual Purpose / Car)'");
                } catch (SQLException ignored) {
                }
                try {
                    stmt.executeUpdate("ALTER TABLE students ADD COLUMN status VARCHAR(30) DEFAULT 'Active Learner'");
                } catch (SQLException ignored) {
                }
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "Failed to verify student table schema", ex);
            }
        }
    }

    private void initStudentComponents() {
        ensureStudentTableSchema();

        if (cmbStudentClass.getItemCount() == 0) {
            cmbStudentClass.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
                "Class B (Dual Purpose / Car)",
                "Class A (Motorcycle)",
                "Class B1 (Auto Light Vehicle)",
                "Class A & B (Combo)",
                "Class C (Heavy Vehicle)",
                "Class D (Bus)"
            }));
        }

        if (cmbStudentStatus.getItemCount() == 0) {
            cmbStudentStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
                "Active Learner",
                "Theory Passed",
                "Practical Training",
                "Trial / Exam Ready",
                "Completed"
            }));
        }

        // Attach action listener for search button and Enter key on search field
        for (java.awt.event.ActionListener al : btnSearchStudent.getActionListeners()) {
            btnSearchStudent.removeActionListener(al);
        }
        btnSearchStudent.addActionListener(e -> searchStudents());

        for (java.awt.event.ActionListener al : txtSearchStudent.getActionListeners()) {
            txtSearchStudent.removeActionListener(al);
        }
        txtSearchStudent.addActionListener(e -> searchStudents());

        clearStudentForm();
    }

    private void loadStudents() {
        loadStudents(null);
    }

    private void loadStudents(String keyword) {
        DefaultTableModel dtm = (DefaultTableModel) tableStudents.getModel();
        dtm.setRowCount(0);
        Connection conn = getConnection();
        if (conn == null) {
            return;
        }

        boolean hasFilter = (keyword != null && !keyword.trim().isEmpty());
        String sql;
        if (hasFilter) {
            sql = "SELECT student_id, full_name, nic, phone, address, vehicle_class, status FROM students "
                    + "WHERE full_name LIKE ? OR nic LIKE ? OR phone LIKE ? OR CAST(student_id AS CHAR) LIKE ? "
                    + "ORDER BY student_id ASC";
        } else {
            sql = "SELECT student_id, full_name, nic, phone, address, vehicle_class, status FROM students ORDER BY student_id ASC";
        }

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            if (hasFilter) {
                String searchPattern = "%" + keyword.trim() + "%";
                pst.setString(1, searchPattern);
                pst.setString(2, searchPattern);
                pst.setString(3, searchPattern);
                pst.setString(4, searchPattern);
            }
            try (ResultSet rs = pst.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    int id = rs.getInt("student_id");
                    row.add(String.format("STU-%04d", id));
                    row.add(rs.getString("full_name"));
                    row.add(rs.getString("nic") != null ? rs.getString("nic") : "");
                    row.add(rs.getString("phone") != null ? rs.getString("phone") : "");
                    row.add(rs.getString("address") != null ? rs.getString("address") : "");
                    row.add(rs.getString("vehicle_class") != null ? rs.getString("vehicle_class") : "Class B (Dual Purpose / Car)");
                    row.add(rs.getString("status") != null ? rs.getString("status") : "Active Learner");
                    dtm.addRow(row);
                    count++;
                }
                lblStudentCount.setText("Showing " + count + " enrolled students | Click row to inspect details");
                if (!hasFilter) {
                    lblCountStudents.setText(String.valueOf(count));
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to load students: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchStudents() {
        String query = txtSearchStudent.getText().trim();
        loadStudents(query);
    }

    private void clearStudentForm() {
        lblStudentIDVal.setText("STU-Auto");
        txtStudentName.setText("");
        txtStudentNIC.setText("");
        txtStudentPhone.setText("");
        txtStudentAddress.setText("");
        if (cmbStudentClass.getItemCount() > 0) {
            cmbStudentClass.setSelectedIndex(0);
        }
        if (cmbStudentStatus.getItemCount() > 0) {
            cmbStudentStatus.setSelectedIndex(0);
        }
        tableStudents.clearSelection();
        btnAddStudent.setVisible(true);
        btnUpdateStudent.setVisible(false);
        btnDeleteStudent.setVisible(false);
        btnClearStudent.setVisible(false);
    }

    private void ensureInstructorTableSchema() {
        Connection conn = getConnection();
        if (conn != null) {
            try (java.sql.Statement stmt = conn.createStatement()) {
                try {
                    stmt.executeUpdate("ALTER TABLE instructors ADD COLUMN nic VARCHAR(12) DEFAULT NULL");
                } catch (SQLException ignored) {
                }
                try {
                    stmt.executeUpdate("ALTER TABLE instructors ADD COLUMN license_no VARCHAR(50) DEFAULT NULL");
                } catch (SQLException ignored) {
                }
                try {
                    stmt.executeUpdate("ALTER TABLE instructors ADD COLUMN vehicle_class VARCHAR(50) DEFAULT 'Class B (Dual Purpose / Car)'");
                } catch (SQLException ignored) {
                }
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "Failed to verify instructor table schema", ex);
            }
        }
    }

    private void initInstructorComponents() {
        ensureInstructorTableSchema();

        if (cmbInstCategory.getItemCount() == 0) {
            cmbInstCategory.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
                "Class B (Dual Purpose / Car)",
                "Class A (Motorcycle)",
                "Class B1 (Auto Light Vehicle)",
                "Class A & B (Combo)",
                "Class C (Heavy Vehicle)",
                "Class D (Bus)"
            }));
        }

        if (cmbInstStatus.getItemCount() == 0) {
            cmbInstStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
                "Available",
                "On Duty",
                "On Leave"
            }));
        }

        if (cmbInstFilterStatus.getItemCount() == 0) {
            cmbInstFilterStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
                "All Statuses",
                "Available",
                "On Duty",
                "On Leave"
            }));
        }

        for (java.awt.event.ActionListener al : btnInstRefresh.getActionListeners()) {
            btnInstRefresh.removeActionListener(al);
        }
        btnInstRefresh.addActionListener(e -> {
            String kw = txtInstSearch.getText().trim();
            String st = (String) cmbInstFilterStatus.getSelectedItem();
            loadInstructors(kw, st);
        });

        for (java.awt.event.ActionListener al : btnInstClear.getActionListeners()) {
            btnInstClear.removeActionListener(al);
        }
        btnInstClear.addActionListener(e -> clearInstructorForm());

        txtInstSearch.addActionListener(e -> {
            String kw = txtInstSearch.getText().trim();
            String st = (String) cmbInstFilterStatus.getSelectedItem();
            loadInstructors(kw, st);
        });

        cmbInstFilterStatus.addActionListener(e -> {
            String kw = txtInstSearch.getText().trim();
            String st = (String) cmbInstFilterStatus.getSelectedItem();
            loadInstructors(kw, st);
        });

        tableInstructors.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableInstructorsMouseClicked(evt);
            }
        });

        clearInstructorForm();
    }

    private void loadInstructors() {
        loadInstructors(null, null);
    }

    private void loadInstructors(String keyword, String statusFilter) {
        DefaultTableModel dtm = (DefaultTableModel) tableInstructors.getModel();
        dtm.setRowCount(0);
        Connection conn = getConnection();
        if (conn == null) {
            return;
        }

        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty() && !keyword.equals("Search instructors..."));
        boolean hasStatus = (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("All Statuses"));

        StringBuilder sql = new StringBuilder("SELECT instructor_id, full_name, phone, nic, license_no, vehicle_class, status FROM instructors WHERE 1=1 ");
        if (hasKeyword) {
            sql.append("AND (full_name LIKE ? OR phone LIKE ? OR nic LIKE ? OR license_no LIKE ? OR CAST(instructor_id AS CHAR) LIKE ?) ");
        }
        if (hasStatus) {
            sql.append("AND status = ? ");
        }
        sql.append("ORDER BY instructor_id ASC");

        try (PreparedStatement pst = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (hasKeyword) {
                String pattern = "%" + keyword.trim() + "%";
                pst.setString(paramIndex++, pattern);
                pst.setString(paramIndex++, pattern);
                pst.setString(paramIndex++, pattern);
                pst.setString(paramIndex++, pattern);
                pst.setString(paramIndex++, pattern);
            }
            if (hasStatus) {
                pst.setString(paramIndex++, statusFilter.trim());
            }

            try (ResultSet rs = pst.executeQuery()) {
                int count = 0;
                int activeCount = 0;
                int availableCount = 0;
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    int id = rs.getInt("instructor_id");
                    row.add(String.format("INS-%03d", id));
                    row.add(rs.getString("full_name"));
                    row.add(rs.getString("phone") != null ? rs.getString("phone") : "");
                    row.add(rs.getString("nic") != null ? rs.getString("nic") : "");
                    row.add(rs.getString("license_no") != null ? rs.getString("license_no") : "");
                    row.add(rs.getString("vehicle_class") != null ? rs.getString("vehicle_class") : "Class B (Dual Purpose / Car)");
                    String status = rs.getString("status") != null ? rs.getString("status") : "Available";
                    row.add(status);
                    dtm.addRow(row);
                    count++;
                    if ("On Duty".equalsIgnoreCase(status)) {
                        activeCount++;
                    } else if ("Available".equalsIgnoreCase(status)) {
                        availableCount++;
                    }
                }
                lblInstTableCount.setText("Showing " + count + " instructors | Click a row to view or edit profile");
                lblInstBadgeTotalCount.setText(String.valueOf(count));
                lblInstBadgeActiveCount.setText(String.valueOf(activeCount));
                lblInstBadgeAvailableCount.setText(String.valueOf(availableCount));
                if (!hasKeyword && !hasStatus) {
                    lblCountInstructors.setText(String.valueOf(count));
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to load instructors: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearInstructorForm() {
        txtInstId.setText("INS-Auto");
        txtInstFullName.setText("");
        txtInstPhone.setText("");
        txtInstNic.setText("");
        txtInstLicense.setText("");
        if (cmbInstCategory.getItemCount() > 0) {
            cmbInstCategory.setSelectedIndex(0);
        }
        if (cmbInstStatus.getItemCount() > 0) {
            cmbInstStatus.setSelectedIndex(0);
        }
        tableInstructors.clearSelection();
        btnInstAdd.setVisible(true);
        btnInstUpdate.setVisible(false);
        btnInstDelete.setVisible(false);
        btnInstClear.setVisible(false);
    }

    private void tableInstructorsMouseClicked(java.awt.event.MouseEvent evt) {
        int row = tableInstructors.getSelectedRow();
        if (row >= 0) {
            txtInstId.setText(String.valueOf(tableInstructors.getValueAt(row, 0)));
            txtInstFullName.setText(String.valueOf(tableInstructors.getValueAt(row, 1)));
            txtInstPhone.setText(String.valueOf(tableInstructors.getValueAt(row, 2)));
            txtInstNic.setText(String.valueOf(tableInstructors.getValueAt(row, 3)));
            txtInstLicense.setText(String.valueOf(tableInstructors.getValueAt(row, 4)));
            String vClass = String.valueOf(tableInstructors.getValueAt(row, 5));
            for (int i = 0; i < cmbInstCategory.getItemCount(); i++) {
                if (cmbInstCategory.getItemAt(i).equalsIgnoreCase(vClass) || cmbInstCategory.getItemAt(i).contains(vClass)) {
                    cmbInstCategory.setSelectedIndex(i);
                    break;
                }
            }
            String status = String.valueOf(tableInstructors.getValueAt(row, 6));
            for (int i = 0; i < cmbInstStatus.getItemCount(); i++) {
                if (cmbInstStatus.getItemAt(i).equalsIgnoreCase(status)) {
                    cmbInstStatus.setSelectedIndex(i);
                    break;
                }
            }

            btnInstAdd.setVisible(false);
            btnInstUpdate.setVisible(true);
            btnInstDelete.setVisible(true);
            btnInstClear.setVisible(true);
        }
    }

    private void ensureVehicleTableSchema() {
        Connection conn = getConnection();
        if (conn != null) {
            try (java.sql.Statement stmt = conn.createStatement()) {
                try {
                    stmt.executeUpdate("ALTER TABLE vehicles ADD COLUMN model VARCHAR(100) DEFAULT NULL");
                } catch (SQLException ignored) {
                }
                try {
                    stmt.executeUpdate("ALTER TABLE vehicles ADD COLUMN vehicle_class VARCHAR(50) DEFAULT 'Class B (Dual Purpose / Car)'");
                } catch (SQLException ignored) {
                }
                try {
                    stmt.executeUpdate("ALTER TABLE vehicles ADD COLUMN transmission VARCHAR(20) DEFAULT 'Manual'");
                } catch (SQLException ignored) {
                }
                try {
                    stmt.executeUpdate("ALTER TABLE vehicles ADD COLUMN fuel_type VARCHAR(20) DEFAULT 'Petrol'");
                } catch (SQLException ignored) {
                }
                try {
                    stmt.executeUpdate("ALTER TABLE vehicles ADD COLUMN mileage VARCHAR(50) DEFAULT NULL");
                } catch (SQLException ignored) {
                }
                try {
                    stmt.executeUpdate("ALTER TABLE vehicles MODIFY COLUMN vehicle_type VARCHAR(50) DEFAULT 'Car'");
                } catch (SQLException ignored) {
                }
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "Failed to verify vehicle table schema", ex);
            }
        }
    }

    private void initVehicleComponents() {
        ensureVehicleTableSchema();

        if (cmbVehCategory.getItemCount() == 0) {
            cmbVehCategory.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
                "Class B (Dual Purpose / Car)",
                "Class A (Motorcycle)",
                "Class B1 (Auto Light Vehicle)",
                "Class A & B (Combo)",
                "Class C (Heavy Vehicle / Van)",
                "Class D (Bus)"
            }));
        }

        if (cmbVehTransmission.getItemCount() == 0) {
            cmbVehTransmission.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
                "Auto",
                "Manual"
            }));
        }

        if (cmbVehFuel.getItemCount() == 0) {
            cmbVehFuel.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
                "Petrol",
                "Diesel",
                "Hybrid",
                "Electric"
            }));
        }

        if (cmbVehStatus.getItemCount() == 0) {
            cmbVehStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
                "Available",
                "In Session",
                "Under Maintenance"
            }));
        }

        if (cmbVehFilterStatus.getItemCount() == 0) {
            cmbVehFilterStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
                "All Statuses",
                "Available",
                "In Session",
                "Under Maintenance"
            }));
        }

        for (java.awt.event.ActionListener al : btnVehRefresh.getActionListeners()) {
            btnVehRefresh.removeActionListener(al);
        }
        btnVehRefresh.addActionListener(e -> {
            String kw = txtVehSearch.getText().trim();
            String st = (String) cmbVehFilterStatus.getSelectedItem();
            loadVehicles(kw, st);
        });

        for (java.awt.event.ActionListener al : btnVehClear.getActionListeners()) {
            btnVehClear.removeActionListener(al);
        }
        btnVehClear.addActionListener(e -> clearVehicleForm());

        txtVehSearch.addActionListener(e -> {
            String kw = txtVehSearch.getText().trim();
            String st = (String) cmbVehFilterStatus.getSelectedItem();
            loadVehicles(kw, st);
        });

        cmbVehFilterStatus.addActionListener(e -> {
            String kw = txtVehSearch.getText().trim();
            String st = (String) cmbVehFilterStatus.getSelectedItem();
            loadVehicles(kw, st);
        });

        tableVehicles.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableVehiclesMouseClicked(evt);
            }
        });

        clearVehicleForm();
    }

    private void loadVehicles() {
        loadVehicles(null, null);
    }

    private void loadVehicles(String keyword, String statusFilter) {
        DefaultTableModel dtm = (DefaultTableModel) tableVehicles.getModel();
        dtm.setRowCount(0);
        Connection conn = getConnection();
        if (conn == null) {
            return;
        }

        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty() && !keyword.equals("Search vehicles..."));
        boolean hasStatus = (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("All Statuses"));

        StringBuilder sql = new StringBuilder("SELECT vehicle_id, model, vehicle_number, vehicle_class, transmission, fuel_type, status, mileage FROM vehicles WHERE 1=1 ");
        if (hasKeyword) {
            sql.append("AND (model LIKE ? OR vehicle_number LIKE ? OR vehicle_class LIKE ? OR transmission LIKE ? OR fuel_type LIKE ? OR CAST(vehicle_id AS CHAR) LIKE ?) ");
        }
        if (hasStatus) {
            sql.append("AND status = ? ");
        }
        sql.append("ORDER BY vehicle_id ASC");

        try (PreparedStatement pst = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (hasKeyword) {
                String pattern = "%" + keyword.trim() + "%";
                pst.setString(paramIndex++, pattern);
                pst.setString(paramIndex++, pattern);
                pst.setString(paramIndex++, pattern);
                pst.setString(paramIndex++, pattern);
                pst.setString(paramIndex++, pattern);
                pst.setString(paramIndex++, pattern);
            }
            if (hasStatus) {
                pst.setString(paramIndex++, statusFilter.trim());
            }

            try (ResultSet rs = pst.executeQuery()) {
                int count = 0;
                int readyCount = 0;
                int serviceCount = 0;
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    int id = rs.getInt("vehicle_id");
                    row.add(String.format("VEH-%03d", id));
                    row.add(rs.getString("model") != null ? rs.getString("model") : "");
                    row.add(rs.getString("vehicle_number") != null ? rs.getString("vehicle_number") : "");
                    row.add(rs.getString("vehicle_class") != null ? rs.getString("vehicle_class") : "Class B (Dual Purpose / Car)");
                    row.add(rs.getString("transmission") != null ? rs.getString("transmission") : "Manual");
                    row.add(rs.getString("fuel_type") != null ? rs.getString("fuel_type") : "Petrol");
                    String status = rs.getString("status") != null ? rs.getString("status") : "Available";
                    row.add(status);
                    dtm.addRow(row);
                    count++;
                    if ("Available".equalsIgnoreCase(status) || "Ready".equalsIgnoreCase(status)) {
                        readyCount++;
                    } else if ("Under Maintenance".equalsIgnoreCase(status)) {
                        serviceCount++;
                    }
                }
                lblVehTableCount.setText("Showing " + count + " vehicles | Click a row to view or edit details");
                lblVehBadgeTotalCount.setText(String.valueOf(count));
                lblVehBadgeReadyCount.setText(String.valueOf(readyCount));
                lblVehBadgeServiceCount.setText(String.valueOf(serviceCount));
                if (!hasKeyword && !hasStatus) {
                    lblCountVehicles.setText(String.valueOf(count));
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to load vehicles: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearVehicleForm() {
        txtVehId.setText("VEH-Auto");
        txtVehModel.setText("");
        txtVehPlate.setText("");
        txtVehMileage.setText("");
        if (cmbVehCategory.getItemCount() > 0) {
            cmbVehCategory.setSelectedIndex(0);
        }
        if (cmbVehTransmission.getItemCount() > 0) {
            cmbVehTransmission.setSelectedIndex(0);
        }
        if (cmbVehFuel.getItemCount() > 0) {
            cmbVehFuel.setSelectedIndex(0);
        }
        if (cmbVehStatus.getItemCount() > 0) {
            cmbVehStatus.setSelectedIndex(0);
        }
        tableVehicles.clearSelection();
        btnVehAdd.setVisible(true);
        btnVehUpdate.setVisible(false);
        btnVehDelete.setVisible(false);
        btnVehClear.setVisible(false);
    }

    private void tableVehiclesMouseClicked(java.awt.event.MouseEvent evt) {
        int row = tableVehicles.getSelectedRow();
        if (row >= 0) {
            String vehIdStr = String.valueOf(tableVehicles.getValueAt(row, 0));
            txtVehId.setText(vehIdStr);
            txtVehModel.setText(String.valueOf(tableVehicles.getValueAt(row, 1)));
            txtVehPlate.setText(String.valueOf(tableVehicles.getValueAt(row, 2)));

            String vClass = String.valueOf(tableVehicles.getValueAt(row, 3));
            for (int i = 0; i < cmbVehCategory.getItemCount(); i++) {
                if (cmbVehCategory.getItemAt(i).equalsIgnoreCase(vClass) || cmbVehCategory.getItemAt(i).contains(vClass)) {
                    cmbVehCategory.setSelectedIndex(i);
                    break;
                }
            }

            String trans = String.valueOf(tableVehicles.getValueAt(row, 4));
            for (int i = 0; i < cmbVehTransmission.getItemCount(); i++) {
                if (cmbVehTransmission.getItemAt(i).equalsIgnoreCase(trans)) {
                    cmbVehTransmission.setSelectedIndex(i);
                    break;
                }
            }

            String fuel = String.valueOf(tableVehicles.getValueAt(row, 5));
            for (int i = 0; i < cmbVehFuel.getItemCount(); i++) {
                if (cmbVehFuel.getItemAt(i).equalsIgnoreCase(fuel)) {
                    cmbVehFuel.setSelectedIndex(i);
                    break;
                }
            }

            String status = String.valueOf(tableVehicles.getValueAt(row, 6));
            for (int i = 0; i < cmbVehStatus.getItemCount(); i++) {
                if (cmbVehStatus.getItemAt(i).equalsIgnoreCase(status)) {
                    cmbVehStatus.setSelectedIndex(i);
                    break;
                }
            }

            try {
                int cleanId = Integer.parseInt(vehIdStr.replaceAll("[^0-9]", ""));
                Connection conn = getConnection();
                if (conn != null) {
                    String mSql = "SELECT mileage FROM vehicles WHERE vehicle_id = ?";
                    try (PreparedStatement mPst = conn.prepareStatement(mSql)) {
                        mPst.setInt(1, cleanId);
                        try (ResultSet mRs = mPst.executeQuery()) {
                            if (mRs.next()) {
                                String mil = mRs.getString("mileage");
                                txtVehMileage.setText(mil != null ? mil : "");
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed to load vehicle mileage", ex);
            }

            btnVehAdd.setVisible(false);
            btnVehUpdate.setVisible(true);
            btnVehDelete.setVisible(true);
            btnVehClear.setVisible(true);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        btnDashboard = new javax.swing.JButton();
        btnStudent = new javax.swing.JButton();
        btnBooking = new javax.swing.JButton();
        btnBookingManage = new javax.swing.JButton();
        btnInstructors = new javax.swing.JButton();
        btnVehicle = new javax.swing.JButton();
        btnUserManagement = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        panelHeaderLeft = new javax.swing.JPanel();
        lblHeaderTitle = new javax.swing.JLabel();
        lblHeaderSubtitle = new javax.swing.JLabel();
        panelHeaderRight = new javax.swing.JPanel();
        lblDate = new javax.swing.JLabel();
        lblRole = new javax.swing.JLabel();
        lblUsername = new javax.swing.JLabel();
        Contructor = new javax.swing.JPanel();
        Student = new javax.swing.JPanel();
        panelStudentHeader = new javax.swing.JPanel();
        panelStudentHeaderLeft = new javax.swing.JPanel();
        lblStudentHeaderTitle = new javax.swing.JLabel();
        lblStudentHeaderSub = new javax.swing.JLabel();
        panelStudentHeaderRight = new javax.swing.JPanel();
        txtSearchStudent = new javax.swing.JTextField();
        btnSearchStudent = new javax.swing.JButton();
        btnResetStudent = new javax.swing.JButton();
        panelStudentBody = new javax.swing.JPanel();
        panelStudentFormCard = new javax.swing.JPanel();
        lblStudentCardTitle = new javax.swing.JLabel();
        lblStudentCardSub = new javax.swing.JLabel();
        lblStudentID = new javax.swing.JLabel();
        lblStudentIDVal = new javax.swing.JLabel();
        lblStudentName = new javax.swing.JLabel();
        txtStudentName = new javax.swing.JTextField();
        lblStudentNIC = new javax.swing.JLabel();
        txtStudentNIC = new javax.swing.JTextField();
        lblStudentPhone = new javax.swing.JLabel();
        txtStudentPhone = new javax.swing.JTextField();
        lblStudentAddress = new javax.swing.JLabel();
        txtStudentAddress = new javax.swing.JTextField();
        lblStudentClass = new javax.swing.JLabel();
        cmbStudentClass = new javax.swing.JComboBox<>();
        lblStudentStatus = new javax.swing.JLabel();
        cmbStudentStatus = new javax.swing.JComboBox<>();
        btnAddStudent = new javax.swing.JButton();
        btnUpdateStudent = new javax.swing.JButton();
        btnDeleteStudent = new javax.swing.JButton();
        btnClearStudent = new javax.swing.JButton();
        panelStudentTableCard = new javax.swing.JPanel();
        panelStudentTableHeader = new javax.swing.JPanel();
        lblStudentTableTitle = new javax.swing.JLabel();
        lblStudentTableSub = new javax.swing.JLabel();
        scrollStudentTable = new javax.swing.JScrollPane();
        tableStudents = new javax.swing.JTable();
        panelStudentTableFooter = new javax.swing.JPanel();
        lblStudentCount = new javax.swing.JLabel();
        user_Management = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        btnAddUser = new javax.swing.JButton();
        btnUpdateUser = new javax.swing.JButton();
        btnDeleteUser = new javax.swing.JButton();
        btnClearUser = new javax.swing.JButton();
        jPasswordField1 = new javax.swing.JPasswordField();
        jLabel7 = new javax.swing.JLabel();
        jPasswordField2 = new javax.swing.JPasswordField();
        jLabel6 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        lblUserID = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        CheckFirstTimeLog = new javax.swing.JCheckBox();
        Dashbord = new javax.swing.JPanel();
        panelDashboardCenter = new javax.swing.JPanel();
        panelTopSection = new javax.swing.JPanel();
        panelSummaryCards = new javax.swing.JPanel();
        panelCardStudents = new javax.swing.JPanel();
        lblIconStudents = new javax.swing.JLabel();
        panelCardStudentsText = new javax.swing.JPanel();
        lblTitleStudents = new javax.swing.JLabel();
        lblCountStudents = new javax.swing.JLabel();
        lblTrendStudents = new javax.swing.JLabel();
        panelCardBookings = new javax.swing.JPanel();
        lblIconBookings = new javax.swing.JLabel();
        panelCardBookingsText = new javax.swing.JPanel();
        lblTitleBookings = new javax.swing.JLabel();
        lblCountBookings = new javax.swing.JLabel();
        lblTrendBookings = new javax.swing.JLabel();
        panelCardInstructors = new javax.swing.JPanel();
        lblIconInstructors = new javax.swing.JLabel();
        panelCardInstructorsText = new javax.swing.JPanel();
        lblTitleInstructors = new javax.swing.JLabel();
        lblCountInstructors = new javax.swing.JLabel();
        lblTrendInstructors = new javax.swing.JLabel();
        panelCardVehicles = new javax.swing.JPanel();
        lblIconVehicles = new javax.swing.JLabel();
        panelCardVehiclesText = new javax.swing.JPanel();
        lblTitleVehicles = new javax.swing.JLabel();
        lblCountVehicles = new javax.swing.JLabel();
        lblTrendVehicles = new javax.swing.JLabel();
        panelQuickActions = new javax.swing.JPanel();
        panelQuickHeader = new javax.swing.JPanel();
        lblQuickTitle = new javax.swing.JLabel();
        lblQuickSub = new javax.swing.JLabel();
        panelQuickGrid = new javax.swing.JPanel();
        panelActionRegister = new javax.swing.JPanel();
        panelActionRegText = new javax.swing.JPanel();
        lblActionRegTitle = new javax.swing.JLabel();
        lblActionRegDesc = new javax.swing.JLabel();
        btnActionRegister = new javax.swing.JButton();
        panelActionBooking = new javax.swing.JPanel();
        panelActionBkText = new javax.swing.JPanel();
        lblActionBkTitle = new javax.swing.JLabel();
        lblActionBkDesc = new javax.swing.JLabel();
        btnActionBooking = new javax.swing.JButton();
        panelActionManage = new javax.swing.JPanel();
        panelActionMgText = new javax.swing.JPanel();
        lblActionMgTitle = new javax.swing.JLabel();
        lblActionMgDesc = new javax.swing.JLabel();
        btnActionManage = new javax.swing.JButton();
        panelRecentBookings = new javax.swing.JPanel();
        panelRecentHeader = new javax.swing.JPanel();
        panelRecentTitles = new javax.swing.JPanel();
        lblRecentTitle = new javax.swing.JLabel();
        lblRecentSub = new javax.swing.JLabel();
        btnViewAllBookings = new javax.swing.JButton();
        scrollRecentBookings = new javax.swing.JScrollPane();
        tableRecentBookings = new javax.swing.JTable();
        Instructors = new javax.swing.JPanel();
        panelInstHeader = new javax.swing.JPanel();
        panelInstHeaderLeft = new javax.swing.JPanel();
        lblInstHeaderIcon = new javax.swing.JLabel();
        panelInstHeaderTitles = new javax.swing.JPanel();
        lblInstHeaderTitle = new javax.swing.JLabel();
        lblInstHeaderSubtitle = new javax.swing.JLabel();
        panelInstHeaderRight = new javax.swing.JPanel();
        panelInstBadgeTotal = new javax.swing.JPanel();
        lblInstBadgeTotalCount = new javax.swing.JLabel();
        lblInstBadgeTotalLabel = new javax.swing.JLabel();
        panelInstBadgeActive = new javax.swing.JPanel();
        lblInstBadgeActiveCount = new javax.swing.JLabel();
        lblInstBadgeActiveLabel = new javax.swing.JLabel();
        panelInstBadgeAvailable = new javax.swing.JPanel();
        lblInstBadgeAvailableCount = new javax.swing.JLabel();
        lblInstBadgeAvailableLabel = new javax.swing.JLabel();
        panelInstMain = new javax.swing.JPanel();
        panelInstFormCard = new javax.swing.JPanel();
        panelInstFormHeader = new javax.swing.JPanel();
        lblInstFormHeader = new javax.swing.JLabel();
        scrollInstForm = new javax.swing.JScrollPane();
        panelInstFormFields = new javax.swing.JPanel();
        lblInstId = new javax.swing.JLabel();
        txtInstId = new javax.swing.JTextField();
        lblInstFullName = new javax.swing.JLabel();
        txtInstFullName = new javax.swing.JTextField();
        lblInstPhone = new javax.swing.JLabel();
        txtInstPhone = new javax.swing.JTextField();
        lblInstNic = new javax.swing.JLabel();
        txtInstNic = new javax.swing.JTextField();
        lblInstLicense = new javax.swing.JLabel();
        txtInstLicense = new javax.swing.JTextField();
        lblInstCategory = new javax.swing.JLabel();
        cmbInstCategory = new javax.swing.JComboBox<>();
        lblInstStatus = new javax.swing.JLabel();
        cmbInstStatus = new javax.swing.JComboBox<>();
        panelInstFormActions = new javax.swing.JPanel();
        btnInstAdd = new javax.swing.JButton();
        btnInstUpdate = new javax.swing.JButton();
        btnInstDelete = new javax.swing.JButton();
        btnInstClear = new javax.swing.JButton();
        panelInstTableCard = new javax.swing.JPanel();
        panelInstTableTop = new javax.swing.JPanel();
        lblInstTableTitle = new javax.swing.JLabel();
        panelInstSearchFilter = new javax.swing.JPanel();
        txtInstSearch = new javax.swing.JTextField();
        cmbInstFilterStatus = new javax.swing.JComboBox<>();
        btnInstRefresh = new javax.swing.JButton();
        scrollInstTable = new javax.swing.JScrollPane();
        tableInstructors = new javax.swing.JTable();
        panelInstTableBottom = new javax.swing.JPanel();
        lblInstTableCount = new javax.swing.JLabel();
        Vehicles = new javax.swing.JPanel();
        panelVehHeader = new javax.swing.JPanel();
        panelVehHeaderLeft = new javax.swing.JPanel();
        lblVehHeaderIcon = new javax.swing.JLabel();
        panelVehHeaderTitles = new javax.swing.JPanel();
        lblVehHeaderTitle = new javax.swing.JLabel();
        lblVehHeaderSubtitle = new javax.swing.JLabel();
        panelVehHeaderRight = new javax.swing.JPanel();
        panelVehBadgeTotal = new javax.swing.JPanel();
        lblVehBadgeTotalCount = new javax.swing.JLabel();
        lblVehBadgeTotalLabel = new javax.swing.JLabel();
        panelVehBadgeReady = new javax.swing.JPanel();
        lblVehBadgeReadyCount = new javax.swing.JLabel();
        lblVehBadgeReadyLabel = new javax.swing.JLabel();
        panelVehBadgeService = new javax.swing.JPanel();
        lblVehBadgeServiceCount = new javax.swing.JLabel();
        lblVehBadgeServiceLabel = new javax.swing.JLabel();
        panelVehMain = new javax.swing.JPanel();
        panelVehFormCard = new javax.swing.JPanel();
        panelVehFormHeader = new javax.swing.JPanel();
        lblVehFormHeader = new javax.swing.JLabel();
        scrollVehForm = new javax.swing.JScrollPane();
        panelVehFormFields = new javax.swing.JPanel();
        lblVehId = new javax.swing.JLabel();
        txtVehId = new javax.swing.JTextField();
        lblVehModel = new javax.swing.JLabel();
        txtVehModel = new javax.swing.JTextField();
        lblVehPlate = new javax.swing.JLabel();
        txtVehPlate = new javax.swing.JTextField();
        lblVehCategory = new javax.swing.JLabel();
        cmbVehCategory = new javax.swing.JComboBox<>();
        lblVehTransmission = new javax.swing.JLabel();
        cmbVehTransmission = new javax.swing.JComboBox<>();
        lblVehFuel = new javax.swing.JLabel();
        cmbVehFuel = new javax.swing.JComboBox<>();
        lblVehStatus = new javax.swing.JLabel();
        cmbVehStatus = new javax.swing.JComboBox<>();
        lblVehMileage = new javax.swing.JLabel();
        txtVehMileage = new javax.swing.JTextField();
        panelVehFormActions = new javax.swing.JPanel();
        btnVehAdd = new javax.swing.JButton();
        btnVehUpdate = new javax.swing.JButton();
        btnVehDelete = new javax.swing.JButton();
        btnVehClear = new javax.swing.JButton();
        panelVehTableCard = new javax.swing.JPanel();
        panelVehTableTop = new javax.swing.JPanel();
        lblVehTableTitle = new javax.swing.JLabel();
        panelVehSearchFilter = new javax.swing.JPanel();
        txtVehSearch = new javax.swing.JTextField();
        cmbVehFilterStatus = new javax.swing.JComboBox<>();
        btnVehRefresh = new javax.swing.JButton();
        scrollVehTable = new javax.swing.JScrollPane();
        tableVehicles = new javax.swing.JTable();
        panelVehTableBottom = new javax.swing.JPanel();
        lblVehTableCount = new javax.swing.JLabel();
        Bookings = new javax.swing.JPanel();
        panelBkHeader = new javax.swing.JPanel();
        panelBkHeaderLeft = new javax.swing.JPanel();
        lblBkHeaderIcon = new javax.swing.JLabel();
        panelBkHeaderTitles = new javax.swing.JPanel();
        lblBkHeaderTitle = new javax.swing.JLabel();
        lblBkHeaderSubtitle = new javax.swing.JLabel();
        panelBkHeaderRight = new javax.swing.JPanel();
        panelBkBadgeTotal = new javax.swing.JPanel();
        lblBkBadgeTotalCount = new javax.swing.JLabel();
        lblBkBadgeTotalLabel = new javax.swing.JLabel();
        panelBkBadgeConfirmed = new javax.swing.JPanel();
        lblBkBadgeConfirmedCount = new javax.swing.JLabel();
        lblBkBadgeConfirmedLabel = new javax.swing.JLabel();
        panelBkBadgePending = new javax.swing.JPanel();
        lblBkBadgePendingCount = new javax.swing.JLabel();
        lblBkBadgePendingLabel = new javax.swing.JLabel();
        panelBkMain = new javax.swing.JPanel();
        panelBkFormCard = new javax.swing.JPanel();
        panelBkFormHeader = new javax.swing.JPanel();
        lblBkFormHeader = new javax.swing.JLabel();
        scrollBkForm = new javax.swing.JScrollPane();
        panelBkFormFields = new javax.swing.JPanel();
        lblBkId = new javax.swing.JLabel();
        txtBkId = new javax.swing.JTextField();
        lblBkStudent = new javax.swing.JLabel();
        txtBkStudent = new javax.swing.JTextField();
        lblBkInstructor = new javax.swing.JLabel();
        cmbBkInstructor = new javax.swing.JComboBox<>();
        lblBkVehicle = new javax.swing.JLabel();
        cmbBkVehicle = new javax.swing.JComboBox<>();
        lblBkLessonType = new javax.swing.JLabel();
        cmbBkLessonType = new javax.swing.JComboBox<>();
        lblBkDateTime = new javax.swing.JLabel();
        txtBkDateTime = new javax.swing.JTextField();
        lblBkStatus = new javax.swing.JLabel();
        cmbBkStatus = new javax.swing.JComboBox<>();
        lblBkPayment = new javax.swing.JLabel();
        cmbBkPayment = new javax.swing.JComboBox<>();
        panelBkFormActions = new javax.swing.JPanel();
        btnBkAdd = new javax.swing.JButton();
        btnBkUpdate = new javax.swing.JButton();
        btnBkDelete = new javax.swing.JButton();
        btnBkClear = new javax.swing.JButton();
        panelBkTableCard = new javax.swing.JPanel();
        panelBkTableTop = new javax.swing.JPanel();
        lblBkTableTitle = new javax.swing.JLabel();
        panelBkSearchFilter = new javax.swing.JPanel();
        txtBkSearch = new javax.swing.JTextField();
        cmbBkFilterStatus = new javax.swing.JComboBox<>();
        btnBkRefresh = new javax.swing.JButton();
        scrollBkTable = new javax.swing.JScrollPane();
        tableBookings = new javax.swing.JTable();
        panelBkTableBottom = new javax.swing.JPanel();
        lblBkTableCount = new javax.swing.JLabel();
        Bookings_Management = new javax.swing.JPanel();
        panelBmHeader = new javax.swing.JPanel();
        panelBmHeaderLeft = new javax.swing.JPanel();
        lblBmHeaderIcon = new javax.swing.JLabel();
        panelBmHeaderTitles = new javax.swing.JPanel();
        lblBmHeaderTitle = new javax.swing.JLabel();
        lblBmHeaderSubtitle = new javax.swing.JLabel();
        panelBmHeaderRight = new javax.swing.JPanel();
        panelBmBadgeTotal = new javax.swing.JPanel();
        lblBmBadgeTotalCount = new javax.swing.JLabel();
        lblBmBadgeTotalLabel = new javax.swing.JLabel();
        panelBmBadgeToday = new javax.swing.JPanel();
        lblBmBadgeTodayCount = new javax.swing.JLabel();
        lblBmBadgeTodayLabel = new javax.swing.JLabel();
        panelBmBadgePending = new javax.swing.JPanel();
        lblBmBadgePendingCount = new javax.swing.JLabel();
        lblBmBadgePendingLabel = new javax.swing.JLabel();
        panelBmMain = new javax.swing.JPanel();
        panelBmFormCard = new javax.swing.JPanel();
        panelBmFormHeader = new javax.swing.JPanel();
        lblBmFormHeader = new javax.swing.JLabel();
        scrollBmForm = new javax.swing.JScrollPane();
        panelBmFormFields = new javax.swing.JPanel();
        lblBmId = new javax.swing.JLabel();
        txtBmId = new javax.swing.JTextField();
        lblBmStudent = new javax.swing.JLabel();
        txtBmStudent = new javax.swing.JTextField();
        lblBmInstructor = new javax.swing.JLabel();
        cmbBmInstructor = new javax.swing.JComboBox<>();
        lblBmVehicle = new javax.swing.JLabel();
        cmbBmVehicle = new javax.swing.JComboBox<>();
        lblBmDate = new javax.swing.JLabel();
        txtBmDate = new javax.swing.JTextField();
        lblBmTimeSlot = new javax.swing.JLabel();
        cmbBmTimeSlot = new javax.swing.JComboBox<>();
        lblBmStatus = new javax.swing.JLabel();
        cmbBmStatus = new javax.swing.JComboBox<>();
        lblBmRemarks = new javax.swing.JLabel();
        txtBmRemarks = new javax.swing.JTextField();
        panelBmFormActions = new javax.swing.JPanel();
        btnBmConfirm = new javax.swing.JButton();
        btnBmReschedule = new javax.swing.JButton();
        btnBmCancel = new javax.swing.JButton();
        btnBmClear = new javax.swing.JButton();
        panelBmTableCard = new javax.swing.JPanel();
        panelBmTableTop = new javax.swing.JPanel();
        lblBmTableTitle = new javax.swing.JLabel();
        panelBmSearchFilter = new javax.swing.JPanel();
        txtBmSearch = new javax.swing.JTextField();
        cmbBmFilterStatus = new javax.swing.JComboBox<>();
        btnBmRefresh = new javax.swing.JButton();
        scrollBmTable = new javax.swing.JScrollPane();
        tableBookingManagement = new javax.swing.JTable();
        panelBmTableBottom = new javax.swing.JPanel();
        lblBmTableCount = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridLayout(1, 0));

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setBackground(new java.awt.Color(204, 204, 255));
        jPanel2.setPreferredSize(new java.awt.Dimension(200, 439));
        jPanel2.setLayout(new java.awt.GridLayout(10, 1, 5, 5));

        btnDashboard.setBackground(new java.awt.Color(204, 204, 255));
        btnDashboard.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnDashboard.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_combo_chart_32px.png"))); // NOI18N
        btnDashboard.setText("Dashboard");
        btnDashboard.setBorder(null);
        btnDashboard.setMargin(null);
        btnDashboard.setPreferredSize(new java.awt.Dimension(150, 50));
        btnDashboard.addActionListener(this::btnDashboardActionPerformed);
        jPanel2.add(btnDashboard);

        btnStudent.setBackground(new java.awt.Color(204, 204, 255));
        btnStudent.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnStudent.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_student_male_32px.png"))); // NOI18N
        btnStudent.setText("Students");
        btnStudent.setBorder(null);
        btnStudent.setPreferredSize(new java.awt.Dimension(150, 50));
        btnStudent.addActionListener(this::btnStudentActionPerformed);
        jPanel2.add(btnStudent);

        btnBooking.setBackground(new java.awt.Color(204, 204, 255));
        btnBooking.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnBooking.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_new_ticket_32px.png"))); // NOI18N
        btnBooking.setText("Bookings");
        btnBooking.setBorder(null);
        btnBooking.setPreferredSize(new java.awt.Dimension(150, 50));
        btnBooking.addActionListener(this::btnBookingActionPerformed);
        jPanel2.add(btnBooking);

        btnBookingManage.setBackground(new java.awt.Color(204, 204, 255));
        btnBookingManage.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnBookingManage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_event_management_32px.png"))); // NOI18N
        btnBookingManage.setText("Booking Manage");
        btnBookingManage.setBorder(null);
        btnBookingManage.setPreferredSize(new java.awt.Dimension(150, 50));
        btnBookingManage.addActionListener(this::btnBookingManageActionPerformed);
        jPanel2.add(btnBookingManage);

        btnInstructors.setBackground(new java.awt.Color(204, 204, 255));
        btnInstructors.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnInstructors.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_personal_trainer_32px.png"))); // NOI18N
        btnInstructors.setText("Instructors");
        btnInstructors.setBorder(null);
        btnInstructors.setPreferredSize(new java.awt.Dimension(150, 50));
        btnInstructors.addActionListener(this::btnInstructorsActionPerformed);
        jPanel2.add(btnInstructors);

        btnVehicle.setBackground(new java.awt.Color(204, 204, 255));
        btnVehicle.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnVehicle.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_car_32px.png"))); // NOI18N
        btnVehicle.setText("Vehicles");
        btnVehicle.setBorder(null);
        btnVehicle.setPreferredSize(new java.awt.Dimension(150, 50));
        btnVehicle.addActionListener(this::btnVehicleActionPerformed);
        jPanel2.add(btnVehicle);

        btnUserManagement.setBackground(new java.awt.Color(204, 204, 255));
        btnUserManagement.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnUserManagement.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_conference_32px.png"))); // NOI18N
        btnUserManagement.setText("User Management");
        btnUserManagement.setBorder(null);
        btnUserManagement.setPreferredSize(new java.awt.Dimension(150, 50));
        btnUserManagement.addActionListener(this::btnUserManagementActionPerformed);
        jPanel2.add(btnUserManagement);

        jButton8.setBackground(new java.awt.Color(255, 51, 51));
        jButton8.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton8.setForeground(new java.awt.Color(255, 255, 255));
        jButton8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Logout_32px_1.png"))); // NOI18N
        jButton8.setText("LOG OUT");
        jButton8.setBorder(null);
        jButton8.setPreferredSize(new java.awt.Dimension(150, 50));
        jButton8.addActionListener(this::jButton8ActionPerformed);
        jPanel2.add(jButton8);

        jPanel1.add(jPanel2, java.awt.BorderLayout.LINE_START);

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setPreferredSize(new java.awt.Dimension(884, 75));
        jPanel6.setLayout(new java.awt.BorderLayout());

        panelHeaderLeft.setBackground(new java.awt.Color(255, 255, 255));
        panelHeaderLeft.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 24, 10, 10));
        panelHeaderLeft.setLayout(new java.awt.GridLayout(2, 1));

        lblHeaderTitle.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lblHeaderTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblHeaderTitle.setText("Dashboard");
        panelHeaderLeft.add(lblHeaderTitle);

        lblHeaderSubtitle.setForeground(new java.awt.Color(100, 116, 139));
        lblHeaderSubtitle.setText("SafeDrive Pro - Driving School Practical Booking System");
        panelHeaderLeft.add(lblHeaderSubtitle);

        jPanel6.add(panelHeaderLeft, java.awt.BorderLayout.LINE_START);

        panelHeaderRight.setBackground(new java.awt.Color(255, 255, 255));
        panelHeaderRight.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 10, 10, 24));
        panelHeaderRight.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 16, 0));

        lblDate.setForeground(new java.awt.Color(100, 116, 139));
        lblDate.setText("Monday, 21 September 2026");
        panelHeaderRight.add(lblDate);

        lblRole.setFont(new java.awt.Font("Segoe UI", 1, 11)); // NOI18N
        lblRole.setForeground(new java.awt.Color(37, 99, 235));
        lblRole.setText("ADMIN");
        panelHeaderRight.add(lblRole);

        lblUsername.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblUsername.setForeground(new java.awt.Color(15, 23, 42));
        lblUsername.setText("Admin User");
        panelHeaderRight.add(lblUsername);

        jPanel6.add(panelHeaderRight, java.awt.BorderLayout.LINE_END);

        jPanel1.add(jPanel6, java.awt.BorderLayout.PAGE_START);

        Contructor.setLayout(new java.awt.CardLayout());

        Student.setBackground(new java.awt.Color(248, 250, 252));
        Student.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 24, 20, 24));
        Student.setLayout(new java.awt.BorderLayout(0, 14));

        panelStudentHeader.setBackground(new java.awt.Color(248, 250, 252));
        panelStudentHeader.setLayout(new java.awt.BorderLayout(16, 0));

        panelStudentHeaderLeft.setBackground(new java.awt.Color(248, 250, 252));
        panelStudentHeaderLeft.setLayout(new java.awt.GridLayout(2, 1, 0, 2));

        lblStudentHeaderTitle.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lblStudentHeaderTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblStudentHeaderTitle.setText("Student Management");
        panelStudentHeaderLeft.add(lblStudentHeaderTitle);

        lblStudentHeaderSub.setForeground(new java.awt.Color(100, 116, 139));
        lblStudentHeaderSub.setText("Enroll learners, manage admission records, and track training status");
        panelStudentHeaderLeft.add(lblStudentHeaderSub);

        panelStudentHeader.add(panelStudentHeaderLeft, java.awt.BorderLayout.LINE_START);

        panelStudentHeaderRight.setBackground(new java.awt.Color(248, 250, 252));
        panelStudentHeaderRight.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 4));

        txtSearchStudent.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtSearchStudent.setPreferredSize(new java.awt.Dimension(220, 34));
        panelStudentHeaderRight.add(txtSearchStudent);

        btnSearchStudent.setBackground(new java.awt.Color(37, 99, 235));
        btnSearchStudent.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnSearchStudent.setForeground(new java.awt.Color(255, 255, 255));
        btnSearchStudent.setText("Search");
        btnSearchStudent.setBorder(null);
        btnSearchStudent.setFocusPainted(false);
        btnSearchStudent.setPreferredSize(new java.awt.Dimension(80, 34));
        panelStudentHeaderRight.add(btnSearchStudent);

        btnResetStudent.setBackground(new java.awt.Color(241, 245, 249));
        btnResetStudent.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnResetStudent.setForeground(new java.awt.Color(37, 99, 235));
        btnResetStudent.setText("Show All");
        btnResetStudent.setBorder(null);
        btnResetStudent.setFocusPainted(false);
        btnResetStudent.setPreferredSize(new java.awt.Dimension(85, 34));
        btnResetStudent.addActionListener(this::btnResetStudentActionPerformed);
        panelStudentHeaderRight.add(btnResetStudent);

        panelStudentHeader.add(panelStudentHeaderRight, java.awt.BorderLayout.LINE_END);

        Student.add(panelStudentHeader, java.awt.BorderLayout.PAGE_START);

        panelStudentBody.setBackground(new java.awt.Color(248, 250, 252));
        panelStudentBody.setLayout(new java.awt.BorderLayout(16, 0));

        panelStudentFormCard.setBackground(new java.awt.Color(255, 255, 255));
        panelStudentFormCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelStudentFormCard.setPreferredSize(new java.awt.Dimension(430, 520));

        lblStudentCardTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblStudentCardTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblStudentCardTitle.setText("Student Registration");

        lblStudentCardSub.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblStudentCardSub.setForeground(new java.awt.Color(100, 116, 139));
        lblStudentCardSub.setText("Fill out required details to register or update a learner");

        lblStudentID.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStudentID.setForeground(new java.awt.Color(51, 65, 85));
        lblStudentID.setText("Student ID");

        lblStudentIDVal.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblStudentIDVal.setForeground(new java.awt.Color(37, 99, 235));
        lblStudentIDVal.setText("STU-Auto");

        lblStudentName.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStudentName.setForeground(new java.awt.Color(51, 65, 85));
        lblStudentName.setText("Full Name *");

        txtStudentName.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        lblStudentNIC.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStudentNIC.setForeground(new java.awt.Color(51, 65, 85));
        lblStudentNIC.setText("NIC Number *");

        txtStudentNIC.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        lblStudentPhone.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStudentPhone.setForeground(new java.awt.Color(51, 65, 85));
        lblStudentPhone.setText("Phone Number *");

        txtStudentPhone.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        lblStudentAddress.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStudentAddress.setForeground(new java.awt.Color(51, 65, 85));
        lblStudentAddress.setText("Address");

        txtStudentAddress.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        lblStudentClass.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStudentClass.setForeground(new java.awt.Color(51, 65, 85));
        lblStudentClass.setText("Vehicle Class");

        cmbStudentClass.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N

        lblStudentStatus.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStudentStatus.setForeground(new java.awt.Color(51, 65, 85));
        lblStudentStatus.setText("Learning Status");

        cmbStudentStatus.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N

        btnAddStudent.setBackground(new java.awt.Color(37, 99, 235));
        btnAddStudent.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnAddStudent.setForeground(new java.awt.Color(255, 255, 255));
        btnAddStudent.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Add_Male_User_Group_25px.png"))); // NOI18N
        btnAddStudent.setText("Add Student");
        btnAddStudent.setBorder(null);
        btnAddStudent.setFocusPainted(false);
        btnAddStudent.addActionListener(this::btnAddStudentActionPerformed);

        btnUpdateStudent.setBackground(new java.awt.Color(102, 153, 255));
        btnUpdateStudent.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnUpdateStudent.setForeground(new java.awt.Color(255, 255, 255));
        btnUpdateStudent.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Female_User_Update_25px.png"))); // NOI18N
        btnUpdateStudent.setText("Update");
        btnUpdateStudent.setBorder(null);
        btnUpdateStudent.setFocusPainted(false);
        btnUpdateStudent.addActionListener(this::btnUpdateStudentActionPerformed);

        btnDeleteStudent.setBackground(new java.awt.Color(204, 0, 51));
        btnDeleteStudent.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnDeleteStudent.setForeground(new java.awt.Color(255, 255, 255));
        btnDeleteStudent.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Delete_25px.png"))); // NOI18N
        btnDeleteStudent.setText("Delete");
        btnDeleteStudent.setBorder(null);
        btnDeleteStudent.setFocusPainted(false);
        btnDeleteStudent.addActionListener(this::btnDeleteStudentActionPerformed);

        btnClearStudent.setBackground(new java.awt.Color(100, 116, 139));
        btnClearStudent.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnClearStudent.setForeground(new java.awt.Color(255, 255, 255));
        btnClearStudent.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_broom_25px.png"))); // NOI18N
        btnClearStudent.setText("Clear");
        btnClearStudent.setBorder(null);
        btnClearStudent.setFocusPainted(false);
        btnClearStudent.addActionListener(this::btnClearStudentActionPerformed);

        javax.swing.GroupLayout panelStudentFormCardLayout = new javax.swing.GroupLayout(panelStudentFormCard);
        panelStudentFormCard.setLayout(panelStudentFormCardLayout);
        panelStudentFormCardLayout.setHorizontalGroup(
            panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelStudentFormCardLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblStudentCardTitle)
                    .addComponent(lblStudentCardSub)
                    .addGroup(panelStudentFormCardLayout.createSequentialGroup()
                        .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblStudentID, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                            .addComponent(lblStudentName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblStudentNIC, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblStudentPhone, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblStudentAddress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblStudentClass, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblStudentStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblStudentIDVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtStudentName)
                            .addComponent(txtStudentNIC)
                            .addComponent(txtStudentPhone)
                            .addComponent(txtStudentAddress)
                            .addComponent(cmbStudentClass, 0, 260, Short.MAX_VALUE)
                            .addComponent(cmbStudentStatus, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(panelStudentFormCardLayout.createSequentialGroup()
                        .addComponent(btnAddStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnUpdateStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelStudentFormCardLayout.createSequentialGroup()
                        .addComponent(btnDeleteStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnClearStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(26, Short.MAX_VALUE))
        );
        panelStudentFormCardLayout.setVerticalGroup(
            panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelStudentFormCardLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(lblStudentCardTitle)
                .addGap(2, 2, 2)
                .addComponent(lblStudentCardSub)
                .addGap(16, 16, 16)
                .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblStudentIDVal, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStudentName)
                    .addComponent(txtStudentName, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStudentNIC)
                    .addComponent(txtStudentNIC, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStudentPhone)
                    .addComponent(txtStudentPhone, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStudentAddress)
                    .addComponent(txtStudentAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStudentClass)
                    .addComponent(cmbStudentClass, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStudentStatus)
                    .addComponent(cmbStudentStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAddStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnUpdateStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(panelStudentFormCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnDeleteStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnClearStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(151, Short.MAX_VALUE))
        );

        panelStudentBody.add(panelStudentFormCard, java.awt.BorderLayout.LINE_START);

        panelStudentTableCard.setBackground(new java.awt.Color(255, 255, 255));
        panelStudentTableCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelStudentTableCard.setLayout(new java.awt.BorderLayout(0, 8));

        panelStudentTableHeader.setBackground(new java.awt.Color(255, 255, 255));
        panelStudentTableHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 8, 16));
        panelStudentTableHeader.setLayout(new java.awt.GridLayout(2, 1, 0, 2));

        lblStudentTableTitle.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblStudentTableTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblStudentTableTitle.setText("Registered Students Directory");
        panelStudentTableHeader.add(lblStudentTableTitle);

        lblStudentTableSub.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblStudentTableSub.setForeground(new java.awt.Color(100, 116, 139));
        lblStudentTableSub.setText("List of all enrolled students, contact numbers, vehicle classes, and status");
        panelStudentTableHeader.add(lblStudentTableSub);

        panelStudentTableCard.add(panelStudentTableHeader, java.awt.BorderLayout.PAGE_START);

        scrollStudentTable.setBackground(new java.awt.Color(255, 255, 255));
        scrollStudentTable.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tableStudents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"STU-1001", "Kasun Perera", "199824501234", "0771234567", "12/A, Temple Rd, Colombo", "Class B (Dual Purpose / Car)", "Practical Training"},
                {"STU-1002", "Nimali Fernando", "200165403219", "0719876543", "45, Galle Rd, Kalutara", "Class A & B (Combo)", "Theory Passed"},
                {"STU-1003", "Dinesh Jayasinghe", "199512304567", "0754567890", "78, Kandy Rd, Kadawatha", "Class B (Dual Purpose / Car)", "Active Learner"},
                {"STU-1004", "Sanduni Wickramasinghe", "200278901245", "0763456789", "15, High Level Rd, Nugegoda", "Class B1 (Auto Light Vehicle)", "Trial / Exam Ready"},
                {"STU-1005", "Ruwan Tharaka", "199732109876", "0702345678", "89, Negombo Rd, Ja-Ela", "Class A (Motorcycle)", "Practical Training"},
                {"STU-1006", "Anoma Senanayake", "199265409812", "0725678901", "23, Station Rd, Gampaha", "Class B (Dual Purpose / Car)", "Completed"}
            },
            new String [] {
                "Student ID", "Full Name", "NIC", "Phone", "Address", "Vehicle Class", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableStudents.setFillsViewportHeight(true);
        tableStudents.setRowHeight(34);
        tableStudents.setShowGrid(false);
        tableStudents.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableStudentsMouseClicked(evt);
            }
        });
        scrollStudentTable.setViewportView(tableStudents);

        panelStudentTableCard.add(scrollStudentTable, java.awt.BorderLayout.CENTER);

        panelStudentTableFooter.setBackground(new java.awt.Color(255, 255, 255));
        panelStudentTableFooter.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16));
        panelStudentTableFooter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        lblStudentCount.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblStudentCount.setForeground(new java.awt.Color(100, 116, 139));
        lblStudentCount.setText("Showing 6 enrolled students | Click row to inspect details");
        panelStudentTableFooter.add(lblStudentCount);

        panelStudentTableCard.add(panelStudentTableFooter, java.awt.BorderLayout.PAGE_END);

        panelStudentBody.add(panelStudentTableCard, java.awt.BorderLayout.CENTER);

        Student.add(panelStudentBody, java.awt.BorderLayout.CENTER);

        Contructor.add(Student, "cardStudents");

        user_Management.setBackground(new java.awt.Color(248, 250, 252));
        user_Management.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 24, 20, 24));
        user_Management.setLayout(new java.awt.BorderLayout(16, 0));

        jPanel5.setBackground(new java.awt.Color(0, 0, 0));
        jPanel5.setLayout(new java.awt.BorderLayout());

        jTable1.setFillsViewportHeight(true);
        jTable1.setRowHeight(30);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "User_ID", "Username", "NIC", "role"
            }
        ));
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jPanel5.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        user_Management.add(jPanel5, java.awt.BorderLayout.CENTER);

        jPanel4.setBackground(new java.awt.Color(248, 250, 252));
        jPanel4.setPreferredSize(new java.awt.Dimension(430, 520));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("ADD USER");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("User ID ");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Username   ");

        jTextField2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("Password   ");

        jTextField4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("NIC ");

        btnAddUser.setBackground(new java.awt.Color(102, 153, 255));
        btnAddUser.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnAddUser.setForeground(new java.awt.Color(255, 255, 255));
        btnAddUser.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Add_Male_User_Group_25px.png"))); // NOI18N
        btnAddUser.setText(" Add User");
        btnAddUser.setBorder(null);
        btnAddUser.addActionListener(this::btnAddUserActionPerformed);

        btnUpdateUser.setBackground(new java.awt.Color(102, 153, 255));
        btnUpdateUser.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnUpdateUser.setForeground(new java.awt.Color(255, 255, 255));
        btnUpdateUser.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Female_User_Update_25px.png"))); // NOI18N
        btnUpdateUser.setText("Update ");
        btnUpdateUser.setBorder(null);
        btnUpdateUser.addActionListener(this::btnUpdateUserActionPerformed);

        btnDeleteUser.setBackground(new java.awt.Color(204, 0, 51));
        btnDeleteUser.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnDeleteUser.setForeground(new java.awt.Color(255, 255, 255));
        btnDeleteUser.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Delete_25px.png"))); // NOI18N
        btnDeleteUser.setText("Delete ");
        btnDeleteUser.setBorder(null);
        btnDeleteUser.addActionListener(this::btnDeleteUserActionPerformed);

        btnClearUser.setBackground(new java.awt.Color(204, 0, 51));
        btnClearUser.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnClearUser.setForeground(new java.awt.Color(255, 255, 255));
        btnClearUser.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_broom_25px.png"))); // NOI18N
        btnClearUser.setText("Clear");
        btnClearUser.setBorder(null);
        btnClearUser.addActionListener(this::btnClearUserActionPerformed);

        jPasswordField1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        jLabel7.setText("Re-Password   ");

        jPasswordField2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setText("ROLE");

        jComboBox1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Staff", "Admin" }));

        jLabel8.setForeground(new java.awt.Color(255, 51, 51));
        jLabel8.setText("If you leave the password field in this file blank,");

        jLabel9.setForeground(new java.awt.Color(255, 51, 51));
        jLabel9.setText(" the default password will be added automatically.");

        lblUserID.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_password_25px_1.png"))); // NOI18N
        jLabel10.setText("Change Default Password");
        jLabel10.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel10MouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jLabel10MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jLabel10MouseExited(evt);
            }
        });

        CheckFirstTimeLog.setForeground(new java.awt.Color(204, 0, 0));
        CheckFirstTimeLog.setText("Disable First Time Login Change Password");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel10)
                .addGap(29, 29, 29))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(CheckFirstTimeLog)
                .addContainerGap())
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(40, 40, 40)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(5, 5, 5)
                                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPasswordField2, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(24, 24, 24)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(btnUpdateUser, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnDeleteUser, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnClearUser, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(39, 39, 39)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblUserID, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(23, 23, 23)
                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(6, 6, 6))))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(85, 85, 85)
                        .addComponent(btnAddUser, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblUserID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE))
                .addGap(16, 16, 16)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPasswordField2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CheckFirstTimeLog)
                .addGap(13, 13, 13)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnAddUser, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnUpdateUser, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDeleteUser, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnClearUser, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(162, Short.MAX_VALUE))
        );

        user_Management.add(jPanel4, java.awt.BorderLayout.LINE_START);

        Contructor.add(user_Management, "cardUserManagement");

        Dashbord.setBackground(new java.awt.Color(248, 250, 252));
        Dashbord.setLayout(new java.awt.BorderLayout());

        panelDashboardCenter.setBackground(new java.awt.Color(248, 250, 252));
        panelDashboardCenter.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 24, 20, 24));
        panelDashboardCenter.setLayout(new java.awt.BorderLayout());

        panelTopSection.setBackground(new java.awt.Color(248, 250, 252));
        panelTopSection.setLayout(new java.awt.BorderLayout());

        panelSummaryCards.setBackground(new java.awt.Color(248, 250, 252));
        panelSummaryCards.setPreferredSize(new java.awt.Dimension(800, 110));
        panelSummaryCards.setLayout(new java.awt.GridLayout(1, 4, 16, 0));

        panelCardStudents.setBackground(new java.awt.Color(255, 255, 255));
        panelCardStudents.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelCardStudents.setLayout(new java.awt.BorderLayout());

        lblIconStudents.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 10));
        lblIconStudents.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_student_male_32px.png"))); // NOI18N
        panelCardStudents.add(lblIconStudents, java.awt.BorderLayout.LINE_START);

        panelCardStudentsText.setBackground(new java.awt.Color(255, 255, 255));
        panelCardStudentsText.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 10, 10));
        panelCardStudentsText.setLayout(new java.awt.GridLayout(3, 1));

        lblTitleStudents.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblTitleStudents.setForeground(new java.awt.Color(100, 116, 139));
        lblTitleStudents.setText("Total Students");
        panelCardStudentsText.add(lblTitleStudents);

        lblCountStudents.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblCountStudents.setForeground(new java.awt.Color(15, 23, 42));
        lblCountStudents.setText("148");
        panelCardStudentsText.add(lblCountStudents);

        lblTrendStudents.setFont(new java.awt.Font("Segoe UI", 1, 11)); // NOI18N
        lblTrendStudents.setForeground(new java.awt.Color(22, 163, 74));
        lblTrendStudents.setText("+12% this month");
        panelCardStudentsText.add(lblTrendStudents);

        panelCardStudents.add(panelCardStudentsText, java.awt.BorderLayout.CENTER);

        panelSummaryCards.add(panelCardStudents);

        panelCardBookings.setBackground(new java.awt.Color(255, 255, 255));
        panelCardBookings.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelCardBookings.setLayout(new java.awt.BorderLayout());

        lblIconBookings.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 10));
        lblIconBookings.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_new_ticket_32px.png"))); // NOI18N
        panelCardBookings.add(lblIconBookings, java.awt.BorderLayout.LINE_START);

        panelCardBookingsText.setBackground(new java.awt.Color(255, 255, 255));
        panelCardBookingsText.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 10, 10));
        panelCardBookingsText.setLayout(new java.awt.GridLayout(3, 1));

        lblTitleBookings.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblTitleBookings.setForeground(new java.awt.Color(100, 116, 139));
        lblTitleBookings.setText("Today's Bookings");
        panelCardBookingsText.add(lblTitleBookings);

        lblCountBookings.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblCountBookings.setForeground(new java.awt.Color(15, 23, 42));
        lblCountBookings.setText("14");
        panelCardBookingsText.add(lblCountBookings);

        lblTrendBookings.setFont(new java.awt.Font("Segoe UI", 1, 11)); // NOI18N
        lblTrendBookings.setForeground(new java.awt.Color(13, 148, 136));
        lblTrendBookings.setText("4 in progress");
        panelCardBookingsText.add(lblTrendBookings);

        panelCardBookings.add(panelCardBookingsText, java.awt.BorderLayout.CENTER);

        panelSummaryCards.add(panelCardBookings);

        panelCardInstructors.setBackground(new java.awt.Color(255, 255, 255));
        panelCardInstructors.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelCardInstructors.setLayout(new java.awt.BorderLayout());

        lblIconInstructors.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 10));
        lblIconInstructors.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_personal_trainer_32px.png"))); // NOI18N
        panelCardInstructors.add(lblIconInstructors, java.awt.BorderLayout.LINE_START);

        panelCardInstructorsText.setBackground(new java.awt.Color(255, 255, 255));
        panelCardInstructorsText.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 10, 10));
        panelCardInstructorsText.setLayout(new java.awt.GridLayout(3, 1));

        lblTitleInstructors.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblTitleInstructors.setForeground(new java.awt.Color(100, 116, 139));
        lblTitleInstructors.setText("Total Instructors");
        panelCardInstructorsText.add(lblTitleInstructors);

        lblCountInstructors.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblCountInstructors.setForeground(new java.awt.Color(15, 23, 42));
        lblCountInstructors.setText("8");
        panelCardInstructorsText.add(lblCountInstructors);

        lblTrendInstructors.setFont(new java.awt.Font("Segoe UI", 1, 11)); // NOI18N
        lblTrendInstructors.setForeground(new java.awt.Color(79, 70, 229));
        lblTrendInstructors.setText("All active on duty");
        panelCardInstructorsText.add(lblTrendInstructors);

        panelCardInstructors.add(panelCardInstructorsText, java.awt.BorderLayout.CENTER);

        panelSummaryCards.add(panelCardInstructors);

        panelCardVehicles.setBackground(new java.awt.Color(255, 255, 255));
        panelCardVehicles.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelCardVehicles.setLayout(new java.awt.BorderLayout());

        lblIconVehicles.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 10));
        lblIconVehicles.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_car_32px.png"))); // NOI18N
        panelCardVehicles.add(lblIconVehicles, java.awt.BorderLayout.LINE_START);

        panelCardVehiclesText.setBackground(new java.awt.Color(255, 255, 255));
        panelCardVehiclesText.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 10, 10));
        panelCardVehiclesText.setLayout(new java.awt.GridLayout(3, 1));

        lblTitleVehicles.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblTitleVehicles.setForeground(new java.awt.Color(100, 116, 139));
        lblTitleVehicles.setText("Total Vehicles");
        panelCardVehiclesText.add(lblTitleVehicles);

        lblCountVehicles.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblCountVehicles.setForeground(new java.awt.Color(15, 23, 42));
        lblCountVehicles.setText("12");
        panelCardVehiclesText.add(lblCountVehicles);

        lblTrendVehicles.setFont(new java.awt.Font("Segoe UI", 1, 11)); // NOI18N
        lblTrendVehicles.setForeground(new java.awt.Color(217, 119, 6));
        lblTrendVehicles.setText("10 ready, 2 service");
        panelCardVehiclesText.add(lblTrendVehicles);

        panelCardVehicles.add(panelCardVehiclesText, java.awt.BorderLayout.CENTER);

        panelSummaryCards.add(panelCardVehicles);

        panelTopSection.add(panelSummaryCards, java.awt.BorderLayout.PAGE_START);

        panelQuickActions.setBackground(new java.awt.Color(248, 250, 252));
        panelQuickActions.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 0, 12, 0));
        panelQuickActions.setLayout(new java.awt.BorderLayout());

        panelQuickHeader.setBackground(new java.awt.Color(248, 250, 252));
        panelQuickHeader.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 4));

        lblQuickTitle.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblQuickTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblQuickTitle.setText("Quick Actions");
        panelQuickHeader.add(lblQuickTitle);

        lblQuickSub.setForeground(new java.awt.Color(100, 116, 139));
        lblQuickSub.setText("  — Common shortcuts for daily driving school workflow");
        panelQuickHeader.add(lblQuickSub);

        panelQuickActions.add(panelQuickHeader, java.awt.BorderLayout.PAGE_START);

        panelQuickGrid.setBackground(new java.awt.Color(248, 250, 252));
        panelQuickGrid.setPreferredSize(new java.awt.Dimension(800, 72));
        panelQuickGrid.setLayout(new java.awt.GridLayout(1, 3, 16, 0));

        panelActionRegister.setBackground(new java.awt.Color(255, 255, 255));
        panelActionRegister.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelActionRegister.setLayout(new java.awt.BorderLayout());

        panelActionRegText.setBackground(new java.awt.Color(255, 255, 255));
        panelActionRegText.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 14, 8, 10));
        panelActionRegText.setLayout(new java.awt.GridLayout(2, 1));

        lblActionRegTitle.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblActionRegTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblActionRegTitle.setText("Register New Student");
        panelActionRegText.add(lblActionRegTitle);

        lblActionRegDesc.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblActionRegDesc.setForeground(new java.awt.Color(100, 116, 139));
        lblActionRegDesc.setText("Enroll student & licence class");
        panelActionRegText.add(lblActionRegDesc);

        panelActionRegister.add(panelActionRegText, java.awt.BorderLayout.CENTER);

        btnActionRegister.setBackground(new java.awt.Color(37, 99, 235));
        btnActionRegister.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnActionRegister.setForeground(new java.awt.Color(255, 255, 255));
        btnActionRegister.setText("+ Register");
        btnActionRegister.setBorder(null);
        btnActionRegister.setFocusPainted(false);
        btnActionRegister.setPreferredSize(new java.awt.Dimension(90, 32));
        btnActionRegister.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnActionRegisterMouseClicked(evt);
            }
        });
        panelActionRegister.add(btnActionRegister, java.awt.BorderLayout.PAGE_START);

        panelQuickGrid.add(panelActionRegister);

        panelActionBooking.setBackground(new java.awt.Color(255, 255, 255));
        panelActionBooking.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelActionBooking.setLayout(new java.awt.BorderLayout());

        panelActionBkText.setBackground(new java.awt.Color(255, 255, 255));
        panelActionBkText.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 14, 8, 10));
        panelActionBkText.setLayout(new java.awt.GridLayout(2, 1));

        lblActionBkTitle.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblActionBkTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblActionBkTitle.setText("Create Practical Booking");
        panelActionBkText.add(lblActionBkTitle);

        lblActionBkDesc.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblActionBkDesc.setForeground(new java.awt.Color(100, 116, 139));
        lblActionBkDesc.setText("Schedule driving lesson/test");
        panelActionBkText.add(lblActionBkDesc);

        panelActionBooking.add(panelActionBkText, java.awt.BorderLayout.CENTER);

        btnActionBooking.setBackground(new java.awt.Color(13, 148, 136));
        btnActionBooking.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnActionBooking.setForeground(new java.awt.Color(255, 255, 255));
        btnActionBooking.setText("+ Booking");
        btnActionBooking.setBorder(null);
        btnActionBooking.setFocusPainted(false);
        btnActionBooking.setPreferredSize(new java.awt.Dimension(90, 32));
        btnActionBooking.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnActionBookingMouseClicked(evt);
            }
        });
        panelActionBooking.add(btnActionBooking, java.awt.BorderLayout.PAGE_START);

        panelQuickGrid.add(panelActionBooking);

        panelActionManage.setBackground(new java.awt.Color(255, 255, 255));
        panelActionManage.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelActionManage.setLayout(new java.awt.BorderLayout());

        panelActionMgText.setBackground(new java.awt.Color(255, 255, 255));
        panelActionMgText.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 14, 8, 10));
        panelActionMgText.setLayout(new java.awt.GridLayout(2, 1));

        lblActionMgTitle.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblActionMgTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblActionMgTitle.setText("Booking Management");
        panelActionMgText.add(lblActionMgTitle);

        lblActionMgDesc.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblActionMgDesc.setForeground(new java.awt.Color(100, 116, 139));
        lblActionMgDesc.setText("Assign instructors & slots");
        panelActionMgText.add(lblActionMgDesc);

        panelActionManage.add(panelActionMgText, java.awt.BorderLayout.CENTER);

        btnActionManage.setBackground(new java.awt.Color(79, 70, 229));
        btnActionManage.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnActionManage.setForeground(new java.awt.Color(255, 255, 255));
        btnActionManage.setText("Manage →");
        btnActionManage.setBorder(null);
        btnActionManage.setFocusPainted(false);
        btnActionManage.setPreferredSize(new java.awt.Dimension(90, 32));
        btnActionManage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnActionManageMouseClicked(evt);
            }
        });
        panelActionManage.add(btnActionManage, java.awt.BorderLayout.PAGE_START);

        panelQuickGrid.add(panelActionManage);

        panelQuickActions.add(panelQuickGrid, java.awt.BorderLayout.CENTER);

        panelTopSection.add(panelQuickActions, java.awt.BorderLayout.CENTER);

        panelDashboardCenter.add(panelTopSection, java.awt.BorderLayout.PAGE_START);

        panelRecentBookings.setBackground(new java.awt.Color(248, 250, 252));
        panelRecentBookings.setLayout(new java.awt.BorderLayout());

        panelRecentHeader.setBackground(new java.awt.Color(248, 250, 252));
        panelRecentHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 8, 0));
        panelRecentHeader.setLayout(new java.awt.BorderLayout());

        panelRecentTitles.setBackground(new java.awt.Color(248, 250, 252));
        panelRecentTitles.setLayout(new java.awt.GridLayout(2, 1));

        lblRecentTitle.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblRecentTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblRecentTitle.setText("Recent Practical Bookings");
        panelRecentTitles.add(lblRecentTitle);

        lblRecentSub.setForeground(new java.awt.Color(100, 116, 139));
        lblRecentSub.setText("Latest student driving sessions and instructor assignments");
        panelRecentTitles.add(lblRecentSub);

        panelRecentHeader.add(panelRecentTitles, java.awt.BorderLayout.LINE_START);

        btnViewAllBookings.setBackground(new java.awt.Color(241, 245, 249));
        btnViewAllBookings.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnViewAllBookings.setForeground(new java.awt.Color(37, 99, 235));
        btnViewAllBookings.setText("View All Bookings →");
        btnViewAllBookings.setBorder(null);
        btnViewAllBookings.setFocusPainted(false);
        btnViewAllBookings.setPreferredSize(new java.awt.Dimension(150, 32));
        panelRecentHeader.add(btnViewAllBookings, java.awt.BorderLayout.LINE_END);

        panelRecentBookings.add(panelRecentHeader, java.awt.BorderLayout.PAGE_START);

        scrollRecentBookings.setBackground(new java.awt.Color(255, 255, 255));
        scrollRecentBookings.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        tableRecentBookings.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"BK-1042", "Kasun Perera", "Kamal Silva", "Toyota Vitz (CAB-1234)", "Today, 09:30 AM", "Practical Driving", "Confirmed"},
                {"BK-1043", "Nimali Fernando", "Sunil Perera", "Nissan March (WP-5678)", "Today, 11:00 AM", "Pre-Test Practice", "In Progress"},
                {"BK-1044", "Dinesh Jayasinghe", "Kamal Silva", "Honda Fit (CAR-9012)", "Today, 02:00 PM", "Highway Driving", "Pending"},
                {"BK-1045", "Sanduni Wickramasinghe", "Mahinda Alwis", "Toyota Vitz (CAB-1234)", "Tomorrow, 08:30 AM", "Parallel Parking", "Confirmed"},
                {"BK-1046", "Ruwan Tharaka", "Sunil Perera", "Suzuki Alto (KV-3456)", "Tomorrow, 10:30 AM", "Night Driving", "Pending"},
                {"BK-1047", "Anoma Senanayake", "Mahinda Alwis", "Nissan March (WP-5678)", "Tomorrow, 01:30 PM", "Mock Driving Exam", "Confirmed"}
            },
            new String [] {
                "Booking ID", "Student Name", "Instructor", "Vehicle", "Date & Time", "Lesson Type", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableRecentBookings.setRowHeight(36);
        tableRecentBookings.setShowGrid(false);
        scrollRecentBookings.setViewportView(tableRecentBookings);

        panelRecentBookings.add(scrollRecentBookings, java.awt.BorderLayout.CENTER);

        panelDashboardCenter.add(panelRecentBookings, java.awt.BorderLayout.CENTER);

        Dashbord.add(panelDashboardCenter, java.awt.BorderLayout.CENTER);

        Contructor.add(Dashbord, "cardDashboard");

        Instructors.setBackground(new java.awt.Color(248, 250, 252));
        Instructors.setLayout(new java.awt.BorderLayout(14, 14));

        panelInstHeader.setBackground(new java.awt.Color(255, 255, 255));
        panelInstHeader.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelInstHeader.setLayout(new java.awt.BorderLayout());

        panelInstHeaderLeft.setBackground(new java.awt.Color(255, 255, 255));
        panelInstHeaderLeft.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 14, 10, 10));
        panelInstHeaderLeft.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 4));

        lblInstHeaderIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_personal_trainer_32px.png"))); // NOI18N
        panelInstHeaderLeft.add(lblInstHeaderIcon);

        panelInstHeaderTitles.setBackground(new java.awt.Color(255, 255, 255));
        panelInstHeaderTitles.setLayout(new java.awt.GridLayout(2, 1, 0, 3));

        lblInstHeaderTitle.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblInstHeaderTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblInstHeaderTitle.setText("Instructors Directory & Management");
        panelInstHeaderTitles.add(lblInstHeaderTitle);

        lblInstHeaderSubtitle.setForeground(new java.awt.Color(100, 116, 139));
        lblInstHeaderSubtitle.setText("Manage certified driving instructors, vehicle licenses, and duty status");
        panelInstHeaderTitles.add(lblInstHeaderSubtitle);

        panelInstHeaderLeft.add(panelInstHeaderTitles);

        panelInstHeader.add(panelInstHeaderLeft, java.awt.BorderLayout.LINE_START);

        panelInstHeaderRight.setBackground(new java.awt.Color(255, 255, 255));
        panelInstHeaderRight.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 14));
        panelInstHeaderRight.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 6));

        panelInstBadgeTotal.setBackground(new java.awt.Color(239, 246, 255));
        panelInstBadgeTotal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelInstBadgeTotal.setPreferredSize(new java.awt.Dimension(110, 50));
        panelInstBadgeTotal.setLayout(new java.awt.GridLayout(2, 1));

        lblInstBadgeTotalCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblInstBadgeTotalCount.setForeground(new java.awt.Color(29, 78, 216));
        lblInstBadgeTotalCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInstBadgeTotalCount.setText("8");
        panelInstBadgeTotal.add(lblInstBadgeTotalCount);

        lblInstBadgeTotalLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblInstBadgeTotalLabel.setForeground(new java.awt.Color(30, 64, 175));
        lblInstBadgeTotalLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInstBadgeTotalLabel.setText("Total Instructors");
        panelInstBadgeTotal.add(lblInstBadgeTotalLabel);

        panelInstHeaderRight.add(panelInstBadgeTotal);

        panelInstBadgeActive.setBackground(new java.awt.Color(236, 253, 245));
        panelInstBadgeActive.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelInstBadgeActive.setPreferredSize(new java.awt.Dimension(110, 50));
        panelInstBadgeActive.setLayout(new java.awt.GridLayout(2, 1));

        lblInstBadgeActiveCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblInstBadgeActiveCount.setForeground(new java.awt.Color(4, 120, 87));
        lblInstBadgeActiveCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInstBadgeActiveCount.setText("6");
        panelInstBadgeActive.add(lblInstBadgeActiveCount);

        lblInstBadgeActiveLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblInstBadgeActiveLabel.setForeground(new java.awt.Color(6, 95, 70));
        lblInstBadgeActiveLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInstBadgeActiveLabel.setText("Active On Duty");
        panelInstBadgeActive.add(lblInstBadgeActiveLabel);

        panelInstHeaderRight.add(panelInstBadgeActive);

        panelInstBadgeAvailable.setBackground(new java.awt.Color(254, 243, 199));
        panelInstBadgeAvailable.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelInstBadgeAvailable.setPreferredSize(new java.awt.Dimension(110, 50));
        panelInstBadgeAvailable.setLayout(new java.awt.GridLayout(2, 1));

        lblInstBadgeAvailableCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblInstBadgeAvailableCount.setForeground(new java.awt.Color(180, 83, 9));
        lblInstBadgeAvailableCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInstBadgeAvailableCount.setText("2");
        panelInstBadgeAvailable.add(lblInstBadgeAvailableCount);

        lblInstBadgeAvailableLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblInstBadgeAvailableLabel.setForeground(new java.awt.Color(146, 64, 14));
        lblInstBadgeAvailableLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInstBadgeAvailableLabel.setText("Available Now");
        panelInstBadgeAvailable.add(lblInstBadgeAvailableLabel);

        panelInstHeaderRight.add(panelInstBadgeAvailable);

        panelInstHeader.add(panelInstHeaderRight, java.awt.BorderLayout.LINE_END);

        Instructors.add(panelInstHeader, java.awt.BorderLayout.PAGE_START);

        panelInstMain.setBackground(new java.awt.Color(248, 250, 252));
        panelInstMain.setLayout(new java.awt.BorderLayout(14, 14));

        panelInstFormCard.setBackground(new java.awt.Color(255, 255, 255));
        panelInstFormCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelInstFormCard.setPreferredSize(new java.awt.Dimension(380, 480));
        panelInstFormCard.setLayout(new java.awt.BorderLayout(6, 6));

        panelInstFormHeader.setBackground(new java.awt.Color(255, 255, 255));
        panelInstFormHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 6, 14));
        panelInstFormHeader.setLayout(new java.awt.BorderLayout());

        lblInstFormHeader.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblInstFormHeader.setForeground(new java.awt.Color(30, 41, 59));
        lblInstFormHeader.setText("INSTRUCTOR PROFILE");
        panelInstFormHeader.add(lblInstFormHeader, java.awt.BorderLayout.LINE_START);

        panelInstFormCard.add(panelInstFormHeader, java.awt.BorderLayout.PAGE_START);

        scrollInstForm.setBorder(null);
        scrollInstForm.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panelInstFormFields.setBackground(new java.awt.Color(255, 255, 255));
        panelInstFormFields.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 14, 8, 14));
        panelInstFormFields.setLayout(new java.awt.GridLayout(14, 1, 0, 3));

        lblInstId.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblInstId.setForeground(new java.awt.Color(71, 85, 105));
        lblInstId.setText("Instructor ID");
        panelInstFormFields.add(lblInstId);

        txtInstId.setEditable(false);
        txtInstId.setBackground(new java.awt.Color(241, 245, 249));
        txtInstId.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        txtInstId.setForeground(new java.awt.Color(29, 78, 216));
        txtInstId.setText("INS-101");
        panelInstFormFields.add(txtInstId);

        lblInstFullName.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblInstFullName.setForeground(new java.awt.Color(71, 85, 105));
        lblInstFullName.setText("Full Name *");
        panelInstFormFields.add(lblInstFullName);

        txtInstFullName.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtInstFullName.setText("Robert Silva");
        panelInstFormFields.add(txtInstFullName);

        lblInstPhone.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblInstPhone.setForeground(new java.awt.Color(71, 85, 105));
        lblInstPhone.setText("Phone Number *");
        panelInstFormFields.add(lblInstPhone);

        txtInstPhone.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtInstPhone.setText("077-1234567");
        panelInstFormFields.add(txtInstPhone);

        lblInstNic.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblInstNic.setForeground(new java.awt.Color(71, 85, 105));
        lblInstNic.setText("NIC / National ID");
        panelInstFormFields.add(lblInstNic);

        txtInstNic.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtInstNic.setText("198812345678");
        panelInstFormFields.add(txtInstNic);

        lblInstLicense.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblInstLicense.setForeground(new java.awt.Color(71, 85, 105));
        lblInstLicense.setText("Driving License / Badge No *");
        panelInstFormFields.add(lblInstLicense);

        txtInstLicense.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtInstLicense.setText("DL-992140");
        panelInstFormFields.add(txtInstLicense);

        lblInstCategory.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblInstCategory.setForeground(new java.awt.Color(71, 85, 105));
        lblInstCategory.setText("Vehicle Class / Specialization");
        panelInstFormFields.add(lblInstCategory);

        cmbInstCategory.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelInstFormFields.add(cmbInstCategory);

        lblInstStatus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblInstStatus.setForeground(new java.awt.Color(71, 85, 105));
        lblInstStatus.setText("Duty Status");
        panelInstFormFields.add(lblInstStatus);

        cmbInstStatus.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelInstFormFields.add(cmbInstStatus);

        scrollInstForm.setViewportView(panelInstFormFields);

        panelInstFormCard.add(scrollInstForm, java.awt.BorderLayout.CENTER);

        panelInstFormActions.setBackground(new java.awt.Color(255, 255, 255));
        panelInstFormActions.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 12, 14));
        panelInstFormActions.setLayout(new java.awt.GridLayout(2, 2, 8, 8));

        btnInstAdd.setBackground(new java.awt.Color(37, 99, 235));
        btnInstAdd.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnInstAdd.setForeground(new java.awt.Color(255, 255, 255));
        btnInstAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Add_Male_User_Group_25px.png"))); // NOI18N
        btnInstAdd.setText(" Add");
        btnInstAdd.setFocusPainted(false);
        btnInstAdd.setPreferredSize(new java.awt.Dimension(120, 36));
        btnInstAdd.addActionListener(this::btnInstAddActionPerformed);
        panelInstFormActions.add(btnInstAdd);

        btnInstUpdate.setBackground(new java.awt.Color(5, 150, 105));
        btnInstUpdate.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnInstUpdate.setForeground(new java.awt.Color(255, 255, 255));
        btnInstUpdate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Female_User_Update_25px.png"))); // NOI18N
        btnInstUpdate.setText(" Update");
        btnInstUpdate.setFocusPainted(false);
        btnInstUpdate.setPreferredSize(new java.awt.Dimension(120, 36));
        btnInstUpdate.addActionListener(this::btnInstUpdateActionPerformed);
        panelInstFormActions.add(btnInstUpdate);

        btnInstDelete.setBackground(new java.awt.Color(220, 38, 38));
        btnInstDelete.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnInstDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnInstDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Delete_25px.png"))); // NOI18N
        btnInstDelete.setText(" Delete");
        btnInstDelete.setFocusPainted(false);
        btnInstDelete.setPreferredSize(new java.awt.Dimension(120, 36));
        btnInstDelete.addActionListener(this::btnInstDeleteActionPerformed);
        panelInstFormActions.add(btnInstDelete);

        btnInstClear.setBackground(new java.awt.Color(100, 116, 139));
        btnInstClear.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnInstClear.setForeground(new java.awt.Color(255, 255, 255));
        btnInstClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_broom_25px.png"))); // NOI18N
        btnInstClear.setText(" Clear");
        btnInstClear.setFocusPainted(false);
        btnInstClear.setPreferredSize(new java.awt.Dimension(120, 36));
        btnInstClear.addActionListener(this::btnInstClearActionPerformed);
        panelInstFormActions.add(btnInstClear);

        panelInstFormCard.add(panelInstFormActions, java.awt.BorderLayout.PAGE_END);

        panelInstMain.add(panelInstFormCard, java.awt.BorderLayout.LINE_START);

        panelInstTableCard.setBackground(new java.awt.Color(255, 255, 255));
        panelInstTableCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelInstTableCard.setLayout(new java.awt.BorderLayout());

        panelInstTableTop.setBackground(new java.awt.Color(255, 255, 255));
        panelInstTableTop.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14));
        panelInstTableTop.setLayout(new java.awt.BorderLayout());

        lblInstTableTitle.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblInstTableTitle.setForeground(new java.awt.Color(30, 41, 59));
        lblInstTableTitle.setText("Instructors Directory");
        panelInstTableTop.add(lblInstTableTitle, java.awt.BorderLayout.LINE_START);

        panelInstSearchFilter.setBackground(new java.awt.Color(255, 255, 255));
        panelInstSearchFilter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 2));

        txtInstSearch.setForeground(new java.awt.Color(100, 100, 100));
        txtInstSearch.setText("Search instructors...");
        txtInstSearch.setPreferredSize(new java.awt.Dimension(170, 32));
        panelInstSearchFilter.add(txtInstSearch);

        cmbInstFilterStatus.setPreferredSize(new java.awt.Dimension(120, 32));
        panelInstSearchFilter.add(cmbInstFilterStatus);

        btnInstRefresh.setBackground(new java.awt.Color(248, 245, 241));
        btnInstRefresh.setText("Refresh");
        btnInstRefresh.setFocusPainted(false);
        btnInstRefresh.setPreferredSize(new java.awt.Dimension(80, 32));
        panelInstSearchFilter.add(btnInstRefresh);

        panelInstTableTop.add(panelInstSearchFilter, java.awt.BorderLayout.LINE_END);

        panelInstTableCard.add(panelInstTableTop, java.awt.BorderLayout.PAGE_START);

        scrollInstTable.setBorder(null);

        tableInstructors.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"INS-101", "Robert Silva", "077-1234567", "198812345678", "DL-992140", "Class B (Dual)", "Available"},
                {"INS-102", "Kamal Perera", "071-8899221", "198522334455", "DL-881203", "Class B (Manual)", "On Duty"},
                {"INS-103", "Sunil Fernando", "076-5544332", "198033445566", "DL-773419", "Class C (Heavy)", "Available"},
                {"INS-104", "Nimal Jayasinghe", "075-2233445", "199244556677", "DL-664321", "Class A (Bike)", "On Leave"},
                {"INS-105", "Amila Bandara", "078-9900112", "199055667788", "DL-552190", "Class B (Auto)", "Available"},
                {"INS-106", "Pradeep Kumara", "072-3344556", "198766778899", "DL-441298", "Class B (Dual)", "On Duty"}
            },
            new String [] {
                "Instructor ID", "Full Name", "Phone Number", "NIC", "License / Badge No", "Vehicle Class", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableInstructors.setRowHeight(32);
        tableInstructors.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableInstructors.setShowGrid(false);
        scrollInstTable.setViewportView(tableInstructors);

        panelInstTableCard.add(scrollInstTable, java.awt.BorderLayout.CENTER);

        panelInstTableBottom.setBackground(new java.awt.Color(255, 255, 255));
        panelInstTableBottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14));
        panelInstTableBottom.setLayout(new java.awt.BorderLayout());

        lblInstTableCount.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblInstTableCount.setForeground(new java.awt.Color(100, 116, 139));
        lblInstTableCount.setText("Showing 6 instructors | Click a row to view or edit profile");
        panelInstTableBottom.add(lblInstTableCount, java.awt.BorderLayout.LINE_START);

        panelInstTableCard.add(panelInstTableBottom, java.awt.BorderLayout.PAGE_END);

        panelInstMain.add(panelInstTableCard, java.awt.BorderLayout.CENTER);

        Instructors.add(panelInstMain, java.awt.BorderLayout.CENTER);

        Contructor.add(Instructors, "cardInstructors");

        Vehicles.setBackground(new java.awt.Color(248, 250, 252));
        Vehicles.setLayout(new java.awt.BorderLayout(14, 14));

        panelVehHeader.setBackground(new java.awt.Color(255, 255, 255));
        panelVehHeader.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelVehHeader.setLayout(new java.awt.BorderLayout());

        panelVehHeaderLeft.setBackground(new java.awt.Color(255, 255, 255));
        panelVehHeaderLeft.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 14, 10, 10));
        panelVehHeaderLeft.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 4));

        lblVehHeaderIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_car_32px.png"))); // NOI18N
        panelVehHeaderLeft.add(lblVehHeaderIcon);

        panelVehHeaderTitles.setBackground(new java.awt.Color(255, 255, 255));
        panelVehHeaderTitles.setLayout(new java.awt.GridLayout(2, 1, 0, 3));

        lblVehHeaderTitle.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblVehHeaderTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblVehHeaderTitle.setText("Vehicles Fleet & Maintenance");
        panelVehHeaderTitles.add(lblVehHeaderTitle);

        lblVehHeaderSubtitle.setForeground(new java.awt.Color(100, 116, 139));
        lblVehHeaderSubtitle.setText("Manage training fleet, transmission types, fuel, and service readiness");
        panelVehHeaderTitles.add(lblVehHeaderSubtitle);

        panelVehHeaderLeft.add(panelVehHeaderTitles);

        panelVehHeader.add(panelVehHeaderLeft, java.awt.BorderLayout.LINE_START);

        panelVehHeaderRight.setBackground(new java.awt.Color(255, 255, 255));
        panelVehHeaderRight.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 14));
        panelVehHeaderRight.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 6));

        panelVehBadgeTotal.setBackground(new java.awt.Color(239, 246, 255));
        panelVehBadgeTotal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelVehBadgeTotal.setPreferredSize(new java.awt.Dimension(110, 50));
        panelVehBadgeTotal.setLayout(new java.awt.GridLayout(2, 1));

        lblVehBadgeTotalCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblVehBadgeTotalCount.setForeground(new java.awt.Color(29, 78, 216));
        lblVehBadgeTotalCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblVehBadgeTotalCount.setText("12");
        panelVehBadgeTotal.add(lblVehBadgeTotalCount);

        lblVehBadgeTotalLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblVehBadgeTotalLabel.setForeground(new java.awt.Color(30, 64, 175));
        lblVehBadgeTotalLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblVehBadgeTotalLabel.setText("Total Fleet");
        panelVehBadgeTotal.add(lblVehBadgeTotalLabel);

        panelVehHeaderRight.add(panelVehBadgeTotal);

        panelVehBadgeReady.setBackground(new java.awt.Color(236, 253, 245));
        panelVehBadgeReady.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelVehBadgeReady.setPreferredSize(new java.awt.Dimension(110, 50));
        panelVehBadgeReady.setLayout(new java.awt.GridLayout(2, 1));

        lblVehBadgeReadyCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblVehBadgeReadyCount.setForeground(new java.awt.Color(4, 120, 87));
        lblVehBadgeReadyCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblVehBadgeReadyCount.setText("9");
        panelVehBadgeReady.add(lblVehBadgeReadyCount);

        lblVehBadgeReadyLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblVehBadgeReadyLabel.setForeground(new java.awt.Color(6, 95, 70));
        lblVehBadgeReadyLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblVehBadgeReadyLabel.setText("Ready to Drive");
        panelVehBadgeReady.add(lblVehBadgeReadyLabel);

        panelVehHeaderRight.add(panelVehBadgeReady);

        panelVehBadgeService.setBackground(new java.awt.Color(254, 243, 199));
        panelVehBadgeService.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelVehBadgeService.setPreferredSize(new java.awt.Dimension(110, 50));
        panelVehBadgeService.setLayout(new java.awt.GridLayout(2, 1));

        lblVehBadgeServiceCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblVehBadgeServiceCount.setForeground(new java.awt.Color(180, 83, 9));
        lblVehBadgeServiceCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblVehBadgeServiceCount.setText("3");
        panelVehBadgeService.add(lblVehBadgeServiceCount);

        lblVehBadgeServiceLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblVehBadgeServiceLabel.setForeground(new java.awt.Color(146, 64, 14));
        lblVehBadgeServiceLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblVehBadgeServiceLabel.setText("In Maintenance");
        panelVehBadgeService.add(lblVehBadgeServiceLabel);

        panelVehHeaderRight.add(panelVehBadgeService);

        panelVehHeader.add(panelVehHeaderRight, java.awt.BorderLayout.LINE_END);

        Vehicles.add(panelVehHeader, java.awt.BorderLayout.PAGE_START);

        panelVehMain.setBackground(new java.awt.Color(248, 250, 252));
        panelVehMain.setLayout(new java.awt.BorderLayout(14, 14));

        panelVehFormCard.setBackground(new java.awt.Color(255, 255, 255));
        panelVehFormCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelVehFormCard.setPreferredSize(new java.awt.Dimension(380, 480));
        panelVehFormCard.setLayout(new java.awt.BorderLayout(6, 6));

        panelVehFormHeader.setBackground(new java.awt.Color(255, 255, 255));
        panelVehFormHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 6, 14));
        panelVehFormHeader.setLayout(new java.awt.BorderLayout());

        lblVehFormHeader.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblVehFormHeader.setForeground(new java.awt.Color(30, 41, 59));
        lblVehFormHeader.setText("VEHICLE PROFILE & DETAILS");
        panelVehFormHeader.add(lblVehFormHeader, java.awt.BorderLayout.LINE_START);

        panelVehFormCard.add(panelVehFormHeader, java.awt.BorderLayout.PAGE_START);

        scrollVehForm.setBorder(null);
        scrollVehForm.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panelVehFormFields.setBackground(new java.awt.Color(255, 255, 255));
        panelVehFormFields.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 14, 8, 14));
        panelVehFormFields.setLayout(new java.awt.GridLayout(16, 1, 0, 3));

        lblVehId.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblVehId.setForeground(new java.awt.Color(71, 85, 105));
        lblVehId.setText("Vehicle ID");
        panelVehFormFields.add(lblVehId);

        txtVehId.setEditable(false);
        txtVehId.setBackground(new java.awt.Color(241, 245, 249));
        txtVehId.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        txtVehId.setForeground(new java.awt.Color(29, 78, 216));
        txtVehId.setText("VEH-001");
        panelVehFormFields.add(txtVehId);

        lblVehModel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblVehModel.setForeground(new java.awt.Color(71, 85, 105));
        lblVehModel.setText("Model / Brand *");
        panelVehFormFields.add(lblVehModel);

        txtVehModel.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtVehModel.setText("Toyota Vitz");
        panelVehFormFields.add(txtVehModel);

        lblVehPlate.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblVehPlate.setForeground(new java.awt.Color(71, 85, 105));
        lblVehPlate.setText("Registration / Plate No *");
        panelVehFormFields.add(lblVehPlate);

        txtVehPlate.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtVehPlate.setText("CAB-4512");
        panelVehFormFields.add(txtVehPlate);

        lblVehCategory.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblVehCategory.setForeground(new java.awt.Color(71, 85, 105));
        lblVehCategory.setText("Vehicle Class / Category");
        panelVehFormFields.add(lblVehCategory);

        cmbVehCategory.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelVehFormFields.add(cmbVehCategory);

        lblVehTransmission.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblVehTransmission.setForeground(new java.awt.Color(71, 85, 105));
        lblVehTransmission.setText("Transmission Type");
        panelVehFormFields.add(lblVehTransmission);

        cmbVehTransmission.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelVehFormFields.add(cmbVehTransmission);

        lblVehFuel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblVehFuel.setForeground(new java.awt.Color(71, 85, 105));
        lblVehFuel.setText("Fuel Type");
        panelVehFormFields.add(lblVehFuel);

        cmbVehFuel.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelVehFormFields.add(cmbVehFuel);

        lblVehStatus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblVehStatus.setForeground(new java.awt.Color(71, 85, 105));
        lblVehStatus.setText("Operational Status");
        panelVehFormFields.add(lblVehStatus);

        cmbVehStatus.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelVehFormFields.add(cmbVehStatus);

        lblVehMileage.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblVehMileage.setForeground(new java.awt.Color(71, 85, 105));
        lblVehMileage.setText("Mileage / Last Service");
        panelVehFormFields.add(lblVehMileage);

        txtVehMileage.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtVehMileage.setText("45,200 km (Serviced Aug 2026)");
        panelVehFormFields.add(txtVehMileage);

        scrollVehForm.setViewportView(panelVehFormFields);

        panelVehFormCard.add(scrollVehForm, java.awt.BorderLayout.CENTER);

        panelVehFormActions.setBackground(new java.awt.Color(255, 255, 255));
        panelVehFormActions.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 12, 14));
        panelVehFormActions.setLayout(new java.awt.GridLayout(2, 2, 8, 8));

        btnVehAdd.setBackground(new java.awt.Color(37, 99, 235));
        btnVehAdd.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnVehAdd.setForeground(new java.awt.Color(255, 255, 255));
        btnVehAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Add_Male_User_Group_25px.png"))); // NOI18N
        btnVehAdd.setText(" Add");
        btnVehAdd.setFocusPainted(false);
        btnVehAdd.setPreferredSize(new java.awt.Dimension(120, 36));
        btnVehAdd.addActionListener(this::btnVehAddActionPerformed);
        panelVehFormActions.add(btnVehAdd);

        btnVehUpdate.setBackground(new java.awt.Color(5, 150, 105));
        btnVehUpdate.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnVehUpdate.setForeground(new java.awt.Color(255, 255, 255));
        btnVehUpdate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Female_User_Update_25px.png"))); // NOI18N
        btnVehUpdate.setText(" Update");
        btnVehUpdate.setFocusPainted(false);
        btnVehUpdate.setPreferredSize(new java.awt.Dimension(120, 36));
        btnVehUpdate.addActionListener(this::btnVehUpdateActionPerformed);
        panelVehFormActions.add(btnVehUpdate);

        btnVehDelete.setBackground(new java.awt.Color(220, 38, 38));
        btnVehDelete.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnVehDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnVehDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Delete_25px.png"))); // NOI18N
        btnVehDelete.setText(" Delete");
        btnVehDelete.setFocusPainted(false);
        btnVehDelete.setPreferredSize(new java.awt.Dimension(120, 36));
        btnVehDelete.addActionListener(this::btnVehDeleteActionPerformed);
        panelVehFormActions.add(btnVehDelete);

        btnVehClear.setBackground(new java.awt.Color(100, 116, 139));
        btnVehClear.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnVehClear.setForeground(new java.awt.Color(255, 255, 255));
        btnVehClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_broom_25px.png"))); // NOI18N
        btnVehClear.setText(" Clear");
        btnVehClear.setFocusPainted(false);
        btnVehClear.setPreferredSize(new java.awt.Dimension(120, 36));
        btnVehClear.addActionListener(this::btnVehClearActionPerformed);
        panelVehFormActions.add(btnVehClear);

        panelVehFormCard.add(panelVehFormActions, java.awt.BorderLayout.PAGE_END);

        panelVehMain.add(panelVehFormCard, java.awt.BorderLayout.LINE_START);

        panelVehTableCard.setBackground(new java.awt.Color(255, 255, 255));
        panelVehTableCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelVehTableCard.setLayout(new java.awt.BorderLayout());

        panelVehTableTop.setBackground(new java.awt.Color(255, 255, 255));
        panelVehTableTop.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14));
        panelVehTableTop.setLayout(new java.awt.BorderLayout());

        lblVehTableTitle.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblVehTableTitle.setForeground(new java.awt.Color(30, 41, 59));
        lblVehTableTitle.setText("Vehicles Fleet Directory");
        panelVehTableTop.add(lblVehTableTitle, java.awt.BorderLayout.LINE_START);

        panelVehSearchFilter.setBackground(new java.awt.Color(255, 255, 255));
        panelVehSearchFilter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 2));

        txtVehSearch.setForeground(new java.awt.Color(100, 100, 100));
        txtVehSearch.setText("Search vehicles...");
        txtVehSearch.setPreferredSize(new java.awt.Dimension(170, 32));
        panelVehSearchFilter.add(txtVehSearch);

        cmbVehFilterStatus.setPreferredSize(new java.awt.Dimension(130, 32));
        panelVehSearchFilter.add(cmbVehFilterStatus);

        btnVehRefresh.setBackground(new java.awt.Color(248, 245, 241));
        btnVehRefresh.setText("Refresh");
        btnVehRefresh.setFocusPainted(false);
        btnVehRefresh.setPreferredSize(new java.awt.Dimension(80, 32));
        panelVehSearchFilter.add(btnVehRefresh);

        panelVehTableTop.add(panelVehSearchFilter, java.awt.BorderLayout.LINE_END);

        panelVehTableCard.add(panelVehTableTop, java.awt.BorderLayout.PAGE_START);

        scrollVehTable.setBorder(null);

        tableVehicles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"VEH-001", "Toyota Vitz", "CAB-4512", "Class B (Dual)", "Auto", "Petrol", "Available"},
                {"VEH-002", "Suzuki Alto", "WP-3321", "Class B (Dual)", "Manual", "Petrol", "In Session"},
                {"VEH-003", "Toyota Aqua", "SP-8921", "Class B (Dual)", "Auto", "Hybrid", "Available"},
                {"VEH-004", "Honda Grace", "CAC-1102", "Class B (Dual)", "Auto", "Hybrid", "In Session"},
                {"VEH-005", "Yamaha FZ-S", "BIKE-402", "Class A (Bike)", "Manual", "Petrol", "Available"},
                {"VEH-006", "Suzuki Wagon R", "WP-7714", "Class B (Dual)", "Auto", "Hybrid", "Under Maintenance"},
                {"VEH-007", "Toyota HiAce", "ND-5561", "Class C (Van)", "Manual", "Diesel", "Available"},
                {"VEH-008", "Isuzu Elf", "CP-2290", "Class C (Heavy)", "Manual", "Diesel", "Under Maintenance"}
            },
            new String [] {
                "Vehicle ID", "Model / Name", "Plate No", "Category", "Transmission", "Fuel", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableVehicles.setRowHeight(32);
        tableVehicles.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableVehicles.setShowGrid(false);
        scrollVehTable.setViewportView(tableVehicles);

        panelVehTableCard.add(scrollVehTable, java.awt.BorderLayout.CENTER);

        panelVehTableBottom.setBackground(new java.awt.Color(255, 255, 255));
        panelVehTableBottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14));
        panelVehTableBottom.setLayout(new java.awt.BorderLayout());

        lblVehTableCount.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblVehTableCount.setForeground(new java.awt.Color(100, 116, 139));
        lblVehTableCount.setText("Showing 8 vehicles | Click a row to view or edit details");
        panelVehTableBottom.add(lblVehTableCount, java.awt.BorderLayout.LINE_START);

        panelVehTableCard.add(panelVehTableBottom, java.awt.BorderLayout.PAGE_END);

        panelVehMain.add(panelVehTableCard, java.awt.BorderLayout.CENTER);

        Vehicles.add(panelVehMain, java.awt.BorderLayout.CENTER);

        Contructor.add(Vehicles, "cardVehicles");

        Bookings.setBackground(new java.awt.Color(248, 250, 252));
        Bookings.setLayout(new java.awt.BorderLayout(14, 14));

        panelBkHeader.setBackground(new java.awt.Color(255, 255, 255));
        panelBkHeader.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBkHeader.setLayout(new java.awt.BorderLayout());

        panelBkHeaderLeft.setBackground(new java.awt.Color(255, 255, 255));
        panelBkHeaderLeft.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 14, 10, 10));
        panelBkHeaderLeft.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 4));

        lblBkHeaderIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_new_ticket_32px.png"))); // NOI18N
        panelBkHeaderLeft.add(lblBkHeaderIcon);

        panelBkHeaderTitles.setBackground(new java.awt.Color(255, 255, 255));
        panelBkHeaderTitles.setLayout(new java.awt.GridLayout(2, 1, 0, 3));

        lblBkHeaderTitle.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblBkHeaderTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblBkHeaderTitle.setText("Practical Lesson Bookings & Scheduling");
        panelBkHeaderTitles.add(lblBkHeaderTitle);

        lblBkHeaderSubtitle.setForeground(new java.awt.Color(100, 116, 139));
        lblBkHeaderSubtitle.setText("Reserve student driving slots, assign vehicles & instructors, and track lesson status");
        panelBkHeaderTitles.add(lblBkHeaderSubtitle);

        panelBkHeaderLeft.add(panelBkHeaderTitles);

        panelBkHeader.add(panelBkHeaderLeft, java.awt.BorderLayout.LINE_START);

        panelBkHeaderRight.setBackground(new java.awt.Color(255, 255, 255));
        panelBkHeaderRight.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 14));
        panelBkHeaderRight.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 6));

        panelBkBadgeTotal.setBackground(new java.awt.Color(239, 246, 255));
        panelBkBadgeTotal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBkBadgeTotal.setPreferredSize(new java.awt.Dimension(110, 50));
        panelBkBadgeTotal.setLayout(new java.awt.GridLayout(2, 1));

        lblBkBadgeTotalCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblBkBadgeTotalCount.setForeground(new java.awt.Color(29, 78, 216));
        lblBkBadgeTotalCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBkBadgeTotalCount.setText("28");
        panelBkBadgeTotal.add(lblBkBadgeTotalCount);

        lblBkBadgeTotalLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblBkBadgeTotalLabel.setForeground(new java.awt.Color(30, 64, 175));
        lblBkBadgeTotalLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBkBadgeTotalLabel.setText("Total Sessions");
        panelBkBadgeTotal.add(lblBkBadgeTotalLabel);

        panelBkHeaderRight.add(panelBkBadgeTotal);

        panelBkBadgeConfirmed.setBackground(new java.awt.Color(236, 253, 245));
        panelBkBadgeConfirmed.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBkBadgeConfirmed.setPreferredSize(new java.awt.Dimension(110, 50));
        panelBkBadgeConfirmed.setLayout(new java.awt.GridLayout(2, 1));

        lblBkBadgeConfirmedCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblBkBadgeConfirmedCount.setForeground(new java.awt.Color(4, 120, 87));
        lblBkBadgeConfirmedCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBkBadgeConfirmedCount.setText("18");
        panelBkBadgeConfirmed.add(lblBkBadgeConfirmedCount);

        lblBkBadgeConfirmedLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblBkBadgeConfirmedLabel.setForeground(new java.awt.Color(6, 95, 70));
        lblBkBadgeConfirmedLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBkBadgeConfirmedLabel.setText("Confirmed Slots");
        panelBkBadgeConfirmed.add(lblBkBadgeConfirmedLabel);

        panelBkHeaderRight.add(panelBkBadgeConfirmed);

        panelBkBadgePending.setBackground(new java.awt.Color(254, 243, 199));
        panelBkBadgePending.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBkBadgePending.setPreferredSize(new java.awt.Dimension(110, 50));
        panelBkBadgePending.setLayout(new java.awt.GridLayout(2, 1));

        lblBkBadgePendingCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblBkBadgePendingCount.setForeground(new java.awt.Color(180, 83, 9));
        lblBkBadgePendingCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBkBadgePendingCount.setText("6");
        panelBkBadgePending.add(lblBkBadgePendingCount);

        lblBkBadgePendingLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblBkBadgePendingLabel.setForeground(new java.awt.Color(146, 64, 14));
        lblBkBadgePendingLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBkBadgePendingLabel.setText("Pending Review");
        panelBkBadgePending.add(lblBkBadgePendingLabel);

        panelBkHeaderRight.add(panelBkBadgePending);

        panelBkHeader.add(panelBkHeaderRight, java.awt.BorderLayout.LINE_END);

        Bookings.add(panelBkHeader, java.awt.BorderLayout.PAGE_START);

        panelBkMain.setBackground(new java.awt.Color(248, 250, 252));
        panelBkMain.setLayout(new java.awt.BorderLayout(14, 14));

        panelBkFormCard.setBackground(new java.awt.Color(255, 255, 255));
        panelBkFormCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBkFormCard.setPreferredSize(new java.awt.Dimension(380, 480));
        panelBkFormCard.setLayout(new java.awt.BorderLayout(6, 6));

        panelBkFormHeader.setBackground(new java.awt.Color(255, 255, 255));
        panelBkFormHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 6, 14));
        panelBkFormHeader.setLayout(new java.awt.BorderLayout());

        lblBkFormHeader.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblBkFormHeader.setForeground(new java.awt.Color(30, 41, 59));
        lblBkFormHeader.setText("NEW BOOKING / RESERVATION");
        panelBkFormHeader.add(lblBkFormHeader, java.awt.BorderLayout.LINE_START);

        panelBkFormCard.add(panelBkFormHeader, java.awt.BorderLayout.PAGE_START);

        scrollBkForm.setBorder(null);
        scrollBkForm.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panelBkFormFields.setBackground(new java.awt.Color(255, 255, 255));
        panelBkFormFields.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 14, 8, 14));
        panelBkFormFields.setLayout(new java.awt.GridLayout(16, 1, 0, 3));

        lblBkId.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBkId.setForeground(new java.awt.Color(71, 85, 105));
        lblBkId.setText("Booking ID");
        panelBkFormFields.add(lblBkId);

        txtBkId.setEditable(false);
        txtBkId.setBackground(new java.awt.Color(241, 245, 249));
        txtBkId.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        txtBkId.setForeground(new java.awt.Color(29, 78, 216));
        txtBkId.setText("BK-2042");
        panelBkFormFields.add(txtBkId);

        lblBkStudent.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBkStudent.setForeground(new java.awt.Color(71, 85, 105));
        lblBkStudent.setText("Student Name *");
        panelBkFormFields.add(lblBkStudent);

        txtBkStudent.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtBkStudent.setText("Chamika Silva");
        panelBkFormFields.add(txtBkStudent);

        lblBkInstructor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBkInstructor.setForeground(new java.awt.Color(71, 85, 105));
        lblBkInstructor.setText("Assigned Instructor *");
        panelBkFormFields.add(lblBkInstructor);

        cmbBkInstructor.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelBkFormFields.add(cmbBkInstructor);

        lblBkVehicle.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBkVehicle.setForeground(new java.awt.Color(71, 85, 105));
        lblBkVehicle.setText("Training Vehicle *");
        panelBkFormFields.add(lblBkVehicle);

        cmbBkVehicle.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelBkFormFields.add(cmbBkVehicle);

        lblBkLessonType.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBkLessonType.setForeground(new java.awt.Color(71, 85, 105));
        lblBkLessonType.setText("Lesson / Training Type");
        panelBkFormFields.add(lblBkLessonType);

        cmbBkLessonType.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelBkFormFields.add(cmbBkLessonType);

        lblBkDateTime.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBkDateTime.setForeground(new java.awt.Color(71, 85, 105));
        lblBkDateTime.setText("Date & Time Slot *");
        panelBkFormFields.add(lblBkDateTime);

        txtBkDateTime.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtBkDateTime.setText("Tomorrow, 09:30 AM");
        panelBkFormFields.add(txtBkDateTime);

        lblBkStatus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBkStatus.setForeground(new java.awt.Color(71, 85, 105));
        lblBkStatus.setText("Booking Status");
        panelBkFormFields.add(lblBkStatus);

        cmbBkStatus.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelBkFormFields.add(cmbBkStatus);

        lblBkPayment.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBkPayment.setForeground(new java.awt.Color(71, 85, 105));
        lblBkPayment.setText("Payment Status");
        panelBkFormFields.add(lblBkPayment);

        cmbBkPayment.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelBkFormFields.add(cmbBkPayment);

        scrollBkForm.setViewportView(panelBkFormFields);

        panelBkFormCard.add(scrollBkForm, java.awt.BorderLayout.CENTER);

        panelBkFormActions.setBackground(new java.awt.Color(255, 255, 255));
        panelBkFormActions.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 12, 14));
        panelBkFormActions.setLayout(new java.awt.GridLayout(2, 2, 8, 8));

        btnBkAdd.setBackground(new java.awt.Color(37, 99, 235));
        btnBkAdd.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnBkAdd.setForeground(new java.awt.Color(255, 255, 255));
        btnBkAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Add_Male_User_Group_25px.png"))); // NOI18N
        btnBkAdd.setText(" Book Slot");
        btnBkAdd.setFocusPainted(false);
        btnBkAdd.setPreferredSize(new java.awt.Dimension(120, 36));
        btnBkAdd.addActionListener(this::btnBkAddActionPerformed);
        panelBkFormActions.add(btnBkAdd);

        btnBkUpdate.setBackground(new java.awt.Color(5, 150, 105));
        btnBkUpdate.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnBkUpdate.setForeground(new java.awt.Color(255, 255, 255));
        btnBkUpdate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Female_User_Update_25px.png"))); // NOI18N
        btnBkUpdate.setText(" Update");
        btnBkUpdate.setFocusPainted(false);
        btnBkUpdate.setPreferredSize(new java.awt.Dimension(120, 36));
        panelBkFormActions.add(btnBkUpdate);

        btnBkDelete.setBackground(new java.awt.Color(220, 38, 38));
        btnBkDelete.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnBkDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnBkDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Delete_25px.png"))); // NOI18N
        btnBkDelete.setText(" Cancel");
        btnBkDelete.setFocusPainted(false);
        btnBkDelete.setPreferredSize(new java.awt.Dimension(120, 36));
        panelBkFormActions.add(btnBkDelete);

        btnBkClear.setBackground(new java.awt.Color(100, 116, 139));
        btnBkClear.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnBkClear.setForeground(new java.awt.Color(255, 255, 255));
        btnBkClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_broom_25px.png"))); // NOI18N
        btnBkClear.setText(" Clear");
        btnBkClear.setFocusPainted(false);
        btnBkClear.setPreferredSize(new java.awt.Dimension(120, 36));
        panelBkFormActions.add(btnBkClear);

        panelBkFormCard.add(panelBkFormActions, java.awt.BorderLayout.PAGE_END);

        panelBkMain.add(panelBkFormCard, java.awt.BorderLayout.LINE_START);

        panelBkTableCard.setBackground(new java.awt.Color(255, 255, 255));
        panelBkTableCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBkTableCard.setLayout(new java.awt.BorderLayout());

        panelBkTableTop.setBackground(new java.awt.Color(255, 255, 255));
        panelBkTableTop.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14));
        panelBkTableTop.setLayout(new java.awt.BorderLayout());

        lblBkTableTitle.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblBkTableTitle.setForeground(new java.awt.Color(30, 41, 59));
        lblBkTableTitle.setText("Lesson Bookings Directory");
        panelBkTableTop.add(lblBkTableTitle, java.awt.BorderLayout.LINE_START);

        panelBkSearchFilter.setBackground(new java.awt.Color(255, 255, 255));
        panelBkSearchFilter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 2));

        txtBkSearch.setForeground(new java.awt.Color(100, 100, 100));
        txtBkSearch.setText("Search bookings...");
        txtBkSearch.setPreferredSize(new java.awt.Dimension(170, 32));
        panelBkSearchFilter.add(txtBkSearch);

        cmbBkFilterStatus.setPreferredSize(new java.awt.Dimension(130, 32));
        panelBkSearchFilter.add(cmbBkFilterStatus);

        btnBkRefresh.setBackground(new java.awt.Color(248, 245, 241));
        btnBkRefresh.setText("Refresh");
        btnBkRefresh.setFocusPainted(false);
        btnBkRefresh.setPreferredSize(new java.awt.Dimension(80, 32));
        panelBkSearchFilter.add(btnBkRefresh);

        panelBkTableTop.add(panelBkSearchFilter, java.awt.BorderLayout.LINE_END);

        panelBkTableCard.add(panelBkTableTop, java.awt.BorderLayout.PAGE_START);

        scrollBkTable.setBorder(null);

        tableBookings.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"#BK-2041", "Chamika Silva", "Kamal Perera", "Toyota Vitz (Auto)", "Highway & Parking", "Today, 09:30 AM", "In Progress"},
                {"#BK-2040", "Nimasha Fernando", "Sunil Shantha", "Suzuki Alto (Manual)", "Basic Driving Skills", "Today, 11:00 AM", "Confirmed"},
                {"#BK-2039", "Kasun Bandara", "Kamal Perera", "Toyota Aqua (Hybrid)", "Reverse & Hill Start", "Today, 01:30 PM", "Confirmed"},
                {"#BK-2038", "Dilshan Perera", "Nimal Jayasinghe", "Honda Grace (Auto)", "Traffic Road Test", "Yesterday, 03:00 PM", "Completed"},
                {"#BK-2037", "Anuki Wickrama", "Sunil Shantha", "Suzuki Alto (Manual)", "Night Driving Practice", "Tomorrow, 04:00 PM", "Pending"},
                {"#BK-2036", "Sandun Rodrigo", "Kamal Perera", "Toyota Vitz (Auto)", "City Traffic Maneuvering", "Tomorrow, 10:00 AM", "Confirmed"},
                {"#BK-2035", "Ruvini Gunasekara", "Robert Silva", "Toyota Vitz (Auto)", "Basic Driving Skills", "24 Sep, 02:00 PM", "Confirmed"},
                {"#BK-2034", "Thilina Madushan", "Sunil Fernando", "Toyota HiAce (Van)", "Commercial Van License", "25 Sep, 08:30 AM", "Pending"}
            },
            new String [] {
                "Booking ID", "Student Name", "Instructor", "Vehicle", "Lesson Type", "Date & Time", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableBookings.setRowHeight(32);
        tableBookings.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableBookings.setShowGrid(false);
        scrollBkTable.setViewportView(tableBookings);

        panelBkTableCard.add(scrollBkTable, java.awt.BorderLayout.CENTER);

        panelBkTableBottom.setBackground(new java.awt.Color(255, 255, 255));
        panelBkTableBottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14));
        panelBkTableBottom.setLayout(new java.awt.BorderLayout());

        lblBkTableCount.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblBkTableCount.setForeground(new java.awt.Color(100, 116, 139));
        lblBkTableCount.setText("Showing 8 scheduled bookings | Click a row to view or edit reservation");
        panelBkTableBottom.add(lblBkTableCount, java.awt.BorderLayout.LINE_START);

        panelBkTableCard.add(panelBkTableBottom, java.awt.BorderLayout.PAGE_END);

        panelBkMain.add(panelBkTableCard, java.awt.BorderLayout.CENTER);

        Bookings.add(panelBkMain, java.awt.BorderLayout.CENTER);

        Contructor.add(Bookings, "cardBookings");

        Bookings_Management.setBackground(new java.awt.Color(248, 250, 252));
        Bookings_Management.setLayout(new java.awt.BorderLayout(14, 14));

        panelBmHeader.setBackground(new java.awt.Color(255, 255, 255));
        panelBmHeader.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBmHeader.setLayout(new java.awt.BorderLayout());

        panelBmHeaderLeft.setBackground(new java.awt.Color(255, 255, 255));
        panelBmHeaderLeft.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 14, 10, 10));
        panelBmHeaderLeft.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 4));

        lblBmHeaderIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_event_management_32px.png"))); // NOI18N
        panelBmHeaderLeft.add(lblBmHeaderIcon);

        panelBmHeaderTitles.setBackground(new java.awt.Color(255, 255, 255));
        panelBmHeaderTitles.setLayout(new java.awt.GridLayout(2, 1, 0, 3));

        lblBmHeaderTitle.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblBmHeaderTitle.setForeground(new java.awt.Color(15, 23, 42));
        lblBmHeaderTitle.setText("Booking Operations & Slot Management");
        panelBmHeaderTitles.add(lblBmHeaderTitle);

        lblBmHeaderSubtitle.setForeground(new java.awt.Color(100, 116, 139));
        lblBmHeaderSubtitle.setText("Inspect booking calendar, reassign instructors & vehicles, confirm or reschedule sessions");
        panelBmHeaderTitles.add(lblBmHeaderSubtitle);

        panelBmHeaderLeft.add(panelBmHeaderTitles);

        panelBmHeader.add(panelBmHeaderLeft, java.awt.BorderLayout.LINE_START);

        panelBmHeaderRight.setBackground(new java.awt.Color(255, 255, 255));
        panelBmHeaderRight.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 14));
        panelBmHeaderRight.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 6));

        panelBmBadgeTotal.setBackground(new java.awt.Color(239, 246, 255));
        panelBmBadgeTotal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBmBadgeTotal.setPreferredSize(new java.awt.Dimension(110, 50));
        panelBmBadgeTotal.setLayout(new java.awt.GridLayout(2, 1));

        lblBmBadgeTotalCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblBmBadgeTotalCount.setForeground(new java.awt.Color(29, 78, 216));
        lblBmBadgeTotalCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBmBadgeTotalCount.setText("45");
        panelBmBadgeTotal.add(lblBmBadgeTotalCount);

        lblBmBadgeTotalLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblBmBadgeTotalLabel.setForeground(new java.awt.Color(30, 64, 175));
        lblBmBadgeTotalLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBmBadgeTotalLabel.setText("Total Active");
        panelBmBadgeTotal.add(lblBmBadgeTotalLabel);

        panelBmHeaderRight.add(panelBmBadgeTotal);

        panelBmBadgeToday.setBackground(new java.awt.Color(236, 253, 245));
        panelBmBadgeToday.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBmBadgeToday.setPreferredSize(new java.awt.Dimension(110, 50));
        panelBmBadgeToday.setLayout(new java.awt.GridLayout(2, 1));

        lblBmBadgeTodayCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblBmBadgeTodayCount.setForeground(new java.awt.Color(4, 120, 87));
        lblBmBadgeTodayCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBmBadgeTodayCount.setText("14");
        panelBmBadgeToday.add(lblBmBadgeTodayCount);

        lblBmBadgeTodayLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblBmBadgeTodayLabel.setForeground(new java.awt.Color(6, 95, 70));
        lblBmBadgeTodayLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBmBadgeTodayLabel.setText("Today's Slots");
        panelBmBadgeToday.add(lblBmBadgeTodayLabel);

        panelBmHeaderRight.add(panelBmBadgeToday);

        panelBmBadgePending.setBackground(new java.awt.Color(254, 243, 199));
        panelBmBadgePending.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBmBadgePending.setPreferredSize(new java.awt.Dimension(110, 50));
        panelBmBadgePending.setLayout(new java.awt.GridLayout(2, 1));

        lblBmBadgePendingCount.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblBmBadgePendingCount.setForeground(new java.awt.Color(180, 83, 9));
        lblBmBadgePendingCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBmBadgePendingCount.setText("7");
        panelBmBadgePending.add(lblBmBadgePendingCount);

        lblBmBadgePendingLabel.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblBmBadgePendingLabel.setForeground(new java.awt.Color(146, 64, 14));
        lblBmBadgePendingLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBmBadgePendingLabel.setText("Pending / Alert");
        panelBmBadgePending.add(lblBmBadgePendingLabel);

        panelBmHeaderRight.add(panelBmBadgePending);

        panelBmHeader.add(panelBmHeaderRight, java.awt.BorderLayout.LINE_END);

        Bookings_Management.add(panelBmHeader, java.awt.BorderLayout.PAGE_START);

        panelBmMain.setBackground(new java.awt.Color(248, 250, 252));
        panelBmMain.setLayout(new java.awt.BorderLayout(14, 14));

        panelBmFormCard.setBackground(new java.awt.Color(255, 255, 255));
        panelBmFormCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBmFormCard.setPreferredSize(new java.awt.Dimension(380, 480));
        panelBmFormCard.setLayout(new java.awt.BorderLayout(6, 6));

        panelBmFormHeader.setBackground(new java.awt.Color(255, 255, 255));
        panelBmFormHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 6, 14));
        panelBmFormHeader.setLayout(new java.awt.BorderLayout());

        lblBmFormHeader.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblBmFormHeader.setForeground(new java.awt.Color(30, 41, 59));
        lblBmFormHeader.setText("SLOT CONTROL & REASSIGNMENT");
        panelBmFormHeader.add(lblBmFormHeader, java.awt.BorderLayout.LINE_START);

        panelBmFormCard.add(panelBmFormHeader, java.awt.BorderLayout.PAGE_START);

        scrollBmForm.setBorder(null);
        scrollBmForm.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panelBmFormFields.setBackground(new java.awt.Color(255, 255, 255));
        panelBmFormFields.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 14, 8, 14));
        panelBmFormFields.setLayout(new java.awt.GridLayout(16, 1, 0, 3));

        lblBmId.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBmId.setForeground(new java.awt.Color(71, 85, 105));
        lblBmId.setText("Booking Ref / Slot ID");
        panelBmFormFields.add(lblBmId);

        txtBmId.setEditable(false);
        txtBmId.setBackground(new java.awt.Color(241, 245, 249));
        txtBmId.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        txtBmId.setForeground(new java.awt.Color(29, 78, 216));
        txtBmId.setText("BK-2041");
        panelBmFormFields.add(txtBmId);

        lblBmStudent.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBmStudent.setForeground(new java.awt.Color(71, 85, 105));
        lblBmStudent.setText("Student Name");
        panelBmFormFields.add(lblBmStudent);

        txtBmStudent.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtBmStudent.setText("Chamika Silva");
        panelBmFormFields.add(txtBmStudent);

        lblBmInstructor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBmInstructor.setForeground(new java.awt.Color(71, 85, 105));
        lblBmInstructor.setText("Assigned Instructor");
        panelBmFormFields.add(lblBmInstructor);

        cmbBmInstructor.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelBmFormFields.add(cmbBmInstructor);

        lblBmVehicle.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBmVehicle.setForeground(new java.awt.Color(71, 85, 105));
        lblBmVehicle.setText("Assigned Vehicle");
        panelBmFormFields.add(lblBmVehicle);

        cmbBmVehicle.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelBmFormFields.add(cmbBmVehicle);

        lblBmDate.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBmDate.setForeground(new java.awt.Color(71, 85, 105));
        lblBmDate.setText("Session Date");
        panelBmFormFields.add(lblBmDate);

        txtBmDate.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtBmDate.setText("2026-09-23");
        panelBmFormFields.add(txtBmDate);

        lblBmTimeSlot.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBmTimeSlot.setForeground(new java.awt.Color(71, 85, 105));
        lblBmTimeSlot.setText("Time Slot");
        panelBmFormFields.add(lblBmTimeSlot);

        cmbBmTimeSlot.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelBmFormFields.add(cmbBmTimeSlot);

        lblBmStatus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBmStatus.setForeground(new java.awt.Color(71, 85, 105));
        lblBmStatus.setText("Session Status");
        panelBmFormFields.add(lblBmStatus);

        cmbBmStatus.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        panelBmFormFields.add(cmbBmStatus);

        lblBmRemarks.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBmRemarks.setForeground(new java.awt.Color(71, 85, 105));
        lblBmRemarks.setText("Remarks / Special Requirements");
        panelBmFormFields.add(lblBmRemarks);

        txtBmRemarks.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtBmRemarks.setText("Highway & reverse maneuvers; dual controls");
        panelBmFormFields.add(txtBmRemarks);

        scrollBmForm.setViewportView(panelBmFormFields);

        panelBmFormCard.add(scrollBmForm, java.awt.BorderLayout.CENTER);

        panelBmFormActions.setBackground(new java.awt.Color(255, 255, 255));
        panelBmFormActions.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 12, 14));
        panelBmFormActions.setLayout(new java.awt.GridLayout(2, 2, 8, 8));

        btnBmConfirm.setBackground(new java.awt.Color(37, 99, 235));
        btnBmConfirm.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnBmConfirm.setForeground(new java.awt.Color(255, 255, 255));
        btnBmConfirm.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Add_Male_User_Group_25px.png"))); // NOI18N
        btnBmConfirm.setText(" Confirm Slot");
        btnBmConfirm.setFocusPainted(false);
        btnBmConfirm.setPreferredSize(new java.awt.Dimension(120, 36));
        panelBmFormActions.add(btnBmConfirm);

        btnBmReschedule.setBackground(new java.awt.Color(5, 150, 105));
        btnBmReschedule.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnBmReschedule.setForeground(new java.awt.Color(255, 255, 255));
        btnBmReschedule.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Female_User_Update_25px.png"))); // NOI18N
        btnBmReschedule.setText(" Reassign");
        btnBmReschedule.setFocusPainted(false);
        btnBmReschedule.setPreferredSize(new java.awt.Dimension(120, 36));
        panelBmFormActions.add(btnBmReschedule);

        btnBmCancel.setBackground(new java.awt.Color(220, 38, 38));
        btnBmCancel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnBmCancel.setForeground(new java.awt.Color(255, 255, 255));
        btnBmCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Delete_25px.png"))); // NOI18N
        btnBmCancel.setText(" Cancel Slot");
        btnBmCancel.setFocusPainted(false);
        btnBmCancel.setPreferredSize(new java.awt.Dimension(120, 36));
        panelBmFormActions.add(btnBmCancel);

        btnBmClear.setBackground(new java.awt.Color(100, 116, 139));
        btnBmClear.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnBmClear.setForeground(new java.awt.Color(255, 255, 255));
        btnBmClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_broom_25px.png"))); // NOI18N
        btnBmClear.setText(" Clear Form");
        btnBmClear.setFocusPainted(false);
        btnBmClear.setPreferredSize(new java.awt.Dimension(120, 36));
        panelBmFormActions.add(btnBmClear);

        panelBmFormCard.add(panelBmFormActions, java.awt.BorderLayout.PAGE_END);

        panelBmMain.add(panelBmFormCard, java.awt.BorderLayout.LINE_START);

        panelBmTableCard.setBackground(new java.awt.Color(255, 255, 255));
        panelBmTableCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelBmTableCard.setLayout(new java.awt.BorderLayout());

        panelBmTableTop.setBackground(new java.awt.Color(255, 255, 255));
        panelBmTableTop.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14));
        panelBmTableTop.setLayout(new java.awt.BorderLayout());

        lblBmTableTitle.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblBmTableTitle.setForeground(new java.awt.Color(30, 41, 59));
        lblBmTableTitle.setText("Booking Master Schedule");
        panelBmTableTop.add(lblBmTableTitle, java.awt.BorderLayout.LINE_START);

        panelBmSearchFilter.setBackground(new java.awt.Color(255, 255, 255));
        panelBmSearchFilter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 2));

        txtBmSearch.setForeground(new java.awt.Color(100, 100, 100));
        txtBmSearch.setText("Search bookings...");
        txtBmSearch.setPreferredSize(new java.awt.Dimension(170, 32));
        panelBmSearchFilter.add(txtBmSearch);

        cmbBmFilterStatus.setPreferredSize(new java.awt.Dimension(140, 32));
        panelBmSearchFilter.add(cmbBmFilterStatus);

        btnBmRefresh.setBackground(new java.awt.Color(248, 245, 241));
        btnBmRefresh.setText("Refresh");
        btnBmRefresh.setFocusPainted(false);
        btnBmRefresh.setPreferredSize(new java.awt.Dimension(80, 32));
        panelBmSearchFilter.add(btnBmRefresh);

        panelBmTableTop.add(panelBmSearchFilter, java.awt.BorderLayout.LINE_END);

        panelBmTableCard.add(panelBmTableTop, java.awt.BorderLayout.PAGE_START);

        scrollBmTable.setBorder(null);

        tableBookingManagement.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"BK-2041", "Chamika Silva", "Kamal Perera", "Toyota Vitz (CAB-4512)", "2026-09-23", "08:30 AM - 10:00 AM", "Confirmed"},
                {"BK-2042", "Nadeesha Fernando", "Robert Silva", "Suzuki Alto (WP-3321)", "2026-09-23", "10:30 AM - 12:00 PM", "In Progress"},
                {"BK-2043", "Dinesh Jayawardena", "Sunil Fernando", "Toyota HiAce (ND-5561)", "2026-09-23", "01:00 PM - 02:30 PM", "Pending Approval"},
                {"BK-2044", "Anuki Senaratne", "Amila Bandara", "Toyota Aqua (SP-8921)", "2026-09-23", "03:00 PM - 04:30 PM", "Confirmed"},
                {"BK-2045", "Kasun Wickramasinghe", "Nimal Jayasinghe", "Yamaha FZ (BIKE-402)", "2026-09-24", "08:30 AM - 10:00 AM", "Rescheduled"},
                {"BK-2046", "Sachini Jayasuriya", "Kamal Perera", "Toyota Vitz (CAB-4512)", "2026-09-24", "10:30 AM - 12:00 PM", "Confirmed"},
                {"BK-2047", "Ruwan Gamage", "Robert Silva", "Suzuki Alto (WP-3321)", "2026-09-24", "01:00 PM - 02:30 PM", "Completed"},
                {"BK-2048", "Maleesha De Silva", "Sunil Fernando", "Toyota HiAce (ND-5561)", "2026-09-25", "03:00 PM - 04:30 PM", "Cancelled"}
            },
            new String [] {
                "Booking Ref", "Student Name", "Instructor", "Assigned Vehicle", "Date", "Time Slot", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableBookingManagement.setRowHeight(32);
        tableBookingManagement.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableBookingManagement.setShowGrid(false);
        scrollBmTable.setViewportView(tableBookingManagement);

        panelBmTableCard.add(scrollBmTable, java.awt.BorderLayout.CENTER);

        panelBmTableBottom.setBackground(new java.awt.Color(255, 255, 255));
        panelBmTableBottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14));
        panelBmTableBottom.setLayout(new java.awt.BorderLayout());

        lblBmTableCount.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblBmTableCount.setForeground(new java.awt.Color(100, 116, 139));
        lblBmTableCount.setText("Showing 8 scheduled operations | Select a booking to modify assignment or slot");
        panelBmTableBottom.add(lblBmTableCount, java.awt.BorderLayout.LINE_START);

        panelBmTableCard.add(panelBmTableBottom, java.awt.BorderLayout.PAGE_END);

        panelBmMain.add(panelBmTableCard, java.awt.BorderLayout.CENTER);

        Bookings_Management.add(panelBmMain, java.awt.BorderLayout.CENTER);

        Contructor.add(Bookings_Management, "cardBookingManagement");

        jPanel1.add(Contructor, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDashboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDashboardActionPerformed
        switchCard("cardDashboard");
    }//GEN-LAST:event_btnDashboardActionPerformed

    private void btnStudentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStudentActionPerformed
        switchCard("cardStudents");
        loadStudents();
    }//GEN-LAST:event_btnStudentActionPerformed

    private void btnClearStudentActionPerformed(java.awt.event.ActionEvent evt) {
        btnAddStudent.setVisible(true);
        btnUpdateStudent.setVisible(false);
        btnDeleteStudent.setVisible(false);
        btnClearStudent.setVisible(false);
        clearStudentForm();
    }

    private void btnResetStudentActionPerformed(java.awt.event.ActionEvent evt) {
        txtSearchStudent.setText("");
        loadStudents();
        clearStudentForm();
    }

    private void tableStudentsMouseClicked(java.awt.event.MouseEvent evt) {
        int row = tableStudents.getSelectedRow();
        if (row >= 0) {
            lblStudentIDVal.setText(String.valueOf(tableStudents.getValueAt(row, 0)));
            txtStudentName.setText(String.valueOf(tableStudents.getValueAt(row, 1)));
            txtStudentNIC.setText(String.valueOf(tableStudents.getValueAt(row, 2)));
            txtStudentPhone.setText(String.valueOf(tableStudents.getValueAt(row, 3)));
            txtStudentAddress.setText(String.valueOf(tableStudents.getValueAt(row, 4)));
            String vClass = String.valueOf(tableStudents.getValueAt(row, 5));
            for (int i = 0; i < cmbStudentClass.getItemCount(); i++) {
                if (cmbStudentClass.getItemAt(i).equalsIgnoreCase(vClass) || cmbStudentClass.getItemAt(i).contains(vClass)) {
                    cmbStudentClass.setSelectedIndex(i);
                    break;
                }
            }
            String status = String.valueOf(tableStudents.getValueAt(row, 6));
            for (int i = 0; i < cmbStudentStatus.getItemCount(); i++) {
                if (cmbStudentStatus.getItemAt(i).equalsIgnoreCase(status)) {
                    cmbStudentStatus.setSelectedIndex(i);
                    break;
                }
            }

            btnAddStudent.setVisible(false);
            btnUpdateStudent.setVisible(true);
            btnDeleteStudent.setVisible(true);
            btnClearStudent.setVisible(true);
        }
    }

    private void btnInstructorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInstructorsActionPerformed
        if (!currentRole.equalsIgnoreCase("Admin")) {
            JOptionPane.showMessageDialog(this, "Access Denied! Only Administrators can access User Management.", "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        switchCard("cardInstructors");
        loadInstructors();
    }//GEN-LAST:event_btnInstructorsActionPerformed

    private void btnVehicleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVehicleActionPerformed
        if (!currentRole.equalsIgnoreCase("Admin")) {
            JOptionPane.showMessageDialog(this, "Access Denied! Only Administrators can access Vehicle Management.", "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        switchCard("cardVehicles");
        loadVehicles();
    }//GEN-LAST:event_btnVehicleActionPerformed

    private void btnBookingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBookingActionPerformed
        switchCard("cardBookings");
    }//GEN-LAST:event_btnBookingActionPerformed

    private void btnBookingManageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBookingManageActionPerformed
        switchCard("cardBookingManagement");
    }//GEN-LAST:event_btnBookingManageActionPerformed

    private void btnUserManagementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUserManagementActionPerformed
        if (!currentRole.equalsIgnoreCase("Admin")) {
            JOptionPane.showMessageDialog(this, "Access Denied! Only Administrators can access User Management.", "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        switchCard("cardUserManagement");
        loadUsers();
    }//GEN-LAST:event_btnUserManagementActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose();
            new loging().setVisible(true);
        }
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow != -1) {
            Object idObj = jTable1.getValueAt(selectedRow, 0);
            Object userObj = jTable1.getValueAt(selectedRow, 1);
            Object nicObj = jTable1.getValueAt(selectedRow, 2);
            Object roleObj = jTable1.getValueAt(selectedRow, 3);

            String idStr = idObj != null ? idObj.toString().trim() : "";
            String userStr = userObj != null ? userObj.toString().trim() : "";
            String nicStr = nicObj != null ? nicObj.toString().trim() : "";
            String roleStr = roleObj != null ? roleObj.toString().trim() : "";

            lblUserID.setText(idStr);
            jTextField2.setText(userStr);
            jTextField4.setText(nicStr);

            if (idStr.isEmpty() || userStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please check again your selection!", "Warning", JOptionPane.WARNING_MESSAGE);
            } else {
                btnAddUser.setVisible(false);
                btnUpdateUser.setVisible(true);
                btnDeleteUser.setVisible(true);
                btnClearUser.setVisible(true);
            }

            if (!roleStr.isEmpty()) {
                for (int i = 0; i < jComboBox1.getItemCount(); i++) {
                    if (jComboBox1.getItemAt(i).equalsIgnoreCase(roleStr)) {
                        jComboBox1.setSelectedIndex(i);
                        break;
                    }
                }
            }
            jPasswordField1.setText("");
            jPasswordField2.setText("");

            // Load first_time status from DB for selected user
            try {
                Connection conn = getConnection();
                if (conn != null && !idStr.isEmpty()) {
                    String sql = "SELECT first_time FROM users WHERE user_id = ?";
                    try (PreparedStatement pst = conn.prepareStatement(sql)) {
                        pst.setInt(1, Integer.parseInt(idStr));
                        try (ResultSet rs = pst.executeQuery()) {
                            if (rs.next()) {
                                int ft = rs.getInt("first_time");
                                CheckFirstTimeLog.setSelected(ft == 0 && !rs.wasNull());
                            } else {
                                CheckFirstTimeLog.setSelected(false);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed to load user first_time setting", ex);
                CheckFirstTimeLog.setSelected(false);
            }
        }
    }//GEN-LAST:event_jTable1MouseClicked

    private void btnAddUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddUserActionPerformed
        String username = jTextField2.getText().trim();
        String nic = jTextField4.getText().trim();
        String role = (String) jComboBox1.getSelectedItem();
        String password = new String(jPasswordField1.getPassword()).trim();
        String rePassword = new String(jPasswordField2.getPassword()).trim();

        // 1. Username validation
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Username!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jTextField2.requestFocus();
            return;
        }
        if (username.length() < 3 || username.length() > 50) {
            JOptionPane.showMessageDialog(this, "Username must be between 3 and 50 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jTextField2.requestFocus();
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            JOptionPane.showMessageDialog(this, "Username can only contain letters, numbers, and underscores (no spaces)!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jTextField2.requestFocus();
            return;
        }

        // 2. NIC validation
        if (nic.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter NIC!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jTextField4.requestFocus();
            return;
        }
        if (!nic.matches("^([0-9]{9}[vVxX]|[0-9]{12})$")) {
            JOptionPane.showMessageDialog(this, "Invalid NIC format!\nNIC must be either:\n- 9 digits followed by V or X (e.g., 123456789V)\n- 12 digits (e.g., 200012345678)", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jTextField4.requestFocus();
            return;
        }

        // 3. Role validation
        if (role == null || role.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a Role!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jComboBox1.requestFocus();
            return;
        }

        // 4. Password validation
        String finalPassword;
        String currentDefault = getDefaultPassword();
        if (password.isEmpty()) {
            if (!rePassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter Password to match confirmation password!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                jPasswordField1.requestFocus();
                return;
            }
            finalPassword = currentDefault;
        } else {
            if (password.length() < 4) {
                JOptionPane.showMessageDialog(this, "Password must be at least 4 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                jPasswordField1.requestFocus();
                return;
            }
            if (password.length() > 50) {
                JOptionPane.showMessageDialog(this, "Password cannot exceed 50 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                jPasswordField1.requestFocus();
                return;
            }
            if (!password.equals(rePassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                jPasswordField2.requestFocus();
                return;
            }
            finalPassword = password;
        }

        Connection conn = getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Check if username already exists
        String checkSql = "SELECT user_id FROM users WHERE username = ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
            checkPst.setString(1, username);
            try (ResultSet rs = checkPst.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Username '" + username + "' is already taken! Please choose another.", "Duplicate Username", JOptionPane.WARNING_MESSAGE);
                    jTextField2.requestFocus();
                    return;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 6. Check if NIC already exists
        String checkNicSql = "SELECT user_id FROM users WHERE nic = ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkNicSql)) {
            checkPst.setString(1, nic);
            try (ResultSet rs = checkPst.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "NIC '" + nic + "' is already taken! Please choose another.", "Duplicate NIC", JOptionPane.WARNING_MESSAGE);
                    jTextField4.requestFocus();
                    return;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 7. Insert user
        String firstTimeLog = CheckFirstTimeLog.isSelected() ? "0" : "1";
        String hashedPassword = hashPassword(finalPassword);
        String insertSql = "INSERT INTO users (username, nic, password, role, first_time) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(insertSql)) {
            pst.setString(1, username);
            pst.setString(2, nic);
            pst.setString(3, hashedPassword);
            pst.setString(4, role != null ? role : "Staff");
            pst.setString(5, firstTimeLog);

            int affected = pst.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "User added successfully!" + (password.isEmpty() ? "\nDefault password set to: " + currentDefault : ""));
                loadUsers();
                clearForm();
                btnAddUser.setVisible(true);
                btnhide();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to add user: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnAddUserActionPerformed

    private void btnUpdateUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateUserActionPerformed
        String userIdStr = lblUserID.getText().trim();
        if (userIdStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a user from the table to update!", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid User ID!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String username = jTextField2.getText().trim();
        String nic = jTextField4.getText().trim();
        String role = (String) jComboBox1.getSelectedItem();
        String password = new String(jPasswordField1.getPassword()).trim();
        String rePassword = new String(jPasswordField2.getPassword()).trim();

        // 1. Username validation
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jTextField2.requestFocus();
            return;
        }
        if (username.length() < 3 || username.length() > 50) {
            JOptionPane.showMessageDialog(this, "Username must be between 3 and 50 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jTextField2.requestFocus();
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            JOptionPane.showMessageDialog(this, "Username can only contain letters, numbers, and underscores (no spaces)!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jTextField2.requestFocus();
            return;
        }

        // 2. NIC validation
        if (nic.isEmpty()) {
            JOptionPane.showMessageDialog(this, "NIC cannot be empty!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jTextField4.requestFocus();
            return;
        }
        if (!nic.matches("^([0-9]{9}[vVxX]|[0-9]{12})$")) {
            JOptionPane.showMessageDialog(this, "Invalid NIC format!\nNIC must be either:\n- 9 digits followed by V or X (e.g., 123456789V)\n- 12 digits (e.g., 200012345678)", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jTextField4.requestFocus();
            return;
        }

        // 3. Role validation
        if (role == null || role.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a Role!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jComboBox1.requestFocus();
            return;
        }

        // 4. Password validation (only if password or rePassword entered)
        boolean updatePassword = !password.isEmpty() || !rePassword.isEmpty();
        if (updatePassword) {
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter the new Password!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                jPasswordField1.requestFocus();
                return;
            }
            if (password.length() < 4) {
                JOptionPane.showMessageDialog(this, "Password must be at least 4 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                jPasswordField1.requestFocus();
                return;
            }
            if (password.length() > 50) {
                JOptionPane.showMessageDialog(this, "Password cannot exceed 50 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                jPasswordField1.requestFocus();
                return;
            }
            if (!password.equals(rePassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                jPasswordField2.requestFocus();
                return;
            }
        }

        Connection conn = getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Check if new username is already used by another user
        String checkSql = "SELECT user_id FROM users WHERE username = ? AND user_id != ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
            checkPst.setString(1, username);
            checkPst.setInt(2, userId);
            try (ResultSet rs = checkPst.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Username '" + username + "' is already taken by another user!", "Duplicate Username", JOptionPane.WARNING_MESSAGE);
                    jTextField2.requestFocus();
                    return;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 6. Check if new NIC is already used by another user
        String checkNicSql = "SELECT user_id FROM users WHERE nic = ? AND user_id != ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkNicSql)) {
            checkPst.setString(1, nic);
            checkPst.setInt(2, userId);
            try (ResultSet rs = checkPst.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "NIC '" + nic + "' is already registered to another user!", "Duplicate NIC", JOptionPane.WARNING_MESSAGE);
                    jTextField4.requestFocus();
                    return;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 7. Update user
        String firstTimeLog = CheckFirstTimeLog.isSelected() ? "0" : "1";
        String updateSql;
        if (updatePassword) {
            updateSql = "UPDATE users SET username = ?, nic = ?, password = ?, role = ?, first_time = ? WHERE user_id = ?";
        } else {
            updateSql = "UPDATE users SET username = ?, nic = ?, role = ?, first_time = ? WHERE user_id = ?";
        }

        try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
            pst.setString(1, username);
            pst.setString(2, nic);
            if (updatePassword) {
                pst.setString(3, hashPassword(password));
                pst.setString(4, role != null ? role : "Staff");
                pst.setString(5, firstTimeLog);
                pst.setInt(6, userId);
            } else {
                pst.setString(3, role != null ? role : "Staff");
                pst.setString(4, firstTimeLog);
                pst.setInt(5, userId);
            }

            int affected = pst.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "User updated successfully!");
                loadUsers();
                clearForm();
                btnAddUser.setVisible(true);
                btnhide();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to update user: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnUpdateUserActionPerformed

    private void btnDeleteUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteUserActionPerformed
        String userIdStr = lblUserID.getText().trim();
        if (userIdStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a user from the table to delete!", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid User ID!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String username = jTextField2.getText().trim();
        if (username.equalsIgnoreCase(currentUsername)) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own logged-in account!", "Operation Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete user '" + username + "' (ID: " + userId + ")?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = getConnection();
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database connection not available!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String deleteSql = "DELETE FROM users WHERE user_id = ?";
            try (PreparedStatement pst = conn.prepareStatement(deleteSql)) {
                pst.setInt(1, userId);
                int affected = pst.executeUpdate();
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, "User deleted successfully!");
                    loadUsers();
                    clearForm();
                    btnAddUser.setVisible(true);
                    btnhide();
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this, "Failed to delete user: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnDeleteUserActionPerformed

    private void btnClearUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearUserActionPerformed
        btnAddUser.setVisible(true);
        btnhide();
        clearForm();
    }//GEN-LAST:event_btnClearUserActionPerformed

    private void jLabel10MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel10MouseClicked
        new changeDefaultPassword(currentUsername, currentRole).setVisible(true);
    }//GEN-LAST:event_jLabel10MouseClicked

    private void jLabel10MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel10MouseEntered
        jLabel10.setForeground(Color.blue);
    }//GEN-LAST:event_jLabel10MouseEntered

    private void jLabel10MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel10MouseExited
        jLabel10.setForeground(Color.BLACK);
    }//GEN-LAST:event_jLabel10MouseExited

    private void btnAddStudentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddStudentActionPerformed
        String name = txtStudentName.getText().trim();
        String nic = txtStudentNIC.getText().trim();
        String phone = txtStudentPhone.getText().trim();
        String address = txtStudentAddress.getText().trim();
        String vClass = (String) cmbStudentClass.getSelectedItem();
        String status = (String) cmbStudentStatus.getSelectedItem();

        // 1. Validate Full Name
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Full Name!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentName.requestFocus();
            return;
        }
        if (name.length() < 3 || name.length() > 100) {
            JOptionPane.showMessageDialog(this, "Full Name must be between 3 and 100 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentName.requestFocus();
            return;
        }
        if (!name.matches("^[a-zA-Z\\s.\\-']+$")) {
            JOptionPane.showMessageDialog(this, "Full Name can only contain letters, spaces, dots, and hyphens!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentName.requestFocus();
            return;
        }

        // 2. Validate NIC Number
        if (nic.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter NIC Number!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentNIC.requestFocus();
            return;
        }
        if (!nic.matches("^([0-9]{9}[vVxX]|[0-9]{12})$")) {
            JOptionPane.showMessageDialog(this, "Invalid NIC format!\nNIC must be either:\n- 9 digits followed by V or X (e.g., 123456789V)\n- 12 digits (e.g., 200012345678)", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentNIC.requestFocus();
            return;
        }

        // 3. Validate Phone Number
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Phone Number!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentPhone.requestFocus();
            return;
        }
        if (!phone.matches("^(?:0|\\+94)?[0-9]{9,10}$")) {
            JOptionPane.showMessageDialog(this, "Invalid Phone Number!\nPlease enter a valid phone number (e.g., 0771234567).", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentPhone.requestFocus();
            return;
        }

        // 4. Validate Address
        if (address.length() > 200) {
            JOptionPane.showMessageDialog(this, "Address cannot exceed 200 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentAddress.requestFocus();
            return;
        }

        // Fallbacks for combo selections
        if (vClass == null || vClass.trim().isEmpty()) {
            vClass = "Class B (Dual Purpose / Car)";
        }
        if (status == null || status.trim().isEmpty()) {
            status = "Active Learner";
        }

        Connection conn = getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Check if NIC is already registered
        String checkNicSql = "SELECT student_id FROM students WHERE nic = ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkNicSql)) {
            checkPst.setString(1, nic);
            try (ResultSet rs = checkPst.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "A student with NIC '" + nic + "' is already registered!", "Duplicate NIC", JOptionPane.WARNING_MESSAGE);
                    txtStudentNIC.requestFocus();
                    return;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 6. Insert new student
        String insertSql = "INSERT INTO students (full_name, nic, phone, address, vehicle_class, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, name);
            pst.setString(2, nic);
            pst.setString(3, phone);
            pst.setString(4, address);
            pst.setString(5, vClass);
            pst.setString(6, status);

            int affected = pst.executeUpdate();
            if (affected > 0) {
                String genIdStr = "";
                try (ResultSet genKeys = pst.getGeneratedKeys()) {
                    if (genKeys.next()) {
                        int genId = genKeys.getInt(1);
                        genIdStr = String.format(" (STU-%04d)", genId);
                    }
                }
                JOptionPane.showMessageDialog(this, "Student registered successfully!" + genIdStr, "Success", JOptionPane.INFORMATION_MESSAGE);
                loadStudents();
                clearStudentForm();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to register student: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnAddStudentActionPerformed

    private void btnActionRegisterMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnActionRegisterMouseClicked
        switchCard("cardStudents");
        loadStudents();
    }//GEN-LAST:event_btnActionRegisterMouseClicked

    private void btnActionBookingMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnActionBookingMouseClicked
        switchCard("cardBookings");
    }//GEN-LAST:event_btnActionBookingMouseClicked

    private void btnActionManageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnActionManageMouseClicked
        switchCard("cardBookingManagement");
    }//GEN-LAST:event_btnActionManageMouseClicked

    private void btnUpdateStudentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateStudentActionPerformed
        String studentIdStr = lblStudentIDVal.getText().trim();
        if (studentIdStr.isEmpty() || studentIdStr.equalsIgnoreCase("STU-Auto")) {
            int selectedRow = tableStudents.getSelectedRow();
            if (selectedRow >= 0) {
                studentIdStr = String.valueOf(tableStudents.getValueAt(selectedRow, 0)).trim();
            } else {
                JOptionPane.showMessageDialog(this, "Please select a student from the table to update!", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int studentId;
        try {
            String cleanId = studentIdStr.replaceAll("[^0-9]", "");
            if (cleanId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Invalid Student ID!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            studentId = Integer.parseInt(cleanId);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Student ID!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = txtStudentName.getText().trim();
        String nic = txtStudentNIC.getText().trim();
        String phone = txtStudentPhone.getText().trim();
        String address = txtStudentAddress.getText().trim();
        String vClass = (String) cmbStudentClass.getSelectedItem();
        String status = (String) cmbStudentStatus.getSelectedItem();

        // 1. Validate Full Name
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Full Name!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentName.requestFocus();
            return;
        }
        if (name.length() < 3 || name.length() > 100) {
            JOptionPane.showMessageDialog(this, "Full Name must be between 3 and 100 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentName.requestFocus();
            return;
        }
        if (!name.matches("^[a-zA-Z\\s.\\-']+$")) {
            JOptionPane.showMessageDialog(this, "Full Name can only contain letters, spaces, dots, and hyphens!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentName.requestFocus();
            return;
        }

        // 2. Validate NIC Number
        if (nic.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter NIC Number!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentNIC.requestFocus();
            return;
        }
        if (!nic.matches("^([0-9]{9}[vVxX]|[0-9]{12})$")) {
            JOptionPane.showMessageDialog(this, "Invalid NIC format!\nNIC must be either:\n- 9 digits followed by V or X (e.g., 123456789V)\n- 12 digits (e.g., 200012345678)", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentNIC.requestFocus();
            return;
        }

        // 3. Validate Phone Number
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Phone Number!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentPhone.requestFocus();
            return;
        }
        if (!phone.matches("^(?:0|\\+94)?[0-9]{9,10}$")) {
            JOptionPane.showMessageDialog(this, "Invalid Phone Number!\nPlease enter a valid phone number (e.g., 0771234567).", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentPhone.requestFocus();
            return;
        }

        // 4. Validate Address
        if (address.length() > 200) {
            JOptionPane.showMessageDialog(this, "Address cannot exceed 200 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtStudentAddress.requestFocus();
            return;
        }

        // Fallbacks for combo selections
        if (vClass == null || vClass.trim().isEmpty()) {
            vClass = "Class B (Dual Purpose / Car)";
        }
        if (status == null || status.trim().isEmpty()) {
            status = "Active Learner";
        }

        Connection conn = getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Check if NIC is already registered to another student
        String checkNicSql = "SELECT student_id FROM students WHERE nic = ? AND student_id != ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkNicSql)) {
            checkPst.setString(1, nic);
            checkPst.setInt(2, studentId);
            try (ResultSet rs = checkPst.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "A student with NIC '" + nic + "' is already registered!", "Duplicate NIC", JOptionPane.WARNING_MESSAGE);
                    txtStudentNIC.requestFocus();
                    return;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 6. Update student
        String updateSql = "UPDATE students SET full_name = ?, nic = ?, phone = ?, address = ?, vehicle_class = ?, status = ? WHERE student_id = ?";
        try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
            pst.setString(1, name);
            pst.setString(2, nic);
            pst.setString(3, phone);
            pst.setString(4, address);
            pst.setString(5, vClass);
            pst.setString(6, status);
            pst.setInt(7, studentId);

            int affected = pst.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Student updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadStudents();
                clearStudentForm();
            } else {
                JOptionPane.showMessageDialog(this, "No student record was updated. Please verify that the student exists.", "Update Failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to update student: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnUpdateStudentActionPerformed

    private void btnDeleteStudentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteStudentActionPerformed
        String studentIdStr = lblStudentIDVal.getText().trim();
        if (studentIdStr.isEmpty() || studentIdStr.equalsIgnoreCase("STU-Auto")) {
            int selectedRow = tableStudents.getSelectedRow();
            if (selectedRow >= 0) {
                studentIdStr = String.valueOf(tableStudents.getValueAt(selectedRow, 0)).trim();
            } else {
                JOptionPane.showMessageDialog(this, "Please select a student from the table to delete!", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int studentId;
        try {
            String cleanId = studentIdStr.replaceAll("[^0-9]", "");
            if (cleanId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Invalid Student ID!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            studentId = Integer.parseInt(cleanId);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Student ID!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = txtStudentName.getText().trim();
        String displayName = name.isEmpty() ? studentIdStr : name;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete student '" + displayName + "' (ID: " + studentIdStr + ")?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = getConnection();
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String deleteSql = "DELETE FROM students WHERE student_id = ?";
            try (PreparedStatement pst = conn.prepareStatement(deleteSql)) {
                pst.setInt(1, studentId);
                int affected = pst.executeUpdate();
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, "Student deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadStudents();
                    clearStudentForm();
                } else {
                    JOptionPane.showMessageDialog(this, "No student record was deleted. The record may have already been removed.", "Delete Failed", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this, "Failed to delete student: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnDeleteStudentActionPerformed

    private void btnInstAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInstAddActionPerformed
        String name = txtInstFullName.getText().trim();
        String phone = txtInstPhone.getText().trim();
        String nic = txtInstNic.getText().trim();
        String license = txtInstLicense.getText().trim();
        String vClass = (String) cmbInstCategory.getSelectedItem();
        String status = (String) cmbInstStatus.getSelectedItem();

        // 1. Validate Full Name
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Instructor Full Name!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstFullName.requestFocus();
            return;
        }
        if (name.length() < 3 || name.length() > 100) {
            JOptionPane.showMessageDialog(this, "Full Name must be between 3 and 100 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstFullName.requestFocus();
            return;
        }
        if (!name.matches("^[a-zA-Z\\s.\\-']+$")) {
            JOptionPane.showMessageDialog(this, "Full Name can only contain letters, spaces, dots, and hyphens!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstFullName.requestFocus();
            return;
        }

        // 2. Validate Phone Number
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Phone Number!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstPhone.requestFocus();
            return;
        }
        if (!phone.matches("^(?:0|\\+94)?[0-9]{9,10}$")) {
            JOptionPane.showMessageDialog(this, "Invalid Phone Number!\nPlease enter a valid phone number (e.g., 0771234567).", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstPhone.requestFocus();
            return;
        }

        // 3. Validate NIC (if provided)
        if (!nic.isEmpty()) {
            if (!nic.matches("^([0-9]{9}[vVxX]|[0-9]{12})$")) {
                JOptionPane.showMessageDialog(this, "Invalid NIC format!\nNIC must be either:\n- 9 digits followed by V or X (e.g., 123456789V)\n- 12 digits (e.g., 198812345678)", "Validation Error", JOptionPane.WARNING_MESSAGE);
                txtInstNic.requestFocus();
                return;
            }
        }

        // 4. Validate License / Badge No
        if (license.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Driving License / Badge No!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstLicense.requestFocus();
            return;
        }
        if (license.length() < 3 || license.length() > 50) {
            JOptionPane.showMessageDialog(this, "License / Badge No must be between 3 and 50 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstLicense.requestFocus();
            return;
        }

        // Fallbacks for combo selections
        if (vClass == null || vClass.trim().isEmpty()) {
            vClass = "Class B (Dual Purpose / Car)";
        }
        if (status == null || status.trim().isEmpty()) {
            status = "Available";
        }

        Connection conn = getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Check if NIC is already registered to another instructor
        if (!nic.isEmpty()) {
            String checkNicSql = "SELECT instructor_id FROM instructors WHERE nic = ?";
            try (PreparedStatement checkPst = conn.prepareStatement(checkNicSql)) {
                checkPst.setString(1, nic);
                try (ResultSet rs = checkPst.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(this, "An instructor with NIC '" + nic + "' is already registered!", "Duplicate NIC", JOptionPane.WARNING_MESSAGE);
                        txtInstNic.requestFocus();
                        return;
                    }
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // 6. Check if License / Badge No is already registered
        String checkLicSql = "SELECT instructor_id FROM instructors WHERE license_no = ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkLicSql)) {
            checkPst.setString(1, license);
            try (ResultSet rs = checkPst.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "An instructor with License / Badge No '" + license + "' is already registered!", "Duplicate License", JOptionPane.WARNING_MESSAGE);
                    txtInstLicense.requestFocus();
                    return;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 7. Insert instructor
        String insertSql = "INSERT INTO instructors (full_name, phone, nic, license_no, vehicle_class, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, name);
            pst.setString(2, phone);
            pst.setString(3, nic.isEmpty() ? null : nic);
            pst.setString(4, license);
            pst.setString(5, vClass);
            pst.setString(6, status);

            int affected = pst.executeUpdate();
            if (affected > 0) {
                String genIdStr = "";
                try (ResultSet genKeys = pst.getGeneratedKeys()) {
                    if (genKeys.next()) {
                        int genId = genKeys.getInt(1);
                        genIdStr = String.format(" (INS-%03d)", genId);
                    }
                }
                JOptionPane.showMessageDialog(this, "Instructor added successfully!" + genIdStr, "Success", JOptionPane.INFORMATION_MESSAGE);
                loadInstructors();
                clearInstructorForm();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to add instructor: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnInstAddActionPerformed

    private void btnInstUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInstUpdateActionPerformed
        String instIdStr = txtInstId.getText().trim();
        if (instIdStr.isEmpty() || instIdStr.equalsIgnoreCase("INS-Auto")) {
            int selectedRow = tableInstructors.getSelectedRow();
            if (selectedRow >= 0) {
                instIdStr = String.valueOf(tableInstructors.getValueAt(selectedRow, 0)).trim();
            } else {
                JOptionPane.showMessageDialog(this, "Please select an instructor from the table to update!", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int instructorId;
        try {
            String cleanId = instIdStr.replaceAll("[^0-9]", "");
            if (cleanId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Invalid Instructor ID!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            instructorId = Integer.parseInt(cleanId);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Instructor ID!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = txtInstFullName.getText().trim();
        String phone = txtInstPhone.getText().trim();
        String nic = txtInstNic.getText().trim();
        String license = txtInstLicense.getText().trim();
        String vClass = (String) cmbInstCategory.getSelectedItem();
        String status = (String) cmbInstStatus.getSelectedItem();

        // 1. Validate Full Name
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Instructor Full Name!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstFullName.requestFocus();
            return;
        }
        if (name.length() < 3 || name.length() > 100) {
            JOptionPane.showMessageDialog(this, "Full Name must be between 3 and 100 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstFullName.requestFocus();
            return;
        }
        if (!name.matches("^[a-zA-Z\\s.\\-']+$")) {
            JOptionPane.showMessageDialog(this, "Full Name can only contain letters, spaces, dots, and hyphens!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstFullName.requestFocus();
            return;
        }

        // 2. Validate Phone Number
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Phone Number!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstPhone.requestFocus();
            return;
        }
        if (!phone.matches("^(?:0|\\+94)?[0-9]{9,10}$")) {
            JOptionPane.showMessageDialog(this, "Invalid Phone Number!\nPlease enter a valid phone number (e.g., 0771234567).", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstPhone.requestFocus();
            return;
        }

        // 3. Validate NIC (if provided)
        if (!nic.isEmpty()) {
            if (!nic.matches("^([0-9]{9}[vVxX]|[0-9]{12})$")) {
                JOptionPane.showMessageDialog(this, "Invalid NIC format!\nNIC must be either:\n- 9 digits followed by V or X (e.g., 123456789V)\n- 12 digits (e.g., 198812345678)", "Validation Error", JOptionPane.WARNING_MESSAGE);
                txtInstNic.requestFocus();
                return;
            }
        }

        // 4. Validate License / Badge No
        if (license.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Driving License / Badge No!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstLicense.requestFocus();
            return;
        }
        if (license.length() < 3 || license.length() > 50) {
            JOptionPane.showMessageDialog(this, "License / Badge No must be between 3 and 50 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtInstLicense.requestFocus();
            return;
        }

        // Fallbacks for combo selections
        if (vClass == null || vClass.trim().isEmpty()) {
            vClass = "Class B (Dual Purpose / Car)";
        }
        if (status == null || status.trim().isEmpty()) {
            status = "Available";
        }

        Connection conn = getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Check if NIC is already registered to another instructor
        if (!nic.isEmpty()) {
            String checkNicSql = "SELECT instructor_id FROM instructors WHERE nic = ? AND instructor_id != ?";
            try (PreparedStatement checkPst = conn.prepareStatement(checkNicSql)) {
                checkPst.setString(1, nic);
                checkPst.setInt(2, instructorId);
                try (ResultSet rs = checkPst.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(this, "An instructor with NIC '" + nic + "' is already registered!", "Duplicate NIC", JOptionPane.WARNING_MESSAGE);
                        txtInstNic.requestFocus();
                        return;
                    }
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // 6. Check if License / Badge No is already registered to another instructor
        String checkLicSql = "SELECT instructor_id FROM instructors WHERE license_no = ? AND instructor_id != ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkLicSql)) {
            checkPst.setString(1, license);
            checkPst.setInt(2, instructorId);
            try (ResultSet rs = checkPst.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "An instructor with License / Badge No '" + license + "' is already registered!", "Duplicate License", JOptionPane.WARNING_MESSAGE);
                    txtInstLicense.requestFocus();
                    return;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 7. Update instructor
        String updateSql = "UPDATE instructors SET full_name = ?, phone = ?, nic = ?, license_no = ?, vehicle_class = ?, status = ? WHERE instructor_id = ?";
        try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
            pst.setString(1, name);
            pst.setString(2, phone);
            pst.setString(3, nic.isEmpty() ? null : nic);
            pst.setString(4, license);
            pst.setString(5, vClass);
            pst.setString(6, status);
            pst.setInt(7, instructorId);

            int affected = pst.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Instructor updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadInstructors();
                clearInstructorForm();
            } else {
                JOptionPane.showMessageDialog(this, "No instructor record was updated. Please verify that the instructor exists.", "Update Failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to update instructor: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnInstUpdateActionPerformed

    private void btnInstDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInstDeleteActionPerformed
        String instIdStr = txtInstId.getText().trim();
        if (instIdStr.isEmpty() || instIdStr.equalsIgnoreCase("INS-Auto")) {
            int selectedRow = tableInstructors.getSelectedRow();
            if (selectedRow >= 0) {
                instIdStr = String.valueOf(tableInstructors.getValueAt(selectedRow, 0)).trim();
            } else {
                JOptionPane.showMessageDialog(this, "Please select an instructor from the table to delete!", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int instructorId;
        try {
            String cleanId = instIdStr.replaceAll("[^0-9]", "");
            if (cleanId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Invalid Instructor ID!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            instructorId = Integer.parseInt(cleanId);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Instructor ID!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = txtInstFullName.getText().trim();
        String displayName = name.isEmpty() ? instIdStr : name;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete instructor '" + displayName + "' (ID: " + instIdStr + ")?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = getConnection();
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String deleteSql = "DELETE FROM instructors WHERE instructor_id = ?";
            try (PreparedStatement pst = conn.prepareStatement(deleteSql)) {
                pst.setInt(1, instructorId);
                int affected = pst.executeUpdate();
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, "Instructor deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadInstructors();
                    clearInstructorForm();
                } else {
                    JOptionPane.showMessageDialog(this, "No instructor record was deleted. The record may have already been removed.", "Delete Failed", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this, "Failed to delete instructor: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnInstDeleteActionPerformed

    private void btnInstClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInstClearActionPerformed
        btnInstAdd.setVisible(true);
        btnInstUpdate.setVisible(false);
        btnInstDelete.setVisible(false);
        btnInstClear.setVisible(false);
        loadInstructors();
        clearInstructorForm();
    }//GEN-LAST:event_btnInstClearActionPerformed

    private void btnVehAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVehAddActionPerformed
        String model = txtVehModel.getText().trim();
        String plate = txtVehPlate.getText().trim().toUpperCase();
        String vClass = (String) cmbVehCategory.getSelectedItem();
        String transmission = (String) cmbVehTransmission.getSelectedItem();
        String fuel = (String) cmbVehFuel.getSelectedItem();
        String status = (String) cmbVehStatus.getSelectedItem();
        String mileage = txtVehMileage.getText().trim();

        // 1. Validate Vehicle Model / Brand
        if (model.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Vehicle Model / Brand!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehModel.requestFocus();
            return;
        }
        if (model.length() < 2 || model.length() > 100) {
            JOptionPane.showMessageDialog(this, "Vehicle Model / Brand must be between 2 and 100 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehModel.requestFocus();
            return;
        }

        // 2. Validate Registration / Plate No
        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Registration / Plate No!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehPlate.requestFocus();
            return;
        }
        if (plate.length() < 3 || plate.length() > 20) {
            JOptionPane.showMessageDialog(this, "Plate No must be between 3 and 20 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehPlate.requestFocus();
            return;
        }
        if (!plate.matches("^[a-zA-Z0-9\\s\\-]+$")) {
            JOptionPane.showMessageDialog(this, "Plate No can only contain letters, numbers, spaces, and hyphens (e.g., CAB-4512 or WP-3321)!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehPlate.requestFocus();
            return;
        }

        // 3. Validate Mileage length
        if (mileage.length() > 50) {
            JOptionPane.showMessageDialog(this, "Mileage / Service info cannot exceed 50 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehMileage.requestFocus();
            return;
        }

        // Fallbacks for combo selections
        if (vClass == null || vClass.trim().isEmpty()) {
            vClass = "Class B (Dual Purpose / Car)";
        }
        if (transmission == null || transmission.trim().isEmpty()) {
            transmission = "Auto";
        }
        if (fuel == null || fuel.trim().isEmpty()) {
            fuel = "Petrol";
        }
        if (status == null || status.trim().isEmpty()) {
            status = "Available";
        }

        Connection conn = getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. Check if Plate No already exists
        String checkSql = "SELECT vehicle_id FROM vehicles WHERE vehicle_number = ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
            checkPst.setString(1, plate);
            try (ResultSet rs = checkPst.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "A vehicle with Plate No '" + plate + "' is already registered!", "Duplicate Vehicle", JOptionPane.WARNING_MESSAGE);
                    txtVehPlate.requestFocus();
                    return;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Insert vehicle into DB
        String insertSql = "INSERT INTO vehicles (vehicle_number, vehicle_type, model, vehicle_class, transmission, fuel_type, status, mileage) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, plate);
            pst.setString(2, vClass.length() > 50 ? vClass.substring(0, 50) : vClass);
            pst.setString(3, model);
            pst.setString(4, vClass);
            pst.setString(5, transmission);
            pst.setString(6, fuel);
            pst.setString(7, status);
            pst.setString(8, mileage.isEmpty() ? null : mileage);

            int affected = pst.executeUpdate();
            if (affected > 0) {
                String genIdStr = "";
                try (ResultSet genKeys = pst.getGeneratedKeys()) {
                    if (genKeys.next()) {
                        int genId = genKeys.getInt(1);
                        genIdStr = String.format(" (VEH-%03d)", genId);
                    }
                }
                JOptionPane.showMessageDialog(this, "Vehicle added successfully!" + genIdStr, "Success", JOptionPane.INFORMATION_MESSAGE);
                loadVehicles();
                clearVehicleForm();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to add vehicle: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnVehAddActionPerformed

    private void btnVehUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVehUpdateActionPerformed
        String vehIdStr = txtVehId.getText().trim();
        if (vehIdStr.isEmpty() || vehIdStr.equalsIgnoreCase("VEH-Auto")) {
            int selectedRow = tableVehicles.getSelectedRow();
            if (selectedRow >= 0) {
                vehIdStr = String.valueOf(tableVehicles.getValueAt(selectedRow, 0)).trim();
            } else {
                JOptionPane.showMessageDialog(this, "Please select a vehicle from the table to update!", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int vehicleId;
        try {
            String cleanId = vehIdStr.replaceAll("[^0-9]", "");
            if (cleanId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Invalid Vehicle ID!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            vehicleId = Integer.parseInt(cleanId);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Vehicle ID!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String model = txtVehModel.getText().trim();
        String plate = txtVehPlate.getText().trim().toUpperCase();
        String vClass = (String) cmbVehCategory.getSelectedItem();
        String transmission = (String) cmbVehTransmission.getSelectedItem();
        String fuel = (String) cmbVehFuel.getSelectedItem();
        String status = (String) cmbVehStatus.getSelectedItem();
        String mileage = txtVehMileage.getText().trim();

        // 1. Validate Vehicle Model / Brand
        if (model.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Vehicle Model / Brand!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehModel.requestFocus();
            return;
        }
        if (model.length() < 2 || model.length() > 100) {
            JOptionPane.showMessageDialog(this, "Vehicle Model / Brand must be between 2 and 100 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehModel.requestFocus();
            return;
        }

        // 2. Validate Registration / Plate No
        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Registration / Plate No!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehPlate.requestFocus();
            return;
        }
        if (plate.length() < 3 || plate.length() > 20) {
            JOptionPane.showMessageDialog(this, "Plate No must be between 3 and 20 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehPlate.requestFocus();
            return;
        }
        if (!plate.matches("^[a-zA-Z0-9\\s\\-]+$")) {
            JOptionPane.showMessageDialog(this, "Plate No can only contain letters, numbers, spaces, and hyphens (e.g., CAB-4512 or WP-3321)!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehPlate.requestFocus();
            return;
        }

        // 3. Validate Mileage length
        if (mileage.length() > 50) {
            JOptionPane.showMessageDialog(this, "Mileage / Service info cannot exceed 50 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtVehMileage.requestFocus();
            return;
        }

        // Fallbacks for combo selections
        if (vClass == null || vClass.trim().isEmpty()) {
            vClass = "Class B (Dual Purpose / Car)";
        }
        if (transmission == null || transmission.trim().isEmpty()) {
            transmission = "Auto";
        }
        if (fuel == null || fuel.trim().isEmpty()) {
            fuel = "Petrol";
        }
        if (status == null || status.trim().isEmpty()) {
            status = "Available";
        }

        Connection conn = getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. Check if Plate No is already registered to another vehicle
        String checkSql = "SELECT vehicle_id FROM vehicles WHERE vehicle_number = ? AND vehicle_id != ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
            checkPst.setString(1, plate);
            checkPst.setInt(2, vehicleId);
            try (ResultSet rs = checkPst.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "A vehicle with Plate No '" + plate + "' is already registered!", "Duplicate Vehicle", JOptionPane.WARNING_MESSAGE);
                    txtVehPlate.requestFocus();
                    return;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Update vehicle in DB
        String updateSql = "UPDATE vehicles SET vehicle_number = ?, vehicle_type = ?, model = ?, vehicle_class = ?, transmission = ?, fuel_type = ?, status = ?, mileage = ? WHERE vehicle_id = ?";
        try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
            pst.setString(1, plate);
            pst.setString(2, vClass.length() > 50 ? vClass.substring(0, 50) : vClass);
            pst.setString(3, model);
            pst.setString(4, vClass);
            pst.setString(5, transmission);
            pst.setString(6, fuel);
            pst.setString(7, status);
            pst.setString(8, mileage.isEmpty() ? null : mileage);
            pst.setInt(9, vehicleId);

            int affected = pst.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Vehicle updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadVehicles();
                clearVehicleForm();
            } else {
                JOptionPane.showMessageDialog(this, "No vehicle record was updated. Please verify that the vehicle exists.", "Update Failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Failed to update vehicle: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnVehUpdateActionPerformed

    private void btnVehDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVehDeleteActionPerformed
        String vehIdStr = txtVehId.getText().trim();
        if (vehIdStr.isEmpty() || vehIdStr.equalsIgnoreCase("VEH-Auto")) {
            int selectedRow = tableVehicles.getSelectedRow();
            if (selectedRow >= 0) {
                vehIdStr = String.valueOf(tableVehicles.getValueAt(selectedRow, 0)).trim();
            } else {
                JOptionPane.showMessageDialog(this, "Please select a vehicle from the table to delete!", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int vehicleId;
        try {
            String cleanId = vehIdStr.replaceAll("[^0-9]", "");
            if (cleanId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Invalid Vehicle ID!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            vehicleId = Integer.parseInt(cleanId);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Vehicle ID!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String model = txtVehModel.getText().trim();
        String plate = txtVehPlate.getText().trim();
        String displayName = model.isEmpty() ? vehIdStr : (model + (!plate.isEmpty() ? " (" + plate + ")" : ""));

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete vehicle '" + displayName + "' (ID: " + vehIdStr + ")?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = getConnection();
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if vehicle is linked to any bookings
            String checkBookingSql = "SELECT COUNT(*) FROM bookings WHERE vehicle_id = ?";
            try (PreparedStatement checkPst = conn.prepareStatement(checkBookingSql)) {
                checkPst.setInt(1, vehicleId);
                try (ResultSet rs = checkPst.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(this, "Cannot delete vehicle '" + displayName + "' because it is linked to " + rs.getInt(1) + " booking record(s)!\nPlease cancel or reassign those bookings first.", "Cannot Delete Vehicle", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "Failed to check vehicle bookings constraint", ex);
            }

            String deleteSql = "DELETE FROM vehicles WHERE vehicle_id = ?";
            try (PreparedStatement pst = conn.prepareStatement(deleteSql)) {
                pst.setInt(1, vehicleId);
                int affected = pst.executeUpdate();
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, "Vehicle deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadVehicles();
                    clearVehicleForm();
                } else {
                    JOptionPane.showMessageDialog(this, "No vehicle record was deleted. The record may have already been removed.", "Delete Failed", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this, "Failed to delete vehicle: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnVehDeleteActionPerformed

    private void btnVehClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVehClearActionPerformed
        btnVehAdd.setVisible(true);
        btnVehUpdate.setVisible(false);
        btnVehDelete.setVisible(false);
        btnVehClear.setVisible(false);
        loadVehicles();
        clearVehicleForm();
    }//GEN-LAST:event_btnVehClearActionPerformed

    private void btnBkAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBkAddActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnBkAddActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        FlatLafSetup.setup();
        java.awt.EventQueue.invokeLater(() -> new Dashbord("chamika", "Admin").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Bookings;
    private javax.swing.JPanel Bookings_Management;
    private javax.swing.JCheckBox CheckFirstTimeLog;
    private javax.swing.JPanel Contructor;
    private javax.swing.JPanel Dashbord;
    private javax.swing.JPanel Instructors;
    private javax.swing.JPanel Student;
    private javax.swing.JPanel Vehicles;
    private javax.swing.JButton btnActionBooking;
    private javax.swing.JButton btnActionManage;
    private javax.swing.JButton btnActionRegister;
    private javax.swing.JButton btnAddStudent;
    private javax.swing.JButton btnAddUser;
    private javax.swing.JButton btnBkAdd;
    private javax.swing.JButton btnBkClear;
    private javax.swing.JButton btnBkDelete;
    private javax.swing.JButton btnBkRefresh;
    private javax.swing.JButton btnBkUpdate;
    private javax.swing.JButton btnBmCancel;
    private javax.swing.JButton btnBmClear;
    private javax.swing.JButton btnBmConfirm;
    private javax.swing.JButton btnBmRefresh;
    private javax.swing.JButton btnBmReschedule;
    private javax.swing.JButton btnBooking;
    private javax.swing.JButton btnBookingManage;
    private javax.swing.JButton btnClearStudent;
    private javax.swing.JButton btnClearUser;
    private javax.swing.JButton btnDashboard;
    private javax.swing.JButton btnDeleteStudent;
    private javax.swing.JButton btnDeleteUser;
    private javax.swing.JButton btnInstAdd;
    private javax.swing.JButton btnInstClear;
    private javax.swing.JButton btnInstDelete;
    private javax.swing.JButton btnInstRefresh;
    private javax.swing.JButton btnInstUpdate;
    private javax.swing.JButton btnInstructors;
    private javax.swing.JButton btnResetStudent;
    private javax.swing.JButton btnSearchStudent;
    private javax.swing.JButton btnStudent;
    private javax.swing.JButton btnUpdateStudent;
    private javax.swing.JButton btnUpdateUser;
    private javax.swing.JButton btnUserManagement;
    private javax.swing.JButton btnVehAdd;
    private javax.swing.JButton btnVehClear;
    private javax.swing.JButton btnVehDelete;
    private javax.swing.JButton btnVehRefresh;
    private javax.swing.JButton btnVehUpdate;
    private javax.swing.JButton btnVehicle;
    private javax.swing.JButton btnViewAllBookings;
    private javax.swing.JComboBox<String> cmbBkFilterStatus;
    private javax.swing.JComboBox<String> cmbBkInstructor;
    private javax.swing.JComboBox<String> cmbBkLessonType;
    private javax.swing.JComboBox<String> cmbBkPayment;
    private javax.swing.JComboBox<String> cmbBkStatus;
    private javax.swing.JComboBox<String> cmbBkVehicle;
    private javax.swing.JComboBox<String> cmbBmFilterStatus;
    private javax.swing.JComboBox<String> cmbBmInstructor;
    private javax.swing.JComboBox<String> cmbBmStatus;
    private javax.swing.JComboBox<String> cmbBmTimeSlot;
    private javax.swing.JComboBox<String> cmbBmVehicle;
    private javax.swing.JComboBox<String> cmbInstCategory;
    private javax.swing.JComboBox<String> cmbInstFilterStatus;
    private javax.swing.JComboBox<String> cmbInstStatus;
    private javax.swing.JComboBox<String> cmbStudentClass;
    private javax.swing.JComboBox<String> cmbStudentStatus;
    private javax.swing.JComboBox<String> cmbVehCategory;
    private javax.swing.JComboBox<String> cmbVehFilterStatus;
    private javax.swing.JComboBox<String> cmbVehFuel;
    private javax.swing.JComboBox<String> cmbVehStatus;
    private javax.swing.JComboBox<String> cmbVehTransmission;
    private javax.swing.JButton jButton8;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JPasswordField jPasswordField2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JLabel lblActionBkDesc;
    private javax.swing.JLabel lblActionBkTitle;
    private javax.swing.JLabel lblActionMgDesc;
    private javax.swing.JLabel lblActionMgTitle;
    private javax.swing.JLabel lblActionRegDesc;
    private javax.swing.JLabel lblActionRegTitle;
    private javax.swing.JLabel lblBkBadgeConfirmedCount;
    private javax.swing.JLabel lblBkBadgeConfirmedLabel;
    private javax.swing.JLabel lblBkBadgePendingCount;
    private javax.swing.JLabel lblBkBadgePendingLabel;
    private javax.swing.JLabel lblBkBadgeTotalCount;
    private javax.swing.JLabel lblBkBadgeTotalLabel;
    private javax.swing.JLabel lblBkDateTime;
    private javax.swing.JLabel lblBkFormHeader;
    private javax.swing.JLabel lblBkHeaderIcon;
    private javax.swing.JLabel lblBkHeaderSubtitle;
    private javax.swing.JLabel lblBkHeaderTitle;
    private javax.swing.JLabel lblBkId;
    private javax.swing.JLabel lblBkInstructor;
    private javax.swing.JLabel lblBkLessonType;
    private javax.swing.JLabel lblBkPayment;
    private javax.swing.JLabel lblBkStatus;
    private javax.swing.JLabel lblBkStudent;
    private javax.swing.JLabel lblBkTableCount;
    private javax.swing.JLabel lblBkTableTitle;
    private javax.swing.JLabel lblBkVehicle;
    private javax.swing.JLabel lblBmBadgePendingCount;
    private javax.swing.JLabel lblBmBadgePendingLabel;
    private javax.swing.JLabel lblBmBadgeTodayCount;
    private javax.swing.JLabel lblBmBadgeTodayLabel;
    private javax.swing.JLabel lblBmBadgeTotalCount;
    private javax.swing.JLabel lblBmBadgeTotalLabel;
    private javax.swing.JLabel lblBmDate;
    private javax.swing.JLabel lblBmFormHeader;
    private javax.swing.JLabel lblBmHeaderIcon;
    private javax.swing.JLabel lblBmHeaderSubtitle;
    private javax.swing.JLabel lblBmHeaderTitle;
    private javax.swing.JLabel lblBmId;
    private javax.swing.JLabel lblBmInstructor;
    private javax.swing.JLabel lblBmRemarks;
    private javax.swing.JLabel lblBmStatus;
    private javax.swing.JLabel lblBmStudent;
    private javax.swing.JLabel lblBmTableCount;
    private javax.swing.JLabel lblBmTableTitle;
    private javax.swing.JLabel lblBmTimeSlot;
    private javax.swing.JLabel lblBmVehicle;
    private javax.swing.JLabel lblCountBookings;
    private javax.swing.JLabel lblCountInstructors;
    private javax.swing.JLabel lblCountStudents;
    private javax.swing.JLabel lblCountVehicles;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblHeaderSubtitle;
    private javax.swing.JLabel lblHeaderTitle;
    private javax.swing.JLabel lblIconBookings;
    private javax.swing.JLabel lblIconInstructors;
    private javax.swing.JLabel lblIconStudents;
    private javax.swing.JLabel lblIconVehicles;
    private javax.swing.JLabel lblInstBadgeActiveCount;
    private javax.swing.JLabel lblInstBadgeActiveLabel;
    private javax.swing.JLabel lblInstBadgeAvailableCount;
    private javax.swing.JLabel lblInstBadgeAvailableLabel;
    private javax.swing.JLabel lblInstBadgeTotalCount;
    private javax.swing.JLabel lblInstBadgeTotalLabel;
    private javax.swing.JLabel lblInstCategory;
    private javax.swing.JLabel lblInstFormHeader;
    private javax.swing.JLabel lblInstFullName;
    private javax.swing.JLabel lblInstHeaderIcon;
    private javax.swing.JLabel lblInstHeaderSubtitle;
    private javax.swing.JLabel lblInstHeaderTitle;
    private javax.swing.JLabel lblInstId;
    private javax.swing.JLabel lblInstLicense;
    private javax.swing.JLabel lblInstNic;
    private javax.swing.JLabel lblInstPhone;
    private javax.swing.JLabel lblInstStatus;
    private javax.swing.JLabel lblInstTableCount;
    private javax.swing.JLabel lblInstTableTitle;
    private javax.swing.JLabel lblQuickSub;
    private javax.swing.JLabel lblQuickTitle;
    private javax.swing.JLabel lblRecentSub;
    private javax.swing.JLabel lblRecentTitle;
    private javax.swing.JLabel lblRole;
    private javax.swing.JLabel lblStudentAddress;
    private javax.swing.JLabel lblStudentCardSub;
    private javax.swing.JLabel lblStudentCardTitle;
    private javax.swing.JLabel lblStudentClass;
    private javax.swing.JLabel lblStudentCount;
    private javax.swing.JLabel lblStudentHeaderSub;
    private javax.swing.JLabel lblStudentHeaderTitle;
    private javax.swing.JLabel lblStudentID;
    private javax.swing.JLabel lblStudentIDVal;
    private javax.swing.JLabel lblStudentNIC;
    private javax.swing.JLabel lblStudentName;
    private javax.swing.JLabel lblStudentPhone;
    private javax.swing.JLabel lblStudentStatus;
    private javax.swing.JLabel lblStudentTableSub;
    private javax.swing.JLabel lblStudentTableTitle;
    private javax.swing.JLabel lblTitleBookings;
    private javax.swing.JLabel lblTitleInstructors;
    private javax.swing.JLabel lblTitleStudents;
    private javax.swing.JLabel lblTitleVehicles;
    private javax.swing.JLabel lblTrendBookings;
    private javax.swing.JLabel lblTrendInstructors;
    private javax.swing.JLabel lblTrendStudents;
    private javax.swing.JLabel lblTrendVehicles;
    private javax.swing.JLabel lblUserID;
    private javax.swing.JLabel lblUsername;
    private javax.swing.JLabel lblVehBadgeReadyCount;
    private javax.swing.JLabel lblVehBadgeReadyLabel;
    private javax.swing.JLabel lblVehBadgeServiceCount;
    private javax.swing.JLabel lblVehBadgeServiceLabel;
    private javax.swing.JLabel lblVehBadgeTotalCount;
    private javax.swing.JLabel lblVehBadgeTotalLabel;
    private javax.swing.JLabel lblVehCategory;
    private javax.swing.JLabel lblVehFormHeader;
    private javax.swing.JLabel lblVehFuel;
    private javax.swing.JLabel lblVehHeaderIcon;
    private javax.swing.JLabel lblVehHeaderSubtitle;
    private javax.swing.JLabel lblVehHeaderTitle;
    private javax.swing.JLabel lblVehId;
    private javax.swing.JLabel lblVehMileage;
    private javax.swing.JLabel lblVehModel;
    private javax.swing.JLabel lblVehPlate;
    private javax.swing.JLabel lblVehStatus;
    private javax.swing.JLabel lblVehTableCount;
    private javax.swing.JLabel lblVehTableTitle;
    private javax.swing.JLabel lblVehTransmission;
    private javax.swing.JPanel panelActionBkText;
    private javax.swing.JPanel panelActionBooking;
    private javax.swing.JPanel panelActionManage;
    private javax.swing.JPanel panelActionMgText;
    private javax.swing.JPanel panelActionRegText;
    private javax.swing.JPanel panelActionRegister;
    private javax.swing.JPanel panelBkBadgeConfirmed;
    private javax.swing.JPanel panelBkBadgePending;
    private javax.swing.JPanel panelBkBadgeTotal;
    private javax.swing.JPanel panelBkFormActions;
    private javax.swing.JPanel panelBkFormCard;
    private javax.swing.JPanel panelBkFormFields;
    private javax.swing.JPanel panelBkFormHeader;
    private javax.swing.JPanel panelBkHeader;
    private javax.swing.JPanel panelBkHeaderLeft;
    private javax.swing.JPanel panelBkHeaderRight;
    private javax.swing.JPanel panelBkHeaderTitles;
    private javax.swing.JPanel panelBkMain;
    private javax.swing.JPanel panelBkSearchFilter;
    private javax.swing.JPanel panelBkTableBottom;
    private javax.swing.JPanel panelBkTableCard;
    private javax.swing.JPanel panelBkTableTop;
    private javax.swing.JPanel panelBmBadgePending;
    private javax.swing.JPanel panelBmBadgeToday;
    private javax.swing.JPanel panelBmBadgeTotal;
    private javax.swing.JPanel panelBmFormActions;
    private javax.swing.JPanel panelBmFormCard;
    private javax.swing.JPanel panelBmFormFields;
    private javax.swing.JPanel panelBmFormHeader;
    private javax.swing.JPanel panelBmHeader;
    private javax.swing.JPanel panelBmHeaderLeft;
    private javax.swing.JPanel panelBmHeaderRight;
    private javax.swing.JPanel panelBmHeaderTitles;
    private javax.swing.JPanel panelBmMain;
    private javax.swing.JPanel panelBmSearchFilter;
    private javax.swing.JPanel panelBmTableBottom;
    private javax.swing.JPanel panelBmTableCard;
    private javax.swing.JPanel panelBmTableTop;
    private javax.swing.JPanel panelCardBookings;
    private javax.swing.JPanel panelCardBookingsText;
    private javax.swing.JPanel panelCardInstructors;
    private javax.swing.JPanel panelCardInstructorsText;
    private javax.swing.JPanel panelCardStudents;
    private javax.swing.JPanel panelCardStudentsText;
    private javax.swing.JPanel panelCardVehicles;
    private javax.swing.JPanel panelCardVehiclesText;
    private javax.swing.JPanel panelDashboardCenter;
    private javax.swing.JPanel panelHeaderLeft;
    private javax.swing.JPanel panelHeaderRight;
    private javax.swing.JPanel panelInstBadgeActive;
    private javax.swing.JPanel panelInstBadgeAvailable;
    private javax.swing.JPanel panelInstBadgeTotal;
    private javax.swing.JPanel panelInstFormActions;
    private javax.swing.JPanel panelInstFormCard;
    private javax.swing.JPanel panelInstFormFields;
    private javax.swing.JPanel panelInstFormHeader;
    private javax.swing.JPanel panelInstHeader;
    private javax.swing.JPanel panelInstHeaderLeft;
    private javax.swing.JPanel panelInstHeaderRight;
    private javax.swing.JPanel panelInstHeaderTitles;
    private javax.swing.JPanel panelInstMain;
    private javax.swing.JPanel panelInstSearchFilter;
    private javax.swing.JPanel panelInstTableBottom;
    private javax.swing.JPanel panelInstTableCard;
    private javax.swing.JPanel panelInstTableTop;
    private javax.swing.JPanel panelQuickActions;
    private javax.swing.JPanel panelQuickGrid;
    private javax.swing.JPanel panelQuickHeader;
    private javax.swing.JPanel panelRecentBookings;
    private javax.swing.JPanel panelRecentHeader;
    private javax.swing.JPanel panelRecentTitles;
    private javax.swing.JPanel panelStudentBody;
    private javax.swing.JPanel panelStudentFormCard;
    private javax.swing.JPanel panelStudentHeader;
    private javax.swing.JPanel panelStudentHeaderLeft;
    private javax.swing.JPanel panelStudentHeaderRight;
    private javax.swing.JPanel panelStudentTableCard;
    private javax.swing.JPanel panelStudentTableFooter;
    private javax.swing.JPanel panelStudentTableHeader;
    private javax.swing.JPanel panelSummaryCards;
    private javax.swing.JPanel panelTopSection;
    private javax.swing.JPanel panelVehBadgeReady;
    private javax.swing.JPanel panelVehBadgeService;
    private javax.swing.JPanel panelVehBadgeTotal;
    private javax.swing.JPanel panelVehFormActions;
    private javax.swing.JPanel panelVehFormCard;
    private javax.swing.JPanel panelVehFormFields;
    private javax.swing.JPanel panelVehFormHeader;
    private javax.swing.JPanel panelVehHeader;
    private javax.swing.JPanel panelVehHeaderLeft;
    private javax.swing.JPanel panelVehHeaderRight;
    private javax.swing.JPanel panelVehHeaderTitles;
    private javax.swing.JPanel panelVehMain;
    private javax.swing.JPanel panelVehSearchFilter;
    private javax.swing.JPanel panelVehTableBottom;
    private javax.swing.JPanel panelVehTableCard;
    private javax.swing.JPanel panelVehTableTop;
    private javax.swing.JScrollPane scrollBkForm;
    private javax.swing.JScrollPane scrollBkTable;
    private javax.swing.JScrollPane scrollBmForm;
    private javax.swing.JScrollPane scrollBmTable;
    private javax.swing.JScrollPane scrollInstForm;
    private javax.swing.JScrollPane scrollInstTable;
    private javax.swing.JScrollPane scrollRecentBookings;
    private javax.swing.JScrollPane scrollStudentTable;
    private javax.swing.JScrollPane scrollVehForm;
    private javax.swing.JScrollPane scrollVehTable;
    private javax.swing.JTable tableBookingManagement;
    private javax.swing.JTable tableBookings;
    private javax.swing.JTable tableInstructors;
    private javax.swing.JTable tableRecentBookings;
    private javax.swing.JTable tableStudents;
    private javax.swing.JTable tableVehicles;
    private javax.swing.JTextField txtBkDateTime;
    private javax.swing.JTextField txtBkId;
    private javax.swing.JTextField txtBkSearch;
    private javax.swing.JTextField txtBkStudent;
    private javax.swing.JTextField txtBmDate;
    private javax.swing.JTextField txtBmId;
    private javax.swing.JTextField txtBmRemarks;
    private javax.swing.JTextField txtBmSearch;
    private javax.swing.JTextField txtBmStudent;
    private javax.swing.JTextField txtInstFullName;
    private javax.swing.JTextField txtInstId;
    private javax.swing.JTextField txtInstLicense;
    private javax.swing.JTextField txtInstNic;
    private javax.swing.JTextField txtInstPhone;
    private javax.swing.JTextField txtInstSearch;
    private javax.swing.JTextField txtSearchStudent;
    private javax.swing.JTextField txtStudentAddress;
    private javax.swing.JTextField txtStudentNIC;
    private javax.swing.JTextField txtStudentName;
    private javax.swing.JTextField txtStudentPhone;
    private javax.swing.JTextField txtVehId;
    private javax.swing.JTextField txtVehMileage;
    private javax.swing.JTextField txtVehModel;
    private javax.swing.JTextField txtVehPlate;
    private javax.swing.JTextField txtVehSearch;
    private javax.swing.JPanel user_Management;
    // End of variables declaration//GEN-END:variables

    private void btnhide() {
        btnUpdateUser.setVisible(false);
        btnDeleteUser.setVisible(false);
        btnClearUser.setVisible(false);
        btnUpdateStudent.setVisible(false);
        btnDeleteStudent.setVisible(false);
        btnClearStudent.setVisible(false);
        btnAddStudent.setVisible(true);
        btnInstUpdate.setVisible(false);
        btnInstDelete.setVisible(false);
        btnInstClear.setVisible(false);
        btnInstAdd.setVisible(true);
        btnVehUpdate.setVisible(false);
        btnVehDelete.setVisible(false);
        btnVehClear.setVisible(false);
        btnVehAdd.setVisible(true);
    }
}
