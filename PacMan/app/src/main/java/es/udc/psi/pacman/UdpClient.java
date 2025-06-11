package es.udc.psi.pacman;

import android.util.Log;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UdpClient {

    private final InetAddress host;
    private final int port = UdpServerService.PORT;
    private final DatagramSocket sock;

    public interface OnIdListener { void onId(int id); }

    public UdpClient(String hostIp, OnIdListener cb) throws Exception {
        host = InetAddress.getByName(hostIp);
        sock = new DatagramSocket();
        Log.i("PACMAN_NET","[CLIENT] Socket "+sock.getLocalPort());
        send("JOIN");

        // escucha respuesta ID
        new Thread(() -> {
            try {
                byte[] buf = new byte[32];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                while (!sock.isClosed()) {
                    sock.receive(p);
                    String s = new String(p.getData(), 0, p.getLength());
                    Log.i("PACMAN_NET","[CLIENT] <-- "+s);
                    if (s.startsWith("ID:")) {
                        int id = Integer.parseInt(s.substring(3).trim());
                        cb.onId(id);
                    }
                }
            } catch (Exception e) {
                Log.e("PACMAN_NET","[CLIENT] Hilo recepción",e);
            }
        }).start();
    }

    public void sendDir(int playerId,int dir) { send("DIR:"+playerId+":"+dir); }

    private void send(String s) {
        byte[] d = s.getBytes(StandardCharsets.UTF_8);
        DatagramPacket p = new DatagramPacket(d,d.length,host,port);
        new Thread(() -> { try {
            sock.send(p); Log.i("PACMAN_NET","[CLIENT] --> "+s);
        } catch (Exception e){Log.e("PACMAN_NET","[CLIENT] send()",e);} }).start();
    }
    public void close(){ sock.close(); }
}
