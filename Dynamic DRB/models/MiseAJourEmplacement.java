package com.mdo.gestionpalettes.models;

public class MiseAJourEmplacement {
    private String num_palette;
    private String nouvel_emplacement;

    public MiseAJourEmplacement(String num_palette, String nouvel_emplacement) {
        this.num_palette = num_palette;
        this.nouvel_emplacement = nouvel_emplacement;
    }

    public String getNum_palette() { return num_palette; }
    public void setNum_palette(String num_palette) { this.num_palette = num_palette; }

    public String getNouvel_emplacement() { return nouvel_emplacement; }
    public void setNouvel_emplacement(String nouvel_emplacement) { this.nouvel_emplacement = nouvel_emplacement; }
}
