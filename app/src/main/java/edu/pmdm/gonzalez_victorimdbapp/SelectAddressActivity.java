package edu.pmdm.gonzalez_victorimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import java.util.Arrays;
import java.util.List;

public class SelectAddressActivity extends AppCompatActivity {

    private EditText editSelectedAddress;
    private ImageView staticMapImageView;
    private PlacesClient placesClient;
    private String selectedAddress;

    // ActivityResultLauncher para manejar el resultado de Autocomplete
    private final ActivityResultLauncher<Intent> autocompleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    selectedAddress = place.getAddress();
                    editSelectedAddress.setText(selectedAddress);

                    // Mostrar la imagen estática del lugar seleccionado
                    if (place.getLatLng() != null) {
                        String staticMapUrl = getStaticMapUrl(place.getLatLng().latitude, place.getLatLng().longitude);
                        Glide.with(this).load(staticMapUrl).into(staticMapImageView);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_address);

        editSelectedAddress = findViewById(R.id.editSelectedAddress);
        staticMapImageView = findViewById(R.id.staticMapImageView);
        Button btnSearchAddress = findViewById(R.id.btnSearchAddress);
        Button btnConfirmAddress = findViewById(R.id.btnConfirmAddress);

        // Inicializar Places API con la API Key
        Places.initialize(getApplicationContext(), "AIzaSyAER7D-uvYpBOG3wZjz9z3AeGulqAci-OU");
        placesClient = Places.createClient(this);

        // Botón para buscar direcciones
        btnSearchAddress.setOnClickListener(v -> launchPlacesAutocomplete());

        // Botón para confirmar la dirección seleccionada y devolverla a EditUserActivity
        btnConfirmAddress.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("SELECTED_ADDRESS", selectedAddress);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    /**
     * Lanza el Autocomplete de Google Places API para buscar direcciones.
     */
    private void launchPlacesAutocomplete() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        autocompleteLauncher.launch(intent);
    }

    /**
     * Genera la URL de la imagen estática de Google Maps.
     *
     * @param latitud  Latitud del lugar.
     * @param longitud Longitud del lugar.
     * @return URL de la imagen estática.
     */
    private String getStaticMapUrl(double latitud, double longitud) {
        String apiKey = "AIzaSyAER7D-uvYpBOG3wZjz9z3AeGulqAci-OU"; // Reemplaza con tu API Key
        String size = "600x300"; // Tamaño de la imagen (ancho x alto)
        String markers = "color:red|" + latitud + "," + longitud; // Marcador en la ubicación
        return "https://maps.googleapis.com/maps/api/staticmap?" +
                "center=" + latitud + "," + longitud +
                "&zoom=15" +
                "&size=" + size +
                "&maptype=roadmap" +
                "&markers=" + markers +
                "&key=" + apiKey;
    }
}