package edu.pmdm.gonzalez_victorimdbapp.utils;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import edu.pmdm.gonzalez_victorimdbapp.database.UsersManager;
import edu.pmdm.gonzalez_victorimdbapp.sync.FirebaseUsersSync;


/**
 * Clase que gestiona el ciclo de vida de la aplicación para registrar eventos de login y logout.
 */
public class AppLifecycleManager implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private static final String PREF_NAME = "AppPrefs";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    private static final long LOGOUT_DELAY = 1000; // 1 segundos

    private boolean hasLoggedOut = false;
    private boolean hasLoggedIn = false;
    private boolean isActivityChangingConfigurations = false;
    private int activityReferences = 0;
    private boolean isInBackground = false;
    private boolean isAppClosed = false;

    private final Handler logoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable logoutRunnable = this::handleLogout;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final Context context;

    public AppLifecycleManager(Context context) {
        this.context = context;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        checkForPendingLogout();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        isInBackground = false;
        logoutHandler.removeCallbacks(logoutRunnable);

        // Al reanudar la app, asegurarse de sincronizar con Firestore
        FirebaseUsersSync firebaseUsersSync = new FirebaseUsersSync();
        UsersManager usersManager = new UsersManager(context);
        firebaseUsersSync.syncUsersWithFirestore(usersManager);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        isInBackground = true;
        logoutHandler.postDelayed(logoutRunnable, LOGOUT_DELAY);

        // Sincronizar datos de la base local con Firestore
        FirebaseUsersSync firebaseUsersSync = new FirebaseUsersSync();
        firebaseUsersSync.syncCurrentUserToFirestore();
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (!isActivityChangingConfigurations) {
            activityReferences++;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (!isActivityChangingConfigurations) {
            activityReferences--;
            if (activityReferences == 0 && !isAppClosed) {
                isAppClosed = true;
                logoutHandler.postDelayed(logoutRunnable, LOGOUT_DELAY);
            }
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // No es necesario implementar nada para esta funcionalidad
    }

    @Override
    public void onTrimMemory(int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                registerUserLogout(user);
            }

            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREF_IS_LOGGED_IN, false);
            editor.apply();

            Log.d("AppLifecycleManager", "Logout registrado al minimizar la aplicación.");
        }
    }


    private void handleLogout() {
        if (!hasLoggedOut) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                registerUserLogout(user);
            }

            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREF_IS_LOGGED_IN, false);
            editor.apply();

            hasLoggedOut = true;
            hasLoggedIn = false;
            Log.d("AppLifecycleManager", "Logout automático ejecutado.");
        } else {
            Log.d("AppLifecycleManager", "Logout ya registrado previamente.");
        }
    }

    private void logUserLogin() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String loginTime = dateFormat.format(new Date());
            Map<String, Object> loginEntry = new HashMap<>();
            loginEntry.put("login_time", loginTime);
            loginEntry.put("logout_time", null);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .update("activity_log", FieldValue.arrayUnion(loginEntry))
                    .addOnSuccessListener(aVoid -> {
                        Log.d("AppLifecycleManager", "Login registrado en Firestore.");

                        // Guardar en la base de datos local
                        UsersManager usersManager = new UsersManager(context);
                        usersManager.updateLoginTime(user.getUid(), loginTime);
                    })
                    .addOnFailureListener(e -> Log.e("AppLifecycleManager", "Error registrando login: " + e.getMessage()));
        }
    }

    private void registerUserLogout(FirebaseUser user) {
        String logoutTime = dateFormat.format(new Date());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("activity_log")) {
                        List<Map<String, Object>> activityLog =
                                (List<Map<String, Object>>) documentSnapshot.get("activity_log");

                        if (activityLog != null && !activityLog.isEmpty()) {
                            Map<String, Object> lastLogin = activityLog.get(activityLog.size() - 1);

                            if (lastLogin.get("logout_time") == null) {
                                lastLogin.put("logout_time", logoutTime);

                                FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(user.getUid())
                                        .update("activity_log", activityLog)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("AppLifecycleManager", "Logout registrado correctamente en Firestore.");

                                            // Guardar en la base de datos local
                                            UsersManager usersManager = new UsersManager(context);
                                            usersManager.updateLogoutTime(user.getUid(), logoutTime);
                                        })
                                        .addOnFailureListener(e -> Log.e("AppLifecycleManager", "Error registrando logout: " + e.getMessage()));
                            }
                        }
                    }
                });
    }

    private void checkForPendingLogout() {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean wasLoggedIn = preferences.getBoolean(PREF_IS_LOGGED_IN, false);

        if (wasLoggedIn) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                registerUserLogout(currentUser);
            }

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREF_IS_LOGGED_IN, false);
            editor.apply();

            Log.d("AppLifecycleManager", "Logout pendiente registrado al reiniciar la app.");
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration) {

    }

    @Override
    public void onLowMemory() {

    }
}
