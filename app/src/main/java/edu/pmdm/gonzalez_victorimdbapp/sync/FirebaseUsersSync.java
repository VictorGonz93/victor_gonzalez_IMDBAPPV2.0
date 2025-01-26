package edu.pmdm.gonzalez_victorimdbapp.sync;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                        // Mantener los registros anteriores
                        activityLog = (List<Map<String, Object>>) documentSnapshot.get("activity_log");
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

                    // Actualizar Firestore con nuevos datos
                    db.collection("users")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> System.out.println("Usuario sincronizado correctamente en Firestore con todos los campos."))
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
            System.err.println("No hay usuario autenticado.");
            return;
        }

        String userId = currentUser.getUid();

        // Obtener datos locales
        Map<String, String> localUser = usersManager.getUser(userId);

        if (localUser == null || localUser.isEmpty()) {
            // Si la base de datos local está vacía, traer datos de Firestore
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            List<Map<String, Object>> activityLog =
                                    (List<Map<String, Object>>) documentSnapshot.get("activity_log");

                            String loginTime = null;
                            String logoutTime = null;

                            if (activityLog != null && !activityLog.isEmpty()) {
                                Map<String, Object> lastEntry = activityLog.get(activityLog.size() - 1);
                                loginTime = (String) lastEntry.get("login_time");
                                logoutTime = (String) lastEntry.get("logout_time");
                            }

                            usersManager.addUser(
                                    userId,
                                    name,
                                    email,
                                    loginTime,
                                    logoutTime,
                                    null,  // address
                                    null,  // phone
                                    null   // image
                            );

                            System.out.println("Datos descargados de Firestore a local.");
                        }
                    })
                    .addOnFailureListener(e -> System.err.println("Error sincronizando desde Firestore: " + e.getMessage()));
        } else {
            // Si hay datos locales, sincronizar con Firestore
            syncCurrentUserToFirestore();
        }
    }
}
