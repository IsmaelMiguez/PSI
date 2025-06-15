package es.udc.psi.pacman;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;

public final class IpDialog {


    public interface OnIpEntered {
        void onIp(String ip);
    }

    private IpDialog() {}   // util class


    public static void show(Context ctx, OnIpEntered cb) {

        final EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint(R.string._192_168_1_100);

        new AlertDialog.Builder(ctx)
                .setTitle(R.string.ip_del_host)
                .setMessage(R.string.introduce_la_direcci_n_ip_de_la_pantalla_principal)
                .setView(input)
                .setPositiveButton(R.string.aceptar, (d, w) -> {
                    String ip = input.getText().toString().trim();
                    if (!ip.isEmpty()) cb.onIp(ip);
                })
                .setNegativeButton(R.string.cancelar, (d, w) -> d.dismiss())
                .show();
    }
}
