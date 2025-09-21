package com.mdo.gestionpalettes.activities;

import com.mdo.gestionpalettes.R;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MenuSortieActivity extends BaseActivity {

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Méthode appelée lors de la création de l'activité MenuSortieActivity.
     * Initialise l'écran de menu des sorties avec les boutons pour accéder
     * aux différents types de sortie : Renvoi, Destruction et Production.
     *
     * @param savedInstanceState L'état sauvegardé de l'activité, s'il existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_sortie);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button btnRenvoie = findViewById(R.id.btnRenvoie);
        Button btnDestruction = findViewById(R.id.btnDestruction);
        Button btnProduction = findViewById(R.id.btnProduction);

        btnRenvoie.setOnClickListener(v -> startActivity(new Intent(this, RenvoieActivity.class)));

        btnDestruction.setOnClickListener(v -> startActivity(new Intent(this, DestructionActivity.class)));

        btnProduction.setOnClickListener(v -> startActivity(new Intent(this, SortieProductionActivity.class)));
    }
}
