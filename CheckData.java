import java.sql.*;

public class CheckData {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/localBTP";
        String user = "postgres";
        String password = "Princy@1301";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM ec2.termcourseavailablefor tcaf JOIN ec2.termcourses t ON tcaf.tcatcrid = t.tcrid WHERE t.slot IS NOT NULL");
            if (rs.next()) {
                System.out.println("Total rows with non-null slot: " + rs.getInt(1));
            }
            rs = stmt.executeQuery("SELECT count(*) FROM ec2.termcourseavailablefor tcaf JOIN ec2.termcourses t ON tcaf.tcatcrid = t.tcrid WHERE t.slot IS NOT NULL AND t.tcrtrmid = 78");
            if (rs.next()) {
                System.out.println("Total rows with non-null slot and termid=78: " + rs.getInt(1));
            }
            rs = stmt.executeQuery("SELECT distinct t.tcrtrmid FROM ec2.termcourseavailablefor tcaf JOIN ec2.termcourses t ON tcaf.tcatcrid = t.tcrid");
            System.out.println("Available term ids:");
            while (rs.next()) {
                System.out.println(rs.getInt(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
