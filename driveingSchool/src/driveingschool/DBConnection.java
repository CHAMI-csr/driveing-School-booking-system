package driveingschool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class DBConnection {

    public Connection con;

    public static Connection connect() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/driving_school_db", "root", "6969");

            return conn;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Database Connection Error: \n" + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
    }
     public static void main(String[] args) {
        Connection c = connect();
        if (c != null) {
            JOptionPane.showMessageDialog(null, "Database Connected Successfully!100%");
        }
    }

}
