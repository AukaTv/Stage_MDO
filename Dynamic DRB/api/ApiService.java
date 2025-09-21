package com.mdo.gestionpalettes.api;

import com.mdo.gestionpalettes.models.LoginResponse;
import com.mdo.gestionpalettes.models.PaletteInfosResponse;
import com.mdo.gestionpalettes.models.EntreePalette;
import com.mdo.gestionpalettes.models.EmplacementEntrepot;
import com.mdo.gestionpalettes.models.MiseAJourEmplacement;
import com.mdo.gestionpalettes.models.ValidationInventaire;
import com.mdo.gestionpalettes.models.ValidationDestruction;
import com.mdo.gestionpalettes.models.PaletteConsultation;
import com.mdo.gestionpalettes.models.ValidationRenvoie;
import com.mdo.gestionpalettes.models.ValidationSortieProduction;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Query;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import retrofit2.http.Body;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Field;
import retrofit2.http.Path;


/**
 * Interface définissant les endpoints REST utilisés dans l'application Android via Retrofit.
 * Regroupe les appels pour l'authentification, la gestion des emplacements, l'entrée, 
 * l'inventaire, la destruction, le renvoi, la production et la consultation des palettes.
 */
public interface ApiService {

    // -------------------------------
    // Authentification utilisateur
    @FormUrlEncoded
    @POST("/auth/login")
    Call<LoginResponse> login(
            @Field("grant_type") String grantType,
            @Field("username") String username,
            @Field("password") String password,
            @Field("scope") String scope,
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret
    );

    // -------------------------------
    // Emplacements dans l'entrepôt
    @GET("/emplacement")
    Call<List<EmplacementEntrepot>> getEmplacements();

    // PATCH emplacement palette (optionnel)
    @PATCH("/emplacement/palette")
    Call<Void> majEmplacement(@Body MiseAJourEmplacement body);

    // -------------------------------
    // Gestion des entrées palettes
    @GET("entree/palette/{num_palette}")
    Call<PaletteInfosResponse> getPaletteInfos(@Path("num_palette") String numPalette);

    // Entrée palette (body JSON)
    @POST("/entree")
    Call<Void> entreePalette(@Body EntreePalette entreePalette);

    // -------------------------------
    // Inventaire des palettes
    @POST("/inventaire/valider_inventaire")
    Call<Void> validerInventaire(@Body List<ValidationInventaire> palettes);

    @GET("/inventaire")
    Call<List<PaletteInfosResponse>> getPalettesAInventorier();

    // -------------------------------
    // Destruction des palettes
    @POST("/sorties/valider_destruction")
    Call<Void> validerDestruction(@Body List<ValidationDestruction> palettes);

    @GET("/sorties/destruction")
    Call<List<PaletteInfosResponse>> getPalettesADetruire();

    // -------------------------------
    // Renvoi des palettes
    @GET("sorties/renvoie")
    Call<List<PaletteInfosResponse>> getPalettesARenvoyer();

    @POST("/sorties/valider_renvoie")
    Call<Void> validerRenvoie(@Body List<ValidationRenvoie> palettes);

    // -------------------------------
    // Sortie en production
    @GET("/sorties/production")
    Call<List<PaletteConsultation>> getPalettesEnStock();

    @POST("/sorties/valider_production")
    Call<Void> validerSortieProduction(@Body List<ValidationSortieProduction> palettes);

    // -------------------------------
    // Consultation des palettes
    @GET("/consultation")
    Call<List<PaletteConsultation>> consulterPalettes(
            @Query("num_palette") String numPalette,
            @Query("client") String client,
            @Query("article") String article,
            @Query("statut") String statut,
            @Query("emplacement") String emplacement
    );
}