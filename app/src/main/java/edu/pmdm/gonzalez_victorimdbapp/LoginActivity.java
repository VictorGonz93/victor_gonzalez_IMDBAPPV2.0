package edu.pmdm.gonzalez_victorimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
/**
 * Clase LoginActivity.
 * Maneja el inicio de sesión de los usuarios mediante Google Sign-In y Firebase Authentication.
 * Redirige al usuario a la actividad principal si ya está autenticado.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configurar el logo dinámicamente (opcional)
        ImageView appLogo = findViewById(R.id.app_logo);
        appLogo.setImageResource(R.drawable.app_logo);

        // Configurar Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Verificar si ya hay un usuario autenticado
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Usuario ya autenticado, redirigir a MainActivity
            navigateToMainActivity(currentUser);
            return;
        }

        // Configurar botón de inicio de sesión
        SignInButton signInButton = findViewById(R.id.btnSignIn);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setColorScheme(SignInButton.COLOR_LIGHT);

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Evento de clic para iniciar sesión
        findViewById(R.id.btnSignIn).setOnClickListener(v -> {
            Intent signInIntent = GoogleSignIn.getClient(this, gso).getSignInIntent();
            signInLauncher.launch(signInIntent);
        });
    }

    /**
     * Lanzador para manejar el resultado de Google Sign-In.
     * Procesa el resultado y autentica al usuario en Firebase si el inicio de sesión fue exitoso.
     */
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        Log.w("LoginActivity", "Google sign in failed", e);
                    }
                }
            }
    );

    /**
     * Autentica al usuario en Firebase utilizando las credenciales de Google Sign-In.
     *
     * @param account Cuenta de Google obtenida tras iniciar sesión correctamente.
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        navigateToMainActivity(user);
                    } else {
                        Log.w("LoginActivity", "signInWithCredential:failure", task.getException());
                    }
                });
    }

    /**
     * Navega a la actividad principal pasando la información del usuario autenticado.
     *
     * @param user Objeto FirebaseUser con los datos del usuario autenticado.
     */
    private void navigateToMainActivity(FirebaseUser user) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("USER_NAME", user.getDisplayName());
        intent.putExtra("USER_EMAIL", user.getEmail());
        intent.putExtra("USER_PHOTO", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        Log.d("LoginActivity", "Photo URL: " + user.getPhotoUrl());
        startActivity(intent);
        finish();
    }

}

