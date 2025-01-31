package edu.pmdm.gonzalez_victorimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.gonzalez_victorimdbapp.models.Movie;
import edu.pmdm.gonzalez_victorimdbapp.sync.FirebaseFavoritesSync;

/**
 * Clase que gestiona las operaciones CRUD sobre la tabla de películas favoritas.
 * Ahora la tabla `favorites` está unida a `users` a través del `user_id`.
 */
public class FavoritesManager {

    private final FavoritesDatabaseHelper dbHelper;
    private final FirebaseFavoritesSync firebaseSync;

    public FavoritesManager(Context context) {
        dbHelper = new FavoritesDatabaseHelper(context);
        firebaseSync = new FirebaseFavoritesSync();
        firebaseSync.syncFavoritesWithLocalDatabase(this);
    }

    /**
     * Añade una película a la lista de favoritos de un usuario.
     * Si la película ya existe para el usuario, no se añade de nuevo (CONFLICT_IGNORE).
     */
    public void addFavorite(Movie movie) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("FavoritesManager", "No hay usuario autenticado.");
            return;
        }

        String userId = currentUser.getUid();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_ID, movie.getId());
        values.put(FavoritesDatabaseHelper.COLUMN_USER_ID, userId);
        values.put(FavoritesDatabaseHelper.COLUMN_TITLE, movie.getTitle());
        values.put(FavoritesDatabaseHelper.COLUMN_IMAGE_URL, movie.getImageUrl());
        values.put(FavoritesDatabaseHelper.COLUMN_RELEASE_DATE, movie.getReleaseYear());
        values.put(FavoritesDatabaseHelper.COLUMN_RATING, movie.getRating());

        db.insertWithOnConflict(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );
        db.close();

        // Sincronizar con Firestore
        firebaseSync.addFavoriteToFirestore(movie, userId);
    }

    /**
     * Elimina una película específica de la lista de favoritos de un usuario.
     */
    public void removeFavorite(String movieId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("FavoritesManager", "No hay usuario autenticado.");
            return;
        }

        String userId = currentUser.getUid();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedRows = db.delete(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                FavoritesDatabaseHelper.COLUMN_ID + "=? AND " + FavoritesDatabaseHelper.COLUMN_USER_ID + "=?",
                new String[]{movieId, userId}
        );
        db.close();

        if (deletedRows > 0) {
            Log.d("FavoritesManager", "Película eliminada localmente para el usuario: " + userId);
            firebaseSync.removeFavoriteFromFirestore(movieId, userId);
        } else {
            Log.d("FavoritesManager", "No se encontró la película en favoritos.");
        }
    }

    /**
     * Recupera la lista de películas favoritas de un usuario desde la base de datos.
     */
    public List<Movie> getFavorites() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("FavoritesManager", "No hay usuario autenticado.");
            return new ArrayList<>();
        }

        String userId = currentUser.getUid();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Movie> favoriteMovies = new ArrayList<>();

        try (Cursor cursor = db.query(
                FavoritesDatabaseHelper.TABLE_FAVORITES,
                new String[]{
                        FavoritesDatabaseHelper.COLUMN_ID,
                        FavoritesDatabaseHelper.COLUMN_TITLE,
                        FavoritesDatabaseHelper.COLUMN_IMAGE_URL,
                        FavoritesDatabaseHelper.COLUMN_RELEASE_DATE,
                        FavoritesDatabaseHelper.COLUMN_RATING
                },
                FavoritesDatabaseHelper.COLUMN_USER_ID + "=?",
                new String[]{userId},
                null, null, null)) {

            while (cursor.moveToNext()) {
                Movie movie = new Movie();
                movie.setId(cursor.getString(0));
                movie.setTitle(cursor.getString(1));
                movie.setImageUrl(cursor.getString(2));
                movie.setReleaseYear(cursor.getString(3));
                movie.setRating(cursor.getString(4));
                favoriteMovies.add(movie);
            }
        }

        db.close();
        return favoriteMovies;
    }

}
