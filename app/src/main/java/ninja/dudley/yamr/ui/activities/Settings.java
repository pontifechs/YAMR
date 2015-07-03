package ninja.dudley.yamr.ui.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import ninja.dudley.yamr.R;

public class Settings extends PreferenceActivity
{
    public static final String RTL_ENABLED_KEY = "rtl_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // don't really care that this is deprecated.
        addPreferencesFromResource(R.xml.preferences);
    }
}
