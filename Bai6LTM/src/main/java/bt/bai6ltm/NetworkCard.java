/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
public class NetworkCard {
    private NetworkInterface netInterface;
    public NetworkCard (NetworkInterface netInterface )
    {
        this.netInterface = netInterface;
    }
    public void displayInfo () throws SocketException{
        System.out.println("\n[ Card: " + netInterface.getDisplayName() + " ]");
        System.out.println(" - System Name: " + netInterface.getName());
        //su dung getSubInterface
        Enumeration<NetworkInterface> subIfs = netInterface.getSubInterfaces();
        if (subIfs.hasMoreElements()) {
            System.out.println(" - Sub-Interfaces:");
            // Chuyển Enumeration sang List để duyệt theo phong cách OOP hiện đại
            Collections.list(subIfs).forEach(sub -> {
                System.out.println("   +> " + sub.getName() + " (" + sub.getDisplayName() + ")");
            });
        } else {
            System.out.println(" - Status: No sub-interfaces found.");
        }
    }
}
