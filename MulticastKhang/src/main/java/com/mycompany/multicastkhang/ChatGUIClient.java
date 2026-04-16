package com.mycompany.multicastkhang;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
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
    
    private JTextPane chatPane = new JTextPane();
    private HTMLEditorKit htmlKit = new HTMLEditorKit();
    
    private JTextField txtMsg = new JTextField();
    private JLabel lblTitle = new JLabel("CHƯA CHỌN KÊNH", SwingConstants.LEFT);

    private Color bgDarkest = Color.decode("#1E1F22"), bgSidebar = Color.decode("#2B2D31");  
    private Color bgChat = Color.decode("#313338"), bgInput = Color.decode("#383A40");    
    private Color textMain = Color.decode("#DBDEE1"), textMuted = Color.decode("#949BA4");  
    private Color blurple = Color.decode("#5865F2"), redDanger = Color.decode("#DA373C");

    public ChatGUIClient() {
        setTitle("Discord Clone - Pro UI"); 
        setSize(900, 650); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null);
        
        setupHtmlStyles();
        
        mainPanel.add(createCenteredPanel(createConnectBox()), "CONNECT");
        mainPanel.add(createCenteredPanel(createLoginBox()), "LOGIN");
        mainPanel.add(createMainChatUI(), "CHAT_UI"); 
        add(mainPanel);
    }

    private void setupHtmlStyles() {
        chatPane.setEditable(false);
        chatPane.setContentType("text/html");
        chatPane.setEditorKit(htmlKit);
        chatPane.setBackground(bgChat);
        
        StyleSheet styleSheet = htmlKit.getStyleSheet();
        styleSheet.addRule("body { font-family: 'Segoe UI', sans-serif; font-size: 14px; color: #DBDEE1; margin: 10px; }");
        styleSheet.addRule(".msg-box { margin-bottom: 8px; line-height: 1.5; }");
        styleSheet.addRule(".user { color: #5865F2; font-weight: bold; }");
        styleSheet.addRule(".sys { color: #949BA4; font-style: italic; font-size: 13px; margin-bottom: 8px;}");
        // CSS giúp Emoji (ảnh) nằm thẳng hàng với chữ
        styleSheet.addRule("img.emoji { vertical-align: middle; margin: 0 2px; }"); 
    }

    private void appendHtml(String html) {
        try {
            HTMLDocument doc = (HTMLDocument) chatPane.getDocument();
            htmlKit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
            chatPane.setCaretPosition(doc.getLength());
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- BỘ CHUYỂN ĐỔI EMOJI MÀU (MAGIC HAPPENS HERE) ---
    private String formatMessage(String text) {
        // 1. Chống lỗi hiển thị thẻ HTML do người dùng gõ
        text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        
        // 2. Chuyển Unicode đen trắng thành Link Ảnh Twemoji (Màu chuẩn Discord)
        String baseUrl = "<img class='emoji' width='22' height='22' src='https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/";
        text = text.replace("😀", baseUrl + "1f600.png'>");
        text = text.replace("😂", baseUrl + "1f602.png'>");
        text = text.replace("🥰", baseUrl + "1f970.png'>");
        text = text.replace("😎", baseUrl + "1f60e.png'>");
        text = text.replace("😭", baseUrl + "1f62d.png'>");
        text = text.replace("😡", baseUrl + "1f621.png'>");
        text = text.replace("👍", baseUrl + "1f44d.png'>");
        text = text.replace("❤️", baseUrl + "2764.png'>");
        text = text.replace("🔥", baseUrl + "1f525.png'>");
        text = text.replace("✨", baseUrl + "2728.png'>");
        
        return text;
    }

    private JPanel createCenteredPanel(JPanel innerBox) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(bgDarkest);
        wrapper.add(innerBox, new GridBagConstraints());
        return wrapper;
    }

    private JPanel createConnectBox() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(bgChat);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bgDarkest, 1), new EmptyBorder(30, 40, 30, 40)));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(10, 0, 10, 0); gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbl = new JLabel("CHÀO MỪNG TRỞ LẠI!", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 24)); lbl.setForeground(Color.WHITE); p.add(lbl, gbc);

        gbc.gridy = 1; gbc.insets = new Insets(20, 0, 5, 0);
        JLabel labelIP = new JLabel("ĐỊA CHỈ IP MÁY CHỦ *"); labelIP.setForeground(textMuted); labelIP.setFont(new Font("Segoe UI", Font.BOLD, 12)); p.add(labelIP, gbc);
        
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 20, 0);
        styleInput(txtIP); txtIP.setPreferredSize(new Dimension(350, 45)); p.add(txtIP, gbc);

        gbc.gridy = 3; JButton btn = styleButton("Kết Nối Ngay", blurple); btn.setPreferredSize(new Dimension(350, 45));
        btn.addActionListener(e -> {
            try {
                socket = new Socket(txtIP.getText(), 5555);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                new Thread(this::listen).start(); cardLayout.show(mainPanel, "LOGIN");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Không thể kết nối đến Máy chủ!"); }
        });
        p.add(btn, gbc); return p;
    }

    private JPanel createLoginBox() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(bgChat);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bgDarkest, 1), new EmptyBorder(30, 40, 30, 40)));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(5, 0, 5, 0); gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbl = new JLabel("ĐĂNG NHẬP", SwingConstants.CENTER); lbl.setFont(new Font("Segoe UI", Font.BOLD, 24)); lbl.setForeground(Color.WHITE); p.add(lbl, gbc);
        JLabel subLbl = new JLabel("Rất vui được gặp lại bạn!", SwingConstants.CENTER); subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14)); subLbl.setForeground(textMuted);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 20, 0); p.add(subLbl, gbc);

        gbc.gridy = 2; gbc.insets = new Insets(5, 0, 5, 0);
        JLabel lblUser = new JLabel("TÀI KHOẢN *"); lblUser.setForeground(textMuted); lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12)); p.add(lblUser, gbc);
        gbc.gridy = 3; styleInput(txtUser); txtUser.setPreferredSize(new Dimension(350, 45)); p.add(txtUser, gbc);
        
        gbc.gridy = 4; gbc.insets = new Insets(15, 0, 5, 0);
        JLabel lblPass = new JLabel("MẬT KHẨU *"); lblPass.setForeground(textMuted); lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12)); p.add(lblPass, gbc);
        gbc.gridy = 5; styleInput(txtPass); txtPass.setPreferredSize(new Dimension(350, 45)); p.add(txtPass, gbc);

        gbc.gridy = 6; gbc.insets = new Insets(25, 0, 5, 0);
        JButton btnL = styleButton("Đăng Nhập", blurple); btnL.setPreferredSize(new Dimension(350, 45));
        btnL.addActionListener(e -> out.println("LOGIN|" + txtUser.getText() + "|" + new String(txtPass.getPassword()))); p.add(btnL, gbc);

        gbc.gridy = 7; gbc.insets = new Insets(10, 0, 0, 0);
        JButton btnR = styleButton("Tạo tài khoản mới", new Color(78, 80, 88)); btnR.setPreferredSize(new Dimension(350, 45));
        btnR.addActionListener(e -> out.println("REGISTER|" + txtUser.getText() + "|" + new String(txtPass.getPassword()))); p.add(btnR, gbc); return p;
    }

    private JPanel createMainChatUI() {
        JPanel p = new JPanel(new BorderLayout());
        
        JPanel sidebar = new JPanel(new BorderLayout()); sidebar.setBackground(bgSidebar); sidebar.setPreferredSize(new Dimension(260, 0));
        JPanel sidebarHeader = new JPanel(new BorderLayout()); sidebarHeader.setOpaque(false); sidebarHeader.setBorder(new EmptyBorder(18, 15, 10, 15));
        JLabel sTitle = new JLabel("DANH SÁCH KÊNH TEXT"); sTitle.setFont(new Font("Segoe UI", Font.BOLD, 12)); sTitle.setForeground(textMuted);
        sidebarHeader.add(sTitle, BorderLayout.CENTER); sidebar.add(sidebarHeader, BorderLayout.NORTH);

        roomList.setBackground(bgSidebar); roomList.setForeground(textMuted); roomList.setFont(new Font("Segoe UI", Font.BOLD, 15));
        roomList.setSelectionBackground(Color.decode("#404249")); roomList.setSelectionForeground(Color.WHITE); 
        roomList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText("  #   " + value.toString()); label.setBorder(new EmptyBorder(10, 5, 10, 5)); return label;
            }
        });
        roomList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && roomList.getSelectedValue() != null) out.println("JOIN_ROOM|" + roomList.getSelectedValue()); });
        JScrollPane scrollRooms = new JScrollPane(roomList); scrollRooms.setBorder(null); sidebar.add(scrollRooms, BorderLayout.CENTER);

        JPanel sidebarBottom = new JPanel(new GridLayout(2, 1, 5, 5)); sidebarBottom.setBackground(Color.decode("#232428")); sidebarBottom.setBorder(new EmptyBorder(15, 15, 15, 15));
        JButton btnCreate = styleButton("Tạo Kênh Mới", blurple); JButton btnRefresh = styleButton("Làm Mới", bgInput);
        btnCreate.addActionListener(e -> {
            String rName = JOptionPane.showInputDialog(this, "Nhập tên kênh (ví dụ: game-khtn):");
            if (rName != null && !rName.trim().isEmpty()) out.println("CREATE_ROOM|" + rName.trim().toLowerCase().replace(" ", "-"));
        });
        btnRefresh.addActionListener(e -> out.println("GET_ROOMS"));
        sidebarBottom.add(btnCreate); sidebarBottom.add(btnRefresh); sidebar.add(sidebarBottom, BorderLayout.SOUTH);

        JPanel chatPanel = new JPanel(new BorderLayout()); chatPanel.setBackground(bgChat);
        JPanel chatHeader = new JPanel(new BorderLayout()); chatHeader.setBackground(bgChat);
        chatHeader.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, bgDarkest), new EmptyBorder(12, 20, 12, 20)));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20)); lblTitle.setForeground(Color.WHITE);
        JButton btnLeave = styleButton("Rời Kênh", redDanger); btnLeave.setPreferredSize(new Dimension(100, 35));
        btnLeave.addActionListener(e -> { lblTitle.setText("CHƯA CHỌN KÊNH"); chatPane.setText(""); roomList.clearSelection(); out.println("GET_ROOMS"); });
        chatHeader.add(lblTitle, BorderLayout.CENTER); chatHeader.add(btnLeave, BorderLayout.EAST); chatPanel.add(chatHeader, BorderLayout.NORTH);

        JScrollPane scrollChat = new JScrollPane(chatPane); scrollChat.setBorder(null); chatPanel.add(scrollChat, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0)); inputPanel.setBackground(bgChat); inputPanel.setBorder(new EmptyBorder(15, 20, 20, 20));
        styleInput(txtMsg); txtMsg.setFont(new Font("Segoe Emoji", Font.PLAIN, 15));
        txtMsg.putClientProperty("JTextField.placeholderText", "Nhắn tin vào kênh..."); 
        txtMsg.putClientProperty("JComponent.roundRect", true);
        
        JButton btnEmoji = styleButton("☺", Color.decode("#2B2D31")); 
        btnEmoji.setForeground(textMuted); btnEmoji.setFont(new Font("Segoe Emoji", Font.BOLD, 18));
        btnEmoji.setPreferredSize(new Dimension(50, 45));
        JPopupMenu emojiMenu = new JPopupMenu(); emojiMenu.setBackground(bgChat);
        JPanel emojiBox = new JPanel(new GridLayout(2, 5, 2, 2)); emojiBox.setBackground(bgChat);
        String[] emojis = {"😀", "😂", "🥰", "😎", "😭", "😡", "👍", "❤️", "🔥", "✨"};
        for(String em : emojis) {
            JButton eb = new JButton(em); eb.setFont(new Font("Segoe Emoji", Font.PLAIN, 18));
            eb.setBackground(bgInput); eb.setForeground(Color.WHITE); eb.setFocusPainted(false); eb.setBorderPainted(false);
            eb.addActionListener(e -> { txtMsg.setText(txtMsg.getText() + em); emojiMenu.setVisible(false); txtMsg.requestFocus(); });
            eb.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { eb.setBackground(Color.decode("#404249")); }
                public void mouseExited(MouseEvent e) { eb.setBackground(bgInput); }
            });
            emojiBox.add(eb);
        }
        emojiMenu.add(emojiBox);
        btnEmoji.addActionListener(e -> emojiMenu.show(btnEmoji, 0, -90));

        JButton btnSend = styleButton("GỬI", blurple); btnSend.setPreferredSize(new Dimension(90, 45));
        ActionListener sendAction = e -> { if(!txtMsg.getText().isEmpty()) { out.println("CHAT|" + txtMsg.getText()); txtMsg.setText(""); } };
        txtMsg.addActionListener(sendAction); btnSend.addActionListener(sendAction);
        
        JPanel actionP = new JPanel(new BorderLayout(5, 0)); actionP.setOpaque(false);
        actionP.add(btnEmoji, BorderLayout.WEST); actionP.add(btnSend, BorderLayout.EAST);

        inputPanel.add(txtMsg, BorderLayout.CENTER); inputPanel.add(actionP, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        p.add(sidebar, BorderLayout.WEST); p.add(chatPanel, BorderLayout.CENTER); return p;
    }

    private void styleInput(JTextField txt) {
        txt.setBackground(bgInput); txt.setForeground(textMain); txt.setCaretColor(Color.WHITE);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bgInput, 1), new EmptyBorder(8, 15, 8, 15)));
    }

    private JButton styleButton(String text, Color bg) {
        JButton btn = new JButton(text); btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg); btn.setForeground(Color.WHITE); btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
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
                            roomListModel.clear(); if (p.length > 1) for (String r : p[1].split(",")) roomListModel.addElement(r); break;
                        case "JOIN_SUCCESS": lblTitle.setText("# " + p[1]); chatPane.setText(""); break;
                        case "MSG": 
                            int idx = p[1].indexOf(":");
                            if(idx != -1) {
                                String user = p[1].substring(0, idx);
                                String msg = p[1].substring(idx + 1);
                                // GỌI HÀM FORMAT ĐỂ CHUYỂN EMOJI THÀNH ẢNH MÀU
                                String formattedMsg = formatMessage(msg);
                                appendHtml("<div class='msg-box'><span class='user'>" + user + "</span><span class='msg'>: " + formattedMsg + "</span></div>");
                            } else {
                                appendHtml("<div class='msg-box'>" + formatMessage(p[1]) + "</div>");
                            }
                            break;
                        case "SYS": 
                            appendHtml("<div class='sys'>&mdash; " + p[1] + " &mdash;</div>"); 
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
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.put("TextComponent.arc", 8); 
        } catch (Exception ex) {}
        SwingUtilities.invokeLater(() -> new ChatGUIClient().setVisible(true));
    }
}