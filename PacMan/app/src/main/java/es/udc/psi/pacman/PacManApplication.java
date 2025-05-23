package es.udc.psi.pacman;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class PacManApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Inicializar Firebase
        FirebaseApp.initializeApp(this);
    }
}