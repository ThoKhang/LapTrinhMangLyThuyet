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

    private Color bgDarkest = Color.decode("#1E1F22"), bgSidebar = Color.decode("#2B2D31");  
    private Color bgChat = Color.decode("#313338"), bgInput = Color.decode("#383A40");    
    private Color textMain = Color.decode("#DBDEE1"), textMuted = Color.decode("#949BA4");  
    private Color blurple = Color.decode("#5865F2"), redDanger = Color.decode("#ED4245");

    public ChatGUIClient() {
        setTitle("Discord Clone"); setSize(850, 600); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setLocationRelativeTo(null);
        
        mainPanel.add(createConnectPanel(), "CONNECT");
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createMainChatUI(), "CHAT_UI"); 
        add(mainPanel);
    }

    private JPanel createConnectPanel() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(bgDarkest);
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(10, 10, 10, 10); gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbl = new JLabel("KẾT NỐI MÁY CHỦ", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22)); lbl.setForeground(Color.WHITE); p.add(lbl, gbc);

        gbc.gridy = 1; JLabel labelIP = new JLabel("IP MÁY CHỦ"); labelIP.setForeground(textMuted); p.add(labelIP, gbc);
        gbc.gridy = 2; txtIP.setBackground(bgInput); txtIP.setForeground(textMain); txtIP.setBorder(new EmptyBorder(8, 10, 8, 10)); p.add(txtIP, gbc);

        gbc.gridy = 3; JButton btn = styleButton("Kết Nối Bắt Đầu", blurple);
        btn.addActionListener(e -> {
            try {
                socket = new Socket(txtIP.getText(), 5555);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                new Thread(this::listen).start(); cardLayout.show(mainPanel, "LOGIN");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Lỗi kết nối!"); }
        });
        p.add(btn, gbc); return p;
    }

    private JPanel createLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(bgDarkest);
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(8, 10, 8, 10); gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbl = new JLabel("ĐĂNG NHẬP", SwingConstants.CENTER); lbl.setFont(new Font("Segoe UI", Font.BOLD, 22)); lbl.setForeground(Color.WHITE); p.add(lbl, gbc);
        gbc.gridy = 1; JLabel lblUser = new JLabel("TÀI KHOẢN"); lblUser.setForeground(textMuted); p.add(lblUser, gbc);
        gbc.gridy = 2; txtUser.setBackground(bgInput); txtUser.setForeground(textMain); txtUser.setBorder(new EmptyBorder(8, 10, 8, 10)); p.add(txtUser, gbc);
        gbc.gridy = 3; JLabel lblPass = new JLabel("MẬT KHẨU"); lblPass.setForeground(textMuted); p.add(lblPass, gbc);
        gbc.gridy = 4; txtPass.setBackground(bgInput); txtPass.setForeground(textMain); txtPass.setBorder(new EmptyBorder(8, 10, 8, 10)); p.add(txtPass, gbc);

        JButton btnL = styleButton("Đăng Nhập", blurple);
        btnL.addActionListener(e -> out.println("LOGIN|" + txtUser.getText() + "|" + new String(txtPass.getPassword())));
        gbc.gridy = 5; p.add(btnL, gbc);

        JButton btnR = styleButton("Đăng Ký Mới", new Color(45, 47, 51));
        btnR.addActionListener(e -> out.println("REGISTER|" + txtUser.getText() + "|" + new String(txtPass.getPassword())));
        gbc.gridy = 6; p.add(btnR, gbc); return p;
    }

    private JPanel createMainChatUI() {
        JPanel p = new JPanel(new BorderLayout());
        
        // --- SIDEBAR ---
        JPanel sidebar = new JPanel(new BorderLayout()); sidebar.setBackground(bgSidebar); sidebar.setPreferredSize(new Dimension(230, 0));
        JPanel sidebarHeader = new JPanel(new BorderLayout()); sidebarHeader.setOpaque(false); sidebarHeader.setBorder(new EmptyBorder(15, 15, 5, 15));
        JLabel sTitle = new JLabel("DANH SÁCH KÊNH"); sTitle.setFont(new Font("Segoe UI", Font.BOLD, 12)); sTitle.setForeground(textMuted);
        sidebarHeader.add(sTitle, BorderLayout.CENTER); sidebar.add(sidebarHeader, BorderLayout.NORTH);

        roomList.setBackground(bgSidebar); roomList.setForeground(textMuted); roomList.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roomList.setSelectionBackground(bgInput); roomList.setSelectionForeground(Color.WHITE);
        roomList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText("  # " + value.toString()); label.setBorder(new EmptyBorder(8, 5, 8, 5)); return label;
            }
        });
        roomList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && roomList.getSelectedValue() != null) out.println("JOIN_ROOM|" + roomList.getSelectedValue()); });
        JScrollPane scrollRooms = new JScrollPane(roomList); scrollRooms.setBorder(null); sidebar.add(scrollRooms, BorderLayout.CENTER);

        JPanel sidebarBottom = new JPanel(new GridLayout(2, 1, 5, 5)); sidebarBottom.setOpaque(false); sidebarBottom.setBorder(new EmptyBorder(10, 10, 10, 10));
        JButton btnCreate = styleButton("Tạo Phòng Mới", blurple); JButton btnRefresh = styleButton("Làm Mới Danh Sách", bgInput);
        btnCreate.addActionListener(e -> {
            String rName = JOptionPane.showInputDialog(this, "Nhập tên phòng mới:");
            if (rName != null && !rName.trim().isEmpty()) out.println("CREATE_ROOM|" + rName.trim().toLowerCase().replace(" ", "-"));
        });
        btnRefresh.addActionListener(e -> out.println("GET_ROOMS"));
        sidebarBottom.add(btnCreate); sidebarBottom.add(btnRefresh); sidebar.add(sidebarBottom, BorderLayout.SOUTH);

        // --- KHU VỰC CHAT ---
        JPanel chatPanel = new JPanel(new BorderLayout()); chatPanel.setBackground(bgChat);
        JPanel chatHeader = new JPanel(new BorderLayout()); chatHeader.setBackground(bgChat);
        chatHeader.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, bgDarkest), new EmptyBorder(10, 15, 10, 15)));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18)); lblTitle.setForeground(Color.WHITE);
        JButton btnLeave = styleButton("Rời Phòng", redDanger); btnLeave.setPreferredSize(new Dimension(100, 35));
        btnLeave.addActionListener(e -> { lblTitle.setText("CHƯA CHỌN KÊNH"); chatArea.setText(""); roomList.clearSelection(); out.println("GET_ROOMS"); });
        chatHeader.add(lblTitle, BorderLayout.CENTER); chatHeader.add(btnLeave, BorderLayout.EAST); chatPanel.add(chatHeader, BorderLayout.NORTH);

        chatArea.setEditable(false); chatArea.setBackground(bgChat); chatArea.setForeground(textMain);
        chatArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16)); // FONT HỖ TRỢ EMOJI
        chatArea.setMargin(new Insets(10, 15, 10, 15)); chatArea.setLineWrap(true);
        JScrollPane scrollChat = new JScrollPane(chatArea); scrollChat.setBorder(null); chatPanel.add(scrollChat, BorderLayout.CENTER);

        // --- FOOTER CHỨA EMOJI BỞI ---
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0)); inputPanel.setBackground(bgChat); inputPanel.setBorder(new EmptyBorder(15, 20, 20, 20));
        txtMsg.setBackground(bgInput); txtMsg.setForeground(textMain); txtMsg.setCaretColor(Color.WHITE);
        txtMsg.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15)); // FONT HỖ TRỢ EMOJI
        txtMsg.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bgInput, 1), new EmptyBorder(8, 10, 8, 10)));
        
        // Nút EMOJI
        JButton btnEmoji = styleButton("😀", bgInput); btnEmoji.setPreferredSize(new Dimension(50, 45));
        JPopupMenu emojiMenu = new JPopupMenu(); emojiMenu.setBackground(bgChat);
        JPanel emojiBox = new JPanel(new GridLayout(2, 5, 2, 2)); emojiBox.setBackground(bgChat);
        String[] emojis = {"😀", "😂", "🥰", "😎", "😭", "😡", "👍", "❤️", "🔥", "✨"};
        for(String em : emojis) {
            JButton eb = new JButton(em); eb.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            eb.setBackground(bgInput); eb.setForeground(Color.WHITE); eb.setFocusPainted(false); eb.setBorderPainted(false);
            eb.addActionListener(e -> { txtMsg.setText(txtMsg.getText() + em); emojiMenu.setVisible(false); txtMsg.requestFocus(); });
            emojiBox.add(eb);
        }
        emojiMenu.add(emojiBox);
        btnEmoji.addActionListener(e -> emojiMenu.show(btnEmoji, 0, -80)); // Hiện lên phía trên

        JButton btnSend = styleButton("GỬI", blurple); btnSend.setPreferredSize(new Dimension(90, 45));
        ActionListener sendAction = e -> { if(!txtMsg.getText().isEmpty()) { out.println("CHAT|" + txtMsg.getText()); txtMsg.setText(""); } };
        txtMsg.addActionListener(sendAction); btnSend.addActionListener(sendAction);
        
        JPanel actionP = new JPanel(new BorderLayout(5, 0)); actionP.setOpaque(false);
        actionP.add(btnEmoji, BorderLayout.WEST); actionP.add(btnSend, BorderLayout.EAST);

        inputPanel.add(txtMsg, BorderLayout.CENTER); inputPanel.add(actionP, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        p.add(sidebar, BorderLayout.WEST); p.add(chatPanel, BorderLayout.CENTER); return p;
    }

    private JButton styleButton(String text, Color bg) {
        JButton btn = new JButton(text); btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg); btn.setForeground(Color.WHITE); btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); return btn;
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
                            roomListModel.clear(); if (p.length > 1) for (String r : p[1].split(",")) roomListModel.addElement(r); break;
                        case "JOIN_SUCCESS": lblTitle.setText("# " + p[1]); chatArea.setText(""); break;
                        case "MSG": chatArea.append(p[1] + "\n"); chatArea.setCaretPosition(chatArea.getDocument().getLength()); break;
                        case "SYS": chatArea.append(" — " + p[1] + " —\n"); chatArea.setCaretPosition(chatArea.getDocument().getLength()); break;
                    }
                });
            }
        } catch (IOException e) { }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new FlatDarkLaf()); UIManager.put("ScrollBar.showButtons", false); UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder()); } catch (Exception ex) {}
        SwingUtilities.invokeLater(() -> new ChatGUIClient().setVisible(true));
    }
}