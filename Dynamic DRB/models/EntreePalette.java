package com.mdo.gestionpalettes.models;

public class EntreePalette {
    private String num_palette;
    private String emplacement;

    public EntreePalette(String num_palette, String emplacement) {
        this.num_palette = num_palette;
        this.emplacement = emplacement;
    }

    // Getters et setters
    public String getNum_palette() { return num_palette; }
    public void setNum_palette(String num_palette) { this.num_palette = num_palette; }

    public String getEmplacement() { return emplacement; }
    public void setEmplacement(String emplacement) { this.emplacement = emplacement; }
}