package ninja.dudley.yamr

import android.app.Application
import org.acra.ACRA
import org.acra.ReportingInteractionMode
import org.acra.annotation.ReportsCrashes
import org.acra.sender.HttpSender

/**
 * Created by mdudley on 8/6/15.
 */
ReportsCrashes(
        formUri = "http://dudley.ninja:5984/acra-storage/_design/acra-storage/_update/report",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.error_text,
        reportType = HttpSender.Type.JSON,
        httpMethod = HttpSender.Method.PUT,
        formUriBasicAuthLogin = "reporter",
        formUriBasicAuthPassword = "WelcomeToTheNHK"
)
class YAMR : Application()
{
    override fun onCreate()
    {
        super.onCreate()
        ACRA.init(this);
    }
}