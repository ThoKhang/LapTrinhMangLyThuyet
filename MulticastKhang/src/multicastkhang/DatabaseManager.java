package multicastkhang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    // --- CẤU HÌNH KẾT NỐI SQL SERVER ---
    private static final String SERVER = "localhost";
    private static final String PORT = "1433";
    private static final String DATABASE = "ChatAppDB"; // Tên database bạn đã tạo trong SSMS
    private static final String USER = "sa";            // Thay bằng tài khoản SQL Server của bạn
    private static final String PASS = "123";        // Thay bằng mật khẩu SQL Server của bạn

    // Chuỗi kết nối (trustServerCertificate=true giúp tránh lỗi SSL khi chạy local)
    private static final String URL = "jdbc:sqlserver://" + SERVER + ":" + PORT + 
                                      ";databaseName=" + DATABASE + 
                                      ";encrypt=true;trustServerCertificate=true;";

    // Hàm lấy kết nối
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void init() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 1. Tạo bảng Users (SQL Server dùng NVARCHAR thay vì TEXT cho Primary Key)
            String createUsers = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='users' AND xtype='U') " +
                                 "CREATE TABLE users (username NVARCHAR(255) PRIMARY KEY, password NVARCHAR(255))";
            stmt.execute(createUsers);
            
            // 2. Tạo bảng Rooms
            String createRooms = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='rooms' AND xtype='U') " +
                                 "CREATE TABLE rooms (name NVARCHAR(255) PRIMARY KEY)";
            stmt.execute(createRooms);
            
            // 3. Thêm phòng mặc định (SQL Server dùng IF NOT EXISTS thay vì INSERT OR IGNORE)
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM rooms WHERE name='Lobby') " +
                         "INSERT INTO rooms (name) VALUES ('Lobby')");
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM rooms WHERE name='general') " +
                         "INSERT INTO rooms (name) VALUES ('general')");
            
            System.out.println("[DB] Kết nối SQL Server và khởi tạo bảng thành công.");
        } catch (SQLException e) {
            System.err.println("[DB LỖI] Không thể kết nối SQL Server!");
            System.err.println("Chi tiết: " + e.getMessage());
            System.err.println("=> Hãy chắc chắn: 1. Đã tạo DB 'ChatAppDB'. 2. Sai user/pass. 3. Chưa bật TCP/IP port 1433.");
        }
    }

    public static boolean registerUser(String user, String pass) {
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Sẽ nhảy vào đây nếu username đã tồn tại (Vi phạm Primary Key)
            return false; 
        }
    }

    public static boolean checkLogin(String user, String pass) {
        String sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Trả về true nếu tìm thấy user
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> getAllRooms() {
        List<String> roomList = new ArrayList<>();
        String sql = "SELECT name FROM rooms";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                roomList.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roomList;
    }

    public static void addRoom(String name) {
        String sql = "IF NOT EXISTS (SELECT 1 FROM rooms WHERE name=?) INSERT INTO rooms (name) VALUES (?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void deleteRoom(String name) {
        String sql = "DELETE FROM rooms WHERE name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}