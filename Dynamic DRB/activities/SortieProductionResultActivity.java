package com.mdo.gestionpalettes.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.adapters.PaletteSortieProductionAdapter;
import com.mdo.gestionpalettes.api.ApiClient;
import com.mdo.gestionpalettes.api.ApiService;
import com.mdo.gestionpalettes.models.ValidationSortieProduction;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SortieProductionResultActivity extends BaseActivity {

    /**
     * Méthode appelée lors de la création de l'activité SortieProductionResultActivity.
     * Affiche les palettes sélectionnées pour la sortie production et permet de valider
     * définitivement leur sortie via un appel API ou de revenir à l'écran précédent.
     *
     * @param savedInstanceState L'état précédemment sauvegardé de l'activité, s'il existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sortie_production_result);

        ListView listViewSelectionSortieProd = findViewById(R.id.listViewSelectionSortieProd);
        Button btnValider = findViewById(R.id.btnValiderSortieProd);
        Button btnRetour = findViewById(R.id.btnRetourSortieProd);

        ArrayList<ValidationSortieProduction> palettesSelectionnees =
                getIntent().getParcelableArrayListExtra("palettesSelectionnees");

        PaletteSortieProductionAdapter adapter = new PaletteSortieProductionAdapter(this, palettesSelectionnees);
        listViewSelectionSortieProd.setAdapter(adapter);

        btnRetour.setOnClickListener(v -> finish());

        btnValider.setOnClickListener(v -> {
            if (palettesSelectionnees == null || palettesSelectionnees.isEmpty()) {
                Toast.makeText(this, "Aucune palette à valider", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isOnline()) {
                Toast.makeText(this, "Hors ligne, impossible de valider.", Toast.LENGTH_SHORT).show();
                return;
            }

            ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

            Call<Void> call = apiService.validerSortieProduction(palettesSelectionnees);
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(SortieProductionResultActivity.this, "Sortie validée avec succès", Toast.LENGTH_LONG).show();
                        finish();
                    } else if (response.code() == 401) {
                        handleTokenExpired();
                    } else {
                        Toast.makeText(SortieProductionResultActivity.this, "Erreur validation : " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Toast.makeText(SortieProductionResultActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}