package com.mdo.gestionpalettes.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.adapters.PaletteAInventorierAdapter;
import com.mdo.gestionpalettes.api.ApiClient;
import com.mdo.gestionpalettes.api.ApiService;
import com.mdo.gestionpalettes.models.EmplacementEntrepot;
import com.mdo.gestionpalettes.models.PaletteInfosResponse;
import com.mdo.gestionpalettes.models.ValidationInventaire;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InventaireActivity extends BaseActivity {

    private ListView listViewAInventorier;
    private EditText editTextNumPalette;
    private PaletteAInventorierAdapter adapter;

    private Spinner spinnerClient;
    private ArrayList<String> clientsList = new ArrayList<>();

    private final ArrayList<PaletteInfosResponse> palettesAInventorier = new ArrayList<>();
    private ArrayList<ValidationInventaire> palettesValidees = new ArrayList<>();

    private ApiService apiService;

    private List<EmplacementEntrepot> emplacementsList = new ArrayList<>();
    private final List<EmplacementEntrepot> emplacementsFiltres = new ArrayList<>();
    private final List<String> rackList = new ArrayList<>();

    private final ActivityResultLauncher<Intent> inventaireResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<ValidationInventaire> nouvellesValidees = result.getData().getParcelableArrayListExtra("palettesValideesRetour");
                    if (nouvellesValidees != null) {
                        palettesValidees = nouvellesValidees;
                        chargerPalettesAInventorier();
                    }
                }
            }
    );

    /**
     * Récupère les emplacements disponibles depuis l'API et trie la liste
     * en plaçant les emplacements libres en priorité.
     * Appelle ensuite remplirSpinnerRack() pour afficher les options.
     */
    private void chargerEmplacements(Runnable onLoaded) {
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        Call<List<EmplacementEntrepot>> call = apiService.getEmplacements();

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<EmplacementEntrepot>> call, @NonNull Response<List<EmplacementEntrepot>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    emplacementsList.clear();
                    emplacementsList.addAll(response.body());
                    emplacementsList.sort((o1, o2) ->
                            o1.getEtat().equalsIgnoreCase("Libre") && !o2.getEtat().equalsIgnoreCase("Libre") ? -1 :
                                    !o1.getEtat().equalsIgnoreCase("Libre") && o2.getEtat().equalsIgnoreCase("Libre") ? 1 : 0
                    );
                    rackList.clear();
                    for (EmplacementEntrepot e : emplacementsList) {
                        String rack = e.getEmplacement().split(" ")[0];
                        if (!rackList.contains(rack)) rackList.add(rack);
                    }
                    rackList.sort(String::compareTo);
                    if (onLoaded != null) onLoaded.run();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<EmplacementEntrepot>> call, @NonNull Throwable t) {
                Toast.makeText(InventaireActivity.this, "Erreur chargement emplacements : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Méthode appelée lors de la création de l'activité InventaireActivity.
     * Initialise les composants de l'interface utilisateur, les boutons de scan
     * et de fin d'inventaire, configure l'appel réseau pour charger les palettes,
     * et définit le comportement des boutons.
     *
     * @param savedInstanceState L'état précédemment sauvegardé de l'activité.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventaire);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spinnerClient = findViewById(R.id.spinnerClient);
        listViewAInventorier = findViewById(R.id.listViewAInventorier);
        editTextNumPalette = findViewById(R.id.editTextNumPalette);

        editTextNumPalette.requestFocus();
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Button btnScannerPalette = findViewById(R.id.btnScannerPalette);
        Button btnFinirInventaire = findViewById(R.id.btnFinirInventaire);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        chargerPalettesAInventorier();

        btnScannerPalette.setOnClickListener(v -> {
            if (!isOnline()) {
                Toast.makeText(this, "Connexion réseau requise", Toast.LENGTH_SHORT).show();
                return;
            }

            String numPalette = editTextNumPalette.getText().toString().trim();
            if (numPalette.isEmpty()) {
                Toast.makeText(this, "Veuillez scanner une palette", Toast.LENGTH_SHORT).show();
                return;
            }
            verifierEtValiderPalette(numPalette);
        });

        btnFinirInventaire.setOnClickListener(v -> {
            Intent intent = new Intent(this, InventaireResultActivity.class);
            intent.putParcelableArrayListExtra("palettesValidees", palettesValidees);
            inventaireResultLauncher.launch(intent);
        });
    }

    /**
     * Charge les palettes dont le statut est "A Inventorier" en appelant l'API.
     * Met à jour la liste locale et l'adaptateur de la ListView.
     * Affiche des messages d'erreur si la connexion réseau échoue
     * ou si l'appel API est invalide (par exemple erreur 401).
     */
    private void chargerPalettesAInventorier() {
        if (!isOnline()) {
            Toast.makeText(this, "Connexion réseau requise pour charger les palettes", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<List<PaletteInfosResponse>> call = apiService.getPalettesAInventorier();
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<PaletteInfosResponse>> call, @NonNull Response<List<PaletteInfosResponse>> response) {
                if (response.code() == 401) {
                    handleTokenExpired();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    palettesAInventorier.clear();
                    palettesAInventorier.addAll(response.body());

                    HashSet<String> setClients = new HashSet<>();
                    for (PaletteInfosResponse p : palettesAInventorier) {
                        if (p.getNom_client() != null && !p.getNom_client().isEmpty()) {
                            setClients.add(p.getNom_client());
                        }
                    }
                    clientsList.clear();
                    clientsList.addAll(setClients);
                    Collections.sort(clientsList);
                    clientsList.add(0, "Tous");

                    ArrayAdapter<String> clientAdapter = new ArrayAdapter<>(InventaireActivity.this, android.R.layout.simple_spinner_item, clientsList);
                    clientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerClient.setAdapter(clientAdapter);

                    spinnerClient.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String selectedClient = clientsList.get(position);
                            List<PaletteInfosResponse> filtered;
                            if (selectedClient.equals("Tous")) {
                                filtered = new ArrayList<>(palettesAInventorier);
                            } else {
                                filtered = new ArrayList<>();
                                for (PaletteInfosResponse p : palettesAInventorier) {
                                    if (selectedClient.equalsIgnoreCase(p.getNom_client())) {
                                        filtered.add(p);
                                    }
                                }
                            }
                            adapter = new PaletteAInventorierAdapter(InventaireActivity.this, filtered);
                            listViewAInventorier.setAdapter(adapter);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    spinnerClient.setSelection(0);

                } else {
                    Toast.makeText(InventaireActivity.this, "Erreur chargement : " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PaletteInfosResponse>> call, @NonNull Throwable t) {
                Toast.makeText(InventaireActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Vérifie si le numéro de palette scanné est présent dans la liste à inventorier.
     * Si oui, demande à l'utilisateur de valider la quantité via une boîte de dialogue.
     * Si la validation est effectuée, ajoute la palette à la liste des palettes validées.
     *
     * @param numPalette Le numéro de palette saisi/scanné par l'utilisateur.
     */
    private void verifierEtValiderPalette(String numPalette) {
        PaletteInfosResponse paletteTrouvee = null;
        for (PaletteInfosResponse palette : palettesAInventorier) {
            if (palette.getNum_palette().equalsIgnoreCase(numPalette)) {
                paletteTrouvee = palette;
                break;
            }
        }

        if (paletteTrouvee == null) {
            Toast.makeText(this, "Cette palette n'est pas à inventorier", Toast.LENGTH_SHORT).show();
            return;
        }

        PaletteInfosResponse paletteFinal = paletteTrouvee;

        chargerEmplacements(() -> runOnUiThread(() -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_inventaire_palette, null);
            EditText inputQuantite = dialogView.findViewById(R.id.inputQuantite);
            Spinner spinnerRack = dialogView.findViewById(R.id.spinnerRack);
            Spinner spinnerEmplacement = dialogView.findViewById(R.id.spinnerEmplacement);

            inputQuantite.setText(String.valueOf(paletteFinal.getQuantite()));
            
            ArrayAdapter<String> rackAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rackList);
            rackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRack.setAdapter(rackAdapter);

            String emplacementActuel = paletteFinal.getEmplacement();
            int rackToSelect = 0;
            if (emplacementActuel != null) {
                String rackEmplacement = emplacementActuel.split(" ")[0];
                for (int i = 0; i < rackList.size(); i++) {
                    if (rackList.get(i).equalsIgnoreCase(rackEmplacement)) {
                        rackToSelect = i;
                        break;
                    }
                }
            }
            spinnerRack.setSelection(rackToSelect);

            spinnerRack.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String rackChoisi = rackList.get(position);
                    emplacementsFiltres.clear();
                    for (EmplacementEntrepot e : emplacementsList) {
                        if (e.getEmplacement().startsWith(rackChoisi + " ")) {
                            emplacementsFiltres.add(e);
                        }
                    }
                    ArrayAdapter<EmplacementEntrepot> empAdapter = new ArrayAdapter<>(InventaireActivity.this,
                            android.R.layout.simple_spinner_item, emplacementsFiltres);
                    empAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerEmplacement.setAdapter(empAdapter);

                    String emplacementActuel = paletteFinal.getEmplacement();
                    if (emplacementActuel != null) {
                        int index = -1;
                        for (int i = 0; i < emplacementsFiltres.size(); i++) {
                            if (emplacementsFiltres.get(i).getEmplacement().equalsIgnoreCase(emplacementActuel)) {
                                index = i;
                                break;
                            }
                        }
                        if (index >= 0) {
                            spinnerEmplacement.setSelection(index);
                        }
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Palette " + paletteFinal.getNum_palette())
                    .setView(dialogView)
                    .setPositiveButton("Valider", (dialog, which) -> {
                        String val = inputQuantite.getText().toString().trim();
                        int nouvelleQuantite;
                        try {
                            nouvelleQuantite = Integer.parseInt(val);
                        } catch (Exception e) {
                            Toast.makeText(this, "Quantité invalide", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        EmplacementEntrepot selected = (EmplacementEntrepot) spinnerEmplacement.getSelectedItem();
                        if (selected == null) {
                            Toast.makeText(this, "Sélectionnez un emplacement", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String nouvelEmplacement = selected.getEmplacement();

                        ValidationInventaire validation = new ValidationInventaire(
                                paletteFinal.getNum_palette(),
                                nouvelleQuantite,
                                nouvelEmplacement
                        );
                        palettesValidees.add(validation);
                        palettesAInventorier.remove(paletteFinal);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Palette validée", Toast.LENGTH_SHORT).show();
                        editTextNumPalette.setText("");
                        editTextNumPalette.requestFocus();
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        }));
    }

    /**
     * Gère le retour arrière via la flèche de la barre d'action.
     * Termine l'activité actuelle.
     *
     * @return true pour indiquer que l'action a été gérée.
     */
    @Override
    public boolean onSupportNavigateUp() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Quitter l'inventaire")
                .setMessage("Êtes-vous sûr de vouloir quitter le mode inventaire ?")
                .setPositiveButton("Oui", (dialog, which) -> finish())
                .setNegativeButton("Non", null)
                .show();
        return true;
    }
}
