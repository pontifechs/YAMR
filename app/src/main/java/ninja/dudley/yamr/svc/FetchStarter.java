package ninja.dudley.yamr.svc;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.List;

import ninja.dudley.yamr.R;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.ui.activities.Favorites;

/**
 * Created by mdudley on 6/23/15.
 */
public class FetchStarter extends BroadcastReceiver
{
    public static final String StartChecking = "ninja.dudley.yamr.FetchStarter";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d("FetchStarter", "Received " + intent.getAction());
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
        {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(StartChecking);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES / 5,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES / 5, pi);
        }
        else if (intent.getAction().equals(StartChecking))
        {
            // Check
            Intent i = new Intent(context, FetcherAsync.class);
            i.setAction(FetcherAsync.FETCH_NEW);
            i.setData(Provider.uri(1));   // Hard-code to the first (mangapanda) for now.
            Log.d("FetchStarter", "Kicking it off");
            context.startService(i);

            BroadcastReceiver onFetchNewComplete = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    List<String> newUris = intent.getStringArrayListExtra(FetcherAsync.FETCH_NEW_COMPLETE);

                    // None! Nada!
                    if (newUris.size() == 0)
                    {
                        Log.d("FetchStarter", "No URIs");
                        return;
                    }

                    // Notify
                    Notification.Builder builder = new Notification.Builder(context)
                            .setSmallIcon(R.drawable.yamr_notif_icon)
                            .setContentTitle("YARR")
                            .setContentText(newUris.size() + " New Chapters")
                            .setAutoCancel(true);

                    Intent startFavorites = new Intent(context, Favorites.class);

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addParentStack(Favorites.class);
                    stackBuilder.addNextIntent(startFavorites);
                    PendingIntent pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(pi);
                    NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.notify(0, builder.build());
                }
            };
            context.getApplicationContext().registerReceiver(onFetchNewComplete, new IntentFilter(FetcherAsync.FETCH_NEW_COMPLETE));
        }
    }
}
