package ninja.dudley.yamr.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

/**
* Created by mdudley on 11/13/15. Yup.
 */
class ConnectivityHelper
{
    companion object
    {
        fun isAnyNetworkConnected(context: Context): Boolean
        {
            return isWiFiNetworkConnected(context) || isMobileNetworkConnected(context)
        }

        fun isWiFiNetworkConnected(context: Context): Boolean
        {
            val network:  NetworkInfo? = getConnectivityManager(context).activeNetworkInfo
            return network != null && network.isConnectedOrConnecting && network.type == ConnectivityManager.TYPE_WIFI;
        }

        fun isMobileNetworkConnected(context: Context): Boolean
        {
            val network:  NetworkInfo? = getConnectivityManager(context).activeNetworkInfo
            return network != null && network.isConnectedOrConnecting && network.type == ConnectivityManager.TYPE_MOBILE;
        }

        private fun getConnectivityManager(context: Context): ConnectivityManager
        {
            return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }
    }
}
