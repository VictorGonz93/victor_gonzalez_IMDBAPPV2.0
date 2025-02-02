package edu.pmdm.gonzalez_victorimdbapp.utils;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class RapidApiKeyManager {

    private static final String TAG = "RapidApiKeyManager";
    private List<String> apiKeys; // Lista de claves
    private int currentIndex; // Índice de la clave actual

    public RapidApiKeyManager() {
        apiKeys = new ArrayList<>();
        apiKeys.add("55d85b5f3cmsh4613c645ec2f533p1989bajsna5fafe7facca");
        apiKeys.add("7b21173970msh599bbda534754d7p1b1873jsn8da7181669a2");
        apiKeys.add("0f0abefa4dmsh005245181cedca9p18dc1cjsn3471bf5fb463");
        currentIndex = 0; // Empezamos con la primera clave
        Log.d(TAG, "Inicializando RapidApiKeyManager con clave inicial: " + getCurrentKey());
    }

    // Obtiene la clave actual
    public String getCurrentKey() {
        if (currentIndex < apiKeys.size()) {
            return apiKeys.get(currentIndex);
        } else {
            throw new IllegalStateException("No hay más claves disponibles.");
        }
    }

    // Cambia a la siguiente clave
    public void switchToNextKey() {
        if (currentIndex + 1 < apiKeys.size()) {
            currentIndex++;
            Log.d(TAG, "Se cambió a la siguiente clave: " + getCurrentKey());
        } else {
            throw new IllegalStateException("No hay más claves disponibles para usar.");
        }
    }
}
