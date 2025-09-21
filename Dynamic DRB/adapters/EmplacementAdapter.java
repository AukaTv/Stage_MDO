package com.mdo.gestionpalettes.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mdo.gestionpalettes.models.EmplacementEntrepot;
import com.mdo.gestionpalettes.R;

import java.util.List;

/**
 * Adaptateur personnalisé pour afficher une liste d'emplacements dans un Spinner.
 * Affiche chaque emplacement avec son état ("Libre" ou non) en couleur.
 * Vert pour les emplacements libres, rouge pour les autres.
 */
public class EmplacementAdapter extends ArrayAdapter<EmplacementEntrepot> {
    public EmplacementAdapter(Context context, List<EmplacementEntrepot> emplacements) {
        super(context, android.R.layout.simple_spinner_item, emplacements);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    /**
     * Retourne la vue pour l'élément sélectionné dans le Spinner.
     *
     * @param position Position de l'élément dans la liste.
     * @param convertView Vue réutilisable.
     * @param parent Vue parente.
     * @return La vue personnalisée pour l'élément sélectionné.
     */
    @Override
    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    /**
     * Retourne la vue pour chaque élément du menu déroulant du Spinner.
     *
     * @param position Position de l'élément dans la liste.
     * @param convertView Vue réutilisable.
     * @param parent Vue parente.
     * @return La vue personnalisée pour l'élément du menu déroulant.
     */
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    /**
     * Crée une vue personnalisée pour afficher un emplacement avec son état.
     * Applique une couleur verte si l'emplacement est libre, rouge sinon.
     *
     * @param position Position de l'élément dans la liste.
     * @param convertView Vue potentiellement réutilisable.
     * @param parent Vue parente du Spinner.
     * @return La vue personnalisée à afficher.
     */
    private View getCustomView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView) super.getView(position, convertView, parent);
        EmplacementEntrepot e = getItem(position);
        if (e != null) {
            String label = view.getContext().getString(
                    R.string.emplacement_spinner,
                    e.getEmplacement(),
                    e.getEtat()
            );
            view.setText(label);
            if ("Libre".equalsIgnoreCase(e.getEtat())) {
                view.setTextColor(Color.parseColor("#388E3C")); // Vert
            } else {
                view.setTextColor(Color.parseColor("#D32F2F")); // Rouge
            }
        }
        return view;
    }
}