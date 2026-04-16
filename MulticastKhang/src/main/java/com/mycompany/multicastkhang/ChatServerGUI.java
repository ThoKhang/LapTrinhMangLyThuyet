package com.mycompany.multicastkhang;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ChatServerGUI extends JFrame {
    private static final int PORT = 5555;
    static Map<String, Room> rooms = new ConcurrentHashMap<>();
    static List<ClientHandler> onlineUsers = new CopyOnWriteArrayList<>();

    // --- CÁC COMPONENT GIAO DIỆN ---
    private JTextArea logArea = new JTextArea();
    private JLabel lblOnline = new JLabel("ONLINE: 0", SwingConstants.CENTER);
    private JLabel lblRoomCount = new JLabel("SỐ KÊNH: 0", SwingConstants.CENTER);
    
    private DefaultListModel<String> roomListModel = new DefaultListModel<>();
    private JList<String> roomList = new JList<>(roomListModel);
    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(userListModel);
    
    // Model cho danh sách Database Accounts
    private DefaultListModel<String> accountListModel = new DefaultListModel<>();
    private JList<String> accountList = new JList<>(accountListModel);

    // --- BẢNG MÀU DISCORD ---
    private Color bgDarkest = Color.decode("#1E1F22");
    private Color bgPanel = Color.decode("#2B2D31");
    private Color textMain = Color.decode("#DBDEE1");
    private Color blurple = Color.decode("#5865F2");
    private Color redDanger = Color.decode("#ED4245");
    private Color greenSuccess = Color.decode("#23A559");
    private Color yellowWarn = Color.decode("#FEE75C");

    public ChatServerGUI() {
        setTitle("MÁY CHỦ TRUNG TÂM (ADMIN DASHBOARD)");
        setSize(950, 650); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null);

        // Khởi tạo DB và nạp danh sách phòng
        DatabaseManager.init();
        for (String rName : DatabaseManager.getAllRooms()) {
            rooms.put(rName, new Room(rName));
        }

        initUI();
        startServerThread();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15)); 
        mainPanel.setBackground(bgDarkest);

        // --- 1. HEADER (Thống kê) ---
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 15, 0)); 
        statsPanel.setOpaque(false);
        lblOnline.setFont(new Font("Segoe UI", Font.BOLD, 22)); lblOnline.setForeground(greenSuccess);
        lblRoomCount.setFont(new Font("Segoe UI", Font.BOLD, 22)); lblRoomCount.setForeground(blurple);
        statsPanel.add(createStyledPanel(lblOnline)); 
        statsPanel.add(createStyledPanel(lblRoomCount));
        mainPanel.add(statsPanel, BorderLayout.NORTH);

        // --- 2. TERMINAL LOG AREA ---
        logArea.setEditable(false); 
        logArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        logArea.setBackground(Color.decode("#0D0D0D")); 
        logArea.setForeground(Color.decode("#00FF00"));
        logArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY), "TERMINAL LOGS", 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), textMain
        ));
        mainPanel.add(scrollLog, BorderLayout.CENTER);

        // --- 3. CỘT PHẢI: TABBED PANE (Quản lý) ---
        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.setPreferredSize(new Dimension(300, 0));

        // TAB 1: QUẢN LÝ HOẠT ĐỘNG CHAT
        JPanel chatManagePanel = new JPanel(new GridLayout(2, 1, 10, 15)); 
        chatManagePanel.setOpaque(false);
        
        // Tab 1 -> Quản lý Kênh
        JPanel roomPanel = new JPanel(new BorderLayout(0, 5)); roomPanel.setOpaque(false);
        roomList.setFont(new Font("Segoe UI", Font.PLAIN, 15)); roomList.setBackground(bgPanel); roomList.setForeground(textMain);
        roomPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);
        JPanel roomActionP = new JPanel(new GridLayout(1, 2, 5, 0)); roomActionP.setOpaque(false);
        JButton btnAddRoom = styleButton("Thêm Kênh", blurple); 
        JButton btnDelRoom = styleButton("Xóa Kênh", redDanger);
        roomActionP.add(btnAddRoom); roomActionP.add(btnDelRoom); 
        roomPanel.add(roomActionP, BorderLayout.SOUTH);

        // Tab 1 -> Quản lý Users trong kênh
        JPanel userPanel = new JPanel(new BorderLayout(0, 5)); userPanel.setOpaque(false);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 15)); userList.setBackground(bgPanel); userList.setForeground(textMain);
        userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        JButton btnKick = styleButton("KICK KHỎI KÊNH", yellowWarn); btnKick.setForeground(Color.BLACK);
        userPanel.add(btnKick, BorderLayout.SOUTH);

        chatManagePanel.add(roomPanel); 
        chatManagePanel.add(userPanel);
        rightTabs.addTab("Hoạt Động", chatManagePanel);

        // TAB 2: QUẢN LÝ DATABASE TÀI KHOẢN
        JPanel accountManagePanel = new JPanel(new BorderLayout(0, 10)); 
        accountManagePanel.setOpaque(false);
        accountManagePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        accountList.setFont(new Font("Segoe UI", Font.PLAIN, 15)); accountList.setBackground(bgPanel); accountList.setForeground(textMain);
        accountManagePanel.add(new JScrollPane(accountList), BorderLayout.CENTER);
        
        JPanel accActionP = new JPanel(new GridLayout(3, 1, 5, 5)); accActionP.setOpaque(false);
        JButton btnAddAcc = styleButton("Thêm Tài Khoản", greenSuccess);
        JButton btnEditAcc = styleButton("Đổi Mật Khẩu", blurple);
        JButton btnDelAcc = styleButton("Xóa Tài Khoản", redDanger);
        accActionP.add(btnAddAcc); accActionP.add(btnEditAcc); accActionP.add(btnDelAcc);
        accountManagePanel.add(accActionP, BorderLayout.SOUTH);
        
        rightTabs.addTab("Cơ Sở Dữ Liệu", accountManagePanel);
        
        mainPanel.add(rightTabs, BorderLayout.EAST);
        add(mainPanel);

        // --- SỰ KIỆN GIAO DIỆN ---
        
        // Sự kiện Tab 1:
        roomList.addListSelectionListener(e -> updateRoomUserUI());
        
        btnAddRoom.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Nhập tên kênh:");
            if (name != null && !name.trim().isEmpty()) {
                String safeName = name.trim().toLowerCase().replace(" ", "-");
                DatabaseManager.addRoom(safeName); 
                rooms.put(safeName, new Room(safeName));
                log("[SYSTEM] Đã tạo kênh: #" + safeName); 
                updateRoomListUI();
            }
        });
        
        btnDelRoom.addActionListener(e -> {
            String sel = roomList.getSelectedValue();
            if (sel != null && !sel.equals("general")) {
                DatabaseManager.deleteRoom(sel); 
                Room r = rooms.remove(sel);
                log("[SYSTEM] Đã XÓA kênh: #" + sel);
                for (ClientHandler c : r.clients) c.kickFromRoom();
                updateRoomListUI(); 
                userListModel.clear();
            } else if (sel != null && sel.equals("general")) {
                JOptionPane.showMessageDialog(this, "Không thể xóa kênh mặc định!");
            }
        });
        
        btnKick.addActionListener(e -> {
            String r = roomList.getSelectedValue(), u = userList.getSelectedValue();
            if (r != null && u != null && rooms.containsKey(r)) {
                for (ClientHandler c : rooms.get(r).clients) {
                    if (c.getUsername().equals(u)) { 
                        c.kickFromRoom(); 
                        log("[ADMIN] Đã KICK [" + u + "] khỏi phòng #" + r); 
                        break; 
                    }
                }
            }
        });

        // Sự kiện Tab 2:
        btnAddAcc.addActionListener(e -> {
            String u = JOptionPane.showInputDialog(this, "Tên tài khoản mới:");
            if (u != null && !u.isEmpty()) {
                String p = JOptionPane.showInputDialog(this, "Mật khẩu:");
                if (p != null && !p.isEmpty()) {
                    if (DatabaseManager.registerUser(u, p)) { 
                        log("[DB] Admin đã tạo tài khoản: " + u); 
                        updateAccountListUI(); 
                    } else {
                        JOptionPane.showMessageDialog(this, "Tài khoản đã tồn tại!");
                    }
                }
            }
        });
        
        btnEditAcc.addActionListener(e -> {
            String sel = accountList.getSelectedValue();
            if (sel != null) {
                String p = JOptionPane.showInputDialog(this, "Mật khẩu mới cho tài khoản [" + sel + "]:");
                if (p != null && !p.isEmpty()) { 
                    DatabaseManager.updatePassword(sel, p); 
                    log("[DB] Admin đổi mật khẩu cho tài khoản: " + sel); 
                }
            }
        });
        
        btnDelAcc.addActionListener(e -> {
            String sel = accountList.getSelectedValue();
            if (sel != null && JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn xóa tài khoản [" + sel + "] vĩnh viễn?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                DatabaseManager.deleteUser(sel); 
                log("[DB] Admin ĐÃ XÓA tài khoản: " + sel); 
                updateAccountListUI();
                // Kích user đó ra nếu đang online
                for(ClientHandler c : onlineUsers) { 
                    if(c.getUsername().equals(sel)) c.kickFromRoom(); 
                }
            }
        });
    }

    private JPanel createStyledPanel(JLabel label) {
        JPanel p = new JPanel(new BorderLayout()); 
        p.setBackground(bgPanel);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgDarkest, 2), 
            new EmptyBorder(10, 10, 10, 10)
        ));
        p.add(label, BorderLayout.CENTER); 
        return p;
    }

    private JButton styleButton(String text, Color bg) {
        JButton btn = new JButton(text); 
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg); 
        btn.setForeground(Color.WHITE); 
        btn.setFocusPainted(false); 
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
        btn.setPreferredSize(new Dimension(0, 35)); 
        return btn;
    }

    public void log(String message) { 
        SwingUtilities.invokeLater(() -> { 
            logArea.append(message + "\n"); 
            logArea.setCaretPosition(logArea.getDocument().getLength()); 
        }); 
    }

    public void updateStats() { 
        SwingUtilities.invokeLater(() -> { 
            lblOnline.setText("ONLINE: " + onlineUsers.size()); 
            lblRoomCount.setText("SỐ KÊNH: " + rooms.size()); 
        }); 
    }
    
    public void updateRoomListUI() {
        SwingUtilities.invokeLater(() -> {
            String old = roomList.getSelectedValue(); 
            roomListModel.clear();
            for (String rName : rooms.keySet()) roomListModel.addElement(rName);
            if(old != null && rooms.containsKey(old)) roomList.setSelectedValue(old, true); 
            updateStats();
        });
    }

    public void updateRoomUserUI() {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear(); 
            String sel = roomList.getSelectedValue();
            if (sel != null && rooms.containsKey(sel)) {
                for (ClientHandler c : rooms.get(sel).clients) userListModel.addElement(c.getUsername());
            }
        });
    }

    public void updateAccountListUI() {
        SwingUtilities.invokeLater(() -> { 
            accountListModel.clear(); 
            for(String u : DatabaseManager.getAllUsers()) accountListModel.addElement(u); 
        });
    }

    private void startServerThread() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                log("==========================================");
                log("   MÁY CHỦ SẴN SÀNG HOẠT ĐỘNG");
                log("   Cổng lắng nghe: " + PORT);
                log("   SQL Server: Đã kết nối");
                log("==========================================");
                updateRoomListUI(); 
                updateAccountListUI();
                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientHandler client = new ClientHandler(socket, this);
                    onlineUsers.add(client); 
                    updateStats(); 
                    new Thread(client).start();
                }
            } catch (IOException e) { log("[LỖI KHỞI ĐỘNG] " + e.getMessage()); }
        }).start();
    }

    public static void main(String[] args) {
        try { 
            UIManager.setLookAndFeel(new FlatDarkLaf()); 
            UIManager.put("ScrollBar.showButtons", false);
        } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new ChatServerGUI().setVisible(true));
    }
}

// ================= CLASS ROOM VÀ CLIENT HANDLER =================

class Room {
    String name;
    List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public Room(String name) { this.name = name; }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServerGUI serverGUI;
    private BufferedReader in;
    public PrintWriter out;
    private String username;
    private String currentRoom;

    public ClientHandler(Socket socket, ChatServerGUI serverGUI) { 
        this.socket = socket; 
        this.serverGUI = serverGUI; 
        serverGUI.log("[KẾT NỐI MỚI] Thiết bị kết nối từ: " + socket.getInetAddress().getHostAddress());
    }
    
    public String getUsername() { return username != null ? username : "Khách"; }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String request;
            while ((request = in.readLine()) != null) {
                String[] parts = request.split("\\|");
                switch (parts[0]) {
                    case "REGISTER":
                        if (DatabaseManager.registerUser(parts[1], parts[2])) {
                            out.println("SERVER|Đăng ký thành công! Vui lòng đăng nhập."); 
                            serverGUI.log("[DB] Người dùng vừa đăng ký: " + parts[1]);
                            serverGUI.updateAccountListUI();
                        } else out.println("SERVER|Tài khoản đã tồn tại!");
                        break;
                    case "LOGIN":
                        if (DatabaseManager.checkLogin(parts[1], parts[2])) {
                            this.username = parts[1]; 
                            out.println("LOGIN_SUCCESS|");
                            serverGUI.log("[AUTH] " + username + " đã đăng nhập hệ thống.");
                        } else out.println("SERVER|Sai tài khoản/mật khẩu!");
                        break;
                    case "GET_ROOMS": 
                        out.println("ROOM_LIST|" + String.join(",", ChatServerGUI.rooms.keySet())); 
                        break;
                    case "CREATE_ROOM":
                        String safeName = parts[1].toLowerCase().replace(" ", "-"); 
                        DatabaseManager.addRoom(safeName);
                        if (!ChatServerGUI.rooms.containsKey(safeName)) {
                            ChatServerGUI.rooms.put(safeName, new Room(safeName)); 
                            serverGUI.log("[KÊNH] " + username + " vừa tạo kênh: #" + safeName);
                            serverGUI.updateRoomListUI();
                        }
                        break;
                    case "JOIN_ROOM": 
                        handleJoinRoom(parts[1]); 
                        break;
                    case "CHAT": 
                        // LƯU Ý: Lưu tin nhắn chat thật vào Database
                        broadcastToRoom(username, parts[1], false, true); 
                        break;
                }
            }
        } catch (IOException e) { 
        } finally { cleanup(); }
    }

    private void handleJoinRoom(String roomName) {
        Room targetRoom = ChatServerGUI.rooms.get(roomName);
        if (targetRoom != null) {
            if (currentRoom != null) {
                ChatServerGUI.rooms.get(currentRoom).clients.remove(this);
                // KHÔNG LƯU DB THÔNG BÁO RỜI KÊNH
                broadcastToRoom(username, " đã rời kênh.", true, false);
            }
            currentRoom = roomName; 
            targetRoom.clients.add(this);
            out.println("JOIN_SUCCESS|" + roomName);
            
            // Gửi dữ liệu lịch sử chat cũ
            for (String oldMsg : DatabaseManager.getChatHistory(roomName)) {
                out.println(oldMsg);
            }
            
            // KHÔNG LƯU DB THÔNG BÁO VÀO KÊNH
            broadcastToRoom(username, " vừa trượt vào kênh!", true, false);
            serverGUI.log("[HOẠT ĐỘNG] " + username + " -> vào kênh #" + roomName);
            serverGUI.updateRoomUserUI();
        }
    }

    // Biến saveToDb quyết định việc có lưu tin nhắn này vào SQL hay không
    private void broadcastToRoom(String sender, String message, boolean isSystem, boolean saveToDb) {
        if (currentRoom != null && ChatServerGUI.rooms.containsKey(currentRoom)) {
            Room room = ChatServerGUI.rooms.get(currentRoom);
            if (saveToDb) {
                DatabaseManager.saveMessage(currentRoom, isSystem ? "SYSTEM" : sender, message);
            }
            String protocol = isSystem ? "SYS|" : "MSG|";
            String content = protocol + (isSystem ? message : sender + ": " + message);
            for (ClientHandler client : room.clients) {
                client.out.println(content);
            }
        }
    }

    public void kickFromRoom() {
        if (currentRoom != null) {
            Room room = ChatServerGUI.rooms.get(currentRoom);
            if (room != null) {
                // Xóa người này khỏi danh sách phòng
                room.clients.remove(this);
                // Thông báo cho những người còn lại biết thanh niên này vừa bị sút
                broadcastToRoom(username, " đã bị Admin tiễn khỏi kênh!", true, false);
            }
            currentRoom = null; 
            // Gửi lệnh KICKED về cho Client đó
            if(out != null) out.println("KICKED|"); 
            serverGUI.updateRoomUserUI();
        }
    }

    private void cleanup() {
        ChatServerGUI.onlineUsers.remove(this); 
        serverGUI.updateStats();
        if (username != null) {
            serverGUI.log("[NGẮT KẾT NỐI] " + username + " đã offline.");
        }
        if (currentRoom != null && ChatServerGUI.rooms.containsKey(currentRoom)) {
            ChatServerGUI.rooms.get(currentRoom).clients.remove(this);
            // KHÔNG LƯU DB THÔNG BÁO OFF
            broadcastToRoom(username, " đã ngắt kết nối.", true, false);
            serverGUI.updateRoomUserUI();
        }
        try { socket.close(); } catch (IOException e) {}
    }
}