package edu.pmdm.gonzalez_victorimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import edu.pmdm.gonzalez_victorimdbapp.database.FavoritesDatabaseHelper;
import edu.pmdm.gonzalez_victorimdbapp.database.FavoritesManager;
import edu.pmdm.gonzalez_victorimdbapp.database.UsersManager;
import edu.pmdm.gonzalez_victorimdbapp.sync.FirebaseFavoritesSync;
import edu.pmdm.gonzalez_victorimdbapp.sync.FirebaseUsersSync;
import edu.pmdm.gonzalez_victorimdbapp.utils.FacebookUtils;

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
    private CallbackManager callbackManager;

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

        // Configurar boton de inicio de sesion Correo
        EditText emailField = findViewById(R.id.emailEditText);
        EditText passwordField = findViewById(R.id.passwordEditText);
        Button loginMailButton = findViewById(R.id.loginButton);

        // Funcion inicio de sesion correo
        loginMailButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (!email.isEmpty() && !password.isEmpty()) {
                signInWithEmailAndPassword(email, password);
            } else {
                Toast.makeText(this, "Por favor, ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show();
            }
        });

        // Configurar botón de inicio de sesión Google
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

        // Configurar CallbackManager de Facebook
        callbackManager = CallbackManager.Factory.create();

        // Configurar LoginButton de Facebook
        LoginButton loginButton = findViewById(R.id.btnFacebook);
        loginButton.setPermissions(Arrays.asList("public_profile", "email"));

        // Registrar la devolución de llamada para el botón
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // Manejar el token de Facebook y autenticar con Firebase
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException exception) {
                Toast.makeText(LoginActivity.this, "Error al iniciar sesión con Facebook", Toast.LENGTH_SHORT).show();
            }
        });

        // Verificar el estado de sesión al iniciar la actividad
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            handleFacebookAccessToken(accessToken);
        }
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

    // Funcion para el usuario con Correo y Password.
    private void signInWithEmailAndPassword(String email, String password) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserToLocalAndRemote(firebaseUser);

                            FirebaseFavoritesSync firebaseFavoritesSync = new FirebaseFavoritesSync();
                            FavoritesManager favoritesManager = new FavoritesManager(this);
                            firebaseFavoritesSync.syncFavoritesWithLocalDatabase(favoritesManager);

                            // Enviar datos del usuario a MainActivity
                            navigateToMainActivity(firebaseUser);
                        }
                    } else {
                        Toast.makeText(this, "Error al iniciar sesión: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToLocalAndRemote(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();

        // Acceder a la base de datos local
        UsersManager usersManager = new UsersManager(this);
        Map<String, String> localUserData = usersManager.getUser(userId);

        // Intentar usar el displayName de Firebase; si es nulo, verificar en la base de datos local
        String name = firebaseUser.getDisplayName();
        if (name == null || name.isEmpty()) {
            if (localUserData != null && localUserData.get(FavoritesDatabaseHelper.COLUMN_NAME) != null
                    && !localUserData.get(FavoritesDatabaseHelper.COLUMN_NAME).isEmpty()) {
                name = localUserData.get(FavoritesDatabaseHelper.COLUMN_NAME);
            } else {
                name = "Usuario";
            }
        }

        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "Correo no disponible";
        String image = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null;

        // Si ya existen datos locales, se recuperan address y phone y se conserva la imagen actual si existe
        String address = "";
        String phone = "";
        if (localUserData != null) {
            address = localUserData.getOrDefault(FavoritesDatabaseHelper.COLUMN_ADDRESS, "");
            phone = localUserData.getOrDefault(FavoritesDatabaseHelper.COLUMN_PHONE, "");
            image = localUserData.getOrDefault(FavoritesDatabaseHelper.COLUMN_IMAGE, image);
        }

        // Si el usuario no existe en la BD local, se añade; si existe, se actualiza
        if (!usersManager.userExists(userId)) {
            usersManager.addUser(userId, name, email, null, null, address, phone, image);
        } else {
            usersManager.updateUser(userId, name, email, null, null, address, phone, image);
        }

        // Sincronizar en Firestore
        FirebaseUsersSync firebaseUsersSync = new FirebaseUsersSync();
        firebaseUsersSync.syncBasicUserToFirestore(userId, name, email, address, phone, image);
    }


    /**
     * Manejar el token de acceso de Facebook para autenticar con Firebase y obtener datos adicionales.
     *
     * @param token Token de acceso de Facebook.
     */
    /**
     * Manejar el token de acceso de Facebook para autenticar con Firebase y obtener datos adicionales.
     *
     * @param token Token de acceso de Facebook.
     */
    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        if (user != null) {
                            UsersManager usersManager = new UsersManager(this);
                            FirebaseUsersSync firebaseUsersSync = new FirebaseUsersSync();

                            // Obtener datos del usuario desde SQLite
                            Map<String, String> userData = usersManager.getUser(user.getUid());

                            String address = userData != null ? userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_ADDRESS, "") : "";
                            String phone = userData != null ? userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_PHONE, "") : "";

                            // Obtener imagen de Facebook directamente
                            String image = "https://graph.facebook.com/" + user.getUid() +
                                    "/picture?type=large&access_token=" + token.getToken();

                            // Guardar usuario en SQLite si no existe
                            if (!usersManager.userExists(user.getUid())) {
                                usersManager.addUser(user.getUid(), user.getDisplayName(), user.getEmail(), null, null, address, phone, image);
                            } else {
                                usersManager.updateUser(user.getUid(), user.getDisplayName(), user.getEmail(), null, null, address, phone, image);
                            }

                            // Sincronizar usuario con Firestore
                            firebaseUsersSync.syncBasicUserToFirestore(user.getUid(), user.getDisplayName(), user.getEmail(), address, phone, image);

                            // Obtener datos de Facebook y actualizar MainActivity
                            FacebookUtils.fetchFacebookUserProfile(token, this);
                        }

                        // Redirigir a MainActivity
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error al autenticar con Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    /**
     * Autentica al usuario en Firebase utilizando las credenciales de Google Sign-In.
     *
     * @param account Cuenta de Google obtenida tras iniciar sesión correctamente.
     */
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

                        if (user != null) {
                            UsersManager usersManager = new UsersManager(this);
                            FirebaseUsersSync firebaseUsersSync = new FirebaseUsersSync();

                            // Obtener datos del usuario desde SQLite
                            Map<String, String> userData = usersManager.getUser(user.getUid());

                            String address = userData != null ? userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_ADDRESS, "") : "";
                            String phone = userData != null ? userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_PHONE, "") : "";
                            String image = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

                            // Guardar usuario en SQLite si no existe
                            if (!usersManager.userExists(user.getUid())) {
                                usersManager.addUser(user.getUid(), user.getDisplayName(), user.getEmail(), null, null, address, phone, image);
                            } else {
                                usersManager.updateUser(user.getUid(), user.getDisplayName(), user.getEmail(), null, null, address, phone, image);
                            }

                            // Sincronizar usuario con Firestore
                            firebaseUsersSync.syncBasicUserToFirestore(user.getUid(), user.getDisplayName(), user.getEmail(), address, phone, image);
                        }

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
        UsersManager usersManager = new UsersManager(this);
        Map<String, String> userData = usersManager.getUser(user.getUid());

        // Si no hay datos en la base de datos, usar valores predeterminados de Firebase
        String userName = (userData != null) ? userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_NAME, user.getDisplayName()) : user.getDisplayName();
        String userEmail = (userData != null) ? userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_EMAIL, user.getEmail()) : user.getEmail();
        String userPhoto = (userData != null) ? userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_IMAGE, (user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null)) : null;

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("USER_NAME", userName);
        intent.putExtra("USER_EMAIL", userEmail);
        intent.putExtra("USER_PHOTO", userPhoto);

        startActivity(intent);
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}