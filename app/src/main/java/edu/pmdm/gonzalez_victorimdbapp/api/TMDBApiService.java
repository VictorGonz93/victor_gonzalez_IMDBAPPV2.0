package edu.pmdm.gonzalez_victorimdbapp.api;

import edu.pmdm.gonzalez_victorimdbapp.models.TMDBMovie;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interfaz que define los métodos para interactuar con la API de TMDB (The Movie Database).
 * Proporciona acceso a los endpoints para obtener géneros y buscar películas.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public interface TMDBApiService {

    /**
     * Obtiene la lista de géneros disponibles desde la API de TMDB.
     *
     * @param apiKey Clave de la API para autenticación.
     * @param language Idioma de los resultados (por ejemplo, "en-US").
     * @return Una llamada Retrofit que devuelve una respuesta con los géneros.
     */
    @GET("genre/movie/list")
    Call<TMDBMovie.GenresResponse> getGenres(@Query("api_key") String apiKey, @Query("language") String language);

    /**
     * Busca películas basadas en género, año de lanzamiento y otras opciones.
     *
     * @param apiKey Clave de la API para autenticación.
     * @param language Idioma de los resultados (por ejemplo, "en-US").
     * @param sortBy Orden de los resultados (por ejemplo, "popularity.desc").
     * @param page Número de página de los resultados.
     * @param year Año de lanzamiento de las películas.
     * @param genreId ID del género por el cual filtrar las películas.
     * @return Una llamada Retrofit que devuelve una respuesta con las películas encontradas.
     */
    @GET("discover/movie")
    Call<TMDBMovie.MovieSearchResponse> discoverMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("sort_by") String sortBy,
            @Query("page") int page,
            @Query("primary_release_year") String year,
            @Query("with_genres") int genreId
    );
}