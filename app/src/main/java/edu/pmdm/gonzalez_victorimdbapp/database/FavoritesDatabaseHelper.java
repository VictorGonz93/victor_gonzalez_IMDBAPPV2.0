package edu.pmdm.gonzalez_victorimdbapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Clase que gestiona la base de datos unificada para usuarios y películas favoritas.
 * Contiene dos tablas: "users" (información del usuario) y "favorites" (películas favoritas asociadas a un usuario).
 *
 * @version 2.0
 * @author Victor Gonzalez Villapalo
 */
public class FavoritesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "favorites_db";  // Base de datos única
    private static final int DATABASE_VERSION = 8;  // Incrementado para reflejar cambios estructurales

    // Tabla Usuarios
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_LOGIN_TIME = "login_time";
    public static final String COLUMN_LOGOUT_TIME = "logout_time";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_IMAGE = "image";

    // Tabla Favoritos
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_ID = "id";  // ID de la película
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_RELEASE_DATE = "release_date";
    public static final String COLUMN_RATING = "rating";

    /**
     * Comando SQL para crear la tabla "users".
     */
    private static final String CREATE_TABLE_USERS =
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

    /**
     * Comando SQL para crear la tabla "favorites", con clave foránea referenciando "users".
     */
    private static final String CREATE_TABLE_FAVORITES =
            "CREATE TABLE " + TABLE_FAVORITES + " (" +
                    COLUMN_ID + " TEXT, " +
                    COLUMN_USER_ID + " TEXT, " +  // FK relacionada con `users`
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_IMAGE_URL + " TEXT, " +
                    COLUMN_RELEASE_DATE + " TEXT, " +
                    COLUMN_RATING + " TEXT, " +
                    "PRIMARY KEY (" + COLUMN_ID + ", " + COLUMN_USER_ID + "), " +
                    "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + " (" + COLUMN_USER_ID + ") ON DELETE CASCADE" +
                    ");";

    public FavoritesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Método llamado al crear la base de datos por primera vez.
     * Crea las tablas "users" y "favorites".
     *
     * @param db Instancia de SQLiteDatabase para ejecutar comandos SQL.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_FAVORITES);
    }

    /**
     * Método llamado cuando la versión de la base de datos cambia.
     * Este método mantiene los datos existentes y solo altera la estructura si es necesario.
     *
     * @param db         Instancia de SQLiteDatabase para ejecutar comandos SQL.
     * @param oldVersion Versión anterior de la base de datos.
     * @param newVersion Nueva versión de la base de datos.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
            onCreate(db);
        }
    }

    /**
     * Método llamado cuando se intenta degradar la versión de la base de datos.
     * Este método elimina las tablas y las vuelve a crear desde cero.
     *
     * @param db         Instancia de SQLiteDatabase para ejecutar comandos SQL.
     * @param oldVersion Versión anterior de la base de datos.
     * @param newVersion Nueva versión a la que se intenta degradar.
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        onCreate(db);
    }
}
