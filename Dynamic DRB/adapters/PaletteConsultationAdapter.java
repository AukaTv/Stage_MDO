package com.mdo.gestionpalettes.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mdo.gestionpalettes.R;
import com.mdo.gestionpalettes.models.PaletteConsultation;

import java.util.List;


/**
 * Adaptateur RecyclerView pour afficher les palettes en mode consultation.
 * Affiche les détails de chaque palette : numéro, client, article, quantité, et emplacement.
 */
public class PaletteConsultationAdapter extends RecyclerView.Adapter<PaletteConsultationAdapter.ViewHolder> {

    private List<PaletteConsultation> palettes;

    public PaletteConsultationAdapter(List<PaletteConsultation> palettes) {
        this.palettes = palettes;
    }

    /**
     * Met à jour la liste des palettes affichées et notifie l'adaptateur du changement.
     *
     * @param palettes Nouvelle liste de palettes à afficher.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setPalettes(List<PaletteConsultation> palettes) {
        this.palettes = palettes;
        notifyDataSetChanged();
    }

    /**
     * Crée une nouvelle vue (ViewHolder) à partir du layout XML.
     *
     * @param parent Le ViewGroup parent.
     * @param viewType Le type de vue.
     * @return Un nouveau ViewHolder initialisé.
     */
    @NonNull
    @Override
    public PaletteConsultationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_palette_consultation, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Lie les données d'une palette au ViewHolder pour l'affichage.
     *
     * @param holder Le ViewHolder à mettre à jour.
     * @param position Position de l'élément dans la liste.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PaletteConsultationAdapter.ViewHolder holder, int position) {
        PaletteConsultation palette = palettes.get(position);
        holder.tvNumPalette.setText("N° Pal: " + palette.getNum_palette());
        holder.tvClient.setText("Client: " + palette.getNom_client());
        holder.tvArticle.setText("Article: " + palette.getArticle());
        holder.tvQuantite.setText("Qté: " + palette.getQuantite());
        holder.tvEmplacement.setText("Empl: " + palette.getEmplacement());
        holder.tvStatut.setText("Statut : " + palette.getStatut());
    }

    /**
     * Retourne le nombre total de palettes dans la liste.
     *
     * @return Le nombre d'éléments.
     */
    @Override
    public int getItemCount() {
        return palettes.size();
    }

    /**
     * Classe interne représentant les composants d'affichage d'une palette individuelle.
     * Contient des TextView pour les différents champs d'une palette.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumPalette, tvClient, tvArticle, tvQuantite, tvEmplacement, tvStatut;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumPalette = itemView.findViewById(R.id.tvNumPalette);
            tvClient = itemView.findViewById(R.id.tvClient);
            tvArticle = itemView.findViewById(R.id.tvArticle);
            tvQuantite = itemView.findViewById(R.id.tvQuantite);
            tvEmplacement = itemView.findViewById(R.id.tvEmplacement);
            tvStatut = itemView.findViewById(R.id.tvStatut);
        }
    }
}