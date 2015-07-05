package ninja.dudley.yamr.ui.activities;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;

import ninja.dudley.yamr.R;
import ninja.dudley.yamr.ui.fragments.PageViewer;
import ninja.dudley.yamr.ui.fragments.ProviderViewer;
import ninja.dudley.yamr.ui.fragments.SeriesViewer;
import ninja.dudley.yamr.ui.util.OrientationAware;

public class Reader extends Activity implements ProviderViewer.LoadSeries, SeriesViewer.LoadChapter, OrientationAware.I
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        if (savedInstanceState == null)
        {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            ProviderViewer providerViewer = new ProviderViewer();
            transaction.replace(R.id.reader, providerViewer);
            transaction.commit();
        }
    }

    @Override
    public void loadSeries(Uri series)
    {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        SeriesViewer seriesViewer = new SeriesViewer();
        Bundle args = new Bundle();
        args.putParcelable(SeriesViewer.ArgumentKey, series);
        seriesViewer.setArguments(args);
        transaction.replace(R.id.reader, seriesViewer);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void loadChapter(Uri chapter)
    {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        PageViewer pageViewer = new PageViewer();
        Bundle args = new Bundle();
        args.putParcelable(PageViewer.ChapterArgumentKey, chapter);
        pageViewer.setArguments(args);
        transaction.replace(R.id.reader, pageViewer);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        OrientationAware.handleOrientationAware(this, newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPortrait(Configuration newConfig)
    {
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.reader);
        layout.removeAllViews();
    }

    @Override
    public void onLandscape(Configuration newConfig)
    {
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.reader);
        layout.removeAllViews();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }
}
