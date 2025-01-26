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

    private final UsersDatabaseHelper dbHelper;

    public UsersManager(Context context) {
        dbHelper = new UsersDatabaseHelper(context);
    }

    /**
     * Añade un nuevo usuario a la base de datos.
     * Si el usuario ya existe, no se añade de nuevo (CONFLICT_IGNORE).
     */
    public void addUser(String userId, String name, String email, String loginTime, String logoutTime, String address, String phone, String image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UsersDatabaseHelper.COLUMN_USER_ID, userId);
        values.put(UsersDatabaseHelper.COLUMN_NAME, name);
        values.put(UsersDatabaseHelper.COLUMN_EMAIL, email);
        values.put(UsersDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(UsersDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);
        values.put(UsersDatabaseHelper.COLUMN_ADDRESS, address);
        values.put(UsersDatabaseHelper.COLUMN_PHONE, phone);
        values.put(UsersDatabaseHelper.COLUMN_IMAGE, image);

        db.insertWithOnConflict(
                UsersDatabaseHelper.TABLE_USERS,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );
        db.close();

        // Sincronizar con Firestore
        syncUserToFirestore(userId, name, email, loginTime, logoutTime, address, phone, image);
    }

    /**
     * Actualiza la información de un usuario en la base de datos.
     */
    public void updateUser(String userId, String name, String email, String loginTime, String logoutTime, String address, String phone, String image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UsersDatabaseHelper.COLUMN_NAME, name);
        values.put(UsersDatabaseHelper.COLUMN_EMAIL, email);
        values.put(UsersDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);
        values.put(UsersDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);
        values.put(UsersDatabaseHelper.COLUMN_ADDRESS, address);
        values.put(UsersDatabaseHelper.COLUMN_PHONE, phone);
        values.put(UsersDatabaseHelper.COLUMN_IMAGE, image);

        db.update(UsersDatabaseHelper.TABLE_USERS, values,
                UsersDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();
    }

    /**
     * Elimina un usuario de la base de datos según su ID.
     */
    public void deleteUser(String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                UsersDatabaseHelper.TABLE_USERS,
                UsersDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId}
        );
        db.close();

        // Eliminar también de Firestore
        FirebaseFirestore.getInstance().collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> System.out.println("Usuario eliminado de Firestore"))
                .addOnFailureListener(e -> System.err.println("Error al eliminar usuario: " + e.getMessage()));
    }

    /**
     * Recupera los datos de un usuario específico mediante su ID.
     */
    public Map<String, String> getUser(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                UsersDatabaseHelper.TABLE_USERS,
                null,
                UsersDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null,
                null,
                null
        );

        Map<String, String> userData = null;
        if (cursor != null && cursor.moveToFirst()) {
            userData = new HashMap<>();
            userData.put(UsersDatabaseHelper.COLUMN_USER_ID, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_USER_ID)));
            userData.put(UsersDatabaseHelper.COLUMN_NAME, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_NAME)));
            userData.put(UsersDatabaseHelper.COLUMN_EMAIL, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_EMAIL)));
            userData.put(UsersDatabaseHelper.COLUMN_LOGIN_TIME, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_LOGIN_TIME)));
            userData.put(UsersDatabaseHelper.COLUMN_LOGOUT_TIME, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_LOGOUT_TIME)));
            userData.put(UsersDatabaseHelper.COLUMN_ADDRESS, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_ADDRESS)));
            userData.put(UsersDatabaseHelper.COLUMN_PHONE, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_PHONE)));
            userData.put(UsersDatabaseHelper.COLUMN_IMAGE, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_IMAGE)));
            cursor.close();
        }

        db.close();
        return userData;
    }

    /**
     * Recupera la lista de todos los usuarios almacenados en la base de datos.
     */
    public List<Map<String, String>> getAllUsers() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Map<String, String>> userList = new ArrayList<>();

        Cursor cursor = db.query(
                UsersDatabaseHelper.TABLE_USERS,
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
                user.put(UsersDatabaseHelper.COLUMN_USER_ID, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_USER_ID)));
                user.put(UsersDatabaseHelper.COLUMN_NAME, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_NAME)));
                user.put(UsersDatabaseHelper.COLUMN_EMAIL, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_EMAIL)));
                user.put(UsersDatabaseHelper.COLUMN_LOGIN_TIME, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_LOGIN_TIME)));
                user.put(UsersDatabaseHelper.COLUMN_LOGOUT_TIME, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_LOGOUT_TIME)));
                user.put(UsersDatabaseHelper.COLUMN_ADDRESS, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_ADDRESS)));
                user.put(UsersDatabaseHelper.COLUMN_PHONE, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_PHONE)));
                user.put(UsersDatabaseHelper.COLUMN_IMAGE, cursor.getString(cursor.getColumnIndexOrThrow(UsersDatabaseHelper.COLUMN_IMAGE)));
                userList.add(user);
            } while (cursor.moveToNext());

            cursor.close();
        }

        db.close();
        return userList;
    }

    public void updateLoginTime(String userId, String loginTime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UsersDatabaseHelper.COLUMN_LOGIN_TIME, loginTime);

        db.update(UsersDatabaseHelper.TABLE_USERS, values,
                UsersDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();
        Log.d("UsersManager", "Hora de login actualizada localmente.");
    }

    public void updateLogoutTime(String userId, String logoutTime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UsersDatabaseHelper.COLUMN_LOGOUT_TIME, logoutTime);

        db.update(UsersDatabaseHelper.TABLE_USERS, values,
                UsersDatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId});
        db.close();
        Log.d("UsersManager", "Hora de logout actualizada localmente.");
    }

    private void syncUserToFirestore(String userId, String name, String email, String loginTime, String logoutTime, String address, String phone, String image) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("login_time", loginTime);
        userData.put("logout_time", logoutTime);
        userData.put("address", address);
        userData.put("phone", phone);
        userData.put("image", image);

        db.collection("users").document(userId).set(userData);
    }
}
