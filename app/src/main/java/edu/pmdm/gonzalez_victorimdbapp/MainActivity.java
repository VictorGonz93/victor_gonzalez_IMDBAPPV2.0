package edu.pmdm.gonzalez_victorimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import edu.pmdm.gonzalez_victorimdbapp.databinding.ActivityMainBinding;
import com.squareup.picasso.Picasso;

/**
 * Clase MainActivity.
 * Actúa como el contenedor principal de la aplicación con un DrawerLayout
 * y un NavigationView para navegar entre diferentes fragmentos.
 * Maneja la autenticación del usuario y permite el cierre de sesión.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        setSupportActionBar(binding.appBarMain.toolbar);

        String userName = getIntent().getStringExtra("USER_NAME");
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        String userPhoto = getIntent().getStringExtra("USER_PHOTO");

        if (userName != null && userEmail != null) {
            updateUserInfo(userName, userEmail, userPhoto);
        }

        /**
         * Configura el botón de logout en el encabezado del NavigationView.
         * Cierra la sesión del usuario tanto en Firebase como en Google, y redirige a LoginActivity.
         */
        navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        Button logoutButton = headerView.findViewById(R.id.btnLogout);

        logoutButton.setOnClickListener(v -> {
            // Cerrar sesión de Firebase
            FirebaseAuth.getInstance().signOut();

            // Cerrar sesión de Google
            GoogleSignIn.getClient(
                    this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut();

            // Redirigir a LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Actualiza la información del usuario en el encabezado del DrawerLayout.
     * Muestra el nombre, correo y foto de perfil del usuario autenticado.
     *
     * @param userName  Nombre del usuario.
     * @param userEmail Correo electrónico del usuario.
     * @param userPhoto URL de la foto de perfil del usuario.
     */
    private void updateUserInfo(String userName, String userEmail, String userPhoto) {
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
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

}