package com.mdo.gestionpalettes.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.models.PaletteInfosResponse;

import java.util.List;


/**
 * Adaptateur personnalisé pour afficher une liste de palettes à renvoyer dans un RecyclerView.
 * Affiche les informations principales : numéro, client, article, quantité, emplacement.
 */
public class PaletteARenvoyerAdapter extends RecyclerView.Adapter<PaletteARenvoyerAdapter.ViewHolder> {

    private final Context context;
    private final List<PaletteInfosResponse> paletteList;

    public PaletteARenvoyerAdapter(Context context, List<PaletteInfosResponse> paletteList) {
        this.context = context;
        this.paletteList = paletteList;
    }

    /**
     * Classe interne représentant une vue individuelle pour une palette à renvoyer.
     * Elle contient des TextView pour afficher les champs de la palette.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNumPalette;
        public TextView tvNomClient;
        public TextView tvArticle;
        public TextView tvQuantite;
        public TextView tvEmplacement;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumPalette = itemView.findViewById(R.id.tvNumPaletteRenvoie);
            tvNomClient = itemView.findViewById(R.id.tvNomClientRenvoie);
            tvArticle = itemView.findViewById(R.id.tvArticleRenvoie);
            tvQuantite = itemView.findViewById(R.id.tvQuantiteRenvoie);
            tvEmplacement = itemView.findViewById(R.id.tvEmplacementRenvoie);
        }
    }

    /**
     * Crée une nouvelle vue (ViewHolder) pour un élément de la liste.
     *
     * @param parent Le ViewGroup parent.
     * @param viewType Le type de vue (inutile ici car il y a un seul type).
     * @return Un ViewHolder contenant la vue.
     */
    @NonNull
    @Override
    public PaletteARenvoyerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_palette_a_renvoyer, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Remplit la vue du ViewHolder avec les données d'une palette.
     *
     * @param holder Le ViewHolder à remplir.
     * @param position La position de l'élément dans la liste.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PaletteARenvoyerAdapter.ViewHolder holder, int position) {
        PaletteInfosResponse palette = paletteList.get(position);
        holder.tvNumPalette.setText("Palette : " + palette.getNum_palette());
        holder.tvNomClient.setText("Client : " + palette.getNom_client());
        holder.tvArticle.setText("Article : " + palette.getArticle());

        int quantite = palette.getQuantite();
        if (quantite != 0) {
            holder.tvQuantite.setText("Quantité : " + quantite);
        } else {
            holder.tvQuantite.setText("");
        }

        String emplacement = palette.getEmplacement();
        if (emplacement != null && !emplacement.isEmpty()) {
            holder.tvEmplacement.setText("Emplacement : " + emplacement);
        } else {
            holder.tvEmplacement.setText("");
        }
    }

    /**
     * Retourne le nombre total d'éléments dans la liste.
     *
     * @return Taille de la liste des palettes.
     */
    @Override
    public int getItemCount() {
        return paletteList.size();
    }
}