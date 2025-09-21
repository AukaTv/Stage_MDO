package com.mdo.gestionpalettes.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.models.PaletteInfosResponse;

import java.util.List;


/**
 * Adaptateur personnalisé pour afficher une liste de palettes à détruire.
 * Utilisé pour peupler une vue de liste avec les informations des palettes,
 * y compris numéro, client, article, quantité et emplacement.
 */
public class PaletteADetruireAdapter extends BaseAdapter {

    private final Context context;
    private final List<PaletteInfosResponse> paletteList;

    public PaletteADetruireAdapter(Context context, List<PaletteInfosResponse> paletteList) {
        this.context = context;
        this.paletteList = paletteList;
    }

    /**
     * Retourne le nombre total d'éléments dans la liste des palettes.
     *
     * @return Le nombre de palettes à détruire.
     */
    @Override
    public int getCount() {
        return paletteList.size();
    }

    /**
     * Retourne l'objet PaletteInfosResponse à une position donnée.
     *
     * @param position Position de l'élément dans la liste.
     * @return L'objet PaletteInfosResponse correspondant.
     */
    @Override
    public Object getItem(int position) {
        return paletteList.get(position);
    }

    /**
     * Retourne l'ID de l'élément à une position donnée.
     *
     * @param position Position de l'élément.
     * @return L'ID de l'élément (ici, la position elle-même).
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Génère la vue pour chaque élément de la liste.
     * Remplit les champs de texte avec les données de la palette à détruire.
     *
     * @param position Position de l'élément dans la liste.
     * @param convertView Vue existante à réutiliser.
     * @param parent Le parent auquel la vue sera attachée.
     * @return La vue mise à jour contenant les informations de la palette.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_palette_destruction, parent, false);
        }

        PaletteInfosResponse palette = paletteList.get(position);

        TextView tvNumPalette = convertView.findViewById(R.id.tvNumPaletteDestruction);
        TextView tvNomClient = convertView.findViewById(R.id.tvNomClientDestruction);
        TextView tvArticle = convertView.findViewById(R.id.tvArticleDestruction);
        TextView tvQuantite = convertView.findViewById(R.id.tvQuantiteDestruction);
        TextView tvEmplacement = convertView.findViewById(R.id.tvEmplacementDestruction);

        tvNumPalette.setText("Palette : " + palette.getNum_palette());
        tvNomClient.setText("Client : " + palette.getNom_client());
        tvArticle.setText("Article : " + palette.getArticle());

        int quantite = palette.getQuantite();
        tvQuantite.setText(quantite > 0 ? "Quantité : " + quantite : "");

        tvEmplacement.setText("Emplacement : " + palette.getEmplacement());

        return convertView;
    }
}