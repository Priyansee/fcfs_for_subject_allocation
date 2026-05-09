import java.sql.*;

public class CheckData4 {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/localBTP";
        String user = "postgres";
        String password = "Princy@1301";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT distinct tcrslot FROM ec2.termcourses t WHERE t.tcrtrmid = 78");
            while (rs.next()) {
                System.out.println("tcrslot: " + rs.getLong(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
