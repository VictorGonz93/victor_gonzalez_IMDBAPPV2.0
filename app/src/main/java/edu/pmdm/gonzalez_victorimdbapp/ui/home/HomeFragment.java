package edu.pmdm.gonzalez_victorimdbapp.ui.home;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.gonzalez_victorimdbapp.R;
import edu.pmdm.gonzalez_victorimdbapp.adapter.MovieAdapter;
import edu.pmdm.gonzalez_victorimdbapp.api.IMDBApiService;
import edu.pmdm.gonzalez_victorimdbapp.models.Movie;
import edu.pmdm.gonzalez_victorimdbapp.models.PopularMoviesResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
    private static final String API_KEY = "55d85b5f3cmsh4613c645ec2f533p1989bajsna5fafe7facca";
    private static final String API_HOST = "imdb-com.p.rapidapi.com";

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

        IMDBApiService apiService = new Retrofit.Builder()
                .baseUrl("https://imdb-com.p.rapidapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(IMDBApiService.class);

        Call<PopularMoviesResponse> call = apiService.getTopMeter(API_KEY, API_HOST, "ALL");

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
