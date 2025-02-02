package edu.pmdm.gonzalez_victorimdbapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.PhoneNumberUtils;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.squareup.picasso.Picasso;
import com.hbb20.CountryCodePicker;

import java.util.Map;

import edu.pmdm.gonzalez_victorimdbapp.database.FavoritesDatabaseHelper;
import edu.pmdm.gonzalez_victorimdbapp.database.UsersManager;
import edu.pmdm.gonzalez_victorimdbapp.sync.FirebaseUsersSync;
import edu.pmdm.gonzalez_victorimdbapp.utils.FacebookUtils;
import edu.pmdm.gonzalez_victorimdbapp.utils.KeystoreManager;

public class EditUserActivity extends AppCompatActivity {

    private EditText editName, editEmail, editAddress, editPhone;
    private ImageView profileImageView;
    private String userId;
    private UsersManager usersManager;
    private FirebaseUsersSync firebaseUsersSync;
    private Uri selectedImageUri;
    private CountryCodePicker ccp;

    // Launchers para cámara y galería
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    // Launchers para permisos (uno para cámara, otro para galería)
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    // Launcher para solicitar permisos genéricos (se usa para ambas en este ejemplo)
    // Si lo prefieres, puedes definir launchers separados.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        // Referencias UI
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editAddress = findViewById(R.id.editAddress);
        ccp = findViewById(R.id.ccp);
        editPhone = findViewById(R.id.editPhone);
        // Vincular el EditText con el CCP para que se edite solo la parte local
        ccp.registerCarrierNumberEditText(editPhone);
        profileImageView = findViewById(R.id.profileImageView);
        Button btnSelectAddress = findViewById(R.id.btnSelectAddress);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnSelectImage = findViewById(R.id.btnSelectImage);

        // Ajustar InputFilter para el campo teléfono según el país seleccionado
        updatePhoneInputFilter();
        ccp.setOnCountryChangeListener(() -> updatePhoneInputFilter());

        // Inicializar base de datos y Firestore Sync
        usersManager = new UsersManager(this);
        firebaseUsersSync = new FirebaseUsersSync();

        // Inicializar los launchers para cámara y galería
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            // Obtén el thumbnail (un Bitmap) de la imagen capturada
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            if (imageBitmap != null) {
                                // Muestra el thumbnail en el ImageView
                                profileImageView.setImageBitmap(imageBitmap);
                                // Opcional: Convierte el Bitmap a URI para usarlo posteriormente
                                String path = MediaStore.Images.Media.insertImage(getContentResolver(), imageBitmap, "CapturedImage", null);
                                if (path != null) {
                                    selectedImageUri = Uri.parse(path);
                                }
                            }
                        }
                    }
                }
        );



        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        profileImageView.setImageURI(selectedImageUri);
                    }
                }
        );

        // Inicializar launchers de permisos:
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Permiso para usar la cámara denegado.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Permiso para acceder a la galería denegado.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Obtener usuario autenticado y cargar datos
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            String userEmail = currentUser.getEmail();
            String userName = currentUser.getDisplayName();
            String userPhoto = (currentUser.getPhotoUrl() != null) ? currentUser.getPhotoUrl().toString() : null;

            Map<String, String> userData = usersManager.getUser(userId);
            if (userData != null) {
                editName.setText(userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_NAME, userName));
                editEmail.setText(userEmail); // Solo lectura
                // Para dirección y teléfono, se espera que estén encriptados.
                String encryptedAddress = userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_ADDRESS, "");
                String encryptedPhone = userData.getOrDefault(FavoritesDatabaseHelper.COLUMN_PHONE, "");

                String decryptedAddress = encryptedAddress;
                String decryptedPhone = encryptedPhone;
                try {
                    if (encryptedAddress != null && !encryptedAddress.isEmpty()) {
                        decryptedAddress = new KeystoreManager().decryptData(encryptedAddress);
                    }
                    if (encryptedPhone != null && !encryptedPhone.isEmpty()) {
                        decryptedPhone = new KeystoreManager().decryptData(encryptedPhone);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Si ocurre un error, se deja el valor original (posiblemente vacío)
                }
                editAddress.setText(decryptedAddress);

                // Configurar el CountryCodePicker con el número completo desencriptado (si existe)
                if (decryptedPhone != null && !decryptedPhone.isEmpty()) {
                    ccp.setFullNumber(decryptedPhone);
                }

                // Deshabilitar edición del email
                editEmail.setFocusable(false);
                editEmail.setClickable(false);

                // Cargar imagen del usuario (como ya tienes implementado)
                String storedImageUri = userData.get(FavoritesDatabaseHelper.COLUMN_IMAGE);
                if (storedImageUri != null && !storedImageUri.isEmpty()) {
                    Picasso.get().load(storedImageUri)
                            .placeholder(R.drawable.default_user_image)
                            .error(R.drawable.default_user_image)
                            .into(profileImageView);
                } else {
                    currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null && currentUser.getProviderData().size() > 1 &&
                            "facebook.com".equals(currentUser.getProviderData().get(1).getProviderId())) {
                        String fbImageUrl = FacebookUtils.getFacebookProfileImageUrl(200, 200);
                        if (fbImageUrl != null && !fbImageUrl.isEmpty()) {
                            Picasso.get().load(fbImageUrl)
                                    .placeholder(R.drawable.default_user_image)
                                    .error(R.drawable.default_user_image)
                                    .into(profileImageView);
                        }
                    } else if (userPhoto != null) {
                        Picasso.get().load(userPhoto)
                                .placeholder(R.drawable.default_user_image)
                                .error(R.drawable.default_user_image)
                                .into(profileImageView);
                    }
                }
            }
        }


        // Botón para seleccionar dirección
        btnSelectAddress.setOnClickListener(v -> {
            Intent intent = new Intent(EditUserActivity.this, SelectAddressActivity.class);
            selectAddressLauncher.launch(intent);
        });

        // Botón para seleccionar imagen: muestra el AlertDialog
        btnSelectImage.setOnClickListener(v -> showImageSourceDialog());

        // Botón para guardar cambios
        btnSave.setOnClickListener(v -> saveUserData());
    }

    // Método para mostrar el AlertDialog de selección de imagen
    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen")
                .setItems(new CharSequence[]{"Cámara", "Galería"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // Solicitar permiso para la cámara
                            if (ContextCompat.checkSelfPermission(EditUserActivity.this, Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED) {
                                openCamera();
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                            }
                        } else if (which == 1) {
                            // Solicitar permiso para la galería
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(EditUserActivity.this, Manifest.permission.READ_MEDIA_IMAGES)
                                        == PackageManager.PERMISSION_GRANTED) {
                                    openGallery();
                                } else {
                                    galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                                }
                            } else {
                                if (ContextCompat.checkSelfPermission(EditUserActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                        == PackageManager.PERMISSION_GRANTED) {
                                    openGallery();
                                } else {
                                    galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                                }
                            }
                        }
                    }
                });
        builder.create().show();
    }

    // Método para abrir la cámara
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);

    }



    // Método para abrir la galería
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // Launcher para seleccionar dirección (ya implementado)
    private final ActivityResultLauncher<Intent> selectAddressLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String selectedAddress = result.getData().getStringExtra("SELECTED_ADDRESS");
                            editAddress.setText(selectedAddress);
                        }
                    }
            );

    // Actualiza el InputFilter del campo teléfono según el país seleccionado usando libphonenumber
    private void updatePhoneInputFilter() {
        String countryCode = ccp.getSelectedCountryNameCode();  // Ej: "ES"
        int expectedDigits = getExpectedPhoneNumberLength(countryCode);
        editPhone.setFilters(new InputFilter[]{ new DigitsOnlyInputFilter(expectedDigits) });
    }

    // Usa libphonenumber para obtener la longitud esperada del número nacional para el país
    private int getExpectedPhoneNumberLength(String countryCode) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber exampleNumber = phoneUtil.getExampleNumber(countryCode);
            if (exampleNumber != null) {
                return String.valueOf(exampleNumber.getNationalNumber()).length();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 10; // Valor por defecto
    }

    // Filtro personalizado que cuenta solo dígitos (ignora espacios y otros caracteres)
    private class DigitsOnlyInputFilter implements InputFilter {
        private int maxDigits;
        public DigitsOnlyInputFilter(int maxDigits) {
            this.maxDigits = maxDigits;
        }
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            String newText = dest.subSequence(0, dstart)
                    + source.subSequence(start, end).toString()
                    + dest.subSequence(dend, dest.length());
            String digits = newText.replaceAll("[^0-9]", "");
            if (digits.length() > maxDigits) {
                return "";
            }
            return null;
        }
    }

    // Método para guardar los datos del usuario
    private void saveUserData() {
        String updatedName = editName.getText().toString().trim();
        String updatedAddress = editAddress.getText().toString().trim();
        // Obtener el número completo (prefijo + número local) a través del CCP
        String updatedPhone = ccp.getFullNumberWithPlus();

        if (!PhoneNumberUtils.isGlobalPhoneNumber(updatedPhone)) {
            Toast.makeText(this, "El número telefónico no es válido", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentImage = usersManager.getUser(userId).get(FavoritesDatabaseHelper.COLUMN_IMAGE);
        String updatedImage = (selectedImageUri != null) ? selectedImageUri.toString() : currentImage;

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null &&
                firebaseUser.getProviderData().size() > 1 &&
                "facebook.com".equals(firebaseUser.getProviderData().get(1).getProviderId())) {
            if (updatedImage == null || updatedImage.isEmpty()) {
                String fbImageUrl = FacebookUtils.getFacebookProfileImageUrl(200, 200);
                if (fbImageUrl != null && !fbImageUrl.isEmpty()) {
                    updatedImage = fbImageUrl;
                } else {
                    updatedImage = currentImage;
                }
            }
        }
        if (updatedImage == null || updatedImage.isEmpty()) {
            updatedImage = currentImage;
        }

        // Encriptar la dirección y el teléfono antes de guardar.
        try {
            KeystoreManager keystoreManager = new KeystoreManager();
            String encryptedAddress = keystoreManager.encryptData(updatedAddress);
            String encryptedPhone = keystoreManager.encryptData(updatedPhone);

            if (userId != null) {
                usersManager.updateUser(userId, updatedName, editEmail.getText().toString(),
                        null, null, encryptedAddress, encryptedPhone, updatedImage);
                firebaseUsersSync.syncBasicUserToFirestore(userId, updatedName, editEmail.getText().toString(),
                        encryptedAddress, encryptedPhone, updatedImage);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("UPDATED_NAME", updatedName);
                resultIntent.putExtra("UPDATED_IMAGE", updatedImage);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al encriptar los datos", Toast.LENGTH_SHORT).show();
        }
    }

}
