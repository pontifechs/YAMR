package ninja.dudley.yamr.fetch.impl;

import android.net.Uri;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import ninja.dudley.yamr.db.DBHelper;
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
            Connection.Response response = Jsoup.connect("http://www.mangapanda.com/alphabetical")
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Log.d("Fetch", "HTML downloaded. Parsing.");
            Document doc = response.parse();
            Log.d("Fetch", "Parse complete. Selecting.");
            Elements elements = doc.select(".series_alpha li a[href]");
            Log.d("Fetch", "Selection complete. Iterating");
            for (Element e : elements)
            {
                Series s = new Series();
                s.setProviderId(provider.getId());
                s.setUrl(e.attr("abs:href"));
                Uri inserted = getContentResolver().insert(Uri.parse("content://" + DBHelper.AUTHORITY + "/series"), s.getContentValues());
                Log.d("Fetch", "Inserted: " + inserted.toString());
                Log.d("Fetch", "url: " + s.getUrl());
            }
            provider.setFullyParsed(true);
            Uri providerUri = Uri.parse("content://" + DBHelper.AUTHORITY + "/provider/" + provider.getId());
            getContentResolver().update(providerUri, provider.getContentValues(), null, null);
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
            for (Element e : elements)
            {
                Chapter c = new Chapter();
                c.setSeriesId(series.getId());
                c.setUrl(e.attr("abs:href"));
                Uri inserted = getContentResolver().insert(Uri.parse("content://" + DBHelper.AUTHORITY + "/chapter"), c.getContentValues());
                Log.d("Fetch", "Inserted: " + inserted.toString());
                Log.d("Fetch", "url: " + c.getUrl());
            }
            series.setFullyParsed(true);
            Uri seriesUri = Uri.parse("content://" + DBHelper.AUTHORITY + "/series/" + series.getId());
            getContentResolver().update(seriesUri, series.getContentValues(), null, null);
        } catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fetchChapter(Chapter chapter)
    {

    }

    @Override
    public void fetchPage(Page page)
    {

    }

    @Override
    public String getFetcherIdentifier()
    {
        return "MangaPanda";
    }
}
