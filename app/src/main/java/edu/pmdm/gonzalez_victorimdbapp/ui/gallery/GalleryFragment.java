package edu.pmdm.gonzalez_victorimdbapp.ui.gallery;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.gonzalez_victorimdbapp.R;
import edu.pmdm.gonzalez_victorimdbapp.adapter.MovieAdapter;
import edu.pmdm.gonzalez_victorimdbapp.database.FavoritesManager;
import edu.pmdm.gonzalez_victorimdbapp.models.Movie;

/**
 * Fragmento para mostrar las películas favoritas del usuario.
 * Permite visualizar las películas favoritas y compartirlas en formato JSON vía Bluetooth.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class GalleryFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovieAdapter movieAdapter;
    private List<Movie> favoriteMovies = new ArrayList<>();
    private FavoritesManager favoritesManager;
    private TextView emptyView;
    private Button btnShare;
    // Variable para almacenar temporalmente el JSON a compartir
    private String jsonFavoritesToShare;

    // Lanzadores de permisos modernos
    private final ActivityResultLauncher<String[]> bluetoothPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    // Llamar a compartir cuando los permisos son otorgados
                    shareFavoritesViaBluetooth(jsonFavoritesToShare);
                } else {
                    Toast.makeText(requireContext(), "Se requieren permisos para compartir vía Bluetooth.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        recyclerView = root.findViewById(R.id.recyclerView);
        emptyView = root.findViewById(R.id.emptyView);
        btnShare = root.findViewById(R.id.btnShare);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        favoritesManager = new FavoritesManager(getContext());

        // Obtener películas favoritas del usuario
        favoriteMovies = favoritesManager.getFavorites(userEmail);

        // Manejar visibilidad de la vista vacía
        updateEmptyViewVisibility();

        // Configurar adaptador
        movieAdapter = new MovieAdapter(favoriteMovies, true); // Modo favoritos
        recyclerView.setAdapter(movieAdapter);

        btnShare.setOnClickListener(v -> {
            // Generar JSON de las películas favoritas
            Gson gson = new Gson();
            jsonFavoritesToShare = gson.toJson(favoriteMovies); // Guardar el JSON en la variable

            if (favoriteMovies.isEmpty()) {
                // Mostrar un mensaje si la lista de favoritos está vacía
                Toast.makeText(requireContext(), "No se puede compartir porque tu lista de favoritos está vacía.", Toast.LENGTH_SHORT).show();
            } else {
                // Comprobar permisos y compartir si la lista no está vacía
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31+)
                    bluetoothPermissionLauncher.launch(new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                } else {
                    // Para versiones anteriores a Android 12
                    bluetoothPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                }
            }
        });


        return root;
    }

    /**
     * Comparte las películas favoritas del usuario en formato JSON usando Bluetooth.
     * Muestra un diálogo previo con el contenido en JSON antes de iniciar el proceso de compartir.
     * @param jsonFavorites las películas favoritas en formato JSON.
     */
    private void shareFavoritesViaBluetooth(String jsonFavorites) {
        // Mostrar el JSON en un AlertDialog antes de proceder con Bluetooth
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Películas Favoritas en JSON");
        builder.setMessage(jsonFavorites);
        builder.setPositiveButton("Compartir", (dialog, which) -> {
            // Crear un intent para compartir específicamente a través de Bluetooth
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, jsonFavorites);
            intent.setType("text/plain");

            // Buscar aplicaciones que manejen la acción Bluetooth
            PackageManager packageManager = requireActivity().getPackageManager();
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            boolean bluetoothAppFound = false;
            for (ResolveInfo resolveInfo : resolveInfos) {
                String packageName = resolveInfo.activityInfo.packageName;
                if (packageName.toLowerCase().contains("bluetooth")) { // Verificar si es una app de Bluetooth
                    intent.setPackage(packageName);
                    bluetoothAppFound = true;
                    break;
                }
            }

            if (bluetoothAppFound) {
                startActivity(intent); // Enviar a la app de Bluetooth encontrada
            } else {
                Toast.makeText(requireContext(), "No se encontró una aplicación Bluetooth.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Actualiza la visibilidad de la vista vacía o la lista de favoritos según el contenido de la lista.
     * Muestra un mensaje si la lista de favoritos está vacía.
     */
    private void updateEmptyViewVisibility() {
        if (favoriteMovies.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Recarga la lista de favoritos desde la base de datos al reanudar el fragmento.
     * Actualiza el RecyclerView y la visibilidad de la vista vacía si es necesario.
     */
    @Override
    public void onResume() {
        super.onResume();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Recargar la lista de favoritos desde la base de datos
        favoriteMovies.clear();
        favoriteMovies.addAll(favoritesManager.getFavorites(userEmail));
        movieAdapter.notifyDataSetChanged();

        // Actualizar visibilidad
        updateEmptyViewVisibility();
    }

    /**
     * Limpia las referencias a las vistas al destruir la vista del fragmento, para evitar fugas de memoria.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        emptyView = null;
    }
}
