package com.mdo.gestionpalettes.activities;

import com.mdo.gestionpalettes.R;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MenuActivity extends BaseActivity {
    /**
     * Méthode appelée lors de la création de l'activité MenuActivity.
     * Initialise l'interface principale de l'application avec les boutons
     * pour accéder aux différents modules : Entrée, Inventaire, Sortie et Consultation.
     *
     * @param savedInstanceState L'état précédemment sauvegardé de l'activité, s'il existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btnEntree = findViewById(R.id.btnEntree);
        Button btnInventaire = findViewById(R.id.btnInventaire);
        Button btnSortie = findViewById(R.id.btnSortie);
        Button btnConsultation = findViewById(R.id.btnConsultation);

        btnEntree.setOnClickListener(v -> startActivity(new Intent(this, EntreeActivity.class)));

        btnInventaire.setOnClickListener(v -> startActivity(new Intent(this, InventaireActivity.class)));

        btnSortie.setOnClickListener(v -> startActivity(new Intent(this, MenuSortieActivity.class)));

        btnConsultation.setOnClickListener(v -> startActivity(new Intent(this, ConsultationActivity.class)));
    }
}