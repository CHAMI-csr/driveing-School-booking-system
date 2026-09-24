/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package driveingschool;

import java.awt.CardLayout;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author chiki
 */
public class changeDefaultPassword extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(changeDefaultPassword.class.getName());
    private String currentUsername;
    private String currentRole;
    private Connection con;

    /**
     * Creates new form changeDefaultPassword
     */
    public changeDefaultPassword(String currentUsername, String currentRole) {
        initComponents();
        this.currentUsername = (currentUsername != null) ? currentUsername : "";
        this.currentRole = (currentRole != null) ? currentRole : "";

        lblUsername.setText(this.currentUsername);
        lblUsername1.setText(this.currentUsername);
        role.setText(this.currentRole);
        role1.setText(this.currentRole);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setTitle("Change Default Password");

        switchCard("cardConfirmAdmin");
    }

    private void switchCard(String cardName) {
        CardLayout cl = (CardLayout) jPanel1.getLayout();
        cl.show(jPanel1, cardName);
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

    private boolean verifyAdminPassword(String enteredPassword) {
        Connection conn = getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String sql = "SELECT password FROM users WHERE username = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, this.currentUsername);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    String enteredHash = hashPassword(enteredPassword);
                    return enteredHash.equals(storedHash);
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to verify admin password", ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
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
                logger.log(Level.WARNING, "Failed to read default password from settings", ex);
            }
        }
        return "1234";
    }

    private boolean updateDefaultPassword(String newPassword) {
        Connection conn = getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection not available!", "Database Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            String checkSql = "SELECT COUNT(*) FROM settings";
            try (PreparedStatement pst = conn.prepareStatement(checkSql); ResultSet rs = pst.executeQuery()) {
                boolean exists = false;
                if (rs.next() && rs.getInt(1) > 0) {
                    exists = true;
                }

                String sql;
                if (exists) {
                    sql = "UPDATE settings SET default_password = ?";
                } else {
                    sql = "INSERT INTO settings (default_password) VALUES (?)";
                }

                try (PreparedStatement savePst = conn.prepareStatement(sql)) {
                    savePst.setString(1, newPassword);
                    int affected = savePst.executeUpdate();
                    return affected > 0;
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to update default password in settings", ex);
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return false;
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
        ComfirmPassword = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        role = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPasswordField2 = new javax.swing.JPasswordField();
        jButton2 = new javax.swing.JButton();
        lblUsername = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        ChangePassword = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        role1 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPasswordField3 = new javax.swing.JPasswordField();
        jButton3 = new javax.swing.JButton();
        lblUsername1 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jPasswordField4 = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setLayout(new java.awt.CardLayout());

        ComfirmPassword.setBackground(new java.awt.Color(0, 0, 0));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_password_25px_1.png"))); // NOI18N

        role.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        role.setForeground(new java.awt.Color(255, 255, 255));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Comfirm");

        jButton2.setBackground(new java.awt.Color(204, 255, 0));
        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton2.setText("Comfirm");
        jButton2.setBorder(null);
        jButton2.addActionListener(this::jButton2ActionPerformed);

        lblUsername.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblUsername.setForeground(new java.awt.Color(255, 255, 255));
        lblUsername.setText("Username");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Password");

        javax.swing.GroupLayout ComfirmPasswordLayout = new javax.swing.GroupLayout(ComfirmPassword);
        ComfirmPassword.setLayout(ComfirmPasswordLayout);
        ComfirmPasswordLayout.setHorizontalGroup(
            ComfirmPasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ComfirmPasswordLayout.createSequentialGroup()
                .addGroup(ComfirmPasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ComfirmPasswordLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(role, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(ComfirmPasswordLayout.createSequentialGroup()
                        .addGap(96, 96, 96)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblUsername)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)))
                .addContainerGap(81, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ComfirmPasswordLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(144, 144, 144))
            .addGroup(ComfirmPasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ComfirmPasswordLayout.createSequentialGroup()
                    .addGap(96, 96, 96)
                    .addComponent(jPasswordField2, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(80, Short.MAX_VALUE)))
        );
        ComfirmPasswordLayout.setVerticalGroup(
            ComfirmPasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ComfirmPasswordLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ComfirmPasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(role, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5)
                .addGroup(ComfirmPasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(58, 58, 58)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(62, Short.MAX_VALUE))
            .addGroup(ComfirmPasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ComfirmPasswordLayout.createSequentialGroup()
                    .addGap(84, 84, 84)
                    .addComponent(jPasswordField2, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(114, Short.MAX_VALUE)))
        );

        jPanel1.add(ComfirmPassword, "cardConfirmAdmin");

        ChangePassword.setBackground(new java.awt.Color(0, 0, 0));

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_password_25px_1.png"))); // NOI18N

        role1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        role1.setForeground(new java.awt.Color(255, 255, 255));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Comfirm password");

        jButton3.setBackground(new java.awt.Color(204, 255, 0));
        jButton3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton3.setText("Comfirm");
        jButton3.setBorder(null);
        jButton3.addActionListener(this::jButton3ActionPerformed);

        lblUsername1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblUsername1.setForeground(new java.awt.Color(255, 255, 255));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("Password");

        javax.swing.GroupLayout ChangePasswordLayout = new javax.swing.GroupLayout(ChangePassword);
        ChangePassword.setLayout(ChangePasswordLayout);
        ChangePasswordLayout.setHorizontalGroup(
            ChangePasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ChangePasswordLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ChangePasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ChangePasswordLayout.createSequentialGroup()
                        .addComponent(jPasswordField4, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 29, Short.MAX_VALUE)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(ChangePasswordLayout.createSequentialGroup()
                        .addGroup(ChangePasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8)
                            .addComponent(jPasswordField3, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7)
                            .addGroup(ChangePasswordLayout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(role1, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblUsername1, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        ChangePasswordLayout.setVerticalGroup(
            ChangePasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ChangePasswordLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ChangePasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ChangePasswordLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(lblUsername1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(ChangePasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(role1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(ChangePasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ChangePasswordLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPasswordField3, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPasswordField4, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ChangePasswordLayout.createSequentialGroup()
                        .addGap(149, 149, 149)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        jPanel1.add(ChangePassword, "cardChangePassword");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
// </editor-fold>                        

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        String enteredPassword = new String(jPasswordField2.getPassword()).trim();
        if (enteredPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your password!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jPasswordField2.requestFocus();
            return;
        }

        if (!verifyAdminPassword(enteredPassword)) {
            JOptionPane.showMessageDialog(this, "Incorrect password for user '" + currentUsername + "'!", "Authentication Error", JOptionPane.ERROR_MESSAGE);
            jPasswordField2.setText("");
            jPasswordField2.requestFocus();
            return;
        }

        // Successfully verified admin password -> switch to ChangePassword card
        switchCard("cardChangePassword");
        jPasswordField3.requestFocus();
    }


    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        String newPassword = new String(jPasswordField3.getPassword()).trim();
        String confirmPassword = new String(jPasswordField4.getPassword()).trim();

        if (newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter new default password!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jPasswordField3.requestFocus();
            return;
        }

        if (newPassword.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password must be at least 4 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jPasswordField3.requestFocus();
            return;
        }

        if (newPassword.length() > 50) {
            JOptionPane.showMessageDialog(this, "Password cannot exceed 50 characters!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jPasswordField3.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            jPasswordField4.requestFocus();
            return;
        }

        if (updateDefaultPassword(newPassword)) {
            JOptionPane.showMessageDialog(this, "Default password updated successfully!\nNew Default Password: " + newPassword, "Success", JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
        }
    }//GEN-LAST:event_jButton3ActionPerformed

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
            logger.log(Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        FlatLafSetup.setup();
        java.awt.EventQueue.invokeLater(() -> new changeDefaultPassword("chamika", "Admin").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ChangePassword;
    private javax.swing.JPanel ComfirmPassword;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPasswordField jPasswordField2;
    private javax.swing.JPasswordField jPasswordField3;
    private javax.swing.JPasswordField jPasswordField4;
    private javax.swing.JLabel lblUsername;
    private javax.swing.JLabel lblUsername1;
    private javax.swing.JLabel role;
    private javax.swing.JLabel role1;
    // End of variables declaration//GEN-END:variables
}
