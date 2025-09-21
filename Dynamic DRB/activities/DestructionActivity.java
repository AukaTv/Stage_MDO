package com.mdo.gestionpalettes.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.adapters.PaletteADetruireAdapter;
import com.mdo.gestionpalettes.api.ApiClient;
import com.mdo.gestionpalettes.api.ApiService;
import com.mdo.gestionpalettes.models.PaletteInfosResponse;
import com.mdo.gestionpalettes.models.ValidationDestruction;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DestructionActivity extends BaseActivity {

    private ListView listViewADetruire;
    private EditText editTextNumPaletteDestruction;
    private PaletteADetruireAdapter adapter;

    private final ArrayList<PaletteInfosResponse> palettesADetruire = new ArrayList<>();
    private ArrayList<ValidationDestruction> palettesValidees = new ArrayList<>();

    private ApiService apiService;

    private final ActivityResultLauncher<Intent> destructionResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<ValidationDestruction> nouvellesValidees =
                            result.getData().getParcelableArrayListExtra("palettesValideesRetour");
                    if(nouvellesValidees != null) {
                        palettesValidees = nouvellesValidees;
                        chargerPalettesADetruire();
                    }
                }
            }
    );

    /**
     * Méthode appelée lors de la création de l'activité.
     * Initialise les composants d'interface, les boutons, les listeners et lance le chargement initial des palettes à détruire.
     * Gère également le champ de scan automatique avec recentrage du focus.
     *
     * @param savedInstanceState État de l'activité précédemment sauvegardé (null si nouveau lancement)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destruction);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listViewADetruire = findViewById(R.id.listViewADetruire);
        editTextNumPaletteDestruction = findViewById(R.id.editTextNumPaletteDestruction);
        Button btnScannerPaletteDestruction = findViewById(R.id.btnScannerPaletteDestruction);
        Button btnFinirDestruction = findViewById(R.id.btnFinirDestruction);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        chargerPalettesADetruire();

        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        editTextNumPaletteDestruction.requestFocus();

        btnScannerPaletteDestruction.setOnClickListener(v -> {
            if(!isOnline()) {
                Toast.makeText(this, "Hors ligne, impossible de valider.", Toast.LENGTH_SHORT).show();
                return;
            }

            String numPalette = editTextNumPaletteDestruction.getText().toString().trim();
            if(numPalette.isEmpty()) {
                Toast.makeText(this, "Veuillez scanner une palette", Toast.LENGTH_SHORT).show();
                return;
            }

            verifierEtValiderPalette(numPalette);
        });

        btnFinirDestruction.setOnClickListener(v -> {
            Intent intent = new Intent(this, DestructionResultActivity.class);
            intent.putParcelableArrayListExtra("palettesValidees", palettesValidees);
            destructionResultLauncher.launch(intent);
        });
    }

    /**
     * Effectue un appel à l'API pour récupérer la liste des palettes à détruire.
     * Affiche les résultats dans un ListView via un adapter.
     * Gère les erreurs réseau et le statut HTTP 401 (token expiré).
     */
    private void chargerPalettesADetruire() {
        if(!isOnline()) {
            Toast.makeText(this, "Hors ligne, impossible de charger les palettes.", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<List<PaletteInfosResponse>> call = apiService.getPalettesADetruire();
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<PaletteInfosResponse>> call, @NonNull Response<List<PaletteInfosResponse>> response) {
                if(response.isSuccessful() && response.body() != null) {
                    palettesADetruire.clear();
                    palettesADetruire.addAll(response.body());
                    adapter = new PaletteADetruireAdapter(DestructionActivity.this, palettesADetruire);
                    listViewADetruire.setAdapter(adapter);
                } else if(response.code() == 401) {
                    handleTokenExpired();
                } else {
                    Toast.makeText(DestructionActivity.this, "Erreur chargement : " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PaletteInfosResponse>> call, @NonNull Throwable t) {
                Toast.makeText(DestructionActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Vérifie si la palette scannée est présente dans la liste des palettes à détruire.
     * Si oui, elle est ajoutée à la liste des validations et retirée de l'affichage.
     *
     * @param numPalette Numéro de la palette scannée à vérifier.
     */
    private void verifierEtValiderPalette(String numPalette) {
        PaletteInfosResponse paletteTrouvee = null;
        for(PaletteInfosResponse palette : palettesADetruire) {
            if(palette.getNum_palette().equalsIgnoreCase(numPalette)) {
                paletteTrouvee = palette;
                break;
            }
        }

        if(paletteTrouvee == null) {
            Toast.makeText(this, "Cette palette n'est pas à détruire", Toast.LENGTH_SHORT).show();
            return;
        }

        ValidationDestruction validation = new ValidationDestruction(
                paletteTrouvee.getNum_palette(),
                0,
                paletteTrouvee.getEmplacement()
        );

        palettesValidees.add(validation);
        palettesADetruire.remove(paletteTrouvee);
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Palette ajoutée à la liste des destructions", Toast.LENGTH_SHORT).show();
        editTextNumPaletteDestruction.setText("");
        editTextNumPaletteDestruction.requestFocus();
    }

    /**
     * Méthode déclenchée lors du clic sur le bouton retour dans la barre d'action.
     * Termine simplement l'activité courante.
     *
     * @return true pour indiquer que l'action a été traitée.
     */
    @Override
    public boolean onSupportNavigateUp() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Quitter la destruction")
                .setMessage("Êtes-vous sûr de vouloir quitter le mode destruction ?")
                .setPositiveButton("Oui", (dialog, which) -> finish())
                .setNegativeButton("Non", null)
                .show();
        return true;
    }
}
