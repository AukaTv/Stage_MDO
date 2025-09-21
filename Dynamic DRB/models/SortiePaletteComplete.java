package com.mdo.gestionpalettes.models;

public class SortiePaletteComplete {
    private String num_palette;
    private String zone;
    private String statut;

    public SortiePaletteComplete(String num_palette, String zone, String statut) {
        this.num_palette = num_palette;
        this.zone = zone;
        this.statut = statut;
    }

    public String getNum_palette() {
        return num_palette;
    }

    public void setNum_palette(String num_palette) {
        this.num_palette = num_palette;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}