import java.sql.*;

public class SetupTestData {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/localBTP";
        String user = "postgres";
        String password = "Princy@1301";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("Cleaning up old test data...");
            stmt.execute("DELETE FROM ec2.studentregistrationcourses WHERE srcsrgid BETWEEN 20000 AND 20100");
            stmt.execute("DELETE FROM ec2.studentregistrations WHERE srgid BETWEEN 20000 AND 20100");

            System.out.println("Inserting 100 test students...");
            stmt.execute("INSERT INTO ec2.studentregistrations (srgid, srgstdid) " +
                         "SELECT i, i FROM generate_series(20000, 20100) AS i");

            System.out.println("Resetting seat counts for Slot 1...");
            // Use the correct schema and table names from your logs
            stmt.execute("UPDATE ec2.termcourseavailablefor SET tca_booked = 0 " +
                         "WHERE tcatcrid IN (SELECT tcrid FROM ec2.termcourses WHERE tcrslot = '1')");
            
            System.out.println("Test data setup complete!");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
