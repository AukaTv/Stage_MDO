package com.mdo.gestionpalettes.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.BaseAdapter;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.models.ValidationDestruction;

import java.util.ArrayList;

/**
 * Adaptateur personnalisé pour afficher les palettes validées pour destruction
 * dans une liste avec possibilité de suppression.
 */
public class PaletteDestructionAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<ValidationDestruction> paletteList;

    /**
     * Constructeur de l'adaptateur.
     *
     * @param context Le contexte de l'activité ou de la vue.
     * @param paletteList La liste des palettes à afficher.
     */
    public PaletteDestructionAdapter(Context context, ArrayList<ValidationDestruction> paletteList) {
        this.context = context;
        this.paletteList = paletteList;
    }

    /**
     * Retourne le nombre total de palettes dans la liste.
     *
     * @return Le nombre d'éléments.
     */
    @Override
    public int getCount() {
        return paletteList.size();
    }

    /**
     * Retourne la palette à la position spécifiée.
     *
     * @param position La position de la palette.
     * @return L'objet ValidationDestruction correspondant.
     */
    @Override
    public Object getItem(int position) {
        return paletteList.get(position);
    }

    /**
     * Retourne l'identifiant de l'élément à la position donnée.
     *
     * @param position La position de l'élément.
     * @return L'identifiant de l'élément (ici, sa position).
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Génère et retourne la vue pour une palette à la position donnée.
     *
     * @param position Position de l'élément dans la liste.
     * @param convertView Vue réutilisable.
     * @param parent Vue parente.
     * @return La vue configurée pour l'élément.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_palette_validee, parent, false);
        }

        ValidationDestruction palette = paletteList.get(position);

        TextView tvNumPalette = convertView.findViewById(R.id.tvNumPalette);
        TextView tvQuantite = convertView.findViewById(R.id.tvQuantite);
        TextView tvEmplacement = convertView.findViewById(R.id.tvEmplacement);
        Button btnSupprimer = convertView.findViewById(R.id.btnSupprimer);

        tvNumPalette.setText("Palette : " + palette.getNumPalette());
        tvQuantite.setText("Quantité : " + palette.getQuantite());
        tvEmplacement.setText("Emplacement : " + palette.getEmplacement());

        btnSupprimer.setOnClickListener(v -> {
            paletteList.remove(position);
            notifyDataSetChanged();
            Toast.makeText(context, "Palette supprimée", Toast.LENGTH_SHORT).show();
        });

        return convertView;
    }
}