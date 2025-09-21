package com.mdo.gestionpalettes.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.adapters.PaletteConsultationAdapter;
import com.mdo.gestionpalettes.models.PaletteConsultation;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConsultationActivity extends BaseActivity {

    private EditText etNumPalette, etClient, etArticle, etEmplacement;
    private Spinner spinnerStatut;
    private TextView tvEmpty;
    private PaletteConsultationAdapter adapter;

    /**
     * Méthode appelée lors de la création de l'activité.
     * Initialise les composants d'interface, configure les filtres et déclenche
     * un premier chargement des palettes sans filtre.
     *
     * @param savedInstanceState L'état sauvegardé de l'activité.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etNumPalette = findViewById(R.id.etNumPalette);
        etClient = findViewById(R.id.etClient);
        etArticle = findViewById(R.id.etArticle);
        etEmplacement = findViewById(R.id.etEmplacement);
        spinnerStatut = findViewById(R.id.spinnerStatut);
        Button btnRechercher = findViewById(R.id.btnRechercher);
        RecyclerView rvPalettes = findViewById(R.id.rvPalettes);
        tvEmpty = findViewById(R.id.tvEmpty);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Tous", "En stock", "À Détruire", "Détruite", "En Prod", "A Renvoyer", "Renvoyé", "A Inventorier"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatut.setAdapter(spinnerAdapter);

        adapter = new PaletteConsultationAdapter(new ArrayList<>());
        rvPalettes.setAdapter(adapter);
        rvPalettes.setLayoutManager(new LinearLayoutManager(this));

        btnRechercher.setOnClickListener(v -> chargerPalettes(null));
    }

    /**
     * Méthode principale qui interroge l'API avec les filtres saisis par l'utilisateur.
     * Gère les erreurs réseau, les codes d'erreur HTTP, et l'affichage du message "vide".
     */
    private void chargerPalettes(String statutDefault) {
        if (!isOnline()) { // Méthode héritée de BaseActivity
            Toast.makeText(this, "Hors ligne, impossible de consulter.", Toast.LENGTH_SHORT).show();
            return;
        }

        String numPalette = etNumPalette.getText().toString().trim();
        String client = etClient.getText().toString().trim();
        String article = etArticle.getText().toString().trim();
        String statut = (statutDefault != null) ? statutDefault : spinnerStatut.getSelectedItem().toString();
        if ("Tous".equals(statut)) {
            statut = null;
        }
        String emplacement = etEmplacement.getText().toString().trim();

        Call<List<PaletteConsultation>> call = apiService.consulterPalettes(
                numPalette.isEmpty() ? null : numPalette,
                client.isEmpty() ? null : client,
                article.isEmpty() ? null : article,
                statut,
                emplacement.isEmpty() ? null : emplacement
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<PaletteConsultation>> call, @NonNull Response<List<PaletteConsultation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PaletteConsultation> palettes = response.body();
                    adapter.setPalettes(palettes);
                    tvEmpty.setVisibility(palettes.isEmpty() ? View.VISIBLE : View.GONE);
                } else if (response.code() == 401) {
                    handleTokenExpired();
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    adapter.setPalettes(new ArrayList<>());
                    Toast.makeText(ConsultationActivity.this, "Erreur lors de la récupération.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PaletteConsultation>> call, @NonNull Throwable t) {
                tvEmpty.setVisibility(View.VISIBLE);
                adapter.setPalettes(new ArrayList<>());
                Toast.makeText(ConsultationActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Méthode déclenchée lors du clic sur le bouton retour dans la barre d'action.
     * Termine simplement l'activité courante.
     *
     * @return true pour indiquer que l'action a été traitée.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
