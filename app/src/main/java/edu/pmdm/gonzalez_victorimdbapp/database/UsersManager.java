package edu.pmdm.gonzalez_victorimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

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
    private SQLiteDatabase db;

    public UsersManager(Context context) {
        dbHelper = new FavoritesDatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    /**
     * Añade un nuevo usuario a la base de datos local y lo sincroniza con Firestore.
     * Si el usuario ya existe, no se añade de nuevo (CONFLICT_IGNORE).
     */
    public void addUser(String userId, String name, String email, String loginTime, String logoutTime, String address, String phone, String image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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
        db.close();

        // 🔥 Ahora solo subimos userId, name y email a Firestore
        syncUserToFirestore(userId, name, email);
    }

    /**
     * Actualiza la información de un usuario en la base de datos local.
     */
    public void updateUser(String userId, String name, String email, String loginTime, String logoutTime, String address, String phone, String image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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
        db.close();
    }

    /**
     * Recupera los datos de un usuario específico mediante su ID.
     */
    public Map<String, String> getUser(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_USERS,
                null,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null,
                null,
                null
        );

        Map<String, String> userData = null;
        if (cursor != null && cursor.moveToFirst()) {
            userData = new HashMap<>();
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
                userData.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)));
            }
            cursor.close();
        }
        db.close();
        return userData;
    }

    /**
     * Recupera la lista de todos los usuarios almacenados en la base de datos local.
     */
    public List<Map<String, String>> getAllUsers() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
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

        if (cursor != null && cursor.moveToFirst()) {
            do {
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
            } while (cursor.moveToNext());

            cursor.close();
        }
        db.close();
        return userList;
    }

    // Método para verificar si un usuario ya existe
    public boolean userExists(String userId) {
        // Define la consulta para buscar el usuario por su ID
        String query = "SELECT * FROM users WHERE user_id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{userId});

        // Si el cursor tiene al menos una fila, el usuario existe
        boolean exists = cursor.getCount() > 0;

        // Cierra el cursor para liberar recursos
        cursor.close();

        return exists;
    }

    /**
     * Actualiza la hora de login del usuario en la base de datos local.
     */
    public void updateLoginTime(String userId, String loginTime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);

        int rowsUpdated = db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();

        if (rowsUpdated > 0) {
            Log.d("UsersManager", "Hora de login actualizada localmente.");
        } else {
            Log.d("UsersManager", "No se encontró el usuario para actualizar el login.");
        }
    }

    /**
     * Actualiza la hora de logout del usuario en la base de datos local.
     */
    public void updateLogoutTime(String userId, String logoutTime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);

        int rowsUpdated = db.update(
                FavoritesDatabaseHelper.TABLE_USERS,
                values,
                FavoritesDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId}
        );
        db.close();

        if (rowsUpdated > 0) {
            Log.d("UsersManager", "Logout actualizado localmente para el usuario: " + userId);
        } else {
            Log.d("UsersManager", "No se encontró usuario para actualizar el logout.");
        }
    }

    /**
     * 🔥 Sube SOLO `userId`, `name` y `email` a Firestore.
     */
    private void syncUserToFirestore(String userId, String name, String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d("UsersManager", "Usuario sincronizado con Firestore"))
                .addOnFailureListener(e -> Log.e("UsersManager", "Error al sincronizar usuario en Firestore", e));
    }
}
