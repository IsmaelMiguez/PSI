package es.udc.psi.pacman;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText etUsername, etEmail, etPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Vincular vistas
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Verificar si el usuario ya inició sesión
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Si ya hay sesión iniciada, ir directamente a MainActivity
            startMainActivity();
        }
    }

    public void registerUser(View view) {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validación de campos
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.error_fields_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar barra de progreso
        progressBar.setVisibility(View.VISIBLE);

        // Registrar usuario con email y contraseña
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Registro exitoso
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Actualizar el perfil del usuario con el nombre de usuario
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "User profile updated.");
                                            }
                                        }
                                    });

                            // Guardar información adicional en Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", username);
                            userData.put("email", email);
                            userData.put("highScore", 0);
                            userData.put("isGuest", false);

                            db.collection("users")
                                    .document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User data stored in Firestore"))
                                    .addOnFailureListener(e -> Log.w(TAG, "Error storing user data", e));

                            Toast.makeText(LoginActivity.this, getString(R.string.register_successful),
                                    Toast.LENGTH_SHORT).show();

                            startMainActivity();
                        } else {
                            // Si falla el registro
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, getString(R.string.error_auth_failed) + ": " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }

                        // Ocultar barra de progreso
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    public void loginUser(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validación de campos
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.error_fields_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar barra de progreso
        progressBar.setVisibility(View.VISIBLE);

        // Iniciar sesión con email y contraseña
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Inicio de sesión exitoso
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, getString(R.string.login_successful),
                                    Toast.LENGTH_SHORT).show();
                            startMainActivity();
                        } else {
                            // Si falla el inicio de sesión
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, getString(R.string.error_auth_failed) + ": " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }

                        // Ocultar barra de progreso
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    public void continueAsGuest(View view) {
        // Iniciar sesión como invitado (anónimo)
        progressBar.setVisibility(View.VISIBLE);
    
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Inicio de sesión anónimo exitoso
                            Log.d(TAG, "signInAnonymously:success");
                            FirebaseUser user = mAuth.getCurrentUser();
    
                            // Generar un nombre de usuario para invitado
                            String guestUsername = "Invitado_" + user.getUid().substring(0, 5);
    
                            // Guardar información de invitado en Firestore
                            Map<String, Object> guestData = new HashMap<>();
                            guestData.put("username", guestUsername);
                            guestData.put("isGuest", true);
                            guestData.put("highScore", 0);
    
                            // También actualizar el displayName para acceso inmediato
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(guestUsername)
                                    .build();
    
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "Perfil de invitado actualizado");
                                        }
                                    });
    
                            db.collection("users")
                                    .document(user.getUid())
                                    .set(guestData)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Datos de invitado guardados en Firestore"))
                                    .addOnFailureListener(e -> Log.w(TAG, "Error guardando datos de invitado", e));
    
                            Toast.makeText(LoginActivity.this, getString(R.string.guest_mode_active),
                                    Toast.LENGTH_SHORT).show();
                            startMainActivity();
                        } else {
                            // Si falla el inicio de sesión anónimo
                            Log.w(TAG, "signInAnonymously:failure", task.getException());
                            Toast.makeText(LoginActivity.this, getString(R.string.error_auth_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
    
                        // Ocultar barra de progreso
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Para que el usuario no pueda volver a la pantalla de login con el botón atrás
    }
}