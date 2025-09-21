package com.mdo.gestionpalettes.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.adapters.PaletteValideeAdapter;
import com.mdo.gestionpalettes.api.ApiClient;
import com.mdo.gestionpalettes.api.ApiService;
import com.mdo.gestionpalettes.models.ValidationInventaire;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InventaireResultActivity extends BaseActivity implements PaletteValideeAdapter.OnPaletteSupprimeeListener {

    private ArrayList<ValidationInventaire> palettesValidees;

    /**
     * Méthode appelée à la création de l'activité InventaireResultActivity.
     * Récupère les palettes validées, initialise l'adaptateur pour l'affichage dans la liste,
     * et gère les boutons de retour et de validation avec appel à l'API.
     *
     * @param savedInstanceState État de l'activité sauvegardé, si existant.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventaire_result);

        ListView listViewPalettes = findViewById(R.id.listViewPalettes);
        Button btnValider = findViewById(R.id.btnValiderInventaire);
        Button btnRetour = findViewById(R.id.btnRetourInventaire);

        palettesValidees = getIntent().getParcelableArrayListExtra("palettesValidees");
        if (palettesValidees == null) palettesValidees = new ArrayList<>();

        PaletteValideeAdapter adapter = new PaletteValideeAdapter(this, palettesValidees, this);
        listViewPalettes.setAdapter(adapter);

        btnRetour.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra("palettesValideesRetour", palettesValidees);
            setResult(RESULT_OK, intent);
            finish();
        });

        btnValider.setOnClickListener(v -> {
            if (!isOnline()) {
                Toast.makeText(this, "Connexion réseau requise", Toast.LENGTH_SHORT).show();
                return;
            }
            if (palettesValidees.isEmpty()) {
                Toast.makeText(this, "Aucune palette à valider", Toast.LENGTH_SHORT).show();
                return;
            }

            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
            Log.d("DEBUG_JSON", gson.toJson(palettesValidees));

            ApiService apiService = ApiClient.getInventaireClient(this).create(ApiService.class);

            Call<Void> call = apiService.validerInventaire(palettesValidees);
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.code() == 401) {
                        handleTokenExpired();
                        return;
                    }
                    if (response.isSuccessful()) {
                        Toast.makeText(InventaireResultActivity.this, "Inventaire validé avec succès", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String errorMsg = "Erreur : " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += " | " + response.errorBody().string();
                            }
                        } catch (Exception e) {
                            errorMsg += " | Impossible de lire l'erreur";
                        }
                        Log.e("DEBUG_ERROR", errorMsg);
                        Toast.makeText(InventaireResultActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e("DEBUG_ERROR", "Erreur réseau : " + t.getMessage(), t);
                    Toast.makeText(InventaireResultActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Callback appelé lorsqu'une palette est supprimée via l'adaptateur.
     * Affiche un message de confirmation avec le numéro de la palette.
     *
     * @param palette La palette supprimée.
     */
    @Override
    public void onPaletteSupprimee(ValidationInventaire palette) {
        Toast.makeText(this, "Palette supprimée : " + palette.getNumPalette(), Toast.LENGTH_SHORT).show();
    }
}
