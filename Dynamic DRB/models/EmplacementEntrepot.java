package com.mdo.gestionpalettes.models;

import androidx.annotation.NonNull;

public class EmplacementEntrepot {
    private String emplacement;
    private String etat;

    public String getEmplacement() { return emplacement; }
    public void setEmplacement(String emplacement) { this.emplacement = emplacement; }
    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    @NonNull
    @Override
    public String toString() {
        return getEmplacement();
    }
}