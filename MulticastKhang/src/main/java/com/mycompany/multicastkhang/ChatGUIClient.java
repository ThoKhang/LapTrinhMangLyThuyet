package com.mycompany.multicastkhang;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
    
    private JPanel chatBox;
    private JScrollPane scrollChat;
    private JTextField txtMsg = new JTextField();
    
    // --- QUẢN LÝ TRẠNG THÁI GIAO DIỆN SÁNG/TỐI ---
    private boolean isDarkMode = true;
    
    private Color bgDarkest = Color.decode("#1E1F22"), bgSidebar = Color.decode("#2B2D31");  
    private Color bgChat = Color.decode("#313338"), bgInput = Color.decode("#383A40");    
    private Color textMain = Color.decode("#DBDEE1"), textMuted = Color.decode("#949BA4");  
    private final Color blurple = Color.decode("#5865F2"), redDanger = Color.decode("#DA373C");

    // --- CÁC PANEL & COMPONENT CẦN ĐỔI MÀU ---
    private JPanel pnlConnectBox = new JPanel(new GridBagLayout());
    private JPanel pnlLoginBox = new JPanel(new GridBagLayout());
    private JPanel pnlSidebar = new JPanel(new BorderLayout());
    private JPanel pnlSidebarBottom = new JPanel(new GridLayout(3, 1, 5, 5)); // 3 nút
    private JPanel pnlChatPanel = new JPanel(new BorderLayout());
    private JPanel pnlChatHeader = new JPanel(new BorderLayout());
    private JPanel pnlInputPanel = new JPanel(new BorderLayout(10, 0));
    
    private JLabel lblConnectTitle = new JLabel("CHÀO MỪNG TRỞ LẠI!", SwingConstants.CENTER);
    private JLabel lblLoginTitle = new JLabel("ĐĂNG NHẬP", SwingConstants.CENTER);
    private JLabel lblTitle = new JLabel("CHƯA CHỌN KÊNH", SwingConstants.LEFT);
    
    private JButton btnTheme, btnRefresh, btnEmoji;

    // --- LƯU TRỮ LỊCH SỬ CHAT CỤC BỘ ĐỂ VẼ LẠI KHI ĐỔI THEME ---
    static class ChatMessage {
        String sender, msg; boolean isSystem;
        public ChatMessage(String s, String m, boolean sys) { sender=s; msg=m; isSystem=sys; }
    }
    private List<ChatMessage> currentRoomMessages = new ArrayList<>();

    public ChatGUIClient() {
        setTitle("Discord Clone - Dual Theme UI"); 
        setSize(900, 650); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null);
        
        mainPanel.add(createCenteredPanel(buildConnectBox()), "CONNECT");
        mainPanel.add(createCenteredPanel(buildLoginBox()), "LOGIN");
        mainPanel.add(buildMainChatUI(), "CHAT_UI"); 
        
        applyCustomColors(); // Khởi tạo màu lần đầu
        add(mainPanel);
    }

    // ================== LOGIC ĐỔI GIAO DIỆN (MAGIC HAPPENS HERE) ==================
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        
        if (isDarkMode) {
            bgDarkest = Color.decode("#1E1F22"); bgSidebar = Color.decode("#2B2D31");
            bgChat = Color.decode("#313338"); bgInput = Color.decode("#383A40");
            textMain = Color.decode("#DBDEE1"); textMuted = Color.decode("#949BA4");
            try { UIManager.setLookAndFeel(new FlatDarkLaf()); } catch(Exception e){}
            btnTheme.setText("Giao Diện: Tối");
        } else {
            // TÔNG MÀU SÁNG (LIGHT MODE) CHUẨN DISCORD
            bgDarkest = Color.decode("#E3E5E8"); bgSidebar = Color.decode("#F2F3F5");
            bgChat = Color.decode("#FFFFFF"); bgInput = Color.decode("#EBEDEF");
            textMain = Color.decode("#313338"); textMuted = Color.decode("#5C5E66");
            try { UIManager.setLookAndFeel(new FlatLightLaf()); } catch(Exception e){}
            btnTheme.setText("Giao Diện: Sáng");
        }
        
        // Yêu cầu Java Swing cập nhật lại toàn bộ cây giao diện
        SwingUtilities.updateComponentTreeUI(this);
        applyCustomColors(); 
        
        // Xóa sạch khung chat và vẽ lại các bong bóng với màu mới
        chatBox.removeAll();
        for (ChatMessage m : currentRoomMessages) {
            appendMessage(m.sender, m.msg, m.isSystem);
        }
        chatBox.revalidate(); chatBox.repaint();
    }

    private void applyCustomColors() {
        mainPanel.setBackground(bgDarkest);
        
        // Màn Connect
        pnlConnectBox.setBackground(bgChat);
        pnlConnectBox.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bgDarkest, 1), new EmptyBorder(30, 40, 30, 40)));
        lblConnectTitle.setForeground(textMain);
        
        // Màn Login
        pnlLoginBox.setBackground(bgChat);
        pnlLoginBox.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bgDarkest, 1), new EmptyBorder(30, 40, 30, 40)));
        lblLoginTitle.setForeground(textMain);
        
        // Màn Chat UI
        pnlSidebar.setBackground(bgSidebar);
        pnlSidebarBottom.setBackground(isDarkMode ? Color.decode("#232428") : Color.decode("#E3E5E8"));
        
        roomList.setBackground(bgSidebar); roomList.setForeground(textMuted);
        roomList.setSelectionBackground(isDarkMode ? Color.decode("#404249") : Color.decode("#D4D7DC"));
        roomList.setSelectionForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        
        pnlChatPanel.setBackground(bgChat);
        pnlChatHeader.setBackground(bgChat);
        pnlChatHeader.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, bgDarkest), new EmptyBorder(12, 20, 12, 20)));
        lblTitle.setForeground(textMain);
        
        chatBox.setBackground(bgChat);
        scrollChat.setBackground(bgChat);
        scrollChat.getViewport().setBackground(bgChat);
        pnlInputPanel.setBackground(bgChat);
        
        // Cập nhật các ô nhập liệu
        styleInput(txtIP); styleInput(txtUser); styleInput(txtPass); styleInput(txtMsg);
        
        // Cập nhật nút
        if(btnRefresh != null) btnRefresh.setBackground(bgInput);
        if(btnTheme != null) { btnTheme.setBackground(bgInput); btnTheme.setForeground(textMain); }
        if(btnEmoji != null) btnEmoji.setBackground(bgSidebar);
    }
    // ==============================================================================

    private JPanel createCenteredPanel(JPanel innerBox) {
        JPanel wrapper = new JPanel(new GridBagLayout()); 
        wrapper.setOpaque(false);
        wrapper.add(innerBox, new GridBagConstraints()); return wrapper;
    }

    private JPanel buildConnectBox() {
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(10, 0, 10, 0); gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        lblConnectTitle.setFont(new Font("Segoe UI", Font.BOLD, 24)); pnlConnectBox.add(lblConnectTitle, gbc);
        
        gbc.gridy = 1; gbc.insets = new Insets(20, 0, 5, 0); JLabel labelIP = new JLabel("ĐỊA CHỈ IP MÁY CHỦ *"); labelIP.setFont(new Font("Segoe UI", Font.BOLD, 12)); pnlConnectBox.add(labelIP, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 20, 0); txtIP.setPreferredSize(new Dimension(350, 45)); pnlConnectBox.add(txtIP, gbc);
        gbc.gridy = 3; JButton btn = styleButton("Kết Nối Ngay", blurple); btn.setPreferredSize(new Dimension(350, 45));
        btn.addActionListener(e -> {
            try {
                socket = new Socket(txtIP.getText(), 5555);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                new Thread(this::listen).start(); cardLayout.show(mainPanel, "LOGIN");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Không thể kết nối đến Máy chủ!"); }
        }); pnlConnectBox.add(btn, gbc); return pnlConnectBox;
    }

    private JPanel buildLoginBox() {
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(5, 0, 5, 0); gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        lblLoginTitle.setFont(new Font("Segoe UI", Font.BOLD, 24)); pnlLoginBox.add(lblLoginTitle, gbc);
        
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 20, 0); JLabel subLbl = new JLabel("Rất vui được gặp lại bạn!", SwingConstants.CENTER); subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14)); pnlLoginBox.add(subLbl, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(5, 0, 5, 0); JLabel lblUser = new JLabel("TÀI KHOẢN *"); lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12)); pnlLoginBox.add(lblUser, gbc);
        gbc.gridy = 3; txtUser.setPreferredSize(new Dimension(350, 45)); pnlLoginBox.add(txtUser, gbc);
        gbc.gridy = 4; gbc.insets = new Insets(15, 0, 5, 0); JLabel lblPass = new JLabel("MẬT KHẨU *"); lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12)); pnlLoginBox.add(lblPass, gbc);
        gbc.gridy = 5; txtPass.setPreferredSize(new Dimension(350, 45)); pnlLoginBox.add(txtPass, gbc);

        gbc.gridy = 6; gbc.insets = new Insets(25, 0, 5, 0); JButton btnL = styleButton("Đăng Nhập", blurple); btnL.setPreferredSize(new Dimension(350, 45));
        btnL.addActionListener(e -> out.println("LOGIN|" + txtUser.getText() + "|" + new String(txtPass.getPassword()))); pnlLoginBox.add(btnL, gbc);
        
        gbc.gridy = 7; gbc.insets = new Insets(10, 0, 0, 0); JButton btnR = styleButton("Tạo tài khoản mới", new Color(78, 80, 88)); btnR.setPreferredSize(new Dimension(350, 45));
        btnR.addActionListener(e -> out.println("REGISTER|" + txtUser.getText() + "|" + new String(txtPass.getPassword()))); pnlLoginBox.add(btnR, gbc); 
        return pnlLoginBox;
    }

    private JPanel buildMainChatUI() {
        pnlSidebar.setPreferredSize(new Dimension(260, 0));
        JPanel sidebarHeader = new JPanel(new BorderLayout()); sidebarHeader.setOpaque(false); sidebarHeader.setBorder(new EmptyBorder(18, 15, 10, 15));
        JLabel sTitle = new JLabel("DANH SÁCH KÊNH TEXT"); sTitle.setFont(new Font("Segoe UI", Font.BOLD, 12)); sTitle.setForeground(Color.GRAY);
        sidebarHeader.add(sTitle, BorderLayout.CENTER); pnlSidebar.add(sidebarHeader, BorderLayout.NORTH);

        roomList.setFont(new Font("Segoe UI", Font.BOLD, 15));
        roomList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText("  #   " + value.toString()); label.setBorder(new EmptyBorder(10, 5, 10, 5)); return label;
            }
        });
        roomList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && roomList.getSelectedValue() != null) out.println("JOIN_ROOM|" + roomList.getSelectedValue()); });
        JScrollPane scrollRooms = new JScrollPane(roomList); scrollRooms.setBorder(null); pnlSidebar.add(scrollRooms, BorderLayout.CENTER);

        pnlSidebarBottom.setBorder(new EmptyBorder(15, 15, 15, 15));
        JButton btnCreate = styleButton("Tạo Kênh Mới", blurple); 
        btnRefresh = styleButton("Làm Mới", bgInput);
        btnTheme = styleButton("Giao Diện: Tối", bgInput); // NÚT THEME MỚI
        
        btnCreate.addActionListener(e -> {
            String rName = JOptionPane.showInputDialog(this, "Nhập tên kênh:");
            if (rName != null && !rName.trim().isEmpty()) out.println("CREATE_ROOM|" + rName.trim().toLowerCase().replace(" ", "-"));
        });
        btnRefresh.addActionListener(e -> out.println("GET_ROOMS"));
        btnTheme.addActionListener(e -> toggleTheme()); // GỌI LỆNH ĐỔI THEME
        
        pnlSidebarBottom.add(btnCreate); pnlSidebarBottom.add(btnRefresh); pnlSidebarBottom.add(btnTheme); 
        pnlSidebar.add(pnlSidebarBottom, BorderLayout.SOUTH);

        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20)); 
        JButton btnLeave = styleButton("Rời Kênh", redDanger); btnLeave.setPreferredSize(new Dimension(100, 35));
        btnLeave.addActionListener(e -> { 
            lblTitle.setText("CHƯA CHỌN KÊNH"); 
            currentRoomMessages.clear(); chatBox.removeAll(); chatBox.revalidate(); chatBox.repaint(); 
            roomList.clearSelection(); out.println("GET_ROOMS"); 
        });
        pnlChatHeader.add(lblTitle, BorderLayout.CENTER); pnlChatHeader.add(btnLeave, BorderLayout.EAST); pnlChatPanel.add(pnlChatHeader, BorderLayout.NORTH);

        chatBox = new JPanel(); chatBox.setLayout(new BoxLayout(chatBox, BoxLayout.Y_AXIS)); chatBox.setBorder(new EmptyBorder(15, 5, 15, 5));
        scrollChat = new JScrollPane(chatBox); scrollChat.setBorder(null); scrollChat.getVerticalScrollBar().setUnitIncrement(16); 
        pnlChatPanel.add(scrollChat, BorderLayout.CENTER);

        pnlInputPanel.setBorder(new EmptyBorder(15, 20, 20, 20));
        txtMsg.setFont(new Font("Segoe UI", Font.PLAIN, 15)); txtMsg.putClientProperty("JTextField.placeholderText", "Nhắn tin vào kênh..."); 
        
        btnEmoji = styleButton("☺", bgSidebar); btnEmoji.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18)); btnEmoji.setPreferredSize(new Dimension(50, 45));
        JPopupMenu emojiMenu = new JPopupMenu(); 
        JPanel emojiBox = new JPanel(new GridLayout(2, 5, 2, 2)); emojiBox.setOpaque(false);
        String[] emojis = {"😀", "😂", "🥰", "😎", "😭", "😡", "👍", "❤️", "🔥", "✨"};
        for(String em : emojis) {
            JButton eb = new JButton(em); eb.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18)); eb.setBackground(Color.decode("#404249")); eb.setForeground(Color.WHITE); eb.setFocusPainted(false); eb.setBorderPainted(false);
            eb.addActionListener(e -> { txtMsg.setText(txtMsg.getText() + em); emojiMenu.setVisible(false); txtMsg.requestFocus(); });
            emojiBox.add(eb);
        }
        emojiMenu.add(emojiBox); btnEmoji.addActionListener(e -> { emojiMenu.setBackground(bgChat); emojiMenu.show(btnEmoji, 0, -90); });

        JButton btnSend = styleButton("GỬI", blurple); btnSend.setPreferredSize(new Dimension(90, 45));
        ActionListener sendAction = e -> { if(!txtMsg.getText().isEmpty()) { out.println("CHAT|" + txtMsg.getText()); txtMsg.setText(""); } };
        txtMsg.addActionListener(sendAction); btnSend.addActionListener(sendAction);
        
        JPanel actionP = new JPanel(new BorderLayout(5, 0)); actionP.setOpaque(false);
        actionP.add(btnEmoji, BorderLayout.WEST); actionP.add(btnSend, BorderLayout.EAST);
        pnlInputPanel.add(txtMsg, BorderLayout.CENTER); pnlInputPanel.add(actionP, BorderLayout.EAST); pnlChatPanel.add(pnlInputPanel, BorderLayout.SOUTH);

        JPanel mainWrap = new JPanel(new BorderLayout()); mainWrap.add(pnlSidebar, BorderLayout.WEST); mainWrap.add(pnlChatPanel, BorderLayout.CENTER); 
        return mainWrap;
    }

    // --- HÀM TẠO BONG BÓNG CHAT CHUẨN ---
    private void appendMessage(String sender, String msg, boolean isSystem) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 10, 5, 10));

        if (isSystem) {
            JLabel lbl = new JLabel("— " + msg + " —", SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.ITALIC, 12)); lbl.setForeground(textMuted);
            row.add(lbl, BorderLayout.CENTER);
        } else {
            boolean isMe = sender.equals(txtUser.getText().trim());

            JPanel bubble = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                    g2.dispose();
                }
            };
            bubble.setOpaque(false); 
            bubble.setBackground(isMe ? blurple : bgSidebar); // Bubble sẽ tự cập nhật màu khi bgSidebar đổi màu sáng/tối
            bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

            JLabel lblText = new JLabel("<html><div style='font-family: Segoe UI;'>" + formatMessage(msg) + "</div></html>");
            lblText.setFont(new Font("Segoe UI", Font.PLAIN, 14)); 
            lblText.setForeground(isMe ? Color.WHITE : textMain);
            bubble.add(lblText, BorderLayout.CENTER);

            JLabel lblName = new JLabel(isMe ? "Bạn" : sender);
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 11)); lblName.setForeground(textMuted);

            JPanel wrapper = new JPanel(new BorderLayout(0, 4)); wrapper.setOpaque(false);

            if (isMe) {
                lblName.setHorizontalAlignment(SwingConstants.RIGHT);
                wrapper.add(lblName, BorderLayout.NORTH); wrapper.add(bubble, BorderLayout.EAST); row.add(wrapper, BorderLayout.EAST);
            } else {
                lblName.setHorizontalAlignment(SwingConstants.LEFT);
                wrapper.add(lblName, BorderLayout.NORTH); wrapper.add(bubble, BorderLayout.WEST); row.add(wrapper, BorderLayout.WEST);
            }
        }

        chatBox.add(row); chatBox.revalidate(); chatBox.repaint();
        SwingUtilities.invokeLater(() -> { JScrollBar vertical = scrollChat.getVerticalScrollBar(); vertical.setValue(vertical.getMaximum()); });
    }

    private void styleInput(JTextField txt) {
        txt.setBackground(bgInput); txt.setForeground(textMain); txt.setCaretColor(textMain);
        txt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bgInput, 1), new EmptyBorder(8, 15, 8, 15)));
    }

    private JButton styleButton(String text, Color bg) {
        JButton btn = new JButton(text); btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg); btn.setForeground(Color.WHITE); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
        btn.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); } public void mouseExited(MouseEvent e) { btn.setBackground(bg); } });
        return btn;
    }

    private String formatMessage(String text) {
        text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String baseUrl = "<img style='vertical-align: middle; margin: 0 1px;' width='20' height='20' src='https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/";
        text = text.replace("😀", baseUrl + "1f600.png'>"); text = text.replace("😂", baseUrl + "1f602.png'>"); text = text.replace("🥰", baseUrl + "1f970.png'>");
        text = text.replace("😎", baseUrl + "1f60e.png'>"); text = text.replace("😭", baseUrl + "1f62d.png'>"); text = text.replace("😡", baseUrl + "1f621.png'>");
        text = text.replace("👍", baseUrl + "1f44d.png'>"); text = text.replace("❤️", baseUrl + "2764.png'>"); text = text.replace("🔥", baseUrl + "1f525.png'>"); text = text.replace("✨", baseUrl + "2728.png'>");
        return text;
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
                        case "ROOM_LIST": roomListModel.clear(); if (p.length > 1) for (String r : p[1].split(",")) roomListModel.addElement(r); break;
                        case "JOIN_SUCCESS": 
                            lblTitle.setText("# " + p[1]); 
                            currentRoomMessages.clear(); chatBox.removeAll(); chatBox.revalidate(); chatBox.repaint(); 
                            break;
                        case "MSG": 
                            int idx = p[1].indexOf(":");
                            if(idx != -1) {
                                String sender = p[1].substring(0, idx).trim(), msg = p[1].substring(idx + 1).trim();
                                currentRoomMessages.add(new ChatMessage(sender, msg, false)); // Lưu bộ nhớ tạm
                                appendMessage(sender, msg, false);
                            } else {
                                currentRoomMessages.add(new ChatMessage("", p[1], true));
                                appendMessage("", p[1], true);
                            }
                            break;
                        case "SYS": 
                            currentRoomMessages.add(new ChatMessage("", p[1], true)); // Lưu bộ nhớ tạm
                            appendMessage("", p[1], true);
                            break;
                        case "KICKED":
                            JOptionPane.showMessageDialog(this, "Bạn đã bị Admin kích khỏi kênh này!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
                            lblTitle.setText("CHƯA CHỌN KÊNH"); 
                            currentRoomMessages.clear(); chatBox.removeAll(); chatBox.revalidate(); chatBox.repaint(); 
                            roomList.clearSelection(); out.println("GET_ROOMS"); 
                            break;
                    }
                });
            }
        } catch (IOException e) { }
    }

    public static void main(String[] args) {
        try { 
            UIManager.setLookAndFeel(new FlatDarkLaf()); 
            UIManager.put("ScrollBar.showButtons", false); UIManager.put("ScrollBar.thumbArc", 999); UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.put("TextComponent.arc", 15); UIManager.put("Component.arc", 15); UIManager.put("Button.arc", 15);
        } catch (Exception ex) {}
        SwingUtilities.invokeLater(() -> new ChatGUIClient().setVisible(true));
    }
}