package es.udc.psi.pacman;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class UdpServerService extends Service implements Runnable {

    public static final int PORT = 55555;
    private DatagramSocket socket;
    private final Map<InetAddress,Integer> players = new HashMap<>();
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override public int onStartCommand(Intent i,int f,int id) {
        new Thread(this).start();
        return START_STICKY;
    }

    @Override public void run() {
        try {
            socket = new DatagramSocket(PORT);
            byte[] buf = new byte[64];
            DatagramPacket p = new DatagramPacket(buf, buf.length);

            while (!socket.isClosed()) {
                socket.receive(p);
                InetAddress ip = p.getAddress();
                String msg = new String(p.getData(),0,p.getLength());

                if (msg.equals("JOIN")) {
                    if (!players.containsKey(ip) && players.size()<2)
                        players.put(ip, players.size());          // 0 ó 1
                    continue;
                }
                if (msg.startsWith("DIR:")) {
                    String[] s = msg.split(":");
                    int player = Integer.parseInt(s[1]);
                    int dir    = Integer.parseInt(s[2]);
                    ui.post(() -> ExtendedPlayActivity.get()
                            .onRemoteDirection(player, dir));
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override public void onDestroy() {
        if (socket!=null) socket.close();
    }
    @Override public IBinder onBind(Intent i){ return null; }
}
