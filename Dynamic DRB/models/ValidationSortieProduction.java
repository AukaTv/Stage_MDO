package com.mdo.gestionpalettes.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class ValidationSortieProduction implements Parcelable {
    @SerializedName("num_palette")
    private String numPalette;

    @SerializedName("quantite")
    private Integer quantite;

    @SerializedName("statut")
    private String statut;

    @SerializedName("emplacement")
    private String emplacement;


    public ValidationSortieProduction(String numPalette, Integer quantite, String statut, String emplacement) {
        this.numPalette = numPalette;
        this.quantite = quantite;
        this.statut = statut;
        this.emplacement = emplacement;
    }

    protected ValidationSortieProduction(Parcel in) {
        numPalette = in.readString();
        if (in.readByte() == 0) {
            quantite = null;
        } else {
            quantite = in.readInt();
        }
        statut = in.readString();
        emplacement = in.readString();
    }

    public static final Creator<ValidationSortieProduction> CREATOR = new Creator<ValidationSortieProduction>() {
        @Override
        public ValidationSortieProduction createFromParcel(Parcel in) {
            return new ValidationSortieProduction(in);
        }

        @Override
        public ValidationSortieProduction[] newArray(int size) {
            return new ValidationSortieProduction[size];
        }
    };

    public String getNumPalette() { return numPalette; }
    public Integer getQuantite() { return quantite; }
    public String getStatut() { return statut; }
    public String getEmplacement() { return emplacement; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(numPalette);
        if (quantite == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(quantite);
        }
        parcel.writeString(statut);
        parcel.writeString(emplacement);
    }
}
