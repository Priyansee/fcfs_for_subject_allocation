import java.sql.*;

public class CheckData6 {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/localBTP";
        String user = "postgres";
        String password = "Princy@1301";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, "ec2", "studentregistrationcourses", null);
            System.out.println("Columns in studentregistrationcourses:");
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                String nullable = rs.getString("IS_NULLABLE");
                String def = rs.getString("COLUMN_DEF");
                if ("NO".equals(nullable) && def == null) {
                    System.out.println(col + " - NOT NULL (No Default)");
                } else if ("NO".equals(nullable)) {
                    System.out.println(col + " - NOT NULL (Default: " + def + ")");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
