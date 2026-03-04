/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bt.bai7ltm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 *
 * @author Admin
 */
public class MyServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataInputStream din;
    private DataOutputStream dout;
    private int port;

    // Constructor: Khởi tạo Server với cổng cụ thể
    public MyServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server (OOP) dang doi toi cong " + port + "...");
            
            clientSocket = serverSocket.accept();
            System.out.println("Client đã kết nối!");

            // Đóng gói các luồng vào đối tượng
            din = new DataInputStream(clientSocket.getInputStream());
            dout = new DataOutputStream(clientSocket.getOutputStream());
            
            chatLoop(); // Chạy vòng lặp chat

        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void chatLoop() throws IOException {
        Scanner scanner = new Scanner(System.in);
        String msgIn = "";
        while (!msgIn.equals("exit")) {
            msgIn = din.readUTF();
            System.out.println("Client: " + msgIn);

            System.out.print("Server trả lời: ");
            String msgOut = scanner.nextLine();
            dout.writeUTF(msgOut);
            dout.flush();
        }
    }

    private void closeConnection() {
        try {
            if (din != null) din.close();
            if (dout != null) dout.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MyServer(1234).start();
    }
}