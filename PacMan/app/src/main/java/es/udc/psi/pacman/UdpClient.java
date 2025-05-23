package es.udc.psi.pacman;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class UdpClient {

    private final InetAddress host;
    private final int port = UdpServerService.PORT;
    private final DatagramSocket sock;

    public UdpClient(String hostIp) throws Exception {
        host = InetAddress.getByName(hostIp);
        sock = new DatagramSocket();
        send("JOIN");
    }

    public void sendDir(int playerId,int dir) {
        send("DIR:"+playerId+":"+dir);
    }

    private void send(String s) {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        DatagramPacket p = new DatagramPacket(data,data.length,host,port);
        new Thread(() -> { try { sock.send(p);} catch (Exception ignored){} })
                .start();
    }
    public void close() { sock.close(); }
}
