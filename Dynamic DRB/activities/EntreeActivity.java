package com.mdo.gestionpalettes.activities;

import com.mdo.gestionpalettes.models.PaletteInfosResponse;
import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.api.ApiClient;
import com.mdo.gestionpalettes.api.ApiService;
import com.mdo.gestionpalettes.models.EmplacementEntrepot;
import com.mdo.gestionpalettes.models.EntreePalette;
import com.mdo.gestionpalettes.adapters.EmplacementAdapter;
import com.mdo.gestionpalettes.utils.NetworkMonitor;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EntreeActivity extends BaseActivity {

    private static final String TAG = "EntreeActivity";

    private EditText etNumPalette;
    private Button btnChargerInfos, btnValiderEntree, btnRetry;
    private TextView tvInfosPalette, bannerOffline;
    private Spinner spinnerRack, spinnerEmplacements;
    private PaletteInfosResponse infosPalette;
    private List<EmplacementEntrepot> emplacementsList = new ArrayList<>();
    private final List<EmplacementEntrepot> emplacementsFiltres = new ArrayList<>();
    private final List<String> rackList = new ArrayList<>();
    private Runnable pendingAction = null;
    private EmplacementAdapter emplacementAdapter;

    /**
     * Méthode appelée lors de la création de l'activité EntreeActivity.
     * Initialise l'interface utilisateur, configure les champs, spinners et boutons,
     * et gère la logique de chargement des informations de palette et validation d'entrée.
     *
     * @param savedInstanceState L'état sauvegardé de l'activité, s'il existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entree);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etNumPalette = findViewById(R.id.etNumPalette);
        btnChargerInfos = findViewById(R.id.btnChargerInfos);
        btnValiderEntree = findViewById(R.id.btnValiderEntree);
        btnRetry = findViewById(R.id.btnRetry);
        tvInfosPalette = findViewById(R.id.tvInfosPalette);
        spinnerRack = findViewById(R.id.spinnerRack);
        spinnerEmplacements = findViewById(R.id.spinnerEmplacements);
        bannerOffline = findViewById(R.id.bannerOffline);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        etNumPalette.requestFocus();
        imm.showSoftInput(etNumPalette, InputMethodManager.SHOW_IMPLICIT);

        etNumPalette.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                v.postDelayed(() -> {
                    v.requestFocus();
                    imm.showSoftInput(etNumPalette, InputMethodManager.SHOW_IMPLICIT);
                }, 100);
            }
        });

        NetworkMonitor.register(this, new NetworkMonitor.NetworkCallback() {
            @Override
            public void onConnected(String type) {
                runOnUiThread(() -> {
                    bannerOffline.setVisibility(View.GONE);
                    btnRetry.setVisibility(View.GONE);
                    Toast.makeText(EntreeActivity.this, getString(R.string.connected, type), Toast.LENGTH_SHORT).show();
                    if (pendingAction != null) {
                        pendingAction.run();
                        pendingAction = null;
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    bannerOffline.setVisibility(View.VISIBLE);
                    btnRetry.setVisibility(View.VISIBLE);
                    Toast.makeText(EntreeActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                });
            }
        });

        btnRetry.setOnClickListener(v -> {
            btnRetry.setVisibility(View.GONE);
            if (pendingAction != null) {
                pendingAction.run();
                pendingAction = null;
            }
        });

        chargerEmplacements();

        btnChargerInfos.setOnClickListener(v -> {
            if (!isOnline()) {
                Toast.makeText(this, getString(R.string.need_connection), Toast.LENGTH_SHORT).show();
                pendingAction = btnChargerInfos::performClick;
                btnRetry.setVisibility(View.VISIBLE);
                return;
            }
            String numPalette = etNumPalette.getText().toString().trim();
            Log.d(TAG, "Recherche palette : '" + numPalette + "'");
            ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
            Call<PaletteInfosResponse> call = apiService.getPaletteInfos(numPalette);

            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<PaletteInfosResponse> call, @NonNull Response<PaletteInfosResponse> response) {
                    Log.d(TAG, "API response code: " + response.code());
                    if (response.code() == 401) {
                        handleTokenExpired();
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        infosPalette = response.body();
                        Log.d(TAG, "Palette trouvée: " + infosPalette.getNum_palette());
                        tvInfosPalette.setText(getString(R.string.infos_palette,
                                infosPalette.getNum_palette(),
                                infosPalette.getNom_client(),
                                infosPalette.getArticle(),
                                infosPalette.getQuantite(),
                                infosPalette.getEmplacement()
                        ));
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                            Log.e(TAG, "Erreur API, body: " + errorBody);
                        } catch (IOException e) {
                            Log.e(TAG, "Erreur lecture body erreur", e);
                        }
                        tvInfosPalette.setText("");
                        Toast.makeText(EntreeActivity.this, getString(R.string.palette_introuvable), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<PaletteInfosResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Erreur réseau : " + t.getMessage(), t);
                    Toast.makeText(EntreeActivity.this, getString(R.string.network_error, t.getMessage()), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnValiderEntree.setOnClickListener(v -> {
            if (!isOnline()) {
                Toast.makeText(this, getString(R.string.no_network), Toast.LENGTH_SHORT).show();
                pendingAction = btnValiderEntree::performClick;
                btnRetry.setVisibility(View.VISIBLE);
                return;
            }
            if (infosPalette == null) {
                Toast.makeText(this, getString(R.string.load_palette_first), Toast.LENGTH_SHORT).show();
                return;
            }
            EmplacementEntrepot selected = (EmplacementEntrepot) spinnerEmplacements.getSelectedItem();
            if (selected == null) {
                Toast.makeText(this, getString(R.string.select_emplacement), Toast.LENGTH_SHORT).show();
                return;
            }
            String emplacement = selected.getEmplacement();

            ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

            EntreePalette entreePalette = new EntreePalette(
                    infosPalette.getNum_palette(),
                    emplacement
            );

            Call<Void> call = apiService.entreePalette(entreePalette);

            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.code() == 401) {
                        handleTokenExpired();
                        return;
                    }
                    if (response.isSuccessful()) {
                        Toast.makeText(EntreeActivity.this, getString(R.string.entree_enregistree), Toast.LENGTH_SHORT).show();
                        etNumPalette.setText("");
                        tvInfosPalette.setText("");
                        spinnerRack.setSelection(0);
                        spinnerEmplacements.setSelection(0);
                        infosPalette = null;
                        chargerEmplacements();
                    } else {
                        String errorMsg = "Erreur API, code: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += ", body: " + response.errorBody().string();
                            }
                        } catch (Exception e) {
                            errorMsg += ", erreur lecture corps réponse";
                        }
                        Log.e(TAG, errorMsg);
                        Toast.makeText(EntreeActivity.this, "Erreur lors de l'entrée : " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "Erreur réseau lors de l'entrée", t);
                    Toast.makeText(EntreeActivity.this, getString(R.string.network_error, t.getMessage()), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Récupère les emplacements disponibles depuis l'API et trie la liste
     * en plaçant les emplacements libres en priorité.
     * Appelle ensuite remplirSpinnerRack() pour afficher les options.
     */
    private void chargerEmplacements() {
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        Call<List<EmplacementEntrepot>> call = apiService.getEmplacements();

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<EmplacementEntrepot>> call, @NonNull Response<List<EmplacementEntrepot>> response) {
                if (response.code() == 401) {
                    handleTokenExpired();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    emplacementsList = response.body();
                    emplacementsList.sort((o1, o2) ->
                            o1.getEtat().equalsIgnoreCase("Libre") && !o2.getEtat().equalsIgnoreCase("Libre") ? -1 :
                                    !o1.getEtat().equalsIgnoreCase("Libre") && o2.getEtat().equalsIgnoreCase("Libre") ? 1 : 0
                    );
                    remplirSpinnerRack();
                } else {
                    Log.e(TAG, "Erreur chargement emplacements: code " + response.code());
                    Toast.makeText(EntreeActivity.this, "Erreur chargement emplacements: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<EmplacementEntrepot>> call, @NonNull Throwable t) {
                Log.e(TAG, "Erreur réseau chargement emplacements: " + t.getMessage(), t);
                Toast.makeText(EntreeActivity.this, getString(R.string.chargement_emplacements_failed, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Remplit le spinner de sélection de rack à partir des emplacements disponibles.
     * Filtre dynamiquement les emplacements du rack sélectionné dans un second spinner.
     */
    private void remplirSpinnerRack() {
        Set<String> racks = new HashSet<>();
        for (EmplacementEntrepot e : emplacementsList) {
            String rack = e.getEmplacement().split(" ")[0];
            racks.add(rack);
        }
        rackList.clear();
        rackList.addAll(racks);
        rackList.sort(String::compareTo);
        ArrayAdapter<String> rackAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rackList);
        rackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRack.setAdapter(rackAdapter);

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
                emplacementAdapter = new EmplacementAdapter(EntreeActivity.this, emplacementsFiltres);
                spinnerEmplacements.setAdapter(emplacementAdapter);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (!rackList.isEmpty()) {
            spinnerRack.setSelection(0);
        }
    }

    /**
     * Gère le comportement du bouton de retour dans la barre d'action.
     * Termine simplement l'activité en cours.
     *
     * @return true pour indiquer que l'action a été gérée.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
