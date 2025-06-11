package es.udc.psi.pacman;

import android.content.Context;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

// NetUtils.java (nueva clase util)
public final class NetUtils {
    public static String getWifiIPv4(Context ctx) {
        try {
            for (NetworkInterface ni :
                    Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address)
                        return addr.getHostAddress();
                }
            }
        } catch (SocketException ignored) {}
        return "0.0.0.0";
    }
}
