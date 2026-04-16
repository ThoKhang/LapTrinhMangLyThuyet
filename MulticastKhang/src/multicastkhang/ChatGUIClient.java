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

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    private JTextField txtIP = new JTextField("127.0.0.1", 15);
    private JTextField txtUser = new JTextField(15);
    private JPasswordField txtPass = new JPasswordField(15);
    private DefaultListModel<String> roomListModel = new DefaultListModel<>();
    private JList<String> roomList = new JList<>(roomListModel);
    private JTextArea chatArea = new JTextArea();
    private JTextField txtMessage = new JTextField();
    private JLabel lblRoomTitle = new JLabel("# general", SwingConstants.LEFT);

    // --- MÀU SẮC DISCORD STYLE ---
    private Color bgMain = new Color(54, 57, 63);       // Nền chính Chat
    private Color bgSidebar = new Color(47, 49, 54);    // Nền danh sách phòng
    private Color bgDark = new Color(32, 34, 37);       // Nền tối (đăng nhập)
    private Color bgInput = new Color(64, 68, 75);      // Nền ô nhập chữ
    private Color textMain = new Color(220, 221, 222);  // Màu chữ chính
    private Color textMuted = new Color(142, 146, 151); // Màu chữ phụ
    private Color blurple = new Color(88, 101, 242);    // Xanh Discord (Nút bấm)
    private Color greenSuccess = new Color(59, 165, 92); // Xanh lá
    private Color redDanger = new Color(237, 66, 69);    // Đỏ

    public ChatGUIClient() {
        setTitle("Discord Clone Client");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        mainPanel.setBackground(bgDark);
        mainPanel.add(createConnectPanel(), "CONNECT");
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createRoomPanel(), "ROOMS");
        mainPanel.add(createChatPanel(), "CHAT");

        add(mainPanel);
        cardLayout.show(mainPanel, "CONNECT");
    }

    private JPanel createConnectPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(bgDark);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("KẾT NỐI SERVER");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        gbc.gridwidth = 2; p.add(title, gbc);

        JLabel lblIP = new JLabel("ĐỊA CHỈ IP");
        lblIP.setForeground(textMuted); lblIP.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridwidth = 1; gbc.gridy = 1; p.add(lblIP, gbc);
        
        gbc.gridx = 1; 
        styleTextField(txtIP);
        p.add(txtIP, gbc);

        JButton btnConnect = styleButton("KẾT NỐI", blurple);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(btnConnect, gbc);

        btnConnect.addActionListener(e -> {
            String ip = txtIP.getText().trim();
            if(ip.isEmpty()) return;
            try {
                socket = new Socket(ip, PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                new Thread(this::listenToServer).start();
                cardLayout.show(mainPanel, "LOGIN");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể kết nối Server!");
            }
        });
        return p;
    }

    private JPanel createLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(bgDark);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("CHÀO MỪNG TRỞ LẠI!", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22)); title.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; p.add(title, gbc);

        JLabel lblUser = new JLabel("TÀI KHOẢN"); lblUser.setForeground(textMuted); lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridwidth = 1; gbc.gridy = 1; p.add(lblUser, gbc);
        gbc.gridx = 1; styleTextField(txtUser); p.add(txtUser, gbc);

        JLabel lblPass = new JLabel("MẬT KHẨU"); lblPass.setForeground(textMuted); lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridx = 0; gbc.gridy = 2; p.add(lblPass, gbc);
        gbc.gridx = 1; styleTextField(txtPass); p.add(txtPass, gbc);

        JButton btnLogin = styleButton("Đăng Nhập", blurple);
        JButton btnRegister = styleButton("Đăng Ký", greenSuccess);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setOpaque(false); btnPanel.add(btnLogin); btnPanel.add(btnRegister);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; p.add(btnPanel, gbc);

        btnLogin.addActionListener(e -> out.println("LOGIN|" + txtUser.getText() + "|" + new String(txtPass.getPassword())));
        btnRegister.addActionListener(e -> out.println("REGISTER|" + txtUser.getText() + "|" + new String(txtPass.getPassword())));
        return p;
    }

    private JPanel createRoomPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bgSidebar);

        JLabel title = new JLabel("KÊNH TEXT", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12)); title.setForeground(textMuted);
        title.setBorder(new EmptyBorder(20, 15, 10, 15));
        p.add(title, BorderLayout.NORTH);

        roomList.setFont(new Font("Segoe UI", Font.BOLD, 15));
        roomList.setBackground(bgSidebar); roomList.setForeground(textMuted);
        roomList.setSelectionBackground(bgInput); roomList.setSelectionForeground(Color.WHITE);
        // Custom cell renderer để thêm dấu # trước tên phòng giống Discord
        roomList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText("  #  " + value.toString());
                label.setBorder(new EmptyBorder(8, 5, 8, 5));
                return label;
            }
        });

        JScrollPane scroll = new JScrollPane(roomList);
        scroll.setBorder(null); scroll.getViewport().setBackground(bgSidebar);
        p.add(scroll, BorderLayout.CENTER);

        JPanel bottomP = new JPanel(new GridLayout(2, 1, 5, 5));
        bottomP.setBackground(bgDark); bottomP.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JButton btnJoin = styleButton("Vào Kênh", blurple);
        JButton btnCreate = styleButton("Tạo Kênh Mới", bgInput);
        btnCreate.setForeground(textMain);

        bottomP.add(btnJoin); bottomP.add(btnCreate);
        p.add(bottomP, BorderLayout.SOUTH);

        btnJoin.addActionListener(e -> {
            if (roomList.getSelectedValue() != null) out.println("JOIN_ROOM|" + roomList.getSelectedValue());
        });
        btnCreate.addActionListener(e -> {
            String rName = JOptionPane.showInputDialog(this, "Nhập tên kênh (ví dụ: test-channel):");
            if (rName != null && !rName.trim().isEmpty()) {
                out.println("CREATE_ROOM|" + rName.trim().toLowerCase().replace(" ", "-"));
            }
        });
        return p;
    }

    private JPanel createChatPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bgMain);
        
        lblRoomTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblRoomTitle.setOpaque(true); lblRoomTitle.setBackground(bgMain); lblRoomTitle.setForeground(Color.WHITE);
        lblRoomTitle.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, bgDark),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JButton btnLeave = styleButton("Rời", redDanger);
        btnLeave.setPreferredSize(new Dimension(70, 30));
        JPanel topP = new JPanel(new BorderLayout()); topP.setBackground(bgMain);
        topP.add(lblRoomTitle, BorderLayout.CENTER); 
        
        JPanel leavePanel = new JPanel(); leavePanel.setBackground(bgMain); leavePanel.setBorder(new EmptyBorder(10,10,10,10));
        leavePanel.add(btnLeave);
        topP.add(leavePanel, BorderLayout.EAST);
        p.add(topP, BorderLayout.NORTH);

        chatArea.setEditable(false); chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        chatArea.setBackground(bgMain); chatArea.setForeground(textMain);
        chatArea.setMargin(new Insets(10, 15, 10, 15)); chatArea.setLineWrap(true);
        JScrollPane scrollChat = new JScrollPane(chatArea);
        scrollChat.setBorder(null); scrollChat.getViewport().setBackground(bgMain);
        p.add(scrollChat, BorderLayout.CENTER);

        JPanel bottomP = new JPanel(new BorderLayout(10, 10));
        bottomP.setBackground(bgMain); bottomP.setBorder(new EmptyBorder(10, 20, 20, 20));
        
        styleTextField(txtMessage);
        txtMessage.setPreferredSize(new Dimension(0, 45));
        bottomP.add(txtMessage, BorderLayout.CENTER);
        p.add(bottomP, BorderLayout.SOUTH);

        ActionListener sendAction = e -> {
            String msg = txtMessage.getText().trim();
            if (!msg.isEmpty()) { out.println("CHAT|" + msg); txtMessage.setText(""); }
        };
        txtMessage.addActionListener(sendAction);
        btnLeave.addActionListener(e -> { out.println("GET_ROOMS"); cardLayout.show(mainPanel, "ROOMS"); });
        return p;
    }

    private JButton styleButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private void styleTextField(JTextField txt) {
        txt.setBackground(bgInput); txt.setForeground(Color.WHITE); txt.setCaretColor(Color.WHITE);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgInput, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    private void listenToServer() {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                String[] parts = response.split("\\|");
                String type = parts[0];
                SwingUtilities.invokeLater(() -> {
                    switch (type) {
                        case "SERVER": JOptionPane.showMessageDialog(this, parts[1], "Thông báo", JOptionPane.INFORMATION_MESSAGE); break;
                        case "LOGIN_SUCCESS": cardLayout.show(mainPanel, "ROOMS"); out.println("GET_ROOMS"); break;
                        case "ROOM_LIST":
                            roomListModel.clear();
                            if (parts.length > 1) for (String r : parts[1].split(",")) roomListModel.addElement(r);
                            break;
                        case "JOIN_SUCCESS":
                            lblRoomTitle.setText("# " + parts[1].toLowerCase());
                            chatArea.setText(""); cardLayout.show(mainPanel, "CHAT");
                            break;
                        case "MSG":
                            chatArea.append(parts[1] + "\n");
                            chatArea.setCaretPosition(chatArea.getDocument().getLength());
                            break;
                        case "KICKED":
                            JOptionPane.showMessageDialog(this, "Bạn đã bị Admin kích khỏi phòng!", "Kicked", JOptionPane.WARNING_MESSAGE);
                            out.println("GET_ROOMS"); cardLayout.show(mainPanel, "ROOMS");
                            break;
                    }
                });
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Mất kết nối tới Server!");
                cardLayout.show(mainPanel, "CONNECT");
            });
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ChatGUIClient().setVisible(true));
    }
}