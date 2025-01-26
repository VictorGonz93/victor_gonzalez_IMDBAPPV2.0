package edu.pmdm.gonzalez_victorimdbapp.sync;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

import edu.pmdm.gonzalez_victorimdbapp.database.FavoritesManager;
import edu.pmdm.gonzalez_victorimdbapp.models.Movie;

/**
 * Clase para manejar la sincronización de la base de datos de favoritos con Firestore.
 */
public class FirebaseFavoritesSync {

    private final FirebaseFirestore db;
    private static final String FAVORITES_COLLECTION_ID = "Bi2Bst134lyb2FzI7eHm";  // ID fija de la colección en Firestore

    public FirebaseFavoritesSync() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Agrega una película a la base de datos de Firebase Firestore.
     * @param movie Objeto de la película a agregar.
     */
    public void addFavoriteToFirestore(Movie movie) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> movieData = new HashMap<>();
        movieData.put("id", movie.getId());
        movieData.put("title", movie.getTitle());
        movieData.put("posterUrl", movie.getImageUrl());
        movieData.put("releaseDate", movie.getReleaseYear());
        movieData.put("rating", movie.getRating());

        db.collection("favorites")
                .document(userId)  // Usamos el ID del usuario como documento
                .collection("movies")
                .document(movie.getId())
                .set(movieData)
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Película agregada a Firestore para el usuario: " + userId);
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error al agregar película: " + e.getMessage());
                });
    }

    /**
     * Elimina una película de la base de datos de Firebase Firestore.
     * @param movieId ID de la película a eliminar.
     */
    public void removeFavoriteFromFirestore(String movieId, String userEmail) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("favorites")
                .document(userId)
                .collection("movies")
                .document(movieId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Película eliminada de Firestore para el usuario: " + userId);
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error al eliminar película: " + e.getMessage());
                });
    }

    /**
     * Sincroniza la base de datos local con Firestore al inicio de la aplicación.
     * @param favoritesManager Instancia del gestor de la base de datos local.
     */
    public void syncFavoritesWithLocalDatabase(FavoritesManager favoritesManager) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("favorites")
                .document(userId)
                .collection("movies")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Movie movie = new Movie();
                        movie.setId(doc.getString("id"));
                        movie.setTitle(doc.getString("title"));
                        movie.setImageUrl(doc.getString("posterUrl"));
                        movie.setReleaseYear(doc.getString("releaseDate"));
                        movie.setRating(doc.getString("rating"));

                        favoritesManager.addFavorite(movie, userId);
                    }
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error en la sincronización de favoritos: " + e.getMessage());
                });
    }

}
