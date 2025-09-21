package com.mdo.gestionpalettes.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.adapters.PaletteDestructionAdapter;
import com.mdo.gestionpalettes.api.ApiClient;
import com.mdo.gestionpalettes.api.ApiService;
import com.mdo.gestionpalettes.models.ValidationDestruction;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DestructionResultActivity extends BaseActivity {

    private ArrayList<ValidationDestruction> palettesValidees;
    private ApiService apiService;

    /**
     * Méthode appelée lors de la création de l'activité DestructionResultActivity.
     * Initialise l'interface, récupère les palettes validées à partir de l'intent,
     * configure la liste et les boutons de retour et de validation.
     *
     * @param savedInstanceState L'état précédemment sauvegardé de l'activité.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destruction_result);

        ListView listViewPalettes = findViewById(R.id.listViewPalettesDestruction);
        Button btnRetour = findViewById(R.id.btnAnnulerDestruction);
        Button btnValider = findViewById(R.id.btnValiderDestruction);

        palettesValidees = getIntent().getParcelableArrayListExtra("palettesValidees");
        if (palettesValidees == null) palettesValidees = new ArrayList<>();

        apiService = ApiClient.getClient(this).create(ApiService.class);

        PaletteDestructionAdapter adapter = new PaletteDestructionAdapter(this, palettesValidees);
        listViewPalettes.setAdapter(adapter);

        btnRetour.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra("palettesValideesRetour", palettesValidees);
            setResult(RESULT_OK, intent);
            finish();
        });

        btnValider.setOnClickListener(v -> {
            if (!isOnline()) {
                Toast.makeText(this, "Hors ligne, impossible de valider.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (palettesValidees.isEmpty()) {
                Toast.makeText(this, "Aucune palette à valider", Toast.LENGTH_SHORT).show();
                return;
            }

            Call<Void> call = apiService.validerDestruction(palettesValidees);
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(DestructionResultActivity.this, "Destruction validée avec succès", Toast.LENGTH_LONG).show();
                        finish();
                    } else if (response.code() == 401) {
                        handleTokenExpired();
                    } else {
                        Toast.makeText(DestructionResultActivity.this, "Erreur lors de la validation : " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Toast.makeText(DestructionResultActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}