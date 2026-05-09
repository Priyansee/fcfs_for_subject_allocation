import java.sql.*;

public class CheckData2 {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/localBTP";
        String user = "postgres";
        String password = "Princy@1301";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT tcrid, slot, tcrslot FROM ec2.termcourses LIMIT 10");
            System.out.println("TermCourses Sample:");
            while (rs.next()) {
                System.out.println("id: " + rs.getLong(1) + ", slot(varchar): " + rs.getString(2) + ", tcrslot(int): " + rs.getLong(3));
            }

            rs = stmt.executeQuery("SELECT tcaid, tcatcrid, slot FROM ec2.termcourseavailablefor LIMIT 10");
            System.out.println("TermCourseAvailableFor Sample:");
            while (rs.next()) {
                System.out.println("id: " + rs.getLong(1) + ", tcrid: " + rs.getLong(2) + ", slot: " + rs.getString(3));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
