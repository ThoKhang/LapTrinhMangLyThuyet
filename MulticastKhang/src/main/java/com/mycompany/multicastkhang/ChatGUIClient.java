package com.mycompany.multicastkhang;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class ChatGUIClient extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    
    private JTextField txtIP = new JTextField("127.0.0.1"), txtUser = new JTextField();
    private JPasswordField txtPass = new JPasswordField();
    private DefaultListModel<String> roomListModel = new DefaultListModel<>();
    private JList<String> roomList = new JList<>(roomListModel);
    private JTextArea chatArea = new JTextArea();
    private JTextField txtMsg = new JTextField();
    private JLabel lblTitle = new JLabel("CHƯA CHỌN KÊNH", SwingConstants.LEFT);

    // --- BẢNG MÀU CHUẨN DISCORD ---
    private Color bgDarkest = Color.decode("#1E1F22");  
    private Color bgSidebar = Color.decode("#2B2D31");  
    private Color bgChat = Color.decode("#313338");     
    private Color bgInput = Color.decode("#383A40");    
    private Color textMain = Color.decode("#DBDEE1");   
    private Color textMuted = Color.decode("#949BA4");  
    private Color blurple = Color.decode("#5865F2");    
    private Color redDanger = Color.decode("#ED4245");

    public ChatGUIClient() {
        setTitle("Discord Clone"); 
        setSize(850, 600); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null);
        
        mainPanel.add(createConnectPanel(), "CONNECT");
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createMainChatUI(), "CHAT_UI"); 
        
        add(mainPanel);
    }

    private JPanel createConnectPanel() {
        JPanel p = new JPanel(new GridBagLayout()); 
        p.setBackground(bgDarkest);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbl = new JLabel("KẾT NỐI MÁY CHỦ", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22)); lbl.setForeground(Color.WHITE);
        gbc.gridwidth = 2; p.add(lbl, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1; 
        JLabel labelIP = new JLabel("IP MÁY CHỦ"); labelIP.setForeground(textMuted); labelIP.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(labelIP, gbc);
        
        gbc.gridy = 2; txtIP.setBackground(bgInput); txtIP.setForeground(textMain); txtIP.setBorder(new EmptyBorder(8, 10, 8, 10));
        p.add(txtIP, gbc);

        gbc.gridy = 3; JButton btn = styleButton("Kết Nối Bắt Đầu", blurple);
        btn.setPreferredSize(new Dimension(250, 40));
        btn.addActionListener(e -> {
            try {
                socket = new Socket(txtIP.getText(), 5555);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                new Thread(this::listen).start();
                cardLayout.show(mainPanel, "LOGIN");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Lỗi kết nối!"); }
        });
        p.add(btn, gbc);
        return p;
    }

    private JPanel createLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(bgDarkest);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10); gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbl = new JLabel("ĐĂNG NHẬP", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22)); lbl.setForeground(Color.WHITE);
        gbc.gridy = 0; p.add(lbl, gbc);

        JLabel lblUser = new JLabel("TÀI KHOẢN"); lblUser.setForeground(textMuted); lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridy = 1; p.add(lblUser, gbc);
        
        txtUser.setBackground(bgInput); txtUser.setForeground(textMain); txtUser.setBorder(new EmptyBorder(8, 10, 8, 10));
        gbc.gridy = 2; p.add(txtUser, gbc);

        JLabel lblPass = new JLabel("MẬT KHẨU"); lblPass.setForeground(textMuted); lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridy = 3; p.add(lblPass, gbc);

        txtPass.setBackground(bgInput); txtPass.setForeground(textMain); txtPass.setBorder(new EmptyBorder(8, 10, 8, 10));
        gbc.gridy = 4; p.add(txtPass, gbc);

        JButton btnL = styleButton("Đăng Nhập", blurple);
        btnL.setPreferredSize(new Dimension(250, 40));
        btnL.addActionListener(e -> out.println("LOGIN|" + txtUser.getText() + "|" + new String(txtPass.getPassword())));
        gbc.gridy = 5; p.add(btnL, gbc);

        JButton btnR = styleButton("Đăng Ký Mới", new Color(45, 47, 51));
        btnR.setPreferredSize(new Dimension(250, 40));
        btnR.addActionListener(e -> out.println("REGISTER|" + txtUser.getText() + "|" + new String(txtPass.getPassword())));
        gbc.gridy = 6; p.add(btnR, gbc);
        return p;
    }

    private JPanel createMainChatUI() {
        JPanel p = new JPanel(new BorderLayout());
        
        // ================= SIDEBAR (Bên trái) =================
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(bgSidebar);
        sidebar.setPreferredSize(new Dimension(230, 0));
        
        // Header Sidebar
        JPanel sidebarHeader = new JPanel(new BorderLayout());
        sidebarHeader.setOpaque(false);
        sidebarHeader.setBorder(new EmptyBorder(15, 15, 5, 15));
        JLabel sTitle = new JLabel("DANH SÁCH KÊNH");
        sTitle.setFont(new Font("Segoe UI", Font.BOLD, 12)); sTitle.setForeground(textMuted);
        sidebarHeader.add(sTitle, BorderLayout.CENTER);
        sidebar.add(sidebarHeader, BorderLayout.NORTH);

        // Danh sách phòng
        roomList.setBackground(bgSidebar); roomList.setForeground(textMuted);
        roomList.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roomList.setSelectionBackground(bgInput); roomList.setSelectionForeground(Color.WHITE);
        roomList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText("  # " + value.toString());
                label.setBorder(new EmptyBorder(8, 5, 8, 5));
                return label;
            }
        });
        
        // Click để vào phòng
        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && roomList.getSelectedValue() != null) {
                out.println("JOIN_ROOM|" + roomList.getSelectedValue());
            }
        });
        
        JScrollPane scrollRooms = new JScrollPane(roomList);
        scrollRooms.setBorder(null); 
        sidebar.add(scrollRooms, BorderLayout.CENTER);

        // THÊM LẠI: Nút Tạo Phòng & Làm Mới ở dưới cùng Sidebar
        JPanel sidebarBottom = new JPanel(new GridLayout(2, 1, 5, 5));
        sidebarBottom.setOpaque(false);
        sidebarBottom.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JButton btnCreate = styleButton("Tạo Phòng Mới", blurple);
        JButton btnRefresh = styleButton("Làm Mới Danh Sách", bgInput);
        
        btnCreate.addActionListener(e -> {
            String rName = JOptionPane.showInputDialog(this, "Nhập tên phòng mới:");
            if (rName != null && !rName.trim().isEmpty()) {
                out.println("CREATE_ROOM|" + rName.trim().toLowerCase().replace(" ", "-"));
            }
        });
        btnRefresh.addActionListener(e -> out.println("GET_ROOMS"));
        
        sidebarBottom.add(btnCreate);
        sidebarBottom.add(btnRefresh);
        sidebar.add(sidebarBottom, BorderLayout.SOUTH);

        // ================= KHU VỰC CHAT (Bên phải) =================
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(bgChat);

        // THÊM LẠI: Header có nút Rời Phòng
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(bgChat);
        chatHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, bgDarkest), 
            new EmptyBorder(10, 15, 10, 15)
        ));
        
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18)); lblTitle.setForeground(Color.WHITE);
        JButton btnLeave = styleButton("Rời Phòng", redDanger);
        btnLeave.setPreferredSize(new Dimension(100, 35));
        
        btnLeave.addActionListener(e -> {
            lblTitle.setText("CHƯA CHỌN KÊNH");
            chatArea.setText("");
            roomList.clearSelection(); // Bỏ chọn highlight bên danh sách
            out.println("GET_ROOMS");
        });
        
        chatHeader.add(lblTitle, BorderLayout.CENTER);
        chatHeader.add(btnLeave, BorderLayout.EAST);
        chatPanel.add(chatHeader, BorderLayout.NORTH);

        // Vùng chat
        chatArea.setEditable(false); chatArea.setBackground(bgChat); chatArea.setForeground(textMain);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 15)); chatArea.setMargin(new Insets(10, 15, 10, 15));
        chatArea.setLineWrap(true);
        JScrollPane scrollChat = new JScrollPane(chatArea);
        scrollChat.setBorder(null);
        chatPanel.add(scrollChat, BorderLayout.CENTER);

        // THÊM LẠI: Footer có ô nhập và Nút Gửi
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(bgChat); 
        inputPanel.setBorder(new EmptyBorder(15, 20, 20, 20));
        
        txtMsg.setBackground(bgInput); txtMsg.setForeground(textMain); txtMsg.setCaretColor(Color.WHITE);
        txtMsg.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtMsg.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgInput, 1), 
            new EmptyBorder(8, 10, 8, 10)
        ));
        
        JButton btnSend = styleButton("GỬI", blurple);
        btnSend.setPreferredSize(new Dimension(90, 45));
        
        ActionListener sendAction = e -> { 
            if(!txtMsg.getText().isEmpty()) { out.println("CHAT|" + txtMsg.getText()); txtMsg.setText(""); }
        };
        txtMsg.addActionListener(sendAction); // Ấn Enter vẫn gửi được
        btnSend.addActionListener(sendAction); // Hoặc ấn nút Gửi
        
        inputPanel.add(txtMsg, BorderLayout.CENTER);
        inputPanel.add(btnSend, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        p.add(sidebar, BorderLayout.WEST);
        p.add(chatPanel, BorderLayout.CENTER);
        return p;
    }

    private JButton styleButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void listen() {
        try {
            String res;
            while ((res = in.readLine()) != null) {
                String[] p = res.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    switch (p[0]) {
                        case "SERVER": JOptionPane.showMessageDialog(this, p[1]); break;
                        case "LOGIN_SUCCESS": cardLayout.show(mainPanel, "CHAT_UI"); out.println("GET_ROOMS"); break;
                        case "ROOM_LIST": 
                            roomListModel.clear(); 
                            if (p.length > 1) {
                                for (String r : p[1].split(",")) roomListModel.addElement(r); 
                            }
                            break;
                        case "JOIN_SUCCESS": 
                            lblTitle.setText("# " + p[1]); 
                            chatArea.setText(""); 
                            break;
                        case "MSG": 
                            chatArea.append(p[1] + "\n"); 
                            chatArea.setCaretPosition(chatArea.getDocument().getLength()); // Auto cuộn xuống
                            break;
                        case "SYS": 
                            chatArea.append(" — " + p[1] + " —\n"); 
                            chatArea.setCaretPosition(chatArea.getDocument().getLength());
                            break;
                    }
                });
            }
        } catch (IOException e) { }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        } catch (Exception ex) {
            System.err.println("Lỗi giao diện FlatLaf");
        }
        SwingUtilities.invokeLater(() -> new ChatGUIClient().setVisible(true));
    }
}