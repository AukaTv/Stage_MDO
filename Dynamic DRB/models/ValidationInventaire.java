package com.mdo.gestionpalettes.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import android.os.Parcel;
import android.os.Parcelable;

public class ValidationInventaire implements Parcelable {
    @Expose
    @SerializedName("num_palette")
    private String numPalette;

    @Expose
    @SerializedName("quantite")
    private int quantite;

    @Expose
    @SerializedName("statut")
    private String statut;

    @Expose
    @SerializedName("emplacement")
    private String emplacement;

    public ValidationInventaire(String numPalette, int quantite, String emplacement) {
        this.numPalette = numPalette;
        this.quantite = quantite;
        this.emplacement = emplacement;
        this.statut = "En stock";
    }


    protected ValidationInventaire(Parcel in) {
        numPalette = in.readString();
        quantite = in.readInt();
        statut = in.readString();
        emplacement = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(numPalette);
        dest.writeInt(quantite);
        dest.writeString(statut);
        dest.writeString(emplacement);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ValidationInventaire> CREATOR = new Creator<ValidationInventaire>() {
        @Override
        public ValidationInventaire createFromParcel(Parcel in) {
            return new ValidationInventaire(in);
        }

        @Override
        public ValidationInventaire[] newArray(int size) {
            return new ValidationInventaire[size];
        }
    };

    public String getNumPalette() {
        return numPalette;
    }

    public int getQuantite() {
        return quantite;
    }

    public String getStatut() {
        return statut;
    }

    public String getEmplacement() {
        return emplacement;
    }
}
