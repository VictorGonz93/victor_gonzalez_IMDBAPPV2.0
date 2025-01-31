package edu.pmdm.gonzalez_victorimdbapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.Map;

import edu.pmdm.gonzalez_victorimdbapp.database.FavoritesDatabaseHelper;
import edu.pmdm.gonzalez_victorimdbapp.database.UsersManager;
import edu.pmdm.gonzalez_victorimdbapp.sync.FirebaseUsersSync;

/**
 * Clase EditUserActivity.
 * Permite al usuario editar su información (nombre, dirección, teléfono e imagen).
 */
public class EditUserActivity extends AppCompatActivity {

    private EditText editName, editEmail, editAddress, editPhone;
    private ImageView profileImageView;
    private String userId;
    private UsersManager usersManager;
    private FirebaseUsersSync firebaseUsersSync;
    private Uri selectedImageUri; // 📌 Guarda la imagen seleccionada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        // Referencias UI
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editAddress = findViewById(R.id.editAddress);
        editPhone = findViewById(R.id.editPhone);
        profileImageView = findViewById(R.id.profileImageView);
        Button btnSelectAddress = findViewById(R.id.btnSelectAddress);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnSelectImage = findViewById(R.id.btnSelectImage); // 📌 Botón para imagen

        // Inicializar base de datos y Firestore Sync
        usersManager = new UsersManager(this);
        firebaseUsersSync = new FirebaseUsersSync();

        // Obtener usuario autenticado de Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            String userEmail = currentUser.getEmail();
            String userName = currentUser.getDisplayName();
            String userPhoto = (currentUser.getPhotoUrl() != null) ? currentUser.getPhotoUrl().toString() : null;

            // Obtener datos del usuario desde SQLite
            Map<String, String> userData = usersManager.getUser(userId);
            if (userData != null) {
                editName.setText(userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_NAME, userName));
                editEmail.setText(userEmail); // Mostrar el email, aunque no es editable
                editAddress.setText(userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_ADDRESS, ""));
                editPhone.setText(userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_PHONE, ""));

                // Deshabilitar la edición del email
                editEmail.setFocusable(false);
                editEmail.setClickable(false);

                // Cargar imagen si existe
                String storedImageUri = userData.get(FavoritesDatabaseHelper.COLUMN_IMAGE);
                if (storedImageUri != null && !storedImageUri.isEmpty()) {
                    Picasso.get().load(storedImageUri).into(profileImageView);
                } else if (userPhoto != null) {
                    Picasso.get().load(userPhoto).into(profileImageView);
                }
            }
        }

        // Botón para seleccionar dirección con Google Places
        btnSelectAddress.setOnClickListener(v -> {
            Intent intent = new Intent(EditUserActivity.this, SelectAddressActivity.class);
            selectAddressLauncher.launch(intent);
        });

        // 📌 Botón para seleccionar imagen
        btnSelectImage.setOnClickListener(v -> checkPermissionsAndOpenGallery());

        // Botón para guardar cambios
        btnSave.setOnClickListener(v -> saveUserData());
    }

    /**
     * Launcher para obtener la dirección seleccionada desde SelectAddressActivity.
     */
    private final ActivityResultLauncher<Intent> selectAddressLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String selectedAddress = result.getData().getStringExtra("SELECTED_ADDRESS");
                            editAddress.setText(selectedAddress);
                        }
                    });

    /**
     * Guarda los cambios del usuario en la base de datos local y Firestore.
     */
    private void saveUserData() {
        String updatedName = editName.getText().toString().trim();
        String updatedAddress = editAddress.getText().toString().trim();
        String updatedPhone = editPhone.getText().toString().trim();
        String updatedImage = (selectedImageUri != null) ? selectedImageUri.toString() : null;

        if (userId != null) {
            // Actualizar en SQLite
            usersManager.updateUser(userId, updatedName, editEmail.getText().toString(), null, null, updatedAddress, updatedPhone, updatedImage);

            // Actualizar en Firestore (solo los campos básicos)
            firebaseUsersSync.syncBasicUserToFirestore(userId, updatedName, editEmail.getText().toString());

            // Enviar los datos actualizados de vuelta a MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("UPDATED_NAME", updatedName);
            resultIntent.putExtra("UPDATED_IMAGE", updatedImage);
            setResult(RESULT_OK, resultIntent);

            // Cerrar la actividad
            finish();
        } else {
            Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Verifica permisos y abre la galería.
     */
    private void checkPermissionsAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else { // Android 12 o inferior
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    Toast.makeText(this, "Permiso denegado. No se puede acceder a la galería.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    profileImageView.setImageURI(selectedImageUri);
                }
            }
    );

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
}
