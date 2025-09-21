package com.mdo.gestionpalettes.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import android.os.Parcel;
import android.os.Parcelable;

public class ValidationDestruction implements Parcelable {
    @Expose
    @SerializedName("num_palette")
    private String numPalette;

    @Expose(serialize = false)
    private int quantite;

    @Expose
    @SerializedName("statut")
    private String statut;

    @Expose(serialize = false)
    private String emplacement;

    public ValidationDestruction(String numPalette, int quantite, String emplacement) {
        this.numPalette = numPalette;
        this.quantite = quantite;
        this.emplacement = emplacement;
        this.statut = "Détruite";
    }

    protected ValidationDestruction(Parcel in) {
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

    public static final Creator<ValidationDestruction> CREATOR = new Creator<ValidationDestruction>() {
        @Override
        public ValidationDestruction createFromParcel(Parcel in) {
            return new ValidationDestruction(in);
        }

        @Override
        public ValidationDestruction[] newArray(int size) {
            return new ValidationDestruction[size];
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
