package edu.pmdm.gonzalez_victorimdbapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Clase que extiende SQLiteOpenHelper para gestionar la base de datos de usuarios.
 * Crea y actualiza la base de datos, asegurando el almacenamiento y recuperación de los datos de los usuarios.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class UsersDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "users_db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_LOGIN_TIME = "login_time";
    public static final String COLUMN_LOGOUT_TIME = "logout_time";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_IMAGE = "image";

    /**
     * Comando SQL para crear la tabla "users".
     * Contiene columnas para almacenar la información del usuario como ID, nombre, correo,
     * hora de inicio/cierre de sesión, dirección, teléfono e imagen de perfil.
     */
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_EMAIL + " TEXT, " +
                    COLUMN_LOGIN_TIME + " TEXT, " +
                    COLUMN_LOGOUT_TIME + " TEXT, " +
                    COLUMN_ADDRESS + " TEXT, " +
                    COLUMN_PHONE + " TEXT, " +
                    COLUMN_IMAGE + " TEXT" +
                    ");";

    public UsersDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Método llamado al crear la base de datos por primera vez.
     * Crea la tabla "users" para almacenar la información de los usuarios.
     *
     * @param db Instancia de SQLiteDatabase para ejecutar comandos SQL.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    /**
     * Método llamado cuando la versión de la base de datos cambia.
     * Este método elimina la tabla existente y la vuelve a crear con la nueva estructura.
     *
     * @param db Instancia de SQLiteDatabase para ejecutar comandos SQL.
     * @param oldVersion Versión anterior de la base de datos.
     * @param newVersion Nueva versión de la base de datos.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS); // Elimina la tabla existente
            onCreate(db); // Recrea la tabla
        }
    }

    /**
     * Método llamado cuando se intenta degradar la versión de la base de datos.
     * Este método elimina la tabla existente y la vuelve a crear desde cero.
     *
     * @param db Instancia de SQLiteDatabase para ejecutar comandos SQL.
     * @param oldVersion Versión anterior de la base de datos.
     * @param newVersion Nueva versión a la que se intenta degradar.
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}
