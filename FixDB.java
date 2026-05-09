import java.sql.*;

public class FixDB {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/localBTP";
        String user = "postgres";
        String password = "Princy@1301";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            
            // Get max ID
            ResultSet rsMax = stmt.executeQuery("SELECT COALESCE(MAX(srcid), 0) FROM ec2.studentregistrationcourses");
            long maxId = 0;
            if(rsMax.next()){
                 maxId = rsMax.getLong(1);
            }
            long nextVal = maxId + 1;
            
            System.out.println("Max srcid is " + maxId + ". Creating sequence starting at " + nextVal);
            
            try {
                stmt.execute("CREATE SEQUENCE ec2.studentregistrationcourses_srcid_seq START WITH " + nextVal);
                System.out.println("Sequence created.");
            } catch (SQLException e) {
                System.out.println("Sequence might already exist: " + e.getMessage());
            }
            
            stmt.execute("ALTER TABLE ec2.studentregistrationcourses ALTER COLUMN srcid SET DEFAULT nextval('ec2.studentregistrationcourses_srcid_seq')");
            System.out.println("Column default updated successfully.");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
