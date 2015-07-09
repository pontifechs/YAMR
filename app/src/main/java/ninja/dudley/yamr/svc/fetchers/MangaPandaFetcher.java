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
import java.util.ArrayList;
import java.util.List;

import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Genre;
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

    private Document fetchUrl(String url) throws IOException
    {
        Connection.Response response = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                .referrer("http://www.google.com").method(Connection.Method.GET).execute();
        return response.parse();
    }

    // TODO:: See about generalizing this so that FetcherSync can load up what/how to parse based on
    // either pure selectors, pure xpath, or a mix of selectors/javascript (via rhino)?
    // Kind of huge, but whatever haha.

    @Override
    public Provider fetchProvider(Provider provider, FetchBehavior behavior)
    {
        Log.d("Fetch", "Starting a Provider fetch");
        try
        {
            if (behavior == FetchBehavior.LazyFetch && provider.fullyParsed)
            {
                Log.d("Fetch", "Already parsed, skipping");
                return provider;
            }
            Document doc = fetchUrl(provider.url);
            Elements elements = doc.select(".series_alpha li a[href]");

            final int statusStride = (int)Math.ceil(elements.size() / 100.0f);
            int index = 0;
            for (Element e : elements)
            {
                index++;
                if (index % statusStride == 0 && listener != null)
                {
                    float progress = index / (float)elements.size();
                    Log.d("Fetch", "ProviderFetch progress: " + progress);
                    listener.notifyProviderStatus(progress);
                }

                String url = e.absUrl("href");
                if (seriesExists(url))
                {
                    continue;
                }
                Series s = new Series(provider.id);
                s.url = url;
                s.name = e.ownText();
                context.getContentResolver().insert(Series.baseUri(), s.getContentValues());
            }
            provider.fullyParsed = true;
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
    public Series fetchSeries(Series series, FetchBehavior behavior)
    {
        Log.d("Fetch", "Starting Series fetch");
        try
        {
            if (behavior == FetchBehavior.LazyFetch && series.fullyParsed)
            {
                Log.d("Fetch", "Already parsed. Ignoring");
                return series;
            }
            Document doc = fetchUrl(series.url);
            // Parse series info
            Elements propertyElements = doc.select(".propertytitle");
            for (Element property : propertyElements)
            {
                String propTitle = property.text();
                Element sibling = property.parent().select("td:eq(1)").first();
                switch (propTitle)
                {
                    case "Alternate Name:":
                        series.alternateName = sibling.text();
                        break;
                    case "Status:":
                        series.complete = !sibling.text().equals("Ongoing");
                        break;
                    case "Author":
                        series.author = sibling.text();
                        break;
                    case "Artist:":
                        series.artist = sibling.text();
                        break;
                    case "Genre:":
                    {
                        // Genre string
                        Elements genreElements = sibling.select(".genretags");
                        for (Element genre : genreElements)
                        {
                            String genreName = genre.text();
                            if (!genreExists(genreName))
                            {
                                Genre g = new Genre(genreName);
                                context.getContentResolver().insert(Genre.baseUri(), g.getContentValues());
                            }
                            Genre g = new Genre(context.getContentResolver().query(Genre.baseUri(), null, null, new String[]{genreName}, null));

                            // Now that we have the genre for sure, add the relation.
                            context.getContentResolver().insert(Genre.relator(), Genre.SeriesGenreRelator(series.id, g.id));
                        }
                        break;
                    }
                }
            }
            // Parse thumbnail
            Element thumb = doc.select("#mangaimg img[src]").first();
            series.thumbnailUrl = thumb.absUrl("src");
            series.thumbnailPath = saveThumbnail(series);

            // Parse description
            Element summary = doc.select("#readmangasum p").first();
            series.description = summary.text();

            // Parse chapters
            Elements chapterElements = doc.select("td .chico_manga ~ a[href]");
            final int statusStride = (int)Math.ceil(chapterElements.size() / 100.0f);
            int index = 0;
            for (Element e : chapterElements)
            {
                index++;
                if (index % statusStride == 0 && listener != null)
                {
                    float progress = index / (float) chapterElements.size();
                    Log.d("Fetch", "ProviderFetch progress: " + progress);
                    listener.notifySeriesStatus(progress);
                }

                String url = e.absUrl("href");
                if (chapterExists(url))
                {
                    continue;
                }

                Chapter c = new Chapter(series.id);
                c.url = url;
                c.name = e.parent().ownText().replace(":", "");
                c.number = Float.parseFloat(e.text().replace(series.name, ""));
                context.getContentResolver().insert(Chapter.baseUri(), c.getContentValues());
            }
            series.fullyParsed = true;
            context.getContentResolver().update(series.uri(), series.getContentValues(), null, null);
            Log.d("Fetch", "Iteration complete. Series Fetched.");
        }
        catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
        return series;
    }

    @Override
    public Chapter fetchChapter(Chapter chapter, FetchBehavior behavior)
    {
        try
        {
            if (behavior == FetchBehavior.LazyFetch && chapter.fullyParsed)
            {
                Log.d("Fetch", "Already parsed. Ignoring");
                return chapter;
            }
            Document doc = fetchUrl(chapter.url);
            Elements elements = doc.select("#pageMenu option[value]");
            final int statusStride = (int)Math.ceil(elements.size() / 100.0f);
            int index = 0;
            for (Element e : elements)
            {
                index++;
                if (index % statusStride == 0 && listener != null)
                {
                    float progress = index / (float) elements.size();
                    Log.d("Fetch", "Chapter fetch progress: " + progress);
                    listener.notifyChapterStatus(progress);
                }

                String url = e.absUrl("value");
                if (pageExists(url))
                {
                    continue;
                }

                Page p = new Page(chapter.id);
                p.url = url;
                p.number = Float.parseFloat(e.text());
                context.getContentResolver().insert(Page.baseUri(), p.getContentValues());
            }
            chapter.fullyParsed = true;
            context.getContentResolver().update(chapter.uri(), chapter.getContentValues(), null, null);
            Log.d("Fetch", "Iteration complete. Chapter fetched");
        }
        catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
        return chapter;
    }

    @Override
    public Page fetchPage(Page page, FetchBehavior behavior)
    {
        try
        {
            if (behavior == FetchBehavior.LazyFetch && page.fullyParsed)
            {
                Log.d("FetchPage", "Already parsed. Ignoring");
                return page;
            }
            Document doc = fetchUrl(page.url);
            Element element = doc.select("img[src]").first();
            page.imageUrl = element.absUrl("src");
            page.fullyParsed = true;
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

    @Override
    public List<Uri> fetchNew(Provider provider)
    {
        Log.d("FetchStarter", "Starting");
        List<Uri> newChapters = new ArrayList<>();
        try
        {

            Document doc = fetchUrl(provider.newUrl);
            Elements rows = doc.select(".c2");
            for (Element row : rows)
            {
                Element seriesElement = row.select(".chapter").first();
                String seriesUrl = seriesElement.absUrl("href");

                Series series;
                if (seriesExists(seriesUrl))
                {
                    series = new Series(context.getContentResolver().query(Series.baseUri(), null, null, new String[]{seriesUrl}, null));
                }
                else
                {
                    Log.d("Fetch", "Completely New Series!!");
                    series = new Series(provider.id);
                    series.url = seriesUrl;
                    series.name = seriesElement.text();
                    Uri inserted = context.getContentResolver().insert(Series.baseUri(), series.getContentValues());
                    series = new Series(context.getContentResolver().query(inserted, null, null, null, null));
                    fetchSeries(series);
                    continue;
                }

                boolean newChapter = false;
                Elements chapters = row.select(".chaptersrec");
                for (Element chapterElement : chapters)
                {
                    String chapterUrl = chapterElement.absUrl("href");
                    Chapter chapter;
                    if (!chapterExists(chapterUrl))
                    {
                        Log.d("Fetch", "Haven't seen this one.");
                        chapter = new Chapter(series.id);
                        chapter.url = chapterUrl;

                        String body = chapterElement.text();
                        float number = Float.parseFloat(body.replace(series.name, ""));
                        chapter.number = number;

                        context.getContentResolver().insert(Chapter.baseUri(), chapter.getContentValues());
                        newChapter = true;
                        if (!series.favorite)
                        {
                            newChapters.add(chapter.uri());
                        }
                    }
                }
                if (newChapter)
                {
                    series.updated = true;
                }
                context.getContentResolver().update(series.uri(), series.getContentValues(), null, null);
            }
        }
        catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
        return newChapters;
    }
}
