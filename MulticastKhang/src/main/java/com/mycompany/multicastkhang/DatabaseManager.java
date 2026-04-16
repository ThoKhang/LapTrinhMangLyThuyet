package com.mycompany.multicastkhang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String SERVER = "localhost";
    private static final String PORT = "1433";
    private static final String DATABASE = "ChatAppDB";
    private static final String USER = "sa"; // Thay bằng User của bạn
    private static final String PASS = "123"; // Thay bằng Pass của bạn

    private static final String URL = "jdbc:sqlserver://" + SERVER + ":" + PORT + 
                                      ";databaseName=" + DATABASE + 
                                      ";encrypt=true;trustServerCertificate=true;";

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void init() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='users' AND xtype='U') " +
                         "CREATE TABLE users (username NVARCHAR(255) PRIMARY KEY, password NVARCHAR(255))");
            
            stmt.execute("IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='rooms' AND xtype='U') " +
                         "CREATE TABLE rooms (name NVARCHAR(255) PRIMARY KEY)");
            
            stmt.execute("IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='messages' AND xtype='U') " +
                         "CREATE TABLE messages (id INT IDENTITY(1,1) PRIMARY KEY, room_name NVARCHAR(255), " +
                         "username NVARCHAR(255), message_text NVARCHAR(MAX), sent_at DATETIME DEFAULT GETDATE(), " +
                         "FOREIGN KEY (room_name) REFERENCES rooms(name) ON DELETE CASCADE)");

            stmt.execute("IF NOT EXISTS (SELECT 1 FROM rooms WHERE name='general') INSERT INTO rooms (name) VALUES ('general')");
            System.out.println("[DB] Khởi tạo thành công.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- QUẢN LÝ TÀI KHOẢN ---
    public static boolean registerUser(String user, String pass) {
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users VALUES(?, ?)")) {
            pstmt.setString(1, user); pstmt.setString(2, pass);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static boolean checkLogin(String user, String pass) {
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM users WHERE username=? AND password=?")) {
            pstmt.setString(1, user); pstmt.setString(2, pass);
            return pstmt.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public static List<String> getAllUsers() {
        List<String> list = new ArrayList<>();
        try (Connection conn = getConnection(); ResultSet rs = conn.createStatement().executeQuery("SELECT username FROM users")) {
            while (rs.next()) list.add(rs.getString("username"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static void deleteUser(String username) {
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            pstmt.setString(1, username); pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void updatePassword(String username, String newPass) {
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password = ? WHERE username = ?")) {
            pstmt.setString(1, newPass); pstmt.setString(2, username); pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- QUẢN LÝ KÊNH & TIN NHẮN ---
    public static void saveMessage(String room, String user, String text) {
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO messages (room_name, username, message_text) VALUES (?, ?, ?)")) {
            pstmt.setString(1, room); pstmt.setString(2, user); pstmt.setString(3, text);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static List<String> getChatHistory(String room) {
        List<String> history = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT TOP 50 username, message_text FROM messages WHERE room_name=? ORDER BY sent_at ASC")) {
            pstmt.setString(1, room);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String u = rs.getString("username");
                String t = rs.getString("message_text");
                history.add(u.equals("SYSTEM") ? "SYS|" + t : "MSG|" + u + ": " + t);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return history;
    }

    public static List<String> getAllRooms() {
        List<String> list = new ArrayList<>();
        try (Connection conn = getConnection(); ResultSet rs = conn.createStatement().executeQuery("SELECT name FROM rooms")) {
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static void addRoom(String name) {
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("IF NOT EXISTS (SELECT 1 FROM rooms WHERE name=?) INSERT INTO rooms VALUES (?)")) {
            pstmt.setString(1, name); pstmt.setString(2, name); pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void deleteRoom(String name) {
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("DELETE FROM rooms WHERE name = ?")) {
            pstmt.setString(1, name); pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}