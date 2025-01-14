package edu.pmdm.gonzalez_victorimdbapp.ui.slideshow;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.pmdm.gonzalez_victorimdbapp.MovieListActivity;
import edu.pmdm.gonzalez_victorimdbapp.R;
import edu.pmdm.gonzalez_victorimdbapp.api.TMDBApiService;
import edu.pmdm.gonzalez_victorimdbapp.models.TMDBMovie;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Fragmento que permite a los usuarios buscar películas por género y año.
 * Muestra un spinner con géneros de películas obtenidos desde la API de TMDB
 * y un campo de texto para ingresar el año.
 * Los resultados de la búsqueda se muestran en `MovieListActivity`.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class SlideshowFragment extends Fragment {

    private Spinner genreSpinner;
    private EditText yearEditText;
    private Button searchButton;

    private List<TMDBMovie.GenresResponse.Genre> genreList = new ArrayList<>();
    private final String apiKey = "e9f84dbecca6c65f600d95bee2badcf5";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_slideshow, container, false);

        // Inicializa las vistas
        genreSpinner = root.findViewById(R.id.genre_spinner);
        yearEditText = root.findViewById(R.id.year_edit_text);
        searchButton = root.findViewById(R.id.search_button);

        // Cargar géneros
        fetchGenres();

        // Configurar el botón de búsqueda
        searchButton.setOnClickListener(v -> {
            try {
                String selectedGenre = genreSpinner.getSelectedItem().toString();
                String year = yearEditText.getText().toString().trim();

                if (selectedGenre.isEmpty()) {
                    Toast.makeText(requireContext(), "Por favor, selecciona un género.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (year.isEmpty()) {
                    Toast.makeText(requireContext(), "Por favor, introduce un año.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int yearInt;
                try {
                    yearInt = Integer.parseInt(year);
                    if (yearInt < 1940 || yearInt > Calendar.getInstance().get(Calendar.YEAR)) {
                        Toast.makeText(requireContext(), "Introduce un año entre 1940 y el año actual.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Introduce un año válido.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int genreId = getGenreId(selectedGenre);
                if (genreId == -1) {
                    Toast.makeText(requireContext(), "No se encontró el género seleccionado.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Iniciar MovieListActivity con los parámetros seleccionados
                Intent intent = new Intent(requireContext(), MovieListActivity.class);
                intent.putExtra("GENRE_ID", genreId);
                intent.putExtra("YEAR", year);
                startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(requireContext(), "Ocurrió un error durante la búsqueda.", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    /**
     * Realiza una llamada a la API de TMDB para obtener los géneros de películas.
     * Los géneros obtenidos se utilizan para llenar el spinner.
     */
    private void fetchGenres() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TMDBApiService apiService = retrofit.create(TMDBApiService.class);
        Call<TMDBMovie.GenresResponse> call = apiService.getGenres(apiKey, "en-US");

        call.enqueue(new Callback<TMDBMovie.GenresResponse>() {
            @Override
            public void onResponse(Call<TMDBMovie.GenresResponse> call, Response<TMDBMovie.GenresResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    genreList = response.body().genres;
                    List<String> genreNames = new ArrayList<>();
                    for (TMDBMovie.GenresResponse.Genre genre : genreList) {
                        genreNames.add(genre.name);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_item, genreNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    genreSpinner.setAdapter(adapter);
                } else {
                    Toast.makeText(requireContext(), "No se pudieron cargar los géneros.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TMDBMovie.GenresResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "Error al cargar géneros: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Busca el ID del género seleccionado a partir de su nombre.
     * @param genreName el nombre del género.
     * @return el ID del género o -1 si no se encuentra.
     */
    private int getGenreId(String genreName) {
        for (TMDBMovie.GenresResponse.Genre genre : genreList) {
            if (genre.name.equalsIgnoreCase(genreName)) {
                return genre.id;
            }
        }
        return -1; // Devuelve -1 si no se encuentra el género
    }
}
