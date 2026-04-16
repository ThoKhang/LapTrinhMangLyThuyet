package multicastkhang;

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

    private JTextArea logArea = new JTextArea();
    private JLabel lblOnline = new JLabel("ONLINE: 0", SwingConstants.CENTER);
    private JLabel lblRoomCount = new JLabel("SỐ PHÒNG: 0", SwingConstants.CENTER);
    
    private DefaultListModel<String> roomListModel = new DefaultListModel<>();
    private JList<String> roomList = new JList<>(roomListModel);
    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(userListModel);

    // Dark Theme Server
    private Color bgDark = new Color(32, 34, 37);
    private Color bgPanel = new Color(47, 49, 54);
    private Color textMain = new Color(220, 221, 222);

    public ChatServerGUI() {
        setTitle("MÁY CHỦ TRUNG TÂM (ADMIN DASHBOARD)");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // KHỞI TẠO DATABASE VÀ LOAD PHÒNG
        DatabaseManager.init();
        List<String> dbRooms = DatabaseManager.getAllRooms();
        for (String rName : dbRooms) {
            rooms.put(rName, new Room(rName));
        }

        initUI();
        startServerThread();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(bgDark);

        // Header
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        statsPanel.setOpaque(false);
        lblOnline.setFont(new Font("Segoe UI", Font.BOLD, 20)); lblOnline.setForeground(new Color(59, 165, 92));
        lblRoomCount.setFont(new Font("Segoe UI", Font.BOLD, 20)); lblRoomCount.setForeground(new Color(88, 101, 242));
        statsPanel.add(createStyledPanel(lblOnline)); statsPanel.add(createStyledPanel(lblRoomCount));
        mainPanel.add(statsPanel, BorderLayout.NORTH);

        // Log Area
        logArea.setEditable(false); logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        logArea.setBackground(new Color(14, 14, 14)); logArea.setForeground(new Color(59, 165, 92));
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "Terminal Logs", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), textMain));
        mainPanel.add(scrollLog, BorderLayout.CENTER);

        // Right Panel
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 10, 15)); rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(280, 0));

        // Room Panel
        JPanel roomPanel = new JPanel(new BorderLayout(0, 5)); roomPanel.setOpaque(false);
        roomPanel.setBorder(BorderFactory.createTitledBorder(null, "Quản Lý Kênh", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Segoe UI", Font.BOLD, 14), textMain));
        roomList.setFont(new Font("Segoe UI", Font.PLAIN, 14)); roomList.setBackground(bgPanel); roomList.setForeground(textMain);
        roomPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);
        
        JPanel roomActionP = new JPanel(new GridLayout(1, 2, 5, 0)); roomActionP.setOpaque(false);
        JButton btnAddRoom = new JButton("Thêm Mới"); btnAddRoom.setBackground(new Color(88, 101, 242)); btnAddRoom.setForeground(Color.WHITE);
        JButton btnDelRoom = new JButton("Xóa Kênh"); btnDelRoom.setBackground(new Color(237, 66, 69)); btnDelRoom.setForeground(Color.WHITE);
        roomActionP.add(btnAddRoom); roomActionP.add(btnDelRoom);
        roomPanel.add(roomActionP, BorderLayout.SOUTH);

        // User Panel
        JPanel userPanel = new JPanel(new BorderLayout(0, 5)); userPanel.setOpaque(false);
        userPanel.setBorder(BorderFactory.createTitledBorder(null, "Thành viên Online", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Segoe UI", Font.BOLD, 14), textMain));
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14)); userList.setBackground(bgPanel); userList.setForeground(textMain);
        userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        
        JButton btnKick = new JButton("KICK NGƯỜI DÙNG");
        btnKick.setBackground(new Color(250, 166, 26)); btnKick.setForeground(Color.WHITE);
        userPanel.add(btnKick, BorderLayout.SOUTH);

        rightPanel.add(roomPanel); rightPanel.add(userPanel);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        add(mainPanel);

        // Events
        roomList.addListSelectionListener(e -> updateRoomUserUI());

        btnAddRoom.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Nhập tên kênh mới:");
            if (name != null && !name.trim().isEmpty() && !rooms.containsKey(name)) {
                String safeName = name.trim().toLowerCase().replace(" ", "-");
                DatabaseManager.addRoom(safeName); // Lưu DB
                rooms.put(safeName, new Room(safeName));
                log("[SYSTEM] Đã tạo kênh: " + safeName);
                updateRoomListUI();
            }
        });

        btnDelRoom.addActionListener(e -> {
            String selected = roomList.getSelectedValue();
            if (selected != null) {
                if (selected.equals("Lobby") || selected.equals("general")) { JOptionPane.showMessageDialog(this, "Không thể xóa kênh mặc định!"); return; }
                DatabaseManager.deleteRoom(selected); // Xóa DB
                Room room = rooms.remove(selected);
                log("[SYSTEM] Đã XÓA kênh: " + selected);
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
                            log("[ADMIN] Đã kích [" + selectedUser + "] khỏi [" + selectedRoom + "]");
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
            BorderFactory.createLineBorder(new Color(32, 34, 37), 2),
            new EmptyBorder(10, 10, 10, 10)
        ));
        p.add(label, BorderLayout.CENTER);
        return p;
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
                log("=== SERVER KHỞI ĐỘNG THÀNH CÔNG ===");
                log("Cổng: " + PORT + " | CSDL: Đã kết nối");
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
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new ChatServerGUI().setVisible(true));
    }
}

// ================= CLASS ROOM & CLIENT HANDLER =================

class Room {
    String name;
    List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    List<String> history = new CopyOnWriteArrayList<>();
    public Room(String name) { this.name = name; }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServerGUI serverGUI;
    private BufferedReader in;
    public PrintWriter out;
    private String username;
    private String currentRoom;

    public ClientHandler(Socket socket, ChatServerGUI serverGUI) { this.socket = socket; this.serverGUI = serverGUI; }
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
                        } else {
                            out.println("SERVER|Tài khoản đã tồn tại!");
                        }
                        break;
                    case "LOGIN":
                        if (DatabaseManager.checkLogin(parts[1], parts[2])) {
                            this.username = parts[1]; 
                            out.println("LOGIN_SUCCESS|");
                            serverGUI.log("[AUTH] " + username + " đăng nhập thành công.");
                        } else {
                            out.println("SERVER|Sai tài khoản hoặc mật khẩu!");
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
                            serverGUI.log("[USER] " + username + " tạo kênh: " + safeName);
                            serverGUI.updateRoomListUI();
                        }
                        break;
                    case "JOIN_ROOM": handleJoinRoom(parts[1]); break;
                    case "CHAT": broadcastToRoom(username + ": " + parts[1], true); break;
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
                broadcastToRoom("SERVER|" + username + " đã rời kênh.", false);
            }
            currentRoom = roomName; targetRoom.clients.add(this);
            out.println("JOIN_SUCCESS|" + roomName);
            for (String oldMsg : targetRoom.history) out.println("MSG|" + oldMsg);
            broadcastToRoom("SERVER|" + username + " vừa trượt vào kênh!", false);
            serverGUI.updateRoomUserUI();
        }
    }

    private void broadcastToRoom(String message, boolean saveToHistory) {
        if (currentRoom != null && ChatServerGUI.rooms.containsKey(currentRoom)) {
            Room room = ChatServerGUI.rooms.get(currentRoom);
            if (saveToHistory) room.history.add(message);
            for (ClientHandler client : room.clients) client.out.println("MSG|" + message);
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
        if (username != null) serverGUI.log("[NGẮT KẾT NỐI] " + username + " đã offline.");
        if (currentRoom != null && ChatServerGUI.rooms.containsKey(currentRoom)) {
            ChatServerGUI.rooms.get(currentRoom).clients.remove(this);
            broadcastToRoom("SERVER|" + username + " đã ngắt kết nối.", false);
            serverGUI.updateRoomUserUI();
        }
        try { socket.close(); } catch (IOException e) {}
    }
}