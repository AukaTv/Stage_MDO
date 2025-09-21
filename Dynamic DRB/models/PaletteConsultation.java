package com.mdo.gestionpalettes.models;

public class PaletteConsultation {
    private String num_palette;
    private String nom_client;
    private String article;
    private int quantite;
    private String emplacement;
    private String statut;

    public String getNum_palette() { return num_palette; }
    public String getNom_client() { return nom_client; }
    public String getArticle() { return article; }
    public int getQuantite() { return quantite; }
    public String getEmplacement() { return emplacement; }
    public String getStatut() { return statut; }
}
