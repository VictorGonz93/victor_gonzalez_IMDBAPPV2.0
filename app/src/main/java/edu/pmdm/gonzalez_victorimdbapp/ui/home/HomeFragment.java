package edu.pmdm.gonzalez_victorimdbapp.ui.home;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.gonzalez_victorimdbapp.utils.IMDBApiClient;
import edu.pmdm.gonzalez_victorimdbapp.R;
import edu.pmdm.gonzalez_victorimdbapp.adapter.MovieAdapter;
import edu.pmdm.gonzalez_victorimdbapp.api.IMDBApiService;
import edu.pmdm.gonzalez_victorimdbapp.models.Movie;
import edu.pmdm.gonzalez_victorimdbapp.models.PopularMoviesResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragmento para mostrar una lista de las películas más populares.
 * Obtiene los datos de las películas desde la API de IMDB y los muestra en un RecyclerView.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovieAdapter movieAdapter;
    private List<Movie> movieList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = root.findViewById(R.id.recyclerView);

        // Configurar RecyclerView con GridLayoutManager
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columnas

        movieAdapter = new MovieAdapter(movieList, false); // No está en modo favoritos
        recyclerView.setAdapter(movieAdapter);

        // Llamar a fetchMovies() solo si la lista está vacía
        if (movieList.isEmpty()) {
            fetchMovies();
        }

        return root;
    }

    /**
     * Realiza la llamada a la API de IMDB para obtener las películas más populares.
     * Limita los resultados a los primeros 10 elementos y actualiza la lista de películas y el RecyclerView.
     */
    private void fetchMovies() {
        IMDBApiService apiService = IMDBApiClient.getApiService();

        Call<PopularMoviesResponse> call = apiService.getTopMeter(
                IMDBApiClient.getApiKey(), // Obtén la clave actual desde el gestor de claves
                "imdb-com.p.rapidapi.com",
                "ALL"
        );

        call.enqueue(new Callback<PopularMoviesResponse>() {
            @Override
            public void onResponse(Call<PopularMoviesResponse> call, Response<PopularMoviesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PopularMoviesResponse.Edge> edges = response.body().getData().getTopMeterTitles().getEdges();

                    // Limitar a los primeros 10 elementos
                    int limit = Math.min(edges.size(), 10);
                    for (int i = 0; i < limit; i++) {
                        PopularMoviesResponse.Node node = edges.get(i).getNode();

                        // Extraer los datos de la película
                        String id = node.getId();
                        String title = node.getTitleText() != null ? node.getTitleText().getText() : "Título desconocido";
                        String imageUrl = node.getPrimaryImage() != null ? node.getPrimaryImage().getUrl() : null;
                        String releaseYear = node.getReleaseYear() != null ? String.valueOf(node.getReleaseYear().getYear()) : "Fecha desconocida";

                        // Crear un objeto Movie con los datos básicos
                        Movie movie = new Movie();
                        movie.setId(id);
                        movie.setTitle(title);
                        movie.setImageUrl(imageUrl);
                        movie.setReleaseYear(releaseYear);

                        // Añadir la película a la lista
                        movieList.add(movie);
                    }

                    // Notificar al adaptador para actualizar el RecyclerView
                    movieAdapter.notifyDataSetChanged();
                } else if (response.code() == 429) { // Código HTTP 429: Límite de llamadas excedido
                    IMDBApiClient.switchApiKey(); // Cambia a la siguiente clave
                    fetchMovies(); // Reintenta la solicitud con la nueva clave
                } else {
                    Log.e("API_ERROR", "Respuesta no exitosa: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PopularMoviesResponse> call, Throwable t) {
                Log.e("API_ERROR", "Error en la llamada a la API", t);
            }
        });
    }

}
