package edu.pmdm.gonzalez_victorimdbapp.utils;

import android.util.Log;
import edu.pmdm.gonzalez_victorimdbapp.api.IMDBApiService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class IMDBApiClient {

    private static final String BASE_URL = "https://imdb-com.p.rapidapi.com/";
    private static final String TAG = "IMDBApiClient";
    private static IMDBApiService apiService;
    private static RapidApiKeyManager apiKeyManager = new RapidApiKeyManager();

    // Obtiene el servicio API
    public static IMDBApiService getApiService() {
        if (apiService == null) {
            Log.d(TAG, "Creando Retrofit con BASE_URL: " + BASE_URL);
            // Imprime la clave actual que se usará
            Log.d(TAG, "Usando API Key: " + getApiKey());
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(IMDBApiService.class);
        }
        return apiService;
    }

    // Obtiene la clave actual
    public static String getApiKey() {
        return apiKeyManager.getCurrentKey();
    }

    // Cambia a la siguiente clave
    public static void switchApiKey() {
        try {
            apiKeyManager.switchToNextKey();
            Log.d(TAG, "Clave API cambiada. Nueva clave: " + getApiKey());
            // Si fuera necesario, podrías reinicializar apiService para que se use la nueva clave en futuras llamadas.
            apiService = null;  // Se forzará la recreación del servicio en el siguiente getApiService()
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }
}
