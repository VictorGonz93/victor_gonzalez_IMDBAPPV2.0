package edu.pmdm.gonzalez_victorimdbapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Clase que extiende SQLiteOpenHelper para gestionar la base de datos de películas favoritas.
 * Crea y actualiza la base de datos, asegurando el almacenamiento y recuperación de las películas favoritas
 * para cada usuario.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class FavoritesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "favorites_db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USER_EMAIL = "user_email";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_RELEASE_DATE = "release_date";
    public static final String COLUMN_RATING = "rating";

    /**
     * Comando SQL para crear la tabla "favorites".
     * Contiene columnas para ID de la película, correo del usuario, título, URL de la imagen, fecha de lanzamiento y calificación.
     */
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_FAVORITES + " (" +
                    COLUMN_ID + " TEXT, " +
                    COLUMN_USER_EMAIL + " TEXT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_IMAGE_URL + " TEXT, " +
                    COLUMN_RELEASE_DATE + " TEXT, " +
                    COLUMN_RATING + " TEXT, " +
                    "PRIMARY KEY (" + COLUMN_ID + ", " + COLUMN_USER_EMAIL + "));";

    public FavoritesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Método llamado al crear la base de datos por primera vez.
     * Crea la tabla "favorites" para almacenar películas favoritas.
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
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES); // Elimina la tabla existente
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
        // Eliminar la tabla y volver a crearla
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        onCreate(db);
    }
}
