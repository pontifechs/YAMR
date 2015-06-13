package ninja.dudley.yamr.ui.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import ninja.dudley.yamr.R;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.svc.FetcherAsync;
import ninja.dudley.yamr.svc.Paging;
import ninja.dudley.yamr.ui.util.TouchImageView;

public class PageViewer extends Fragment implements TouchImageView.SwipeListener
{

    public static final String ChapterArgumentKey = "chapter";

    private Chapter chapter;
    private Page page;

    private BroadcastReceiver fetchChapterCompleteReceiver;
    private BroadcastReceiver loadPageCompleteReceiver;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_page_viewer, container, false);
        fetchChapterCompleteReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                chapter = new Chapter(getActivity().getContentResolver().query(intent.getData(), null, null, null, null));

                Uri pagesQuery = chapter.uri().buildUpon().appendPath("pages").build();
                Cursor pages = getActivity().getContentResolver().query(pagesQuery, null, null, null, null);
                Page firstPage = new Page(pages);

                Intent fetchPage = new Intent(getActivity(), FetcherAsync.class);
                fetchPage.setAction(FetcherAsync.FETCH_PAGE);
                fetchPage.setData(firstPage.uri());
                getActivity().startService(fetchPage);
            }
        };

        loadPageCompleteReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                page = new Page(getActivity().getContentResolver().query(intent.getData(), null, null, null, null));
                TouchImageView imageView = (TouchImageView) getActivity().findViewById(R.id.imageView);
                Drawable d = Drawable.createFromPath(page.getImagePath());
                imageView.setImageDrawable(d);
            }
        };

        TouchImageView imageView = (TouchImageView) layout.findViewById(R.id.imageView);
        imageView.register(this);

        Intent fetchChapter = new Intent(getActivity(), FetcherAsync.class);
        fetchChapter.setAction(FetcherAsync.FETCH_CHAPTER);
        Uri chapterUri = getArguments().getParcelable(ChapterArgumentKey);
        fetchChapter.setData(chapterUri);
        getActivity().startService(fetchChapter);
        return layout;
    }

    @Override
    public void onResume()
    {
        super.onPause();
        IntentFilter chapterCompleteFilter = new IntentFilter(FetcherAsync.FETCH_CHAPTER_COMPLETE);
        try
        {
            chapterCompleteFilter.addDataType(getActivity().getContentResolver().getType(Chapter.baseUri()));
        }
        catch (IntentFilter.MalformedMimeTypeException e)
        {
            // I'm a little more OK with this, as Provider.baseUri() is static.
            throw new AssertionError(e);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchChapterCompleteReceiver, chapterCompleteFilter);
        IntentFilter pageCompleteFilter = new IntentFilter();
        pageCompleteFilter.addAction(FetcherAsync.FETCH_PAGE_COMPLETE) ;
        pageCompleteFilter.addAction(Paging.NEXT_PAGE_COMPLETE);
        pageCompleteFilter.addAction(Paging.PREV_PAGE_COMPLETE);
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

    }

    @Override
    public void onPause()
    {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(fetchChapterCompleteReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(loadPageCompleteReceiver);
    }

    @Override
    public void onSwipeLeft()
    {
        if (page != null)
        {
            Intent nextIntent = new Intent(getActivity(), Paging.class);
            nextIntent.setAction(Paging.NEXT_PAGE);
            nextIntent.setData(page.uri());
            getActivity().startService(nextIntent);
        }
    }

    @Override
    public void onSwipeRight()
    {
        if (page != null)
        {
            Intent nextIntent = new Intent(getActivity(), Paging.class);
            nextIntent.setAction(Paging.PREV_PAGE);
            nextIntent.setData(page.uri());
            getActivity().startService(nextIntent);
        }
    }
}



