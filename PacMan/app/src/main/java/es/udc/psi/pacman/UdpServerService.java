package es.udc.psi.pacman;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UdpServerService extends Service implements Runnable {

    public static final int PORT = 55555;
    private DatagramSocket socket;
    private Thread thread;
    private final Map<InetAddress,Integer> players = new HashMap<>();

    @Override public void onCreate() {
        super.onCreate();
        try {
            socket = new DatagramSocket(PORT);
            socket.setReuseAddress(true);
            thread = new Thread(this);
            thread.start();
            Log.i("PACMAN_NET","[SERVER] Iniciado en puerto "+PORT);
        } catch(Exception e){
            Log.e("PACMAN_NET","[SERVER] NO se pudo abrir el puerto",e);
            stopSelf();
        }
    }

    @Override public void run() {
        byte[] buf = new byte[32];
        DatagramPacket p = new DatagramPacket(buf, buf.length);

        while(!socket.isClosed()) {
            try {
                socket.receive(p);
                String msg = new String(p.getData(),0,p.getLength());
                InetAddress ip = p.getAddress();
                Log.i("PACMAN_NET","[SERVER] "+ip+" -> "+msg);

                if("JOIN".equals(msg)) {
                    int id = players.computeIfAbsent(ip, k->players.size());
                    send("ID:"+id,ip,p.getPort());
                    Log.i("PACMAN_NET","[SERVER] Asignado ID "+id+" a "+ip);

                    // notifica a la UI para redimensionar arrays
                    ExtendedPlayActivity act = ExtendedPlayActivity.get();
                    if(act!=null) act.runOnUiThread(
                            ()-> act.onPlayersChanged(players.size()));
                    continue;
                }
                if(msg.startsWith("DIR:")) {            // formato DIR:id:dir
                    String[] parts = msg.split(":");
                    int player = Integer.parseInt(parts[1]);
                    int dir    = Integer.parseInt(parts[2]);
                    ExtendedPlayActivity act = ExtendedPlayActivity.get();
                    if(act!=null) act.onRemoteDirection(player,dir);
                }
            } catch(Exception e){
                Log.e("PACMAN_NET","[SERVER] Error recibiendo",e);
            }
        }
    }

    private void send(String s,InetAddress ip,int port){
        try{
            byte[] data = s.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data,data.length,ip,port));
            Log.i("PACMAN_NET","[SERVER] --> "+ip+" : "+s);
        }catch(Exception e){ Log.e("PACMAN_NET","[SERVER] Error enviando",e);}
    }

    @Override public void onDestroy() {
        socket.close();
        Log.i("PACMAN_NET","[SERVER] Cerrado");
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){ return null; }
}
