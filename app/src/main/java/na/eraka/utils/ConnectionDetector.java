package na.eraka.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

public class ConnectionDetector {
 
   private final Context m_context;
 
   public ConnectionDetector(Context context){
       m_context = context;
   }

    public static boolean isNetworkAvailable(Context mContext) {
        if (mContext == null) return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                                               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || 
                                               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                @SuppressWarnings("deprecation")
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }

   public boolean isConnectedToInternet() {
       return isNetworkAvailable(m_context);
   }

    public boolean isOnline() {
        return isNetworkAvailable(m_context);
    }
}
