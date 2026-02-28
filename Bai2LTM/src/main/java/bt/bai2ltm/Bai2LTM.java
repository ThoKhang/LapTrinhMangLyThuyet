/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package bt.bai2ltm;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 *
 * @author Admin
 */
public class Bai2LTM {

    public static void main(String[] args) {
        String host;
        System.out.println("Nhap ten Host can tim: ");
        Scanner input = new Scanner(System.in);
        host = input.nextLine();
        try {
            InetAddress[] address = InetAddress.getAllByName(host);
            System.out.println("Ket qua cua website: "+ host);
            for (int i = 0; i< address.length;i++)
            {
                System.out.println("Dia chi "+(i+1) +": " + address[i].getHostAddress());
            }
        } catch (UnknownHostException e) {
            System.err.println("Khong tim thay dia chi website");
        }
        finally{
            input.close();
        }
    }
}
