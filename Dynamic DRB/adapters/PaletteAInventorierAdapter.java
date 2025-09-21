package com.mdo.gestionpalettes.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.BaseAdapter;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.models.PaletteInfosResponse;

import java.util.List;

/**
 * Adaptateur personnalisé pour afficher une liste de palettes à inventorier.
 * Utilisé pour générer dynamiquement des vues dans une liste affichant les
 * informations de palettes en attente d'inventaire.
 */
public class PaletteAInventorierAdapter extends BaseAdapter {

    private final Context context;
    private final List<PaletteInfosResponse> paletteList;

    public PaletteAInventorierAdapter(Context context, List<PaletteInfosResponse> paletteList) {
        this.context = context;
        this.paletteList = paletteList;
    }

    /**
     * Retourne le nombre total de palettes à inventorier dans la liste.
     *
     * @return Le nombre d'éléments dans la liste.
     */
    @Override
    public int getCount() {
        return paletteList.size();
    }

    /**
     * Retourne l'objet PaletteInfosResponse à la position donnée.
     *
     * @param position Position de l'élément.
     * @return L'objet palette correspondant.
     */
    @Override
    public Object getItem(int position) {
        return paletteList.get(position);
    }

    /**
     * Retourne l'identifiant de l'élément à la position donnée.
     *
     * @param position Position de l'élément.
     * @return L'identifiant de l'élément (ici, la position).
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Crée ou met à jour la vue pour afficher les informations d'une palette.
     * Affiche le numéro de palette, le client et l'emplacement.
     *
     * @param position Position de l'élément dans la liste.
     * @param convertView Vue existante à réutiliser si disponible.
     * @param parent Vue parente à laquelle la vue créée sera attachée.
     * @return La vue complète pour l'élément courant.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_palette_a_inventorier, parent, false);
        }

        PaletteInfosResponse palette = paletteList.get(position);

        TextView tvNumPalette = convertView.findViewById(R.id.tvNumPalette);
        TextView tvNomClient = convertView.findViewById(R.id.tvNomClient);
        TextView tvEmplacement = convertView.findViewById(R.id.tvEmplacement);

        tvNumPalette.setText("Palette : " + palette.getNum_palette());
        tvNomClient.setText("Client : " + palette.getNom_client());
        tvEmplacement.setText("Emplacement : " + palette.getEmplacement());

        return convertView;
    }
}