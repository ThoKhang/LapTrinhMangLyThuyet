/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bt.bai7ltm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 *
 * @author Admin
 */
public class MyClient {
    private Socket socket;
    private DataInputStream din;
    private DataOutputStream dout;
    private String host;
    private int port;

    public MyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() {
        try {
            socket = new Socket(host, port);
            System.out.println("Đã kết nối thành công tới " + host + ":" + port);

            din = new DataInputStream(socket.getInputStream());
            dout = new DataOutputStream(socket.getOutputStream());

            chatLoop();

        } catch (IOException e) {
            System.err.println("Lỗi kết nối: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void chatLoop() throws IOException {
        Scanner scanner = new Scanner(System.in);
        String msgOut = "";
        while (!msgOut.equals("exit")) {
            System.out.print("Client nhập: ");
            msgOut = scanner.nextLine();
            dout.writeUTF(msgOut);
            dout.flush();

            String msgIn = din.readUTF();
            System.out.println("Server phản hồi: " + msgIn);
        }
    }

    private void closeConnection() {
        try {
            if (din != null) din.close();
            if (dout != null) dout.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MyClient("localhost", 1234).connect();
    }
}