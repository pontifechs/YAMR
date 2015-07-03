package ninja.dudley.yamr.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ninja.dudley.yamr.R;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Series;
import ninja.dudley.yamr.svc.Navigation;
import ninja.dudley.yamr.ui.activities.Settings;
import ninja.dudley.yamr.ui.util.TouchImageView;
import ninja.dudley.yamr.util.ProgressTracker;

public class PageViewer extends Fragment implements TouchImageView.SwipeListener, View.OnClickListener, View.OnLongClickListener
{
    public static final String ChapterArgumentKey = "chapter";
    public static final String SeriesArgumentKey = "series";

    private Page page;
    private Series series;
    private ProgressTracker progressTracker;

    private BroadcastReceiver loadPageCompleteReceiver;
    private BroadcastReceiver prevPageFailedReceiver;
    private BroadcastReceiver nextPageFailedReceiver;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Go full-screen
//        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//        getActivity().getActionBar().setBackgroundDrawable(new ColorDrawable(R.color.black_overlay));
        getActivity().getActionBar().hide();

        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_page_viewer, container, false);

        loadPageCompleteReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                page = new Page(getActivity().getContentResolver().query(intent.getData(), null, null, null, null));
                if (series != null && series.getProgressPageId() == -1)
                {
                    series = new Series(getActivity().getContentResolver().query(series.uri(), null, null, null, null));
                }
                TouchImageView imageView = (TouchImageView) getActivity().findViewById(R.id.imageView);
                Drawable d = Drawable.createFromPath(page.getImagePath());
                if (d == null)
                {
                    // TODO:: Handle no image or corrupt image.
                }

                imageView.setImageDrawable(d);
                TextView loadingText = (TextView) getActivity().findViewById(R.id.page_loading_text);
                loadingText.setVisibility(View.INVISIBLE);

                if (series != null)
                {
                    if (progressTracker == null)
                    {
                        progressTracker = new ProgressTracker(getActivity().getContentResolver(), series);
                        progressTracker.handleNextPage(page);
                    }
                    else
                    {
                        progressTracker.handleNextPage(page);
                    }
                }
            }
        };

        nextPageFailedReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                TextView loadingText = (TextView) getActivity().findViewById(R.id.page_loading_text);
                loadingText.setVisibility(View.INVISIBLE);
                // TODO:: Make sure series is available for this
                new AlertDialog.Builder(getActivity())
                        .setTitle("E.N.D.")
                        .setMessage("You've reached the end of  + series.getName() + . Check back later for new chapters.")
                        .setNegativeButton("K.", null)
                        .show();
            }
        };

        prevPageFailedReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                TextView loadingText = (TextView) getActivity().findViewById(R.id.page_loading_text);
                loadingText.setVisibility(View.INVISIBLE);
                // TODO:: Make sure series is available for this
                new AlertDialog.Builder(getActivity())
                        .setTitle("Genesis")
                        .setMessage("You've reached the beginning of  + series.getName() + . Nothing older")
                        .setNegativeButton("K.", null)
                        .show();
            }
        };

        TouchImageView imageView = (TouchImageView) layout.findViewById(R.id.imageView);
        imageView.register(this);
        imageView.setOnClickListener(this);
        imageView.setOnLongClickListener(this);

        if (getArguments().getParcelable(ChapterArgumentKey) != null)
        {
            Intent fetchChapter = new Intent(getActivity(), Navigation.class);
            fetchChapter.setAction(Navigation.FIRST_PAGE_FROM_CHAPTER);
            Uri chapterUri = getArguments().getParcelable(ChapterArgumentKey);
            fetchChapter.setData(chapterUri);
            getActivity().startService(fetchChapter);
        }
        else if (getArguments().getParcelable(SeriesArgumentKey) != null)
        {
            Intent fetchPage = new Intent(getActivity(), Navigation.class);
            fetchPage.setAction(Navigation.PAGE_FROM_SERIES);
            Uri seriesUri = getArguments().getParcelable(SeriesArgumentKey);
            fetchPage.setData(seriesUri);
            getActivity().startService(fetchPage);
            series = new Series(getActivity().getContentResolver().query(seriesUri, null, null, null, null));
        }
        else
        {
            throw new AssertionError("No Chapter or Bookmark argument. Check your intent's arguments");
        }
        return layout;
    }

    @Override
    public void onResume()
    {
        super.onPause();
        IntentFilter pageCompleteFilter = new IntentFilter();
        pageCompleteFilter.addAction(Navigation.FIRST_PAGE_FROM_CHAPTER_COMPLETE);
        pageCompleteFilter.addAction(Navigation.NEXT_PAGE_COMPLETE);
        pageCompleteFilter.addAction(Navigation.PREV_PAGE_COMPLETE);
        pageCompleteFilter.addAction(Navigation.PAGE_FROM_SERIES_COMPLETE);
        try
        {
            pageCompleteFilter.addDataType(getActivity().getContentResolver().getType(Page.baseUri()));
        }
        catch (IntentFilter.MalformedMimeTypeException e)
        {
            // I'm a little more OK with this, as Provider.baseUri() is static.
            throw new AssertionError(e);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(loadPageCompleteReceiver, pageCompleteFilter);

        IntentFilter prevFailedFilter = new IntentFilter();
        prevFailedFilter.addAction(Navigation.PREV_PAGE_DOESNT_EXIST);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(prevPageFailedReceiver, prevFailedFilter);

        IntentFilter nextFailedFilter = new IntentFilter();
        nextFailedFilter.addAction(Navigation.NEXT_PAGE_DOESNT_EXIST);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(nextPageFailedReceiver, nextFailedFilter);
    }

    @Override
    public void onPause()
    {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(loadPageCompleteReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(prevPageFailedReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(nextPageFailedReceiver);
        getActivity().getActionBar().show();
    }

    private void nextPage()
    {
        if (page != null)
        {
            Intent nextIntent = new Intent(getActivity(), Navigation.class);
            nextIntent.setAction(Navigation.NEXT_PAGE);
            nextIntent.setData(page.uri());
            getActivity().startService(nextIntent);
            TextView loadingText = (TextView) getActivity().findViewById(R.id.page_loading_text);
            loadingText.setText("Loading next page");
            loadingText.setVisibility(View.VISIBLE);
        }
    }

    private void prevPage()
    {
        if (page != null)
        {
            Intent nextIntent = new Intent(getActivity(), Navigation.class);
            nextIntent.setAction(Navigation.PREV_PAGE);
            nextIntent.setData(page.uri());
            getActivity().startService(nextIntent);
            TextView loadingText = (TextView) getActivity().findViewById(R.id.page_loading_text);
            loadingText.setText("Loading previous page");
            loadingText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSwipeLeft()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (pref.getBoolean(Settings.RTL_ENABLED_KEY, true))
        {
            nextPage();
        }
        else
        {
            prevPage();
        }
    }

    @Override
    public void onSwipeRight()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (pref.getBoolean(Settings.RTL_ENABLED_KEY, true))
        {
            prevPage();
        }
        else
        {
            nextPage();
        }
    }

    @Override
    public void onClick(View v)
    {
        nextPage();
    }

    @Override
    public boolean onLongClick(View v)
    {
        if (getActivity().getActionBar().isShowing())
        {
            getActivity().getActionBar().hide();
        }
        else
        {
            getActivity().getActionBar().show();
        }
        return true;
    }
}



