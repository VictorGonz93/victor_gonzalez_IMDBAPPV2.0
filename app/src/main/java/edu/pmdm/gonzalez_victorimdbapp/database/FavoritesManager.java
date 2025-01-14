package edu.pmdm.gonzalez_victorimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.gonzalez_victorimdbapp.models.Movie;

/**
 * Clase que gestiona las operaciones CRUD (Crear, Leer, Eliminar) sobre la tabla de películas favoritas
 * en la base de datos.
 * Permite añadir, eliminar y recuperar películas favoritas asociadas a un usuario específico.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class FavoritesManager {

    /** Instancia del helper de la base de datos que facilita las operaciones sobre SQLite. */
    private final FavoritesDatabaseHelper dbHelper;

    public FavoritesManager(Context context) {
        dbHelper = new FavoritesDatabaseHelper(context);
    }

    /**
     * Añade una película a la lista de favoritos de un usuario.
     * Si la película ya existe para el usuario, no se añade de nuevo (CONFLICT_IGNORE).
     *
     * @param movie Objeto de tipo Movie que contiene los datos de la película.
     * @param userEmail Correo electrónico del usuario al que se asocia la película favorita.
     */
    public void addFavorite(Movie movie, String userEmail) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_ID, movie.getId());
        values.put(FavoritesDatabaseHelper.COLUMN_USER_EMAIL, userEmail);
        values.put(FavoritesDatabaseHelper.COLUMN_TITLE, movie.getTitle());
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE_URL, movie.getImageUrl());
        values.put(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE, movie.getReleaseYear());
        values.put(FavoritesDatabaseHelper.COLUMN_RATING, movie.getRating());

        // Inserta o ignora si ya existe para el usuario
        db.insertWithOnConflict(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE // Evitar sobrescribir
        );
        db.close();
    }

    /**
     * Elimina una película específica de la lista de favoritos de un usuario.
     *
     * @param movieId ID de la película que se desea eliminar.
     * @param userEmail Correo electrónico del usuario del que se eliminará la película favorita.
     */
    public void removeFavorite(String movieId, String userEmail) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                FavoritesDatabaseHelper.COLUMN_ID + "=? AND " +
                        FavoritesDatabaseHelper.COLUMN_USER_EMAIL + "=?",
                new String[]{movieId, userEmail}
        );
        db.close();
    }

    /**
     * Recupera la lista de películas favoritas de un usuario desde la base de datos.
     *
     * @param userEmail Correo electrónico del usuario para el que se obtendrán los favoritos.
     * @return Lista de objetos Movie que representan las películas favoritas del usuario.
     */
    public List<Movie> getFavorites(String userEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Movie> favoriteMovies = new ArrayList<>();

        Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                null,
                FavoritesDatabaseHelper.COLUMN_USER_EMAIL + "=?",
                new String[]{userEmail},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Movie movie = new Movie();
                movie.setId(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_ID)));
                movie.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_TITLE)));
                movie.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_IMAGE_URL)));
                movie.setReleaseYear(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE)));
                movie.setRating(cursor.getString(cursor.getColumnIndexOrThrow(FavoritesDatabaseHelper.COLUMN_RATING)));
                favoriteMovies.add(movie);
            } while (cursor.moveToNext());

            cursor.close();
        }

        db.close();
        return favoriteMovies;
    }


}
