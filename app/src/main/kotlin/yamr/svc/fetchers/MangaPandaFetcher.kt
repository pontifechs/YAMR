package ninja.dudley.yamr.svc.fetchers

import android.content.Context
import android.net.Uri
import android.util.Log

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import java.io.IOException
import java.util.ArrayList

import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Genre
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Provider
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherSync

/**
 * Created by mdudley on 5/28/15.
 */
public class MangaPandaFetcher(context: Context) : FetcherSync(context)
{

    throws(IOException::class)
    private fun fetchUrl(url: String): Document
    {
        val response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0").referrer("http://www.google.com").method(Connection.Method.GET).execute()
        return response.parse()
    }

    // TODO:: See about generalizing this so that FetcherSync can load up what/how to parse based on
    // either pure selectors, pure xpath, or a mix of selectors/javascript (via rhino)?
    // Kind of huge, but whatever haha.
    override fun fetchProvider(provider: Provider, behavior: FetcherSync.FetchBehavior): Provider
    {
        Log.d("Fetch", "Starting a Provider fetch")
        try
        {
            if (behavior === FetcherSync.FetchBehavior.LazyFetch && provider.fullyParsed)
            {
                Log.d("Fetch", "Already parsed, skipping")
                return provider
            }
            val doc = fetchUrl(provider.url)
            val elements = doc.select(".series_alpha li a[href]")

            val statusStride = Math.ceil((elements.size() / 100.0f).toDouble()).toInt()
            var index = 0
            for (e in elements)
            {
                index++
                if (index % statusStride == 0 && listener != null)
                {
                    val progress = index / elements.size().toFloat()
                    Log.d("Fetch", "ProviderFetch progress: " + progress)
                    listener?.notifyProviderStatus(progress)
                }

                val url = e.absUrl("href")
                if (seriesExists(url))
                {
                    continue
                }
                val s = Series(provider.id, url, e.ownText())
                context.getContentResolver().insert(Series.baseUri(), s.getContentValues())
            }
            provider.fullyParsed = true
            context.getContentResolver().update(provider.uri(), provider.getContentValues(), null, null)
            Log.d("Fetch", "Iteration complete. Provider Fetched.")
        }
        catch (e: IOException)
        {
            // Shrug?
            throw RuntimeException(e)
        }

        return provider
    }

    override fun fetchSeries(series: Series, behavior: FetcherSync.FetchBehavior): Series
    {
        Log.d("Fetch", "Starting Series fetch")
        try
        {
            if (behavior === FetcherSync.FetchBehavior.LazyFetch && series.fullyParsed)
            {
                Log.d("Fetch", "Already parsed. Ignoring")
                return series
            }
            val doc = fetchUrl(series.url)
            // Parse series info
            val propertyElements = doc.select(".propertytitle")
            for (property in propertyElements)
            {
                val propTitle = property.text()
                val sibling = property.parent().select("td:eq(1)").first()
                when (propTitle)
                {
                    "Alternate Name:" -> series.alternateName = sibling.text()
                    "Status:" -> series.complete = sibling.text() != "Ongoing"
                    "Author" -> series.author = sibling.text()
                    "Artist:" -> series.artist = sibling.text()
                    "Genre:" ->
                    {
                        // Genre string
                        val genreElements = sibling.select(".genretags")
                        for (genre in genreElements)
                        {
                            val genreName = genre.text()
                            if (!genreExists(genreName))
                            {
                                val g = Genre(genreName)
                                context.getContentResolver().insert(Genre.baseUri(), g.getContentValues())
                            }
                            val g = Genre(context.getContentResolver().query(Genre.baseUri(), null, null, arrayOf(genreName), null))

                            // Now that we have the genre for sure, add the relation.
                            context.getContentResolver().insert(Genre.relator(), Genre.SeriesGenreRelator(series.id, g.id))
                        }
                    }
                }
            }
            // Parse thumbnail
            val thumb = doc.select("#mangaimg img[src]").first()
            series.thumbnailUrl = thumb.absUrl("src")
            series.thumbnailPath = saveThumbnail(series)

            // Parse description
            val summary = doc.select("#readmangasum p").first()
            series.description = summary.text()

            // Parse chapters
            val chapterElements = doc.select("td .chico_manga ~ a[href]")
            val statusStride = Math.ceil((chapterElements.size() / 100.0f).toDouble()).toInt()
            var index = 0
            for (e in chapterElements)
            {
                index++
                if (index % statusStride == 0 && listener != null)
                {
                    val progress = index / chapterElements.size().toFloat()
                    Log.d("Fetch", "ProviderFetch progress: " + progress)
                    listener?.notifySeriesStatus(progress)
                }

                val url = e.absUrl("href")
                if (chapterExists(url))
                {
                    continue
                }

                val c = Chapter(series.id, url, java.lang.Float.parseFloat(e.text().replace(series.name, "")))
                c.name = e.parent().ownText().replace(":", "")
                context.getContentResolver().insert(Chapter.baseUri(), c.getContentValues())
            }
            series.fullyParsed = true
            context.getContentResolver().update(series.uri(), series.getContentValues(), null, null)
            Log.d("Fetch", "Iteration complete. Series Fetched.")
        }
        catch (e: IOException)
        {
            // Panic? IDK what might cause this.
            throw RuntimeException(e)
        }

        return series
    }

    override fun fetchChapter(chapter: Chapter, behavior: FetcherSync.FetchBehavior): Chapter
    {
        try
        {
            if (behavior === FetcherSync.FetchBehavior.LazyFetch && chapter.fullyParsed)
            {
                Log.d("Fetch", "Already parsed. Ignoring")
                return chapter
            }
            val doc = fetchUrl(chapter.url)
            val elements = doc.select("#pageMenu option[value]")
            val statusStride = Math.ceil((elements.size() / 100.0f).toDouble()).toInt()
            var index = 0
            for (e in elements)
            {
                index++
                if (index % statusStride == 0 && listener != null)
                {
                    val progress = index / elements.size().toFloat()
                    Log.d("Fetch", "Chapter fetch progress: " + progress)
                    listener?.notifyChapterStatus(progress)
                }

                val url = e.absUrl("value")
                if (pageExists(url))
                {
                    continue
                }

                val p = Page(chapter.id, url, java.lang.Float.parseFloat(e.text()))
                context.getContentResolver().insert(Page.baseUri(), p.getContentValues())
            }
            chapter.fullyParsed = true
            context.getContentResolver().update(chapter.uri(), chapter.getContentValues(), null, null)
            Log.d("Fetch", "Iteration complete. Chapter fetched")
        }
        catch (e: IOException)
        {
            // Panic? IDK what might cause this.
            throw RuntimeException(e)
        }

        return chapter
    }

    override fun fetchPage(page: Page, behavior: FetcherSync.FetchBehavior): Page
    {
        try
        {
            if (behavior === FetcherSync.FetchBehavior.LazyFetch && page.fullyParsed)
            {
                Log.d("FetchPage", "Already parsed. Ignoring")
                return page
            }
            val doc = fetchUrl(page.url)
            val element = doc.select("img[src]").first()
            page.imageUrl = element.absUrl("src")
            page.fullyParsed = true
            savePageImage(page)
            Log.d("FetchPage", "Done")
        }
        catch (e: IOException)
        {
            // Panic? IDK what might cause this.
            throw RuntimeException(e)
        }

        return page
    }

    override fun fetchNew(provider: Provider): List<Uri>
    {
        Log.d("FetchStarter", "Starting")
        val newChapters = ArrayList<Uri>()
        try
        {

            val doc = fetchUrl(provider.newUrl)
            val rows = doc.select(".c2")
            for (row in rows)
            {
                val seriesElement = row.select(".chapter").first()
                val seriesUrl = seriesElement.absUrl("href")

                var series: Series
                if (seriesExists(seriesUrl))
                {
                    series = Series(context.getContentResolver().query(Series.baseUri(), null, null, arrayOf(seriesUrl), null))
                }
                else
                {
                    Log.d("Fetch", "Completely New Series!!")
                    series = Series(provider.id, seriesUrl, seriesElement.text())
                    val inserted = context.getContentResolver().insert(Series.baseUri(), series.getContentValues())
                    series = Series(context.getContentResolver().query(inserted, null, null, null, null))
                    fetchSeries(series)
                    continue
                }

                var newChapter = false
                val chapters = row.select(".chaptersrec")
                for (chapterElement in chapters)
                {
                    val chapterUrl = chapterElement.absUrl("href")
                    val chapter: Chapter
                    if (!chapterExists(chapterUrl))
                    {
                        Log.d("Fetch", "Haven't seen this one.")
                        val body = chapterElement.text()
                        val number = java.lang.Float.parseFloat(body.replace(series.name, ""))
                        chapter = Chapter(series.id, chapterUrl, number)

                        context.getContentResolver().insert(Chapter.baseUri(), chapter.getContentValues())
                        newChapter = true
                        if (series.favorite)
                        {
                            newChapters.add(chapter.uri())
                        }
                    }
                }
                if (newChapter)
                {
                    series.updated = true
                }
                context.getContentResolver().update(series.uri(), series.getContentValues(), null, null)
            }
        }
        catch (e: IOException)
        {
            // Panic? IDK what might cause this.
            throw RuntimeException(e)
        }

        return newChapters
    }
}
