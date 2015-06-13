package ninja.dudley.yamr.svc.fetchers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.model.Series;
import ninja.dudley.yamr.svc.FetcherSync;

/**
 * Created by mdudley on 5/28/15.
 */
public class MangaPandaFetcher extends FetcherSync
{
    public MangaPandaFetcher(Context context)
    {
        super(context);
    }

    // TODO:: See about generalizing this so that FetcherSync can load up what/how to parse based on
    // either pure selectors, pure xpath, or a mix of selectors/javascript (via rhino)?
    // Kind of huge, but whatever haha.

    @Override
    public Provider fetchProvider(Provider provider)
    {
        try
        {
            if (provider.isFullyParsed())
            {
                Log.d("Fetch", "Already parsed. Ignoring");
                return provider;
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

            final int statusStride = (int)Math.ceil(elements.size() / 100.0f);
            int index = 0;
            for (Element e : elements)
            {
                Series s = new Series();
                s.setProviderId(provider.getId());
                s.setUrl(e.absUrl("href"));
                s.setName(e.ownText());
                Uri inserted = context.getContentResolver().insert(Series.baseUri(), s.getContentValues());
                context.getContentResolver().notifyChange(provider.uri().buildUpon().appendPath("series").build(), null);
                Log.d("Fetch", "Inserted: " + inserted.toString());
                Log.d("Fetch", "url: " + s.getUrl());
                index++;
                if (index % statusStride == 0 && listener != null)
                {
                    listener.notifyProviderStatus(index / (float)elements.size());
                }
            }
            provider.setFullyParsed(true);
            context.getContentResolver().update(provider.uri(), provider.getContentValues(), null, null);
            Log.d("Fetch", "Iteration complete. Provider Fetched.");
        }
        catch (IOException e)
        {
            // Shrug?
            throw new RuntimeException(e);
        }
        return provider;
    }

    @Override
    public Series fetchSeries(Series series)
    {
        try
        {
            if (series.isFullyParsed())
            {
                Log.d("FetchSeries", "Already parsed. Ignoring");
                return series;
            }
            Connection.Response response = Jsoup.connect(series.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = response.parse();
            Elements elements = doc.select("td .chico_manga ~ a[href]");
            float number = 1.0f;
            final int statusStride = (int)Math.ceil(elements.size() / 100.0f);
            int index = 0;
            for (Element e : elements)
            {
                Chapter c = new Chapter();
                c.setSeriesId(series.getId());
                c.setUrl(e.attr("abs:href"));
                c.setName(e.parent().ownText().replace(":", ""));
                c.setNumber(number++);
                Uri inserted = context.getContentResolver().insert(Chapter.baseUri(), c.getContentValues());
                Log.d("FetchSeries", "Inserted: " + inserted.toString());
                Log.d("FetchSeries", "url: " + c.getUrl());
                index++;
                if (index % statusStride == 0 && listener != null)
                {
                    listener.notifySeriesStatus(index / (float)elements.size());
                }
            }
            series.setFullyParsed(true);
            context.getContentResolver().update(series.uri(), series.getContentValues(), null, null);
            Log.d("FetchSeries", "Iteration complete. Series Fetched.");
        }
        catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
        return series;
    }

    @Override
    public Chapter fetchChapter(Chapter chapter)
    {
        try
        {
            if (chapter.isFullyParsed())
            {
                Log.d("FetchChapter", "Already parsed. Ignoring");
                return chapter;
            }
            Connection.Response response = Jsoup.connect(chapter.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = response.parse();
            Elements elements = doc.select("#pageMenu option[value]");
            float number = 1.0f;
            final int statusStride = (int)Math.ceil(elements.size() / 100.0f);
            int index = 0;
            for (Element e : elements)
            {
                Page p = new Page();
                p.setChapterId(chapter.getId());
                p.setUrl(e.absUrl("value"));
                p.setNumber(number++);
                Uri inserted = context.getContentResolver().insert(Page.baseUri(), p.getContentValues());
                Log.d("FetchChapter", "Inserted: " + inserted.toString());
                Log.d("FetchChapter", "url: " + p.getUrl());
                index++;
                if (index % statusStride == 0 && listener != null)
                {
                    listener.notifyChapterStatus(index / (float)elements.size());
                }
            }
            chapter.setFullyParsed(true);
            context.getContentResolver().update(chapter.uri(), chapter.getContentValues(), null, null);
            Log.d("FetchChapter", "Iteration complete. Chapter fetched");
        }
        catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
        return chapter;
    }

    @Override
    public Page fetchPage(Page page)
    {
        try
        {
            if (page.isFullyParsed())
            {
                Log.d("FetchPage", "Already parsed. Ignoring");
                return page;
            }
            Connection.Response response = Jsoup.connect(page.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = response.parse();
            Element element = doc.select("img[src]").first();
            page.setImageUrl(element.absUrl("src"));
            page.setFullyParsed(true);
            savePageImage(page);
            Log.d("FetchPage", "Done");
        }
        catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
        return page;
    }
}
