package edu.pmdm.gonzalez_victorimdbapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import edu.pmdm.gonzalez_victorimdbapp.database.FavoritesDatabaseHelper;
import edu.pmdm.gonzalez_victorimdbapp.database.FavoritesManager;
import edu.pmdm.gonzalez_victorimdbapp.database.UsersManager;
import edu.pmdm.gonzalez_victorimdbapp.databinding.ActivityMainBinding;
import edu.pmdm.gonzalez_victorimdbapp.sync.FirebaseFavoritesSync;
import edu.pmdm.gonzalez_victorimdbapp.sync.FirebaseUsersSync;
import edu.pmdm.gonzalez_victorimdbapp.utils.AppLifecycleManager;
import edu.pmdm.gonzalez_victorimdbapp.utils.FacebookUtils;

/**
 * Clase MainActivity: Contenedor principal de la aplicación con navegación
 * Maneja autenticación, sincronización de datos y cierre de sesión.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseUsersSync firebaseUsersSync;
    private AtomicReference<UsersManager> usersManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(new AppLifecycleManager(this));
        }

        // Inicialización de bases de datos
        FavoritesManager favoritesManager = new FavoritesManager(this);
        usersManager = new AtomicReference<>(new UsersManager(this));

        // Sincronización de favoritos con Firestore
        FirebaseFavoritesSync firebaseFavoritesSync = new FirebaseFavoritesSync();
        firebaseFavoritesSync.syncFavoritesWithLocalDatabase(favoritesManager);

        // Configuración de la vista con Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        // Sincronizar usuarios con Firestore
        firebaseUsersSync = new FirebaseUsersSync();
        firebaseUsersSync.syncUsersWithFirestore(usersManager.get());

        // Configuración de DrawerLayout y NavigationView
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Recoger datos del Intent si vienen de LoginActivity
        String userName = getIntent().getStringExtra("USER_NAME");
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        String userPhoto = getIntent().getStringExtra("USER_PHOTO");

        // Si los datos vienen vacíos, buscar en la base de datos local
        if (userName == null || userEmail == null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Map<String, String> userData = usersManager.get().getUser(currentUser.getUid());
                if (userData != null) {
                    userName = userData.get(FavoritesDatabaseHelper.COLUMN_NAME);
                    userEmail = userData.get(FavoritesDatabaseHelper.COLUMN_EMAIL);
                    userPhoto = userData.get(FavoritesDatabaseHelper.COLUMN_IMAGE);
                }
            }
        }

        // Actualizar la UI con la información obtenida
        if (userName != null && userEmail != null) {
            updateUserInfo(userName, userEmail, userPhoto);
        }

        // Configurar botón de logout
        setupLogoutButton();
    }


    /**
     * Maneja la sesión del usuario y actualiza la UI según el proveedor de autenticación.
     *
     * @param currentUser Usuario autenticado en Firebase.
     */
    private void handleUserSession(FirebaseUser currentUser) {
        String userEmail = currentUser.getEmail();
        String userName = currentUser.getDisplayName();
        String userPhoto = (currentUser.getPhotoUrl() != null) ? currentUser.getPhotoUrl().toString() : null;

        // Si el usuario es de Facebook, obtener la imagen con su token
        if ("facebook.com".equals(currentUser.getProviderData().get(1).getProviderId())) {
            AccessToken token = AccessToken.getCurrentAccessToken();
            if (token != null && !token.isExpired()) {
                FacebookUtils.fetchFacebookUserProfile(token, this);
                return; // Detener ejecución, la imagen será cargada desde `FacebookUtils`
            }
        }

        // Si el usuario es de Google o Email, actualizar la UI directamente
        if (userName != null && userEmail != null) {
            updateUserInfo(userName, userEmail, userPhoto);
        }
    }

    /**
     * Configura el botón de logout en el NavigationView.
     */
    private void setupLogoutButton() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        Button logoutButton = headerView.findViewById(R.id.btnLogout);

        logoutButton.setOnClickListener(v -> {
            FirebaseUser currentUserLogout = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUserLogout != null) {
                String userId = currentUserLogout.getUid();
                String logoutTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // Actualizar logout en la base de datos local y Firestore
                usersManager.get().updateLogoutTime(userId, logoutTime);
                firebaseUsersSync.updateLogoutTime();
            }

            // Cerrar sesión en Firebase y proveedores externos
            logoutFromAllServices();
        });
    }

    /**
     * Cierra la sesión del usuario en Firebase, Google y Facebook.
     */
    private void logoutFromAllServices() {
        FirebaseAuth.getInstance().signOut();

        // Cerrar sesión en Google
        GoogleSignIn.getClient(
                this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        ).signOut();

        // Cerrar sesión en Facebook
        LoginManager.getInstance().logOut();

        // Redirigir a LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Actualiza la información del usuario en el Navigation Drawer (nav_header_main.xml).
     *
     * @param userName  Nombre del usuario.
     * @param userEmail Correo electrónico del usuario.
     * @param userPhoto URL de la foto de perfil del usuario.
     */
    public void updateUserInfo(String userName, String userEmail, String userPhoto) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        TextView userNameTextView = headerView.findViewById(R.id.user_name);
        TextView userEmailTextView = headerView.findViewById(R.id.user_email);
        ImageView userImageView = headerView.findViewById(R.id.imageView);

        userNameTextView.setText(userName);
        userEmailTextView.setText(userEmail);

        if (userPhoto != null) {
            // Cargar la imagen de perfil con Picasso
            Picasso.get()
                    .load(userPhoto)
                    .placeholder(R.drawable.default_user_image) // Imagen predeterminada mientras carga
                    .error(R.drawable.default_user_image) // Imagen predeterminada en caso de error
                    .into(userImageView);
        } else {
            // Usar una imagen predeterminada si no hay foto
            userImageView.setImageResource(R.drawable.default_user_image);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.edit_user) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(this, EditUserActivity.class);
                intent.putExtra("USER_ID", currentUser.getUid());
                intent.putExtra("USER_NAME", currentUser.getDisplayName());
                intent.putExtra("USER_EMAIL", currentUser.getEmail());

                // Obtener datos desde SQLite
                UsersManager usersManager = new UsersManager(this);
                Map<String, String> userData = usersManager.getUser(currentUser.getUid());
                if (userData != null) {
                    intent.putExtra("USER_ADDRESS", userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_ADDRESS, ""));
                    intent.putExtra("USER_PHONE", userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_PHONE, ""));
                }

                // Lanzar EditUserActivity y esperar resultado
                editUserLauncher.launch(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private final ActivityResultLauncher<Intent> editUserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String updatedName = result.getData().getStringExtra("UPDATED_NAME");

                    // Actualizar los datos en el Navigation Drawer
                    updateUserInfo(updatedName, FirebaseAuth.getInstance().getCurrentUser().getEmail(), null);

                    // Opcional: Mostrar un mensaje de éxito
                    Toast.makeText(MainActivity.this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
