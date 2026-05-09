import java.sql.*;

public class CheckData3 {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/localBTP";
        String user = "postgres";
        String password = "Princy@1301";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM ec2.termcourseavailablefor tcaf JOIN ec2.termcourses t ON tcaf.tcatcrid = t.tcrid WHERE t.tcrtrmid = 78");
            if (rs.next()) {
                System.out.println("Total rows for termid 78: " + rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
