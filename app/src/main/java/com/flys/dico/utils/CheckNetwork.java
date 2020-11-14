    package com.flys.dico.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;

public class CheckNetwork {

    private Context context;

    // You need to pass the context when creating the class
    public CheckNetwork(Context context) {
        this.context = context;
    }

    // Network Check
    public void registerNetworkCallback() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager
                    .registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                                                        @Override
                                                        public void onAvailable(Network network) {
                                                            Constants.isNetworkConnected = true; // Global Static Variable
                                                        }

                                                        @Override
                                                        public void onLost(Network network) {
                                                            Constants.isNetworkConnected = false; // Global Static Variable
                                                        }

                                                        @Override
                                                        public void onUnavailable() {
                                                            Constants.isNetworkConnected = false; // Global Static Variable
                                                        }
                                                    }

                    );
            Constants.isNetworkConnected = false;
        } catch (Exception e) {
            Constants.isNetworkConnected = false;
        }
    }
}
