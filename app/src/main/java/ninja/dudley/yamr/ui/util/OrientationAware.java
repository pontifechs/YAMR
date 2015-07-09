package ninja.dudley.yamr.ui.util;

import android.content.res.Configuration;

/**
 * Created by mdudley on 5/17/15.
 */
public class OrientationAware {


    // Silly Java, why won't you let me separate my concerns like I want to?
    public static void handleOrientationAware(I fakeThis, Configuration newConfig)
    {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            fakeThis.onPortrait(newConfig);
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            fakeThis.onLandscape(newConfig);
        }
    }

    public interface I
    {
        void onPortrait(Configuration newConfig);
        void onLandscape(Configuration newConfig);
    }
}
