import java.sql.*;

public class CheckData5 {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/localBTP";
        String user = "postgres";
        String password = "Princy@1301";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, "ec2", "studentregistrationcourses", "srcid");
            if (rs.next()) {
                System.out.println("Column: " + rs.getString("COLUMN_NAME"));
                System.out.println("Type: " + rs.getString("TYPE_NAME"));
                System.out.println("Column Def: " + rs.getString("COLUMN_DEF"));
                System.out.println("Is Autoincrement: " + rs.getString("IS_AUTOINCREMENT"));
            }
            // Check for sequences
            ResultSet rsSeq = conn.createStatement().executeQuery("SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'ec2'");
            System.out.println("Sequences in 'ec2' schema:");
            while (rsSeq.next()) {
                System.out.println(rsSeq.getString(1));
            }
            
            // Get Max ID
            ResultSet rsMax = conn.createStatement().executeQuery("SELECT MAX(srcid) FROM ec2.studentregistrationcourses");
            if(rsMax.next()){
                 System.out.println("Max srcid: " + rsMax.getLong(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
