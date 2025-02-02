package edu.pmdm.gonzalez_victorimdbapp.utils;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.Profile;

import org.json.JSONException;

import edu.pmdm.gonzalez_victorimdbapp.MainActivity;

public class FacebookUtils {

    /**
     * Obtiene los datos del perfil de Facebook, usando Profile para nombre y foto,
     * y GraphRequest para obtener el email.
     *
     * @param token   Token de acceso de Facebook.
     * @param context Contexto de la actividad que llama.
     */
    public static void fetchFacebookUserProfile(AccessToken token, Context context) {
        // Primero, obtener datos básicos con Profile
        Profile profile = Profile.getCurrentProfile();
        if (profile == null) {
            Toast.makeText(context, "Perfil no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = profile.getName();
        String photoUrl = profile.getProfilePictureUri(200, 200).toString();

        // Luego, usar GraphRequest para obtener el email
        GraphRequest request = GraphRequest.newMeRequest(token, (jsonObject, response) -> {
            String email = "No disponible";
            try {
                if (jsonObject.has("email")) {
                    email = jsonObject.getString("email");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Actualizar la UI en MainActivity
            if (context instanceof MainActivity) {
                ((MainActivity) context).updateUserInfo(name, email, photoUrl);
            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "email");
        request.setParameters(parameters);
        request.executeAsync();
    }
    /**
     * Retorna la URL de la imagen de perfil de Facebook para el tamaño especificado.
     * Si el perfil no está disponible, retorna null.
     *
     * @param width  Ancho deseado.
     * @param height Alto deseado.
     * @return La URL de la imagen de perfil o null.
     */
    public static String getFacebookProfileImageUrl(int width, int height) {
        Profile profile = Profile.getCurrentProfile();
        if (profile != null) {
            return profile.getProfilePictureUri(width, height).toString();
        }
        return null;
    }
}
