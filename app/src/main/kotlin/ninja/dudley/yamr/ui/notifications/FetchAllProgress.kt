package ninja.dudley.yamr.ui.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import ninja.dudley.yamr.R;

/**
 * Helper class for showing and canceling fetch all progress
 * notifications.
 * <p/>
 * This class makes heavy use of the {@link NotificationCompat.Builder} helper
 * class to create notifications in a backward-compatible way.
 */
public class FetchAllProgress
{
    public companion object
    {
        const private val NOTIFICATION_TAG = "FetchAllProgress";

        public fun notify(context: Context, currentDL: String, status: Float)
        {
            val title = "YAMR Downloading"

            val builder = Notification.Builder(context)
                    // Set appropriate defaults for the notification light, sound,
                    // and vibration.
                    .setDefaults(Notification.DEFAULT_ALL)

                    // Set required fields, including the small icon, the
                    // notification title, and text.
                    .setSmallIcon(R.drawable.yamr_pirate_icon)
                    .setContentTitle(title)
                    .setContentText(currentDL)

                    // All fields below this line are optional.
                    .setProgress(100, (status*100).toInt(), false)

                    // Use a default priority (recognized on devices running Android
                    // 4.1 or later)
                    .setPriority(Notification.PRIORITY_DEFAULT)

                    // Automatically dismiss the notification when it is touched.
                    .setAutoCancel(true);

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_TAG, 0, builder.build());
        }
    }
}
