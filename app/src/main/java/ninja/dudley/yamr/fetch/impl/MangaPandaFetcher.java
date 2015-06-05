package ninja.dudley.yamr.fetch.impl;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import ninja.dudley.yamr.fetch.Fetcher;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.model.Series;

/**
 * Created by mdudley on 5/28/15.
 */
public class MangaPandaFetcher extends Fetcher
{
    public MangaPandaFetcher()
    {
        super("MangaPandaFetcher");
    }

    // TODO:: See about generalizing this so that Fetcher can load up what/how to parse based on
    // either pure selectors, pure xpath, or a mix of selectors/javascript (via rhino)?
    // Kind of huge, but whatever haha.

    @Override
    public void fetchProvider(Provider provider)
    {
        try
        {
            if (provider.isFullyParsed())
            {
                Log.d("Fetch", "Already parsed. Ignoring");
                return;
            }
            Log.d("Fetch", "Starting Provider Fetch.");
            Connection.Response response = Jsoup.connect(provider.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Log.d("Fetch", "HTML downloaded. Parsing.");
            Document doc = response.parse();
            Log.d("Fetch", "Parse complete. Selecting.");
            Elements elements = doc.select(".series_alpha li a[href]");
            Log.d("Fetch", "Selection complete. Iterating");

            final int statusStride = elements.size() / 100;
            int index = 0;
            for (Element e : elements)
            {
                Series s = new Series();
                s.setProviderId(provider.getId());
                s.setUrl(e.absUrl("href"));
                s.setName(e.ownText());
                Uri inserted = getContentResolver().insert(Series.baseUri(), s.getContentValues());
                getContentResolver().notifyChange(provider.uri().buildUpon().appendPath("series").build(), null);
                Log.d("Fetch", "Inserted: " + inserted.toString());
                Log.d("Fetch", "url: " + s.getUrl());
                index++;
                if (index % statusStride == 0)
                {
                    Intent i = new Intent(FETCH_PROVIDER_STATUS);
                    i.putExtra(FETCH_PROVIDER_STATUS, index / (float)elements.size());
                    LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
                }
            }
            provider.setFullyParsed(true);
            getContentResolver().update(provider.uri(), provider.getContentValues(), null, null);
            Intent i = new Intent(FETCH_PROVIDER_COMPLETE);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
            Log.d("Fetch", "Iteration complete. Provider Fetched.");
        } catch (IOException e)
        {
            // Shrug?
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fetchSeries(Series series)
    {
        try
        {
            if (series.isFullyParsed())
            {
                Log.d("FetchSeries", "Already parsed. Ignoring");
                return;
            }
            Connection.Response response = Jsoup.connect(series.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = response.parse();
            Elements elements = doc.select("td .chico_manga ~ a[href]");
            float index = 0;
            for (Element e : elements)
            {
                Chapter c = new Chapter();
                c.setSeriesId(series.getId());
                c.setUrl(e.attr("abs:href"));
                c.setName(e.parent().ownText().replace(":", ""));
                c.setNumber(index++);
                Uri inserted = getContentResolver().insert(Chapter.baseUri(), c.getContentValues());
                Log.d("Fetch", "Inserted: " + inserted.toString());
                Log.d("Fetch", "url: " + c.getUrl());
            }
            series.setFullyParsed(true);
            getContentResolver().update(series.uri(), series.getContentValues(), null, null);
        } catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fetchChapter(Chapter chapter)
    {
        try
        {
            if (chapter.isFullyParsed())
            {
                Log.d("FetchChapter", "Already parsed. Ignoring");
                return;
            }
            Connection.Response response = Jsoup.connect(chapter.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = response.parse();
            Elements elements = doc.select("#pageMenu option[value]");
            float index = 0;
            for (Element e : elements)
            {
                Page p = new Page();
                p.setChapterId(chapter.getId());
                p.setUrl(e.absUrl("value"));
                p.setNumber(index++);
                Uri inserted = getContentResolver().insert(Page.baseUri(), p.getContentValues());
                Log.d("FetchChapter", "Inserted: " + inserted.toString());
                Log.d("FetchChapter", "url: " + p.getUrl());
            }
            chapter.setFullyParsed(true);
            getContentResolver().update(chapter.uri(), chapter.getContentValues(), null, null);
        } catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fetchPage(Page page)
    {
        try
        {
            if (page.isFullyParsed())
            {
                Log.d("FetchPage", "Already parsed. Ignoring");
                return;
            }
            Connection.Response response = Jsoup.connect(page.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = response.parse();
            Element element = doc.select("img[src]").first();
            page.setImagePath(element.absUrl("src"));
            page.setFullyParsed(true);
            getContentResolver().update(page.uri(), page.getContentValues(), null, null);
            Log.d("FetchPage", "Done");
        } catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFetcherIdentifier()
    {
        return "MangaPanda";
    }
}
