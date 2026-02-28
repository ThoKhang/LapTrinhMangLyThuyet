/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package bt.bai1ltm;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author Admin
 */
public class Bai1LTM {

    public static void main(String[] args) throws UnknownHostException {
        try {
            //thong tin localhost
            InetAddress localhost = InetAddress.getLocalHost();
            System.out.println("Thông tin local host");
            System.out.println("Host Name: " + localhost.getHostName());
            System.out.println("IP Address: " + localhost.getAddress() );
            //thong tin null host
            InetAddress nullhost = InetAddress.getByName(null);
            System.out.println("\nThông tin null host");
            System.out.println("Adress: "+ nullhost);
            //thon tin dia chi google
            String url = "www.google.com";
            InetAddress internethost = InetAddress.getByName(url);
            System.out.println("\n Thông tin địa chỉ Internet: " + url);
            System.out.println("Hostname: " + internethost.getHostName());
            System.out.println("IP Adress: "+ internethost.getAddress());
            } catch (Exception e) {
                System.err.println("Không tìm thấy thông tin host: " + e.getMessage());
        }
        
    }
}
