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

    // TODO:: See about generalizing this so that FetcherSync can load up what/how to parse based on
    // either pure selectors, pure xpath, or a mix of selectors/javascript (via rhino)?
    // Kind of huge, but whatever haha.

    @Override
    public Provider fetchProvider(Provider provider, FetchBehavior behavior)
    {
        Log.d("Fetch", "Starting a Provider fetch");
        try
        {
            if (behavior == FetchBehavior.LazyFetch && provider.isFullyParsed())
            {
                Log.d("Fetch", "Already parsed, skipping");
                return provider;
            }
            Connection.Response response = Jsoup.connect(provider.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = response.parse();
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
                Series s = new Series();
                s.setProviderId(provider.getId());
                s.setUrl(url);
                s.setName(e.ownText());
                context.getContentResolver().insert(Series.baseUri(), s.getContentValues());
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
    public Series fetchSeries(Series series, FetchBehavior behavior)
    {
        Log.d("Fetch", "Starting Series fetch");
        try
        {
            if (behavior == FetchBehavior.LazyFetch && series.isFullyParsed())
            {
                Log.d("Fetch", "Already parsed. Ignoring");
                return series;
            }
            Connection.Response response = Jsoup.connect(series.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = response.parse();
            // Parse series info
            Elements propertyElements = doc.select(".propertytitle");
            for (Element property : propertyElements)
            {
                String propTitle = property.text();
                Element sibling = property.parent().select("td:eq(1)").first();
                switch (propTitle)
                {
                    case "Alternate Name:":
                        series.setAlternateName(sibling.text());
                        break;
                    case "Status:":
                        series.setComplete(!sibling.text().equals("Ongoing"));
                        break;
                    case "Author":
                        series.setAuthor(sibling.text());
                        break;
                    case "Artist:":
                        series.setArtist(sibling.text());
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
                            Uri genreRelator = Genre.baseUri().buildUpon().appendPath("relator").build();
                            context.getContentResolver().insert(genreRelator, Genre.SeriesGenreRelator(series.getId(), g.getId()));
                        }

                        break;
                    }
                }
            }
            // Parse thumbnail
            Element thumb = doc.select("#mangaimg img[src]").first();
            series.setThumbnailUrl(thumb.absUrl("src"));
            series.setThumbnailPath(saveThumbnail(series));

            // Parse description
            Element summary = doc.select("#readmangasum p").first();
            series.setDescription(summary.text());

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

                Chapter c = new Chapter();
                c.setSeriesId(series.getId());
                c.setUrl(url);
                c.setName(e.parent().ownText().replace(":", ""));
                c.setNumber(Float.parseFloat(e.text().replace(series.getName(), "")));
                context.getContentResolver().insert(Chapter.baseUri(), c.getContentValues());
            }
            series.setFullyParsed(true);
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
            if (behavior == FetchBehavior.LazyFetch && chapter.isFullyParsed())
            {
                Log.d("Fetch", "Already parsed. Ignoring");
                return chapter;
            }
            Connection.Response response = Jsoup.connect(chapter.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = response.parse();
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

                Page p = new Page();
                p.setChapterId(chapter.getId());
                p.setUrl(url);
                p.setNumber(Float.parseFloat(e.text()));
                context.getContentResolver().insert(Page.baseUri(), p.getContentValues());
            }
            chapter.setFullyParsed(true);
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
            if (behavior == FetchBehavior.LazyFetch && page.isFullyParsed())
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

    @Override
    public void fetchNew(Provider provider)
    {
        Log.d("FetchNew", "Starting");
        try
        {
            Connection.Response response = Jsoup.connect(provider.getNewUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = response.parse();
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
                    series = new Series();
                    series.setUrl(seriesUrl);
                    series.setProviderId(provider.getId());
                    series.setName(seriesElement.text());
                    Uri inserted = context.getContentResolver().insert(Series.baseUri(), series.getContentValues());
                    series = new Series(context.getContentResolver().query(inserted, null, null, null, null));
                    fetchSeries(series);
                    continue;
                }

                Elements chapters = row.select(".chaptersrec");
                for (Element chapterElement : chapters)
                {
                    String chapterUrl = chapterElement.absUrl("href");
                    Chapter chapter;
                    if (!chapterExists(chapterUrl))
                    {
                        Log.d("Fetch", "Haven't seen this one.");
                        chapter = new Chapter();
                        chapter.setUrl(chapterUrl);
                        chapter.setSeriesId(series.getId());

                        String body = chapterElement.text();
                        float number = Float.parseFloat(body.replace(series.getName(), ""));
                        chapter.setNumber(number);

                        context.getContentResolver().insert(Chapter.baseUri(), chapter.getContentValues());
                    }
                }
            }
        }
        catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
    }
}
