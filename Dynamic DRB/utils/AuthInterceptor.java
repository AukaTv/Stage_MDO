package com.mdo.gestionpalettes.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mdo.gestionpalettes.activities.LoginActivity;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        SharedPreferences prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        Request.Builder builder = originalRequest.newBuilder();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        Request requestWithToken = builder.build();

        Response response = chain.proceed(requestWithToken);

        if (response.code() == 401) {
            prefs.edit().remove("token").apply();

            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(context, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("reason", "expired");
                context.startActivity(intent);
            });
        }

        return response;
    }
}
