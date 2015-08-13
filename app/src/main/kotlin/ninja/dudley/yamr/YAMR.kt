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
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://dudley.ninja:5984/acra-storage/_design/acra-storage/_update/report",
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