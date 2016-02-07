package ninja.dudley.yamr.ui.util

import android.content.res.Configuration

/**
* Created by mdudley on 5/17/15. Yup.
*/
object OrientationAware
{
    // Silly Java, why won't you let me separate my concerns like I want to?
    fun handleOrientationAware(fakeThis: I, newConfig: Configuration)
    {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            fakeThis.onPortrait(newConfig)
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            fakeThis.onLandscape(newConfig)
        }
    }

    interface I
    {
        fun onPortrait(newConfig: Configuration)
        fun onLandscape(newConfig: Configuration)
    }
}
