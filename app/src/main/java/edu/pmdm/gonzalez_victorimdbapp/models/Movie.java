package edu.pmdm.gonzalez_victorimdbapp.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Clase que representa una película con atributos como título, imagen, año de lanzamiento,
 * calificación y detalles adicionales para integrarse con la API de películas.
 * Implementa Parcelable para permitir el paso de objetos Movie entre actividades.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class Movie implements Parcelable {
    // Atributos existentes
    private String id;          // ID de la película
    private String title;       // Título
    private String imageUrl;    // URL de la imagen
    private String releaseYear; // Año de lanzamiento
    private String rating;      // Puntuación de la película

    // Nuevos atributos para la nueva API
    private String overview;    // Resumen de la película
    private String genreId;     // ID del género para búsquedas por género

    // Constructor vacío
    public Movie() {
    }

    // Constructor Parcel existente
    protected Movie(Parcel in) {
        id = in.readString();
        title = in.readString();
        imageUrl = in.readString();
        releaseYear = in.readString();
        rating = in.readString();
        overview = in.readString(); // Leer el nuevo campo 'overview'
        genreId = in.readString();  // Leer el nuevo campo 'genreId'
    }

    // CREATOR para los parcelables
    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    // Métodos Parcelables existentes, extendidos con los nuevos campos
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(imageUrl);
        dest.writeString(releaseYear);
        dest.writeString(rating);
        dest.writeString(overview);
        dest.writeString(genreId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters y Setters existentes
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(String releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    // Nuevos Getters y Setters
    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }
}