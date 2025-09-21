package com.mdo.gestionpalettes.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.models.ValidationSortieProduction;

import java.util.List;


/**
 * Adaptateur personnalisé pour gérer l'affichage des palettes en attente de passage en production.
 * Permet à l'utilisateur de voir le numéro de palette, la quantité, et de supprimer une palette de la liste.
 */
public class PaletteSortieProductionAdapter extends BaseAdapter {

    private final Context context;
    private final List<ValidationSortieProduction> paletteList;

    /**
     * Constructeur de l'adaptateur.
     *
     * @param context Le contexte de l'application ou de l'activité appelante.
     * @param paletteList La liste des palettes destinées à la sortie production.
     */
    public PaletteSortieProductionAdapter(Context context, List<ValidationSortieProduction> paletteList) {
        this.context = context;
        this.paletteList = paletteList;
    }

    /**
     * Retourne le nombre total d'éléments dans la liste.
     *
     * @return Le nombre de palettes.
     */
    @Override
    public int getCount() {
        return paletteList.size();
    }

    /**
     * Retourne l'objet ValidationSortieProduction à une position spécifique.
     *
     * @param position La position dans la liste.
     * @return L'objet ValidationSortieProduction correspondant.
     */
    @Override
    public Object getItem(int position) {
        return paletteList.get(position);
    }

    /**
     * Retourne l'identifiant unique pour un élément donné.
     *
     * @param position La position de l'élément.
     * @return L'identifiant, ici la position elle-même.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Construit et retourne la vue associée à une palette pour l'afficher dans la liste.
     * Affiche le numéro de palette et la quantité, et permet la suppression de la palette.
     *
     * @param position La position de l'élément à afficher.
     * @param convertView Vue réutilisable.
     * @param parent Le groupe de vues parent.
     * @return La vue configurée pour l'élément.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_palette_sortie_production, parent, false);
        }

        ValidationSortieProduction palette = paletteList.get(position);

        TextView tvNumPalette = convertView.findViewById(R.id.tvNumPalette);
        TextView tvQuantite = convertView.findViewById(R.id.tvQuantite);
        Button btnSupprimer = convertView.findViewById(R.id.btnSupprimer);
        TextView tvEmplacement = convertView.findViewById(R.id.tvEmplacement);


        tvEmplacement.setText("Emplacement : " + palette.getEmplacement());
        tvNumPalette.setText("Palette : " + palette.getNumPalette());
        tvQuantite.setText("Quantité : " + palette.getQuantite());

        btnSupprimer.setOnClickListener(v -> {
            paletteList.remove(position);
            notifyDataSetChanged();
            Toast.makeText(context, "Palette supprimée", Toast.LENGTH_SHORT).show();
        });

        return convertView;
    }
}