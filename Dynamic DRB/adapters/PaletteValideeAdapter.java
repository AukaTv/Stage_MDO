package com.mdo.gestionpalettes.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.models.ValidationInventaire;

import java.util.List;

/**
 * Adaptateur personnalisé pour afficher les palettes validées dans le mode inventaire.
 * Permet l'affichage des informations de chaque palette (numéro, quantité, emplacement),
 * ainsi que la possibilité de les supprimer de la liste.
 */
public class PaletteValideeAdapter extends ArrayAdapter<ValidationInventaire> {

    /**
     * Interface pour écouter les événements de suppression de palette dans l'adaptateur.
     */
    public interface OnPaletteSupprimeeListener {
        void onPaletteSupprimee(ValidationInventaire palette);
    }

    private final List<ValidationInventaire> palettes;
    private final OnPaletteSupprimeeListener listener;

    /**
     * Constructeur de l'adaptateur.
     *
     * @param context Le contexte de l'application.
     * @param palettes La liste des palettes validées à afficher.
     * @param listener Le listener à appeler lorsqu'une palette est supprimée.
     */
    public PaletteValideeAdapter(Context context, List<ValidationInventaire> palettes, OnPaletteSupprimeeListener listener) {
        super(context, 0, palettes);
        this.palettes = palettes;
        this.listener = listener;
    }

    /**
     * Construit et retourne la vue pour un élément de la liste.
     *
     * @param position La position de l'élément dans la liste.
     * @param convertView La vue réutilisable, si disponible.
     * @param parent Le parent auquel la vue sera attachée.
     * @return La vue correspondant à l'élément de la liste.
     */
    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ValidationInventaire palette = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_palette_validee, parent, false);
        }

        TextView tvNumPalette = convertView.findViewById(R.id.tvNumPalette);
        TextView tvQuantite = convertView.findViewById(R.id.tvQuantite);
        TextView tvEmplacement = convertView.findViewById(R.id.tvEmplacement);
        Button btnSupprimer = convertView.findViewById(R.id.btnSupprimer);

        assert palette != null;
        tvNumPalette.setText("Palette : " + palette.getNumPalette());
        tvQuantite.setText("Quantité : " + palette.getQuantite());
        tvEmplacement.setText("Emplacement : " + palette.getEmplacement());

        btnSupprimer.setOnClickListener(v -> {
            palettes.remove(position);
            notifyDataSetChanged();
            if (listener != null) {
                listener.onPaletteSupprimee(palette);
            }
        });

        return convertView;
    }
}