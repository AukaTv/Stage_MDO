package com.mdo.gestionpalettes.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mdo.gestionpalettes.api.ApiClient;
import com.mdo.gestionpalettes.api.ApiService;
import com.mdo.gestionpalettes.utils.NetworkMonitor;

public abstract class BaseActivity extends AppCompatActivity {

    protected ApiService apiService;
    protected String token;
    private boolean isOnline = true;

    /**
     * Méthode appelée lors de la création de l'activité.
     * Initialise l'API avec le token stocké, et enregistre un moniteur réseau pour détecter
     * les changements de connectivité (connexion/déconnexion).
     *
     * @param savedInstanceState L'état de l'activité s'il existe (restauration).
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        token = prefs.getString("token", "");

        apiService = ApiClient.getClient(this).create(ApiService.class);

        NetworkMonitor.register(this, new NetworkMonitor.NetworkCallback() {
            @Override
            public void onConnected(String type) {
                runOnUiThread(() -> {
                    isOnline = true;
                    onNetworkConnected(type);
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    isOnline = false;
                    onNetworkDisconnected();
                });
            }
        });
    }

    /**
     * Méthode à surcharger dans les classes filles.
     * Elle est appelée automatiquement lorsqu'une connexion réseau est détectée.
     *
     * @param type Le type de connexion détectée (Wi-Fi, Mobile, etc.).
     */
    protected void onNetworkConnected(String type) {
        Toast.makeText(this, "Connecté via " + type, Toast.LENGTH_SHORT).show();
    }

    /**
     * Méthode à surcharger dans les classes filles.
     * Elle est appelée automatiquement lorsqu'une perte de réseau est détectée.
     */
    protected void onNetworkDisconnected() {
        Toast.makeText(this, "Déconnecté du réseau", Toast.LENGTH_LONG).show();
    }

    /**
     * Méthode utilitaire pour vérifier l'état de la connectivité réseau.
     *
     * @return true si l'appareil est connecté, false sinon.
     */
    protected boolean isOnline() {
        return isOnline;
    }

    /**
     * Méthode appelée globalement lorsque le token est expiré.
     * Supprime le token stocké et redirige l'utilisateur vers l'écran de connexion.
     */
    protected void handleTokenExpired() {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        prefs.edit().remove("token").apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("reason", "expired");
        startActivity(intent);
        finish();
    }
}