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

    // --- COMPONENT GIAO DIỆN ---
    private JTextArea logArea = new JTextArea();
    private JLabel lblOnline = new JLabel("ONLINE: 0", SwingConstants.CENTER);
    private JLabel lblRoomCount = new JLabel("SỐ KÊNH: 0", SwingConstants.CENTER);
    
    private DefaultListModel<String> roomListModel = new DefaultListModel<>();
    private JList<String> roomList = new JList<>(roomListModel);
    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(userListModel);

    // --- MÀU SẮC THEME TỐI ---
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

        // KHỞI TẠO DATABASE
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

        // 1. HEADER - THỐNG KÊ (Top)
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        statsPanel.setOpaque(false);
        lblOnline.setFont(new Font("Segoe UI", Font.BOLD, 22)); lblOnline.setForeground(greenSuccess);
        lblRoomCount.setFont(new Font("Segoe UI", Font.BOLD, 22)); lblRoomCount.setForeground(blurple);
        
        statsPanel.add(createStyledPanel(lblOnline)); 
        statsPanel.add(createStyledPanel(lblRoomCount));
        mainPanel.add(statsPanel, BorderLayout.NORTH);

        // 2. NHẬT KÝ LOGS (Center)
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        logArea.setBackground(Color.decode("#0D0D0D")); // Đen tuyền cho Terminal
        logArea.setForeground(Color.decode("#00FF00")); // Chữ xanh lá hacker
        logArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY), "TERMINAL LOGS", 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), textMain
        ));
        mainPanel.add(scrollLog, BorderLayout.CENTER);

        // 3. KHU VỰC QUẢN LÝ (Right)
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 10, 15));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(300, 0));

        // 3.1. Quản lý phòng
        JPanel roomPanel = new JPanel(new BorderLayout(0, 5));
        roomPanel.setOpaque(false);
        roomPanel.setBorder(BorderFactory.createTitledBorder(null, "QUẢN LÝ KÊNH", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 13), textMain));
        
        roomList.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        roomList.setBackground(bgPanel); roomList.setForeground(textMain);
        roomPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);
        
        JPanel roomActionP = new JPanel(new GridLayout(1, 2, 5, 0));
        roomActionP.setOpaque(false);
        JButton btnAddRoom = styleButton("Thêm Kênh", blurple);
        JButton btnDelRoom = styleButton("Xóa Kênh", redDanger);
        roomActionP.add(btnAddRoom); roomActionP.add(btnDelRoom);
        roomPanel.add(roomActionP, BorderLayout.SOUTH);

        // 3.2. Quản lý Users
        JPanel userPanel = new JPanel(new BorderLayout(0, 5));
        userPanel.setOpaque(false);
        userPanel.setBorder(BorderFactory.createTitledBorder(null, "NGƯỜI DÙNG TRONG KÊNH", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 13), textMain));
        
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        userList.setBackground(bgPanel); userList.setForeground(textMain);
        userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        
        JButton btnKick = styleButton("KICK KHỎI KÊNH", yellowWarn);
        btnKick.setForeground(Color.BLACK); // Chữ đen trên nền vàng cho dễ đọc
        userPanel.add(btnKick, BorderLayout.SOUTH);

        rightPanel.add(roomPanel);
        rightPanel.add(userPanel);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        add(mainPanel);

        // --- SỰ KIỆN GIAO DIỆN KHÔI PHỤC ---
        roomList.addListSelectionListener(e -> updateRoomUserUI());

        btnAddRoom.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Nhập tên kênh mới:");
            if (name != null && !name.trim().isEmpty() && !rooms.containsKey(name)) {
                String safeName = name.trim().toLowerCase().replace(" ", "-");
                DatabaseManager.addRoom(safeName); 
                rooms.put(safeName, new Room(safeName));
                log("[SYSTEM] Admin đã tạo kênh thủ công: #" + safeName);
                updateRoomListUI();
            }
        });

        btnDelRoom.addActionListener(e -> {
            String selected = roomList.getSelectedValue();
            if (selected != null) {
                if (selected.equals("general") || selected.equals("lobby")) { 
                    JOptionPane.showMessageDialog(this, "Không thể xóa kênh mặc định!"); return; 
                }
                DatabaseManager.deleteRoom(selected); 
                Room room = rooms.remove(selected);
                log("[SYSTEM] Admin đã XÓA kênh: #" + selected);
                for (ClientHandler client : room.clients) client.kickFromRoom();
                updateRoomListUI();
                userListModel.clear();
            }
        });

        btnKick.addActionListener(e -> {
            String selectedRoom = roomList.getSelectedValue();
            String selectedUser = userList.getSelectedValue();
            if (selectedRoom != null && selectedUser != null) {
                Room room = rooms.get(selectedRoom);
                if (room != null) {
                    for (ClientHandler client : room.clients) {
                        if (client.getUsername().equals(selectedUser)) {
                            client.kickFromRoom();
                            log("[ADMIN] Đã KICK [" + selectedUser + "] ra khỏi #" + selectedRoom);
                            break;
                        }
                    }
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
        btn.setBackground(bg); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 35));
        return btn;
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // Auto cuộn log
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
            String oldSelect = roomList.getSelectedValue();
            roomListModel.clear();
            for (String rName : rooms.keySet()) roomListModel.addElement(rName);
            if(oldSelect != null && rooms.containsKey(oldSelect)) roomList.setSelectedValue(oldSelect, true);
            updateStats();
        });
    }

    public void updateRoomUserUI() {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            String selectedRoom = roomList.getSelectedValue();
            if (selectedRoom != null && rooms.containsKey(selectedRoom)) {
                for (ClientHandler c : rooms.get(selectedRoom).clients) {
                    userListModel.addElement(c.getUsername());
                }
            }
        });
    }

    private void startServerThread() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                log("==========================================");
                log("   SERVER KHỞI ĐỘNG THÀNH CÔNG");
                log("   Cổng: " + PORT + " | SQL Server: Connected");
                log("==========================================");
                updateRoomListUI();
                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientHandler client = new ClientHandler(socket, this);
                    onlineUsers.add(client);
                    updateStats();
                    new Thread(client).start();
                }
            } catch (IOException e) { log("[LỖI SERVER] " + e.getMessage()); }
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

// ================= CLASS ROOM & CLIENT HANDLER LẤY LẠI LOG =================

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
        serverGUI.log("[KẾT NỐI] Có thiết bị mới vừa kết nối tới Server: " + socket.getInetAddress().getHostAddress());
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
                String command = parts[0];

                switch (command) {
                    case "REGISTER":
                        if (DatabaseManager.registerUser(parts[1], parts[2])) {
                            out.println("SERVER|Đăng ký thành công! Vui lòng đăng nhập.");
                            serverGUI.log("[DB] " + parts[1] + " vừa đăng ký tài khoản thành công.");
                        } else {
                            out.println("SERVER|Tài khoản đã tồn tại!");
                            serverGUI.log("[CẢNH BÁO] " + parts[1] + " đăng ký thất bại (Trùng tài khoản).");
                        }
                        break;
                    case "LOGIN":
                        if (DatabaseManager.checkLogin(parts[1], parts[2])) {
                            this.username = parts[1]; 
                            out.println("LOGIN_SUCCESS|");
                            serverGUI.log("[AUTH] " + username + " đã đăng nhập vào hệ thống.");
                        } else {
                            out.println("SERVER|Sai tài khoản hoặc mật khẩu!");
                            serverGUI.log("[AUTH] Có người đăng nhập sai vào tài khoản: " + parts[1]);
                        }
                        break;
                    case "GET_ROOMS": 
                        out.println("ROOM_LIST|" + String.join(",", ChatServerGUI.rooms.keySet())); 
                        break;
                    case "CREATE_ROOM":
                        String safeName = parts[1].toLowerCase().replace(" ", "-");
                        DatabaseManager.addRoom(safeName);
                        if (!ChatServerGUI.rooms.containsKey(safeName)) {
                            ChatServerGUI.rooms.put(safeName, new Room(safeName));
                            serverGUI.log("[KÊNH] " + username + " vừa tạo kênh mới: #" + safeName);
                            serverGUI.updateRoomListUI();
                        }
                        break;
                    case "JOIN_ROOM": handleJoinRoom(parts[1]); break;
                    case "CHAT": broadcastToRoom(username, parts[1], false); break;
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
                broadcastToRoom(username, " đã rời kênh.", true);
            }
            currentRoom = roomName; targetRoom.clients.add(this);
            out.println("JOIN_SUCCESS|" + roomName);
            
            // Gửi lịch sử
            for (String oldMsg : DatabaseManager.getChatHistory(roomName)) out.println(oldMsg);
            
            broadcastToRoom(username, " vừa trượt vào kênh!", true);
            serverGUI.log("[HOẠT ĐỘNG] " + username + " đã vào kênh #" + roomName);
            serverGUI.updateRoomUserUI();
        }
    }

    private void broadcastToRoom(String sender, String message, boolean isSystem) {
        if (currentRoom != null && ChatServerGUI.rooms.containsKey(currentRoom)) {
            Room room = ChatServerGUI.rooms.get(currentRoom);
            DatabaseManager.saveMessage(currentRoom, isSystem ? "SYSTEM" : sender, message);
            String protocol = isSystem ? "SYS|" : "MSG|";
            String content = protocol + (isSystem ? message : sender + ": " + message);
            for (ClientHandler client : room.clients) client.out.println(content);
        }
    }

    public void kickFromRoom() {
        if (currentRoom != null) {
            Room room = ChatServerGUI.rooms.get(currentRoom);
            if (room != null) room.clients.remove(this);
            currentRoom = null;
            if(out != null) out.println("KICKED|");
            serverGUI.updateRoomUserUI();
        }
    }

    private void cleanup() {
        ChatServerGUI.onlineUsers.remove(this); serverGUI.updateStats();
        if (username != null) {
            serverGUI.log("[NGẮT KẾT NỐI] " + username + " đã offline.");
        }
        if (currentRoom != null && ChatServerGUI.rooms.containsKey(currentRoom)) {
            ChatServerGUI.rooms.get(currentRoom).clients.remove(this);
            broadcastToRoom(username, " đã ngắt kết nối.", true);
            serverGUI.updateRoomUserUI();
        }
        try { socket.close(); } catch (IOException e) {}
    }
}