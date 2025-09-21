package com.mdo.gestionpalettes.models;

import com.google.gson.annotations.SerializedName;

public class PaletteInfosResponse {
    @SerializedName("num_palette")
    private String num_palette;

    @SerializedName("nom_client")
    private String nom_client;

    @SerializedName("statut")
    private String statut;

    @SerializedName("article")
    private String article;

    @SerializedName("quantite")
    private int quantite;

    @SerializedName("emplacement")
    private String emplacement;

    public String getNum_palette() {
        return num_palette;
    }

    public void setNum_palette(String num_palette) {
        this.num_palette = num_palette;
    }

    public String getNom_client() {
        return nom_client;
    }

    public void setNom_client(String nom_client) {
        this.nom_client = nom_client;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getArticle() {
        return article;
    }

    public void setArticle(String article) {
        this.article = article;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public String getEmplacement() {
        return emplacement;
    }

    public void setEmplacement(String emplacement) {
        this.emplacement = emplacement;
    }

    public String getNumPalette() {return num_palette;
    }
}