package com.mdo.gestionpalettes.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.adapters.PaletteRenvoieAdapter;
import com.mdo.gestionpalettes.api.ApiClient;
import com.mdo.gestionpalettes.api.ApiService;
import com.mdo.gestionpalettes.models.ValidationRenvoie;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RenvoieResultActivity extends BaseActivity {

    private ArrayList<ValidationRenvoie> palettesValidees;
    private ApiService apiService;

    /**
     * Méthode appelée lors de la création de l'activité RenvoieResultActivity.
     * Affiche la liste des palettes validées pour le renvoi, permet à l'utilisateur
     * de revenir en arrière ou de valider définitivement le renvoi en envoyant les données à l'API.
     *
     * @param savedInstanceState L'état sauvegardé de l'activité, s'il existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_renvoie_result);

        ListView listViewPalettes = findViewById(R.id.listViewPalettesRenvoie);
        Button btnRetour = findViewById(R.id.btnAnnulerRenvoie);
        Button btnValider = findViewById(R.id.btnValiderRenvoie);

        palettesValidees = getIntent().getParcelableArrayListExtra("palettesValidees");
        if (palettesValidees == null) palettesValidees = new ArrayList<>();

        apiService = ApiClient.getClient(this).create(ApiService.class);

        PaletteRenvoieAdapter adapter = new PaletteRenvoieAdapter(this, palettesValidees);
        listViewPalettes.setAdapter(adapter);

        btnRetour.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra("palettesValideesRetour", palettesValidees);
            setResult(RESULT_OK, intent);
            finish();
        });

        btnValider.setOnClickListener(v -> {
            if (palettesValidees.isEmpty()) {
                Toast.makeText(this, "Aucune palette à valider", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isOnline()) {
                Toast.makeText(this, "Hors ligne, impossible de valider.", Toast.LENGTH_SHORT).show();
                return;
            }

            Call<Void> call = apiService.validerRenvoie(palettesValidees);
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(RenvoieResultActivity.this, "Renvoie validé avec succès", Toast.LENGTH_LONG).show();
                        finish();
                    } else if (response.code() == 401) {
                        handleTokenExpired();
                    } else {
                        Toast.makeText(RenvoieResultActivity.this, "Erreur lors de la validation : " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Toast.makeText(RenvoieResultActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}