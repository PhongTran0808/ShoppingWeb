import java.sql.Connection;
import java.sql.DriverManager;

public class CheckDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://mysql-1e26ae34-tuitentu131-e142.g.aivencloud.com:18003/shopping?useSSL=true&requireSSL=true&serverTimezone=UTC";
        try {
            Connection conn = DriverManager.getConnection(url, "avnadmin", "AVNS_b4Ayu0j9wQL1_83CZvm");
            System.out.println("SUCCESS!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
