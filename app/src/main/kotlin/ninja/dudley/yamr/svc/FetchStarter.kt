package ninja.dudley.yamr.svc

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Provider
import ninja.dudley.yamr.ui.activities.Browse

/**
 * Created by mdudley on 6/23/15.
 */
public class FetchStarter : BroadcastReceiver()
{

    override fun onReceive(context: Context, intent: Intent)
    {
        Log.d("FetchStarter", "Received " + intent.getAction())
        if (intent.getAction() == Intent.ACTION_BOOT_COMPLETED)
        {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(StartChecking)
            val pi = PendingIntent.getBroadcast(context, 0, i, 0)
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                             AlarmManager.INTERVAL_FIFTEEN_MINUTES /15,
                                             AlarmManager.INTERVAL_HALF_DAY / 2, pi)
        }
        else if (intent.getAction() == StartChecking)
        {
            // Check
            val i = Intent(context, javaClass<FetcherAsync>())
            i.setAction(FetchNew)
            i.setData(Provider.uri(1))   // Hard-code to the first (mangapanda) for now.
            Log.d("FetchStarter", "Kicking it off")
            context.startService(i)

            val onFetchNewComplete = object : BroadcastReceiver()
            {
                override fun onReceive(context: Context, intent: Intent)
                {
                    val newUris = intent.getStringArrayListExtra(FetchNewComplete)

                    // None! Nada!
                    if (newUris.size() == 0)
                    {
                        Log.d("FetchStarter", "No URIs, BITCH")
                        return
                    }

                    // Notify
                    val builder = Notification.Builder(context)
                            .setSmallIcon(R.drawable.yamr_notif_icon)
                            .setContentTitle("YARR")
                            .setContentText("" + newUris.size() + " New Chapters")
                            .setAutoCancel(true)

                    val startFavorites = Intent(context, javaClass<Browse>())
                    startFavorites.putExtra(Browse.FlowKey, Browse.FlowType.Favorites.toString())
                    val stackBuilder = TaskStackBuilder.create(context)
                    stackBuilder.addParentStack(javaClass<Browse>())
                    stackBuilder.addNextIntent(startFavorites)
                    val pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
                    builder.setContentIntent(pi)
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(0, builder.build())
                }
            }
            context.getApplicationContext()
                    .registerReceiver(onFetchNewComplete,
                                      IntentFilter(FetchNewComplete))
        }
    }

    companion object
    {
        private val Base: String = "ninja.dudley.yamr.FetchStarter"
        public val StartChecking: String = Base + ".StartChecking"
        public val FetchNew: String = Base + ".FetchNew"
        public val FetchNewStatus: String = FetchNew + ".Status"
        public val FetchNewComplete: String = FetchNew + ".Complete"
    }
}
