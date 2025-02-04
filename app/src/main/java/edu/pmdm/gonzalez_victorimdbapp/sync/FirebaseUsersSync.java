package edu.pmdm.gonzalez_victorimdbapp.sync;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.pmdm.gonzalez_victorimdbapp.database.FavoritesDatabaseHelper;
import edu.pmdm.gonzalez_victorimdbapp.database.UsersManager;

import java.util.Date;

/**
 * Clase para manejar la sincronización de la base de datos de usuarios con Firestore.
 */
public class FirebaseUsersSync {

    private final FirebaseFirestore db;
    private final FirebaseUser currentUser;
    private final SimpleDateFormat dateFormat;

    public FirebaseUsersSync() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    public void syncBasicUserToFirestore(String userId, String name, String email, String address, String phone, String image) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Crear el mapa de datos del usuario con solo los campos básicos
        Map<String, Object> userData = new HashMap<>();
        userData.put("user_id", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("address", address);
        userData.put("phone", phone);
        userData.put("image", image);

        // Guardar los datos básicos en Firestore
        db.collection("users")
                .document(userId)
                .set(userData, SetOptions.merge()) // Usar set con merge para evitar sobrescribir datos existentes
                .addOnSuccessListener(aVoid -> Log.d("FirebaseUsersSync", "Usuario básico sincronizado con Firestore."))
                .addOnFailureListener(e -> Log.e("FirebaseUsersSync", "Error al sincronizar usuario básico: " + e.getMessage()));
    }

    /**
     * Sincroniza el usuario actual con Firestore, guardando los datos básicos y el historial de login.
     */
    public void syncCurrentUserToFirestore() {
        if (currentUser == null) {
            System.err.println("No hay usuario autenticado.");
            return;
        }

        String userId = currentUser.getUid();
        String email = currentUser.getEmail();
        String name = currentUser.getDisplayName();
        String address = currentUser.getTenantId();
        String phone = currentUser.getPhoneNumber();
        Uri image = currentUser.getPhotoUrl();

        if (email == null || name == null) {
            System.err.println("Error: Falta información del usuario.");
            return;
        }

        // Obtener la fecha y hora actual formateada
        String loginTime = dateFormat.format(new Date());

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Map<String, Object>> activityLog = new ArrayList<>();

                    if (documentSnapshot.exists() && documentSnapshot.contains("activity_log")) {
                        activityLog = (List<Map<String, Object>>) documentSnapshot.get("activity_log");

                        // Comprobar si ya existe un registro con el mismo login_time
                        for (Map<String, Object> entry : activityLog) {
                            if (entry.get("login_time").equals(loginTime)) {
                                System.out.println("El login ya ha sido registrado.");
                                return;
                            }
                        }
                    }

                    // Agregar nuevo registro de login
                    Map<String, Object> newLog = new HashMap<>();
                    newLog.put("login_time", loginTime);
                    newLog.put("logout_time", null);

                    activityLog.add(newLog);

                    // Datos de usuario actualizados con email y nombre
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("user_id", userId);
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("activity_log", activityLog);
                    userData.put("address", address);
                    userData.put("phone", phone);
                    userData.put("image", image);


                    // Actualizar Firestore con nuevos datos
                    db.collection("users")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> System.out.println("Usuario sincronizado correctamente en Firestore."))
                            .addOnFailureListener(e -> System.err.println("Error al sincronizar usuario: " + e.getMessage()));
                })
                .addOnFailureListener(e -> System.err.println("Error al recuperar datos del usuario: " + e.getMessage()));
    }


    /**
     * Actualiza el tiempo de cierre de sesión del usuario actual en Firestore.
     */
    public void updateLogoutTime() {
        if (currentUser == null) {
            System.err.println("No hay usuario autenticado.");
            return;
        }

        String userId = currentUser.getUid();
        String logoutTime = dateFormat.format(new Date());

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("activity_log")) {
                        List<Map<String, Object>> activityLog =
                                (List<Map<String, Object>>) documentSnapshot.get("activity_log");

                        if (activityLog != null && !activityLog.isEmpty()) {
                            Map<String, Object> lastLogin = activityLog.get(activityLog.size() - 1);

                            if (lastLogin.get("logout_time") == null) {
                                lastLogin.put("logout_time", logoutTime);

                                db.collection("users")
                                        .document(userId)
                                        .update("activity_log", activityLog)
                                        .addOnSuccessListener(aVoid -> System.out.println("Logout registrado correctamente."))
                                        .addOnFailureListener(e -> System.err.println("Error al registrar logout: " + e.getMessage()));
                            } else {
                                System.err.println("Error: No se encontró un login pendiente de logout.");
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> System.err.println("Error al obtener el historial de actividad: " + e.getMessage()));
    }

    /**
     * Sincroniza la base de datos local con Firestore al inicio de la aplicación.
     */
    public void syncUsersWithFirestore(UsersManager usersManager) {
        if (currentUser == null) {
            Log.e("FirebaseUsersSync", "No hay usuario autenticado.");
            return;
        }

        String userId = currentUser.getUid();
        Map<String, String> localUser = usersManager.getUser(userId);

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name, email, address, phone, image;

                    // 🔹 1️⃣ Intentamos obtener el nombre desde Firestore
                    name = documentSnapshot.exists() ? documentSnapshot.getString("name") : null;

                    // 🔹 2️⃣ Si Firestore no tiene nombre, intentamos con Firebase Authentication (Google/Facebook)
                    if (name == null || name.isEmpty()) {
                        String firebaseName = currentUser.getDisplayName();
                        if (firebaseName != null && !firebaseName.isEmpty()) {
                            name = firebaseName; // 🔥 Usamos el nombre de Google/Facebook si está disponible
                        }
                    }

                    // 🔹 3️⃣ Si todavía no hay nombre, intentamos obtenerlo de SQLite
                    if (name == null || name.isEmpty()) {
                        name = localUser.getOrDefault(FavoritesDatabaseHelper.COLUMN_NAME, "Usuario");
                    }

                    // 🔹 4️⃣ Comprobamos si el nombre sigue en "Usuario", si es así, forzamos el valor desde SQLite
                    if ("Usuario".equals(name) && localUser.containsKey(FavoritesDatabaseHelper.COLUMN_NAME)) {
                        name = localUser.get(FavoritesDatabaseHelper.COLUMN_NAME);
                    }

                    email = documentSnapshot.exists() ? documentSnapshot.getString("email") : currentUser.getEmail();
                    address = documentSnapshot.exists() ? documentSnapshot.getString("address") : localUser.getOrDefault(FavoritesDatabaseHelper.COLUMN_ADDRESS, "");
                    phone = documentSnapshot.exists() ? documentSnapshot.getString("phone") : localUser.getOrDefault(FavoritesDatabaseHelper.COLUMN_PHONE, "");
                    image = documentSnapshot.exists() ? documentSnapshot.getString("image") : "";

                    if (image.isEmpty()) {
                        image = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "";
                        image = localUser.getOrDefault(FavoritesDatabaseHelper.COLUMN_IMAGE, image);
                    }

                    Log.e("FirebaseUsersSync", "Nombre final a guardar: " + name); // 🛠 DEBUG

                    // Guardamos los datos en SQLite
                    usersManager.addUser(userId, name, email, null, null, address, phone, image);

                    // Enviamos la actualización a Firestore (garantizando que se sobrescriba)
                    syncBasicUserToFirestore(userId, name, email, address, phone, image);
                })
                .addOnFailureListener(e -> Log.e("FirebaseUsersSync", "Error sincronizando desde Firestore: " + e.getMessage()));
    }


}
