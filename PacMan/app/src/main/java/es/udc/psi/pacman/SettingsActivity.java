package es.udc.psi.pacman;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private TextView tvUserInfo;
    private Button btnLogout, btnDeleteAccount, btnBack;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Verificar si hay usuario logueado
        if (mAuth.getCurrentUser() == null) {
            // Redirigir al login si no hay usuario
            startLoginActivity();
            return;
        }

        // Vincular vistas
        tvUserInfo = findViewById(R.id.tvUserInfo);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        // Configurar listeners
        btnLogout.setOnClickListener(v -> logoutUser());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
        btnBack.setOnClickListener(v -> finish());

        // Mostrar información del usuario
        displayUserInfo();
    }

    private void displayUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.isAnonymous()) {
                // Si es usuario invitado
                tvUserInfo.setText(getString(R.string.guest_user_info));
            } else {
                // Si es usuario registrado
                String displayName = user.getDisplayName();
                String email = user.getEmail();

                tvUserInfo.setText(getString(R.string.user_info, displayName, email));
            }
        }
    }

    private void logoutUser() {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signOut();
        Toast.makeText(this, getString(R.string.logged_out), Toast.LENGTH_SHORT).show();
        startLoginActivity();
    }

    private void showDeleteAccountConfirmation() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_account));
        builder.setMessage(getString(R.string.confirm_delete_account));

        // Si es un usuario por email, solicitar reautenticación
        if (!user.isAnonymous()) {
            builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                showReauthenticateDialog();
            });
        } else {
            // Para usuarios invitados, eliminar directamente
            builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                deleteAccount();
            });
        }

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.cancel();
        });

        builder.show();
    }

    private void showReauthenticateDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.reauth_required));

        // Configurar un campo para la contraseña
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(getString(R.string.password));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
            String password = input.getText().toString().trim();
            if (!password.isEmpty()) {
                reauthenticateUser(user.getEmail(), password);
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.cancel();
        });

        builder.show();
    }

    private void reauthenticateUser(String email, String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Reauntenticación exitosa, proceder a eliminar la cuenta
                        deleteAccount();
                    } else {
                        // Error en la reautenticación
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, getString(R.string.error_auth_failed) + ": " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

private void deleteAccount() {
    FirebaseUser user = mAuth.getCurrentUser();
    if (user == null) return;

    progressBar.setVisibility(View.VISIBLE);

    // Eliminar primero la cuenta de autenticación
    user.delete()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    //  Eliminar los datos de Firestore después de eliminar la autenticación
                    // (Esto puede fallar si las reglas no lo permiten, pero el usuario ya estará eliminado)
                    try {
                        db.collection("users").document(user.getUid())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User data deleted from Firestore");
                                })
                                .addOnFailureListener(e -> {
                                    // Esto podría fallar, pero no bloqueamos el proceso
                                    Log.w(TAG, "Error deleting user data", e);
                                });
                    } catch (Exception e) {
                        Log.w(TAG, "Error trying to delete user data", e);
                    }

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SettingsActivity.this,
                            getString(R.string.account_deleted),
                            Toast.LENGTH_SHORT).show();
                    startLoginActivity();
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SettingsActivity.this,
                            getString(R.string.error_delete_account) + ": " +
                                    task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
}
    private void startLoginActivity() {
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}