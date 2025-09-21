package com.mdo.gestionpalettes.activities;

import android.content.Context;
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
import com.mdo.gestionpalettes.adapters.PaletteARenvoyerAdapter;
import com.mdo.gestionpalettes.api.ApiClient;
import com.mdo.gestionpalettes.api.ApiService;
import com.mdo.gestionpalettes.models.PaletteInfosResponse;
import com.mdo.gestionpalettes.models.ValidationRenvoie;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RenvoieActivity extends BaseActivity {

    private RecyclerView recyclerViewPalettes;
    private EditText editTextNumPaletteRenvoie;
    private PaletteARenvoyerAdapter adapter;

    private final ArrayList<PaletteInfosResponse> palettesARenvoyer = new ArrayList<>();
    private ArrayList<ValidationRenvoie> palettesValidees = new ArrayList<>();

    private ApiService apiService;

    private final ActivityResultLauncher<Intent> renvoieResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<ValidationRenvoie> nouvellesValidees = result.getData().getParcelableArrayListExtra("palettesValideesRetour");
                    if (nouvellesValidees != null) {
                        palettesValidees = nouvellesValidees;
                        chargerPalettesARenvoyer();
                    }
                }
            }
    );

    /**
     * Méthode appelée à la création de l'activité RenvoieActivity.
     * Initialise l'interface utilisateur, configure les boutons et charge
     * les palettes à renvoyer depuis l'API.
     *
     * @param savedInstanceState L'état précédemment sauvegardé de l'activité.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_renvoie);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerViewPalettes = findViewById(R.id.recyclerViewPalettes);
        recyclerViewPalettes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaletteARenvoyerAdapter(this, palettesARenvoyer);
        recyclerViewPalettes.setAdapter(adapter);

        editTextNumPaletteRenvoie = findViewById(R.id.editTextNumPaletteRenvoie);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        editTextNumPaletteRenvoie.requestFocus();

        Button btnScannerPaletteRenvoie = findViewById(R.id.btnScannerPaletteRenvoie);
        Button btnFinirRenvoie = findViewById(R.id.btnFinirRenvoie);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        chargerPalettesARenvoyer();

        btnScannerPaletteRenvoie.setOnClickListener(v -> {
            if (!isOnline()) {
                Toast.makeText(this, "Hors ligne, impossible de scanner.", Toast.LENGTH_SHORT).show();
                return;
            }

            String numPalette = editTextNumPaletteRenvoie.getText().toString().trim();
            if (numPalette.isEmpty()) {
                Toast.makeText(this, "Veuillez scanner une palette", Toast.LENGTH_SHORT).show();
                return;
            }
            verifierEtValiderPalette(numPalette);
        });

        btnFinirRenvoie.setOnClickListener(v -> {
            Intent intent = new Intent(this, RenvoieResultActivity.class);
            intent.putParcelableArrayListExtra("palettesValidees", palettesValidees);
            renvoieResultLauncher.launch(intent);
        });
    }

    /**
     * Charge la liste des palettes à renvoyer depuis l'API distante.
     * Met à jour l'adaptateur de la RecyclerView avec les palettes reçues.
     * Affiche un message d'erreur en cas d'échec réseau ou erreur serveur.
     */
    private void chargerPalettesARenvoyer() {
        if (!isOnline()) {
            Toast.makeText(this, "Hors ligne, impossible de charger les palettes.", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<List<PaletteInfosResponse>> call = apiService.getPalettesARenvoyer();
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<PaletteInfosResponse>> call, @NonNull Response<List<PaletteInfosResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    palettesARenvoyer.clear();
                    palettesARenvoyer.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else if (response.code() == 401) {
                    handleTokenExpired();
                } else {
                    Toast.makeText(RenvoieActivity.this, "Erreur chargement : " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PaletteInfosResponse>> call, @NonNull Throwable t) {
                Toast.makeText(RenvoieActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Vérifie si une palette saisie par l'utilisateur est dans la liste
     * des palettes à renvoyer, puis l'ajoute à la liste des palettes validées.
     *
     * @param numPalette Le numéro de la palette scannée ou saisie.
     */
    private void verifierEtValiderPalette(String numPalette) {
        PaletteInfosResponse paletteTrouvee = null;
        for (PaletteInfosResponse palette : palettesARenvoyer) {
            if (palette.getNum_palette().equalsIgnoreCase(numPalette)) {
                paletteTrouvee = palette;
                break;
            }
        }

        if (paletteTrouvee == null) {
            Toast.makeText(this, "Cette palette n'est pas à renvoyer", Toast.LENGTH_SHORT).show();
            return;
        }

        ValidationRenvoie validation = new ValidationRenvoie(
                paletteTrouvee.getNum_palette(),
                paletteTrouvee.getQuantite(),
                paletteTrouvee.getEmplacement()
        );

        palettesValidees.add(validation);
        palettesARenvoyer.remove(paletteTrouvee);
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Palette ajoutée à la liste des renvois", Toast.LENGTH_SHORT).show();
        editTextNumPaletteRenvoie.setText("");
        editTextNumPaletteRenvoie.requestFocus();
    }

    /**
     * Gère le bouton retour dans la barre d'action.
     *
     * @return true pour indiquer que l'action a été traitée.
     */
    @Override
    public boolean onSupportNavigateUp() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Quitter le renvoie")
                .setMessage("Êtes-vous sûr de vouloir quitter le mode renvoie ?")
                .setPositiveButton("Oui", (dialog, which) -> finish())
                .setNegativeButton("Non", null)
                .show();
        return true;
    }
}