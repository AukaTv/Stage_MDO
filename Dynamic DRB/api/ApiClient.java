package com.mdo.gestionpalettes.api;

import com.mdo.gestionpalettes.activities.LoginActivity;
import com.mdo.gestionpalettes.utils.AuthInterceptor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Classe utilitaire pour fournir des instances de Retrofit configurées avec l'intercepteur d'authentification.
 * Gère deux clients Retrofit : un pour les appels standards et un autre pour les appels spécifiques à l'inventaire.
 */
public class ApiClient {
    private static Retrofit retrofitDefault = null;
    private static Retrofit retrofitInventaire = null;

    /**
     * Retourne une instance Retrofit par défaut, initialisée avec l'intercepteur d'authentification.
     *
     * @param context Le contexte Android pour accéder aux préférences partagées.
     * @return L'instance Retrofit configurée.
     */
    public static Retrofit getClient(Context context) {
        if (retrofitDefault == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(context))
                    .build();

            // Gson par défaut sérialisé
            Gson gson = new GsonBuilder().create();

            retrofitDefault = new Retrofit.Builder()
                    .baseUrl("https://apimdo.fr/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();
        }
        return retrofitDefault;
    }

    /**
     * Retourne une instance Retrofit dédiée à l'inventaire, utilisant un Gson configuré pour exclure les champs non exposés.
     *
     * @param context Le contexte Android pour accéder aux préférences partagées.
     * @return L'instance Retrofit configurée pour l'inventaire.
     */
    public static Retrofit getInventaireClient(Context context) {
        if (retrofitInventaire == null) {
            OkHttpClient client = getHttpClient(context);

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            retrofitInventaire = new Retrofit.Builder()
                    .baseUrl("https://apimdo.fr/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();
        }
        return retrofitInventaire;
    }

    /**
     * Construit un client OkHttp avec gestion automatique du token d'authentification.
     * Si le token est invalide (erreur 401), l'utilisateur est redirigé vers l'écran de connexion.
     *
     * @param context Le contexte Android pour récupérer le token et lancer l'activité de connexion si nécessaire.
     * @return L'instance OkHttpClient avec intercepteur personnalisé.
     */
    private static OkHttpClient getHttpClient(Context context) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    SharedPreferences prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
                    String token = prefs.getString("token", "");

                    Request.Builder requestBuilder = original.newBuilder();
                    if (!token.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                    }

                    Request request = requestBuilder.build();
                    Response response = chain.proceed(request);

                    if (response.code() == 401) {
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.remove("token");
                                editor.apply();

                                Intent intent = new Intent(context, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                intent.putExtra("reason", "expired");
                                context.startActivity(intent);
                            });
                        }
                    }
                    return response;
                })
                .build();
    }
}
