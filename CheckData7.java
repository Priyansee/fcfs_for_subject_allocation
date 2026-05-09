import java.sql.*;

public class CheckData7 {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/localBTP";
        String user = "postgres";
        String password = "Princy@1301";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, "ec2", "studentregistrationcourses", null);
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if (col.equals("srctype") || col.equals("srcstatus") || col.equals("srccreatedat") || col.equals("srcrowstate")) {
                    System.out.println(col + ": " + rs.getString("TYPE_NAME"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
