package es.udc.psi.pacman;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Controller extends AppCompatActivity {

    private UdpClient net;
    private int myId = 0;   // 0 ó 1; opcional: pide al usuario

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_controller);

        String hostIp = getIntent().getStringExtra("HOST_IP");
        try { net = new UdpClient(hostIp); } catch (Exception e){ finish(); }

        findViewById(R.id.btnUp)   .setOnClickListener(v->net.sendDir(myId,0));
        findViewById(R.id.btnRight).setOnClickListener(v->net.sendDir(myId,1));
        findViewById(R.id.btnDown) .setOnClickListener(v->net.sendDir(myId,2));
        findViewById(R.id.btnLeft) .setOnClickListener(v->net.sendDir(myId,3));
    }

    @Override protected void onDestroy() {
        if(net!=null) net.close();
        super.onDestroy();
    }
}
