/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package bt.bai4ltm;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

/**
 *
 * @author Admin
 */
public class Bai4LTM {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Nhập địa chỉ URL (phải có https://): ");
        String urlString = scanner.nextLine();
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            Object content = url.getContent();
            System.out.println("Loại đối tượng nội dung: " + content.getClass().getName());
            System.out.println("--- Bắt đầu đọc nội dung trang web ---");
            System.out.println("---------------------------------------");
            InputStream is = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            reader.close();
        } catch (Exception e) {
            System.err.println("Lỗi: " + e.getMessage());
            System.out.println("Đảm bảo bạn nhập đúng định dạng (VD: https://www.google.com)");
        }
        finally{
            scanner.close();
        }
    }
}
