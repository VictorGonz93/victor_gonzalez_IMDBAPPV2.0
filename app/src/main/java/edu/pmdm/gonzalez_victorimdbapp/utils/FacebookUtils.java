package edu.pmdm.gonzalez_victorimdbapp.utils;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;

import org.json.JSONException;

import edu.pmdm.gonzalez_victorimdbapp.MainActivity;

public class FacebookUtils {

    /**
     * Obtiene la imagen y los datos del usuario de Facebook y los envía a MainActivity.
     *
     * @param token     Token de acceso de Facebook.
     * @param context   Contexto de la actividad que llama.
     */
    public static void fetchFacebookUserProfile(AccessToken token, Context context) {
        GraphRequest request = GraphRequest.newMeRequest(token, (jsonObject, response) -> {
            try {
                String name = jsonObject.getString("name");
                String email = jsonObject.has("email") ? jsonObject.getString("email") : "No disponible";
                String photoUrl = "https://graph.facebook.com/" + jsonObject.getString("id") +
                        "/picture?type=large&access_token=" + token.getToken();

                // Llamar a updateUserInfo() en MainActivity
                if (context instanceof MainActivity) {
                    ((MainActivity) context).updateUserInfo(name, email, photoUrl);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(context, "Error al obtener datos de Facebook", Toast.LENGTH_SHORT).show();
            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }
}
