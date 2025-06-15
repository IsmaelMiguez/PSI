package es.udc.psi.pacman;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class Controller extends AppCompatActivity {

    private UdpClient net;    private int myId = -1;

    @Override protected void onCreate(Bundle s){
        super.onCreate(s); setContentView(R.layout.activity_controller);

        String ipRaw = getIntent().getStringExtra("HOST_IP");
        String ip = ipRaw.split(":")[0];
        Toast.makeText(this,"Conectando a "+ip+"…",Toast.LENGTH_SHORT).show();

        try {
            net = new UdpClient(ip, id -> runOnUiThread(() -> {
                myId = id;
                Toast.makeText(this,"Soy jugador "+id,Toast.LENGTH_SHORT).show();
            }));
        } catch (Exception e){
            Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
            finish(); return;
        }

        findViewById(R.id.btnUp)   .setOnClickListener(v->net.sendDir(myId,0));
        findViewById(R.id.btnRight).setOnClickListener(v->net.sendDir(myId,1));
        findViewById(R.id.btnDown) .setOnClickListener(v->net.sendDir(myId,2));
        findViewById(R.id.btnLeft) .setOnClickListener(v->net.sendDir(myId,3));
    }
    @Override protected void onDestroy(){ if(net!=null) net.close(); super.onDestroy();}
}
