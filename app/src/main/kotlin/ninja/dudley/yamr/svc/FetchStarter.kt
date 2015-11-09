package ninja.dudley.yamr.svc

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.ui.activities.Browse
import java.util.*

/**
* Created by mdudley on 6/23/15. Yup.
*/

public fun fetchNewComplete(thiS: Any, newUris: List<Uri>)
{
    (thiS as FetchStarter).complete(newUris)
}

public class FetchStarter : BroadcastReceiver()
{
    private var context: Context? = null

    public fun complete(newUris: List<Uri>)
    {
        // None! Nada!
        if (newUris.size == 0)
        {
            Log.d("FetchStarter", "No URIs")
            return
        }

        // Get all the series names.
        val seriesList = newUris.map {
            Series(context!!.contentResolver.query(it, null, null, null, null))
        }

        val msg = seriesList.joinToString(transform = { it.name })

        // Notify
        val builder = Notification.Builder(context)
                .setSmallIcon(R.drawable.yamr_pirate_icon)
                .setContentTitle("YARR")
                .setContentText("${newUris.size} New Chapters")
                .setAutoCancel(true)
                .setStyle(Notification.BigTextStyle()
                         .bigText(msg))

        val startFavorites = Intent(context, Browse::class.java)
        startFavorites.putExtra(Browse.FlowKey, Browse.FlowType.Favorites.toString())
        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addParentStack(Browse::class.java)
        stackBuilder.addNextIntent(startFavorites)
        val pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pi)
        val manager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, builder.build())
    }

    override fun onReceive(context: Context, intent: Intent)
    {
        this.context = context
        Log.d("FetchStarter", "Received ${intent.action}")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED)
        {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(StartChecking)
            val pi = PendingIntent.getBroadcast(context, 0, i, 0)
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                             AlarmManager.INTERVAL_FIFTEEN_MINUTES /15,
                                             AlarmManager.INTERVAL_HALF_DAY / 2, pi)
        }
        else if (intent.action == StartChecking)
        {
            FetcherAsync.fetchAllNew(this, ::fetchNewComplete)
        }
    }

    companion object
    {
        private val Base: String = "ninja.dudley.yamr.FetchStarter"
        public val StartChecking: String = "$Base.StartChecking"
    }
}
