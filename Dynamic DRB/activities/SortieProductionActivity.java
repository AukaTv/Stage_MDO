package com.mdo.gestionpalettes.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.adapters.PaletteConsultationAdapter;
import com.mdo.gestionpalettes.api.ApiClient;
import com.mdo.gestionpalettes.api.ApiService;
import com.mdo.gestionpalettes.models.PaletteConsultation;
import com.mdo.gestionpalettes.models.ValidationSortieProduction;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SortieProductionActivity extends BaseActivity {

    private EditText editTextNumPalette;
    private PaletteConsultationAdapter adapter;

    private final List<PaletteConsultation> palettesEnStock = new ArrayList<>();
    private ArrayList<ValidationSortieProduction> palettesSelectionnees = new ArrayList<>();

    private ApiService apiService;

    private final ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<ValidationSortieProduction> nouvellesSelectionnees =
                            result.getData().getParcelableArrayListExtra("palettesSelectionneesRetour");
                    if(nouvellesSelectionnees != null) {
                        palettesSelectionnees = nouvellesSelectionnees;
                    }
                }
            }
    );

    /**
     * Gère le comportement de retour en arrière dans la barre d'action.
     * Termine l'activité actuelle et retourne à l'activité précédente.
     *
     * @return true pour indiquer que l'action a été traitée.
     */
    @Override
    public boolean onSupportNavigateUp() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Quitter la sortie production")
                .setMessage("Êtes-vous sûr de vouloir quitter le mode production ?")
                .setPositiveButton("Oui", (dialog, which) -> finish())
                .setNegativeButton("Non", null)
                .show();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sortie_production);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerViewPalettes = findViewById(R.id.recyclerViewPalettes);
        editTextNumPalette = findViewById(R.id.editTextNumPalette);
        Button btnAjouter = findViewById(R.id.btnAjouterPalette);
        Button btnVoirSelection = findViewById(R.id.btnVoirSelection);

        recyclerViewPalettes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaletteConsultationAdapter(new ArrayList<>());
        recyclerViewPalettes.setAdapter(adapter);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        if (isOnline()) {
            chargerPalettesEnStock();
        } else {
            Toast.makeText(this, "Hors ligne, impossible de charger les palettes.", Toast.LENGTH_SHORT).show();
        }

        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        editTextNumPalette.requestFocus();

        btnAjouter.setOnClickListener(v -> {
            String numPalette = editTextNumPalette.getText().toString().trim();
            if(numPalette.isEmpty()) {
                Toast.makeText(this, "Veuillez scanner ou saisir un numéro de palette", Toast.LENGTH_SHORT).show();
                return;
            }
            verifierEtAjouterPalette(numPalette);
        });

        btnVoirSelection.setOnClickListener(v -> {
            Intent intent = new Intent(this, SortieProductionResultActivity.class);
            intent.putParcelableArrayListExtra("palettesSelectionnees", palettesSelectionnees);
            resultLauncher.launch(intent);
        });
    }

    /**
     * Envoie une requête à l'API pour récupérer les palettes actuellement en stock.
     * Met à jour le RecyclerView avec les palettes reçues ou affiche un message d'erreur en cas d'échec.
     */
    private void chargerPalettesEnStock() {
        Call<List<PaletteConsultation>> call = apiService.getPalettesEnStock();
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<PaletteConsultation>> call, @NonNull Response<List<PaletteConsultation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    palettesEnStock.clear();
                    palettesEnStock.addAll(response.body());
                    adapter.setPalettes(palettesEnStock);
                } else if (response.code() == 401) {
                    handleTokenExpired();
                } else {
                    Toast.makeText(SortieProductionActivity.this, "Erreur chargement: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<PaletteConsultation>> call, @NonNull Throwable t) {
                Toast.makeText(SortieProductionActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Vérifie si la palette saisie est bien en stock.
     * Si oui, l'ajoute à la sélection et la retire de la liste des palettes disponibles.
     *
     * @param numPalette Le numéro de la palette à vérifier et ajouter.
     */
    private void verifierEtAjouterPalette(String numPalette) {
        PaletteConsultation paletteTrouvee = null;
        for(PaletteConsultation p : palettesEnStock) {
            if(p.getNum_palette().equalsIgnoreCase(numPalette)) {
                paletteTrouvee = p;
                break;
            }
        }
        if(paletteTrouvee == null) {
            Toast.makeText(this, "Cette palette n'est pas en stock", Toast.LENGTH_SHORT).show();
            return;
        }

        ValidationSortieProduction validation = new ValidationSortieProduction(
                paletteTrouvee.getNum_palette(),
                paletteTrouvee.getQuantite(),
                "En Prod",
                paletteTrouvee.getEmplacement()
        );

        palettesSelectionnees.add(validation);
        palettesEnStock.remove(paletteTrouvee);
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Palette ajoutée à la sélection", Toast.LENGTH_SHORT).show();
        editTextNumPalette.setText("");
    }
}
