package com.mdo.gestionpalettes.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import androidx.annotation.RequiresApi;

public class NetworkMonitor {

    public interface NetworkCallback {
        void onConnected(String type);
        void onDisconnected();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void register(Context context, NetworkCallback callback) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                NetworkCapabilities nc = cm.getNetworkCapabilities(network);
                if (nc != null) {
                    if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        callback.onConnected("WiFi");
                    } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        callback.onConnected("4G");
                    } else {
                        callback.onConnected("Autre");
                    }
                }
            }

            @Override
            public void onLost(Network network) {
                callback.onDisconnected();
            }
        });
    }
}