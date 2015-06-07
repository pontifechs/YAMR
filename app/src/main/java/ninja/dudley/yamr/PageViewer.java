package ninja.dudley.yamr;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import ninja.dudley.yamr.fetch.Fetcher;
import ninja.dudley.yamr.fetch.impl.MangaPandaFetcher;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.util.TouchImageView;

public class PageViewer extends Activity
{
    private Chapter chapter;
    private Page page;

    private BroadcastReceiver fetchChapterCompleteReceiver;
    private BroadcastReceiver fetchPageCompleteReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_viewer);

        fetchChapterCompleteReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                chapter = new Chapter(getContentResolver().query(intent.getData(), null, null, null, null));

                Uri pagesQuery = chapter.uri().buildUpon().appendPath("pages").build();
                Cursor pages = getContentResolver().query(pagesQuery, null, null, null, null);
                Page firstPage = new Page(pages);

                Intent fetchPage = new Intent(PageViewer.this, MangaPandaFetcher.class);
                fetchPage.setAction(Fetcher.FETCH_PAGE);
                fetchPage.setData(firstPage.uri());
                startService(fetchPage);
            }
        };

        fetchPageCompleteReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                page = new Page(getContentResolver().query(intent.getData(), null, null, null, null));
                TouchImageView imageView = (TouchImageView) findViewById(R.id.imageView);
                Drawable d = Drawable.createFromPath(page.getImagePath());
                imageView.setImageDrawable(d);
            }
        };

        Intent fetchChapter = new Intent(this, MangaPandaFetcher.class);
        fetchChapter.setAction(Fetcher.FETCH_CHAPTER);
        fetchChapter.setData(getIntent().getData());
        startService(fetchChapter);
    }

    @Override
    protected void onResume()
    {
        super.onPause();
        IntentFilter chapterCompleteFilter = new IntentFilter(Fetcher.FETCH_CHAPTER_COMPLETE);
        try
        {
            chapterCompleteFilter.addDataType(getContentResolver().getType(Chapter.baseUri()));
        } catch (IntentFilter.MalformedMimeTypeException e)
        {
            // I'm a little more OK with this, as Provider.baseUri() is static.
            throw new AssertionError(e);
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(fetchChapterCompleteReceiver, chapterCompleteFilter);
        IntentFilter pageCompleteFilter = new IntentFilter(Fetcher.FETCH_PAGE_COMPLETE);
        try
        {
            pageCompleteFilter.addDataType(getContentResolver().getType(Page.baseUri()));
        } catch (IntentFilter.MalformedMimeTypeException e)
        {
            // I'm a little more OK with this, as Provider.baseUri() is static.
            throw new AssertionError(e);
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(fetchPageCompleteReceiver, pageCompleteFilter);
    }

    @Override
    protected void onPause()
    {
        super.onResume();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fetchChapterCompleteReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fetchPageCompleteReceiver);
    }
}



