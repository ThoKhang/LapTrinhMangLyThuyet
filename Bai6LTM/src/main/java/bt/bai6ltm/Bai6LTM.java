/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package bt.bai6ltm;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

/**
 *
 * @author Admin
 */
public class Bai6LTM {

    public void run() {
        try {
            // Lấy danh sách tất cả các card mạng
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            
            System.out.println("===== DANH SÁCH CÁC GIAO TIẾP MẠNG =====");

            // Duyệt danh sách và biến mỗi NetworkInterface thành một đối tượng NetworkCard
            for (NetworkInterface netIntf : Collections.list(nets)) {
                NetworkCard card = new NetworkCard(netIntf);
                card.displayInfo();
            }

        } catch (SocketException e) {
            System.err.println("Lỗi hệ thống mạng: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Bai6LTM app = new Bai6LTM();
        app.run();
    }
}
