/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package bt.bai5ltm;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author Admin
 */
public class Bai5LTM {

    public static void main(String[] args) {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            System.out.println("Địa chỉ IP hiện tại: " + ip.getHostAddress());
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network != null) {
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    System.out.print("Địa chỉ MAC: ");

                    // 4. Sử dụng StringBuilder để định dạng và in địa chỉ MAC
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        // Chuyển đổi byte sang định dạng Hex (Hệ 16)
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    System.out.println(sb.toString());
                } else {
                    System.out.println("Không thể truy cập địa chỉ MAC.");
                }
            }
            else {
                System.out.println("Không tìm thấy giao diện mạng tương ứng.");
            }
        } catch (UnknownHostException e) {
            System.err.println("Lỗi: Không tìm thấy Host: " + e.getMessage());
        }
        catch (SocketException e) {
            System.err.println("Lỗi truy cập mạng: " + e.getMessage());
        }
    }
    
}
