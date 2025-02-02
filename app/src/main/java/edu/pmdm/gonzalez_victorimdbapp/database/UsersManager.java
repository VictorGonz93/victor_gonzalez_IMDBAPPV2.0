package edu.pmdm.gonzalez_victorimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase que gestiona las operaciones CRUD sobre la tabla de usuarios en la base de datos.
 * Permite añadir, actualizar, eliminar y recuperar información de los usuarios registrados en la aplicación.
 */
public class UsersManager {

    private final FavoritesDatabaseHelper dbHelper;
    private final SQLiteDatabase db; // Mantener la base de datos abierta mientras la app esté activa

    public UsersManager(Context context) {
        dbHelper = new FavoritesDatabaseHelper(context);
        db = dbHelper.getWritableDatabase(); // Abrir la base de datos una sola vez
    }

    /**
     * Añade un nuevo usuario a la base de datos local y lo sincroniza con Firestore.
     * Si el usuario ya existe, no se añade de nuevo (CONFLICT_IGNORE).
     */
    public void addUser(String userId, String name, String email, String loginTime, String logoutTime, String address, String phone, String image) {
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);
        values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);
        values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);
        values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, address);
        values.put(FavoritesDatabaseHelper.COLUMN_PHONE, phone);
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, image);

        db.insertWithOnConflict(
                FavoritesDatabaseHelper.TABLE_USERS,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );

        // Sincroniza todos los datos en Firestore
        syncUserToFirestore(userId, name, email, address, phone, image);
    }

    /**
     * Actualiza la información de un usuario en la base de datos local.
     */
    public void updateUser(String userId, String name, String email, String loginTime, String logoutTime, String address, String phone, String image) {
        // Recuperar los datos actuales
        Map<String, String> currentData = getUser(userId);
        if (currentData == null) {
            // Manejar error o agregar usuario nuevo
        }

        // Si loginTime o logoutTime son null, conservar los valores actuales
        if (loginTime == null) {
            loginTime = currentData.get(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME);
        }
        if (logoutTime == null) {
            logoutTime = currentData.get(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME);
        }
        // Agregar comprobación para address y phone
        if (address == null) {
            address = currentData.get(FavoritesDatabaseHelper.COLUMN_ADDRESS);
        }
        if (phone == null) {
            phone = currentData.get(FavoritesDatabaseHelper.COLUMN_PHONE);
        }

        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_NAME, name);
        values.put(FavoritesDatabaseHelper.COLUMN_EMAIL, email);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);
        values.put(FavoritesDatabaseHelper.COLUMN_ADDRESS, address);
        values.put(FavoritesDatabaseHelper.COLUMN_PHONE, phone);
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE, image);

        db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});

        // Sincronizar cambios en Firestore
        syncUserToFirestore(userId, name, email, address, phone, image);
    }

    /**
     * Recupera los datos de un usuario específico mediante su ID.
     */
    public Map<String, String> getUser(String userId) {
        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_USERS,
                null, // Seleccionar todas las columnas
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?", // Condición WHERE
                new String[]{userId}, // Parámetro de la consulta
                null, null, null
        );

        Map<String, String> userData = new HashMap<>();

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    for (String column : new String[]{
                            FavoritesDatabaseHelper.COLUMN_USER_ID,
                            FavoritesDatabaseHelper.COLUMN_NAME,
                            FavoritesDatabaseHelper.COLUMN_EMAIL,
                            FavoritesDatabaseHelper.COLUMN_LOGIN_TIME,
                            FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME,
                            FavoritesDatabaseHelper.COLUMN_ADDRESS,
                            FavoritesDatabaseHelper.COLUMN_PHONE,
                            FavoritesDatabaseHelper.COLUMN_IMAGE
                    }) {
                        int columnIndex = cursor.getColumnIndex(column);
                        if (columnIndex != -1) {
                            userData.put(column, cursor.getString(columnIndex));
                        }
                    }
                } else {
                    Log.e("UsersManager", "No se encontraron datos para el usuario con ID: " + userId);
                }
            } catch (Exception e) {
                Log.e("UsersManager", "Error recuperando datos del usuario: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }

        return userData;
    }

    /**
     * Recupera la lista de todos los usuarios almacenados en la base de datos local.
     */
    public List<Map<String, String>> getAllUsers() {
        List<Map<String, String>> userList = new ArrayList<>();

        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_USERS,
                null,
                null,
                null,
                null,
                null,
                null
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    Map<String, String> user = new HashMap<>();
                    for (String column : new String[]{
                            FavoritesDatabaseHelper.COLUMN_USER_ID,
                            FavoritesDatabaseHelper.COLUMN_NAME,
                            FavoritesDatabaseHelper.COLUMN_EMAIL,
                            FavoritesDatabaseHelper.COLUMN_LOGIN_TIME,
                            FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME,
                            FavoritesDatabaseHelper.COLUMN_ADDRESS,
                            FavoritesDatabaseHelper.COLUMN_PHONE,
                            FavoritesDatabaseHelper.COLUMN_IMAGE
                    }) {
                        user.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)));
                    }
                    userList.add(user);
                }
            } catch (Exception e) {
                Log.e("UsersManager", "Error obteniendo lista de usuarios: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }

        return userList;
    }

    /**
     * Verifica si un usuario ya existe en la base de datos local.
     */
    public boolean userExists(String userId) {
        boolean exists = false;
        Cursor cursor = null;

        try {
            String query = "SELECT COUNT(*) FROM " + FavoritesDatabaseHelper.TABLE_USERS + " WHERE " +
                    FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?";
            cursor = db.rawQuery(query, new String[]{userId});

            if (cursor != null && cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
        } catch (Exception e) {
            Log.e("UsersManager", "Error al verificar existencia de usuario", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return exists;
    }

    /**
     * Actualiza la hora de login del usuario en la base de datos local.
     */
    public void updateLoginTime(String userId, String loginTime) {
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);

        db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
    }

    /**
     * Actualiza la hora de logout del usuario en la base de datos local.
     */
    public void updateLogoutTime(String userId, String logoutTime) {
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);

        db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
    }

    /**
     * Sube todos los datos del usuario a Firestore.
     */
    private void syncUserToFirestore(String userId, String name, String email, String address, String phone, String image) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("address", address);
        userData.put("phone", phone);
        userData.put("image", image);

        firestore.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("UsersManager", "Usuario sincronizado con Firestore"))
                .addOnFailureListener(e -> Log.e("UsersManager", "Error al sincronizar usuario en Firestore", e));
    }

}
