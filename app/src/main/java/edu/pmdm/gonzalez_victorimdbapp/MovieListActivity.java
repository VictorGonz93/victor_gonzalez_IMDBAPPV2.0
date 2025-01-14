package edu.pmdm.gonzalez_victorimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.gonzalez_victorimdbapp.R;
import edu.pmdm.gonzalez_victorimdbapp.adapter.MovieAdapter;
import edu.pmdm.gonzalez_victorimdbapp.database.FavoritesManager;
import edu.pmdm.gonzalez_victorimdbapp.models.Movie;
import edu.pmdm.gonzalez_victorimdbapp.models.TMDBMovie;
import edu.pmdm.gonzalez_victorimdbapp.api.TMDBApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Actividad para mostrar una lista de películas basada en los criterios de búsqueda.
 * Realiza llamadas a la API de TMDB para obtener películas por género y año,
 * y muestra los resultados en un RecyclerView.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class MovieListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MovieAdapter movieAdapter;
    private List<Movie> movieList = new ArrayList<>();

    private static final String TMDB_API_KEY = "e9f84dbecca6c65f600d95bee2badcf5";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Inicializar el adaptador con una lista vacía
        movieList = new ArrayList<>();
        movieAdapter = new MovieAdapter(movieList, false);
        recyclerView.setAdapter(movieAdapter);

        // Obtén parámetros de búsqueda
        int genreId = getIntent().getIntExtra("GENRE_ID", -1);
        String year = getIntent().getStringExtra("YEAR");

        // Realiza la búsqueda
        searchMovies(genreId, year);
    }

    /**
     * Realiza una llamada a la API de TMDB para buscar películas basadas en género y año.
     *
     * @param genreId ID del género seleccionado para la búsqueda.
     * @param year Año seleccionado para filtrar las películas.
     */
    private void searchMovies(int genreId, String year) {
        TMDBApiService apiService = new Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TMDBApiService.class);

        Call<TMDBMovie.MovieSearchResponse> call = apiService.discoverMovies(
                TMDB_API_KEY,
                "en-US",
                "",
                1,
                year,
                genreId
        );

        call.enqueue(new Callback<TMDBMovie.MovieSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<TMDBMovie.MovieSearchResponse> call,
                                   @NonNull Response<TMDBMovie.MovieSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMovieList(response.body().results);
                } else {
                    Toast.makeText(MovieListActivity.this, "No se encontraron películas.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TMDBMovie.MovieSearchResponse> call, @NonNull Throwable t) {
                Toast.makeText(MovieListActivity.this, "Error al cargar películas.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Actualiza la lista de películas con los resultados obtenidos desde la API.
     * Convierte los datos de la API en objetos Movie y los agrega al adaptador.
     *
     * @param results Lista de resultados obtenidos desde la API de TMDB.
     */
    private void updateMovieList(List<TMDBMovie.MovieSearchResponse.MovieResult> results) {
        movieList.clear();
        for (TMDBMovie.MovieSearchResponse.MovieResult result : results) {
            Movie movie = new Movie();
            movie.setId(String.valueOf(result.id));
            movie.setTitle(result.title);
            movie.setImageUrl("https://image.tmdb.org/t/p/w500" + result.posterPath);
            movie.setReleaseYear(result.releaseDate);
            movie.setOverview(result.overview);
            movie.setRating(String.valueOf(result.voteAverage));

            // Formatear el rating a un decimal
            String formattedRating = String.format("%.1f", result.voteAverage);
            movie.setRating(formattedRating);

            movieList.add(movie);
        }
        movieAdapter.notifyDataSetChanged(); // Refleja los cambios en el RecyclerView
    }
}
