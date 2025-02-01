package edu.pmdm.gonzalez_victorimdbapp.utils;

import edu.pmdm.gonzalez_victorimdbapp.api.IMDBApiService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class IMDBApiClient {

    private static final String BASE_URL = "https://imdb-com.p.rapidapi.com/";
    private static IMDBApiService apiService;
    private static RapidApiKeyManager apiKeyManager = new RapidApiKeyManager();

    // Obtiene el servicio API
    public static IMDBApiService getApiService() {
        if (apiService == null) {
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
        } catch (IllegalStateException e) {
            // Maneja el caso donde no hay más claves disponibles
            System.err.println("Error: No hay más claves disponibles.");
        }
    }
}
