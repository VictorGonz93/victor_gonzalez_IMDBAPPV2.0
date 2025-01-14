package edu.pmdm.gonzalez_victorimdbapp.api;

import edu.pmdm.gonzalez_victorimdbapp.models.MovieOverviewResponse;
import edu.pmdm.gonzalez_victorimdbapp.models.PopularMoviesResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * Interfaz que define los métodos para interactuar con la API de IMDB proporcionada por RapidAPI.
 * Permite obtener detalles sobre las películas más populares y la información general de una película específica.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */

public interface IMDBApiService {

    /**
     * Obtiene una lista de películas más populares desde la API de IMDB.
     *
     * @param apiKey Clave de la API para autenticación.
     * @param host Host de la API proporcionado por RapidAPI.
     * @param type Tipo de lista de películas populares (por ejemplo, "MOVIE").
     * @return Una llamada Retrofit que devuelve una respuesta con las películas más populares.
     */
    @GET("title/get-top-meter")
    Call<PopularMoviesResponse> getTopMeter(
            @Header("x-rapidapi-key") String apiKey,
            @Header("x-rapidapi-host") String host,
            @Query("topMeterTitlesType") String type
    );

    /**
     * Obtiene información detallada de una película específica desde la API de IMDB.
     *
     * @param apiKey Clave de la API para autenticación.
     * @param host Host de la API proporcionado por RapidAPI.
     * @param tconst Identificador único de la película en IMDB.
     * @return Una llamada Retrofit que devuelve una respuesta con la información detallada de la película.
     */
    @GET("title/get-overview")
    Call<MovieOverviewResponse> getOverview(
            @Header("x-rapidapi-key") String apiKey,
            @Header("x-rapidapi-host") String host,
            @Query("tconst") String tconst
    );
}
