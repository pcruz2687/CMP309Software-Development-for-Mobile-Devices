package pcruz.dev.personalshopper.models;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class CheckInternetConnection
{
    ConnectivityManager cm;
    NetworkInfo activeNetwork;
    boolean isConnected;

    public CheckInternetConnection(Context context)
    {
        cm = (ConnectivityManager)context.getSystemService(context.CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public boolean isConnected() {
        return isConnected;
    }

}
