/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package bt.bai3ltm;

import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

/**
 *
 * @author Admin
 */
public class Bai3LTM {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("Nhập địa chỉ URL cần lấy Header: ");
        String urlString = input.nextLine();
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            System.out.println("Thong tin website");
            for(int i = 0; ; i++)
            {
                String headerKey = connection.getHeaderFieldKey(i);
                String headerValue = connection.getHeaderField(i);
                if( headerKey==null && headerValue == null )
                {
                    break;
                }
                //key cua dong dau tien HTTP thuong la null
                if(headerKey==null)
                    System.out.println("Trang thai: "+headerValue);
                else
                    System.out.println(headerKey+":"+headerValue);
            }
        } catch (Exception e) {
            System.err.println("Lỗi: " + e.getMessage());
            System.out.println("Lưu ý: Hãy nhập đầy đủ 'https://' trước tên miền.");
        }
        finally{
            input.close();
        }
    }
}
