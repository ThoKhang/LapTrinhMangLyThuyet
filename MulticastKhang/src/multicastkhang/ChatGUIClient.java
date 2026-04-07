package multicastkhang;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class ChatGUIClient extends JFrame {
    private static final int PORT = 5555;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Quản lý chuyển đổi các màn hình
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    // --- CÁC COMPONENT CHÍNH ---
    private JTextField txtIP = new JTextField("127.0.0.1", 15);
    private JTextField txtUser = new JTextField(15);
    private JPasswordField txtPass = new JPasswordField(15);
    private DefaultListModel<String> roomListModel = new DefaultListModel<>();
    private JList<String> roomList = new JList<>(roomListModel);
    private JTextArea chatArea = new JTextArea();
    private JTextField txtMessage = new JTextField();
    private JLabel lblRoomTitle = new JLabel("Tên Phòng", SwingConstants.CENTER);

    // --- MÀU SẮC CHỦ ĐẠO ---
    private Color primaryColor = new Color(52, 152, 219); // Xanh dương
    private Color bgColor = new Color(236, 240, 241); // Xám nhạt

    public ChatGUIClient() {
        setTitle("Ứng Dụng Chat Tốc Độ Cao");
        setSize(450, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        mainPanel.setBackground(bgColor);

        // Thêm các màn hình vào CardLayout
        mainPanel.add(createConnectPanel(), "CONNECT");
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createRoomPanel(), "ROOMS");
        mainPanel.add(createChatPanel(), "CHAT");

        add(mainPanel);
        cardLayout.show(mainPanel, "CONNECT"); // Hiện màn hình nhập IP đầu tiên
    }

    // ================= XÂY DỰNG CÁC MÀN HÌNH =================

    private JPanel createConnectPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(bgColor);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("KẾT NỐI MÁY CHỦ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(primaryColor);
        gbc.gridwidth = 2; p.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1; p.add(new JLabel("Địa chỉ IP:"), gbc);
        gbc.gridx = 1; 
        txtIP.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p.add(txtIP, gbc);

        JButton btnConnect = styleButton("Kết Nối Bắt Đầu", primaryColor);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(btnConnect, gbc);

        btnConnect.addActionListener(e -> {
            String ip = txtIP.getText().trim();
            if(ip.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập IP!"); return;
            }
            try {
                socket = new Socket(ip, PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                new Thread(this::listenToServer).start();
                cardLayout.show(mainPanel, "LOGIN"); // Đổi sang màn hình Login
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể kết nối tới IP: " + ip + "\nVui lòng kiểm tra lại Server.", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
            }
        });
        return p;
    }

    private JPanel createLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(bgColor);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("ĐĂNG NHẬP", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(primaryColor);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; p.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1; p.add(new JLabel("Tài khoản:"), gbc);
        gbc.gridx = 1; txtUser.setFont(new Font("Segoe UI", Font.PLAIN, 14)); p.add(txtUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2; p.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1; txtPass.setFont(new Font("Segoe UI", Font.PLAIN, 14)); p.add(txtPass, gbc);

        JButton btnLogin = styleButton("Đăng nhập", primaryColor);
        JButton btnRegister = styleButton("Đăng ký mới", new Color(46, 204, 113)); // Xanh lá

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(btnLogin); btnPanel.add(btnRegister);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; p.add(btnPanel, gbc);

        btnLogin.addActionListener(e -> out.println("LOGIN|" + txtUser.getText() + "|" + new String(txtPass.getPassword())));
        btnRegister.addActionListener(e -> out.println("REGISTER|" + txtUser.getText() + "|" + new String(txtPass.getPassword())));

        return p;
    }

    private JPanel createRoomPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(bgColor);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("DANH SÁCH PHÒNG CHAT", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18)); title.setForeground(primaryColor);
        p.add(title, BorderLayout.NORTH);

        roomList.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(roomList);
        scroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        p.add(scroll, BorderLayout.CENTER);

        JPanel bottomP = new JPanel(new GridLayout(2, 1, 10, 10));
        bottomP.setOpaque(false);
        JButton btnRefresh = styleButton("Làm mới danh sách", new Color(241, 196, 15)); // Vàng
        JButton btnJoin = styleButton("Vào phòng", primaryColor);
        JButton btnCreate = styleButton("Tạo phòng", new Color(155, 89, 182)); // Tím

        JPanel actionP = new JPanel(new GridLayout(1, 2, 10, 10));
        actionP.setOpaque(false); actionP.add(btnCreate); actionP.add(btnJoin);

        bottomP.add(btnRefresh); bottomP.add(actionP);
        p.add(bottomP, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> out.println("GET_ROOMS"));
        btnJoin.addActionListener(e -> {
            if (roomList.getSelectedValue() != null) out.println("JOIN_ROOM|" + roomList.getSelectedValue());
            else JOptionPane.showMessageDialog(this, "Hãy chọn một phòng!");
        });
        btnCreate.addActionListener(e -> {
            String rName = JOptionPane.showInputDialog(this, "Nhập tên phòng mới:");
            if (rName != null && !rName.trim().isEmpty()) out.println("CREATE_ROOM|" + rName.trim());
        });
        return p;
    }

    private JPanel createChatPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        
        lblRoomTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblRoomTitle.setOpaque(true); lblRoomTitle.setBackground(primaryColor); lblRoomTitle.setForeground(Color.WHITE);
        lblRoomTitle.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JButton btnLeave = styleButton("Rời phòng", new Color(231, 76, 60)); // Đỏ
        JPanel topP = new JPanel(new BorderLayout());
        topP.add(lblRoomTitle, BorderLayout.CENTER); topP.add(btnLeave, BorderLayout.EAST);
        p.add(topP, BorderLayout.NORTH);

        chatArea.setEditable(false); chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        chatArea.setMargin(new Insets(10, 10, 10, 10)); chatArea.setLineWrap(true);
        p.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomP = new JPanel(new BorderLayout(10, 10));
        bottomP.setBackground(bgColor); bottomP.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        txtMessage.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMessage.setPreferredSize(new Dimension(0, 35));
        JButton btnSend = styleButton("GỬI", primaryColor);
        bottomP.add(txtMessage, BorderLayout.CENTER); bottomP.add(btnSend, BorderLayout.EAST);
        p.add(bottomP, BorderLayout.SOUTH);

        ActionListener sendAction = e -> {
            String msg = txtMessage.getText().trim();
            if (!msg.isEmpty()) { out.println("CHAT|" + msg); txtMessage.setText(""); }
        };
        btnSend.addActionListener(sendAction); txtMessage.addActionListener(sendAction);
        btnLeave.addActionListener(e -> {
            out.println("GET_ROOMS");
            cardLayout.show(mainPanel, "ROOMS");
        });
        return p;
    }

    // --- HÀM TIỆN ÍCH LÀM ĐẸP NÚT BẤM ---
    private JButton styleButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(100, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // --- LẮNG NGHE SERVER (Giữ nguyên logic cũ) ---
    private void listenToServer() {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                String[] parts = response.split("\\|");
                String type = parts[0];
                SwingUtilities.invokeLater(() -> {
                    switch (type) {
                        case "SERVER": JOptionPane.showMessageDialog(this, parts[1]); break;
                        case "LOGIN_SUCCESS": cardLayout.show(mainPanel, "ROOMS"); out.println("GET_ROOMS"); break;
                        case "ROOM_LIST":
                            roomListModel.clear();
                            if (parts.length > 1) for (String r : parts[1].split(",")) roomListModel.addElement(r);
                            break;
                        case "JOIN_SUCCESS":
                            lblRoomTitle.setText("PHÒNG: " + parts[1].toUpperCase());
                            chatArea.setText(""); cardLayout.show(mainPanel, "CHAT");
                            break;
                        case "MSG":
                            chatArea.append(parts[1] + "\n");
                            chatArea.setCaretPosition(chatArea.getDocument().getLength());
                            break;
                        case "KICKED":
                            JOptionPane.showMessageDialog(this, "Bạn đã bị Admin kích khỏi phòng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                            out.println("GET_ROOMS");
                            cardLayout.show(mainPanel, "ROOMS");
                            break;
                    }
                });
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Mất kết nối tới Server!");
                cardLayout.show(mainPanel, "CONNECT"); // Quay lại màn hình nhập IP
            });
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ChatGUIClient().setVisible(true));
    }
}