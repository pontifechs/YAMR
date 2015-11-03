package ninja.dudley.yamr.svc

import android.content.ContentResolver
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.model.js.JsChapter
import ninja.dudley.yamr.model.js.JsPage
import ninja.dudley.yamr.model.js.JsSeries
import org.acra.ACRA
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.ArrayList

/**
 * Created by mdudley on 5/19/15.
 */
public open class FetcherSync
{
    private var cx: Context? = null
    private var scope: ScriptableObject? = null
    private val resolver: ContentResolver
    private var provider: Provider? = null

    public constructor(resolver: ContentResolver)
    {
        this.resolver = resolver
    }

    //TODO:: Generify heritage?
    private fun providerFromSeries(series: Series): Provider
    {
        return Provider(resolver.query(Provider.uri(series.providerId), null, null, null, null))
    }

    private fun providerFromChapter(chapter: Chapter): Provider
    {
        val series = Series(resolver.query(Series.uri(chapter.seriesId), null, null, null, null))
        return providerFromSeries(series)
    }

    private fun providerFromPage(page: Page): Provider
    {
        val chapter = Chapter(resolver.query(Chapter.uri(page.chapterId), null, null, null, null))
        return providerFromChapter(chapter)
    }

    private fun getProvider(element: MangaElement): Provider
    {
        when (element.type)
        {
            MangaElement.UriType.Provider ->
            {
                return element as Provider
            }
            MangaElement.UriType.Series ->
            {
                return providerFromSeries(element as Series)
            }
            MangaElement.UriType.Chapter ->
            {
                return providerFromChapter(element as Chapter)
            }
            MangaElement.UriType.Page ->
            {
                return providerFromPage(element as Page)
            }
            MangaElement.UriType.Genre ->
            {
                throw IllegalArgumentException("Can't get provider from Genre")
            }
        }
    }

    protected fun init(mangaElement: MangaElement)
    {
        cx = Context.enter()
        cx!!.optimizationLevel = -1
        scope = cx!!.initStandardObjects()

        this.provider = getProvider(mangaElement)

        try
        {
            ScriptableObject.defineClass(scope, JsSeries::class.java)
            ScriptableObject.defineClass(scope, JsChapter::class.java)
            ScriptableObject.defineClass(scope, JsPage::class.java)
        }
        catch (e: Exception)
        {
            Log.e("RHINO", e.toString())
            // Gotta catch 'em all!
            throw RuntimeException(e)
        }

        cx!!.evaluateString(scope, provider!!.fetchProvider, "fetchProvider", 1, null);
        cx!!.evaluateString(scope, provider!!.stubSeries, "stubSeries", 1, null);
        cx!!.evaluateString(scope, provider!!.fetchSeries, "fetchSeries", 1, null);
        cx!!.evaluateString(scope, provider!!.fetchSeriesGenres, "fetchSeriesGenres", 1, null);
        cx!!.evaluateString(scope, provider!!.stubChapter, "stubChapter", 1, null);
        cx!!.evaluateString(scope, provider!!.fetchChapter, "fetchChapter", 1, null);
        cx!!.evaluateString(scope, provider!!.stubPage, "stubPage", 1, null);
        cx!!.evaluateString(scope, provider!!.fetchPage, "fetchPage", 1, null);
        cx!!.evaluateString(scope, provider!!.fetchNew, "fetchNew", 1, null);
    }

    public interface NotifyStatus
    {
        public fun notify(status: Float): Boolean
    }

    protected var listener: NotifyStatus? = null
    public fun register(listener: NotifyStatus)
    {
        this.listener = listener
    }

    public enum class Behavior
    {
        LazyFetch,
        ForceRefresh;
    }

    public fun fetchProvider(provider: Provider, behavior: Behavior = Behavior.LazyFetch): Provider
    {
        if (behavior === FetcherSync.Behavior.LazyFetch && provider.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return provider
        }

        init(provider)
        Log.d("Fetch", "Starting a Provider fetch")
        try
        {
            val doc = fetchUrl(provider.url)
            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            ScriptableObject.putProperty(scope, "provider", Context.javaToJS(provider, scope))
            val result = cx!!.evaluateString(scope, "fetchProvider(doc, provider);", "fetchProvider", 100, null);
            val elements = Context.jsToJava(result, Elements::class.java) as Elements
            Log.d("RHINO", "${elements.size()}")
            provider.fullyParsed = true
            resolver.update(provider.uri(), provider.getContentValues(), null, null)
            Log.d("Fetch", "Iteration complete. Provider Fetched.")

            val statusStride = Math.ceil((elements.size() / 100.0f).toDouble()).toInt()
            var index = 0
            for (e in elements)
            {

                index++
                if (index % statusStride == 0 && listener != null)
                {
                    val progress = index / elements.size().toFloat()
                    Log.d("Fetch", "ProviderFetch progress: ${progress}")
                    listener?.notify(progress)
                }
                ScriptableObject.putProperty(scope, "element", Context.javaToJS(e, scope))
                val seriesResult = cx!!.evaluateString(scope, "stubSeries(element);", "fetchProvider", 200, null);
                val jsSeries = Context.jsToJava(seriesResult, JsSeries::class.java) as JsSeries
                val s = jsSeries.unJS(provider.id)

                if (seriesExists(s.url))
                {
                    continue
                }
                resolver.insert(Series.baseUri(), s.getContentValues())
            }
            provider.fullyParsed = true
        }
        catch (e: IOException)
        {
            // Shrug?
            throw RuntimeException(e)
        }

        return provider
    }

    public fun fetchSeries(series: Series, behavior: Behavior = Behavior.LazyFetch): Series
    {
        if (behavior === FetcherSync.Behavior.LazyFetch && series.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return series
        }

        init(series)
        Log.d("Fetch", "Starting Series fetch: ${series.id}: ${series.url}")
        try
        {
            val doc = fetchUrl(series.url)
            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            ScriptableObject.putProperty(scope, "series", Context.javaToJS(series, scope))

            val genreResult = cx!!.evaluateString(scope, "fetchSeriesGenres(doc, series);", "fetchProvider", 100, null);
            val genres = Context.jsToJava(genreResult, List::class.java) as List<String>

            // Clean up any the existing relations (Only really relevant with a refresh.)
            resolver.delete(Series.genres(series.id), null, null)

            for (genre in genres)
            {
                if (!genreExists(genre))
                {
                    val g = Genre(genre)
                    resolver.insert(Genre.baseUri(), g.getContentValues())
                }
                val g = Genre(resolver.query(Genre.baseUri(), null, null, arrayOf(genre), null))


                // Now that we have the genre for sure, add the relation.
                resolver.insert(Genre.relator(), Genre.SeriesGenreRelator(series.id, g.id))
            }

            val result = cx!!.evaluateString(scope, "fetchSeries(doc, series);", "fetchProvider", 300, null);
            val elements = Context.jsToJava(result, Elements::class.java) as Elements

            // Parse chapters
            val statusStride = Math.ceil((elements.size() / 100.0f).toDouble()).toInt()
            var index = 0
            for (e in elements)
            {

                index++
                if (index % statusStride == 0 && listener != null)
                {
                    val progress = index / elements.size().toFloat()
                    Log.d("Fetch", "SeriesFetch progress: ${progress}")
                    listener?.notify(progress)
                }
                ScriptableObject.putProperty(scope, "element", Context.javaToJS(e, scope))
                val chapterResult = cx!!.evaluateString(scope, "stubChapter(element);", "fetchProvider", 400, null);
                val jsChapter = Context.jsToJava(chapterResult, JsChapter::class.java) as JsChapter
                val chapter = jsChapter.unJS(series.id)

                if (chapterExists(chapter.url))
                {
                    continue
                }
                resolver.insert(Chapter.baseUri(), chapter.getContentValues())
            }
            series.fullyParsed = true
            series.thumbnailPath = saveThumbnail(series)
            resolver.update(series.uri(), series.getContentValues(), null, null)
            Log.d("Fetch", "Iteration complete. Series Fetched.")
        }
        catch (e: IOException)
        {
            // Panic? IDK what might cause this.
            throw RuntimeException(e)
        }

        return series
    }

    public fun fetchChapter(chapter: Chapter, behavior: Behavior = Behavior.LazyFetch): Chapter
    {
        if (behavior === FetcherSync.Behavior.LazyFetch && chapter.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return chapter
        }

        init(chapter)
        try
        {
            val doc = fetchUrl(chapter.url)
            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            ScriptableObject.putProperty(scope, "chapter", Context.javaToJS(chapter, scope))
            val result = cx!!.evaluateString(scope, "fetchChapter(doc, chapter);", "fetchProvider", 500, null);
            val elements = Context.jsToJava(result, Elements::class.java) as Elements

            val statusStride = Math.ceil((elements.size() / 100.0f).toDouble()).toInt()
            var index = 0
            for (e in elements)
            {
                index++
                if (index % statusStride == 0 && listener != null)
                {
                    val progress = index / elements.size().toFloat()
                    Log.d("Fetch", "Chapter fetch progress: ${progress}")
                    listener?.notify(progress)
                }

                ScriptableObject.putProperty(scope, "element", Context.javaToJS(e, scope))
                val pageResult = cx!!.evaluateString(scope, "stubPage(element);", "fetchProvider", 600, null);
                val jsPage = Context.jsToJava(pageResult, JsPage::class.java) as JsPage
                val page = jsPage.unJS(chapter.id)

                if (pageExists(page.url))
                {
                    continue
                }

                resolver.insert(Page.baseUri(), page.getContentValues())
            }
            chapter.fullyParsed = true
            resolver.update(chapter.uri(), chapter.getContentValues(), null, null)
            Log.d("Fetch", "Iteration complete. Chapter fetched")
        }
        catch (e: IOException)
        {
            // Panic? IDK what might cause this.
            throw RuntimeException(e)
        }

        return chapter
    }

    public fun fetchPage(page: Page, behavior: Behavior = Behavior.LazyFetch): Page
    {
        try
        {
            if (behavior === FetcherSync.Behavior.LazyFetch && page.fullyParsed)
            {
                Log.d("FetchPage", "Already parsed. Ignoring")
                return page
            }
            init(page)
            val doc = fetchUrl(page.url)

            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            ScriptableObject.putProperty(scope, "page", Context.javaToJS(page, scope))
            val result = cx!!.evaluateString(scope, "fetchPage(doc, page);", "fetchProvider", 700, null);
            val p = Context.jsToJava(result, String::class.java) as String

            page.imageUrl = p;
            page.fullyParsed = true
            savePageImage(page)
            Log.d("FetchPage", "Done")
        }
        catch (e: IOException)
        {
            // Panic? IDK what might cause this.
            throw  RuntimeException(e);
        }

        return page
    }

    public fun fetchNew(provider: Provider): List<Uri>
    {
        init(provider)
        Log.d("FetchStarter", "Starting")
        val newChapters = ArrayList<Uri>()
        try
        {
            val doc = fetchUrl(provider.newUrl)
            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            val result = cx!!.evaluateString(scope, "fetchNew(doc);", "fetchProvider", 1000, null);
            val seriesChapterPairs = Context.jsToJava(result, List::class.java) as List<List<Any>>

            for (pair in seriesChapterPairs)
            {
                val jsSeries = pair.get(0) as JsSeries;
                val jsChapter = pair.get(1) as JsChapter;

                var series = jsSeries.unJS(provider.id);
                if (seriesExists(series.url))
                {
                    series = Series(resolver.query(Series.baseUri(), null, null, arrayOf(series.url), null))
                }
                else
                {
                    Log.d("Fetch", "Completely New Series!!")
                    val inserted = resolver.insert(Series.baseUri(), series.getContentValues())
                    series = Series(resolver.query(inserted, null, null, null, null))
                    fetchSeries(series)
                    continue
                }

                val chapter = jsChapter.unJS(series.id);
                if (!chapterExists(chapter.url))
                {
                    Log.d("Fetch", "Haven't seen this one.")
                    resolver.insert(Chapter.baseUri(), chapter.getContentValues())
                    if (series.favorite)
                    {
                        series.updated = true
                        resolver.update(series.uri(), series.getContentValues(), null, null)
                        newChapters.add(chapter.uri())
                    }
                }
            }
        }
        catch (e: IOException)
        {
            // Panic? IDK what might cause this.
            throw RuntimeException(e)
        }

        return newChapters
    }

    public fun fetchAllNew(): List<Uri>
    {
        val ret = ArrayList<Uri>()

        val providersCursor = resolver.query(Provider.all(), null, null, null, null)
        while (providersCursor.moveToNext())
        {
            val provider = Provider(providersCursor, false)
            ret.addAll(fetchNew(provider))
        }
        return ret
    }

    public fun fetchEntireChapter(chapter: Chapter, behavior: Behavior = FetcherSync.Behavior.LazyFetch): Chapter
    {
        fetchChapter(chapter, behavior)
        val pages = resolver.query(chapter.pages(), null, null, null, null)

        // Only send status updates between pages, not status for each page
        val localListener = listener
        listener = null

        var i = 1.0f
        while (pages.moveToNext())
        {
            Log.d("FetchAll", "page ${i / pages.count.toFloat()}")
            val page = Page(pages, false)
            fetchPage(page, behavior)

            localListener?.notify(i++ / pages.count.toFloat())
        }
        listener = localListener
        return chapter
    }

    public fun fetchEntireSeries(series: Series, behavior: Behavior = FetcherSync.Behavior.ForceRefresh): Series
    {
        fetchSeries(series, behavior)
        val chapters = resolver.query(series.chapters(), null, null, null, null)

        // Only send status updates between chapters, not status for each chapter
        val localListener = listener
        listener = null

        var i = 1.0f
        while (chapters.moveToNext())
        {
            Log.d("FetchAll", "chapter ${i / chapters.count.toFloat()}")
            val chapter = Chapter(chapters, false)
            fetchEntireChapter(chapter, behavior)

            localListener?.notify(i++ / chapters.count.toFloat())
        }
        listener = localListener
        return series
    }

    public fun fetchAllSeries()
    {
        val providersCursor = resolver.query(Provider.all(), null, null, null, null)
        var totalSeries = 0
        val providerSeriesList = ArrayList<Cursor>()

        val localListener = listener
        listener = null

        while (providersCursor.moveToNext())
        {
            val provider = Provider(providersCursor, false)
            fetchProvider(provider)
            val providerSeries = resolver.query(provider.series(), null, null, null, null)
            totalSeries += providerSeries.count
            providerSeriesList.add(providerSeries)
        }

        var i = 1.0f;
        providerSeriesList.forEach {
            while (it.moveToNext())
            {
                Log.d("All Series Fetch", "${i / totalSeries.toFloat()}")
                val series = Series(it, false)
                try
                {
                    fetchSeries(series)
                }
                catch (e: Exception)
                {
                    val reporter = ACRA.getErrorReporter()
                    reporter.handleSilentException(Exception("Stale Series? ${series.url}"))
                }
                localListener?.notify(i++ / totalSeries.toFloat())
            }
        }
        listener = localListener
    }

    @Throws(IOException::class)
    private fun fetchUrl(url: String): Document
    {
        val response = Jsoup.connect(url)
                            .timeout(10000)
                            .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                            .referrer("http://www.google.com").method(Connection.Method.GET)
                            .execute()
        return response.parse()
    }

    private fun providerExists(url: String): Boolean
    {
        val c = resolver.query(Provider.baseUri(), null, null, arrayOf(url), null)
        val ret = c.count > 0
        c.close()
        return ret
    }

    private fun seriesExists(url: String): Boolean
    {
        val c = resolver.query(Series.baseUri(), null, null, arrayOf(url), null)
        val ret = c.count > 0
        c.close()
        return ret
    }

    private fun chapterExists(url: String): Boolean
    {
        val c = resolver.query(Chapter.baseUri(), null, null, arrayOf(url), null)
        val ret = c.count > 0
        c.close()
        return ret
    }

    private fun pageExists(url: String): Boolean
    {
        val c = resolver.query(Page.baseUri(), null, null, arrayOf(url), null)
        val ret = c.count > 0
        c.close()
        return ret
    }

    private fun genreExists(name: String): Boolean
    {
        val c = resolver.query(Genre.baseUri(), null, null, arrayOf(name), null)
        val ret = c.count > 0
        c.close()
        return ret
    }

    private fun stripBadCharsForFile(file: String): String
    {
        return file.replace("[ \\\\/\\?%\\*:\\|\"<>]".toRegex(), ".")
    }

    private fun formatFloat(f: Float): String
    {
        if (Math.floor(f.toDouble()).toInt() == f.toInt())
        {
            return "${f.toInt()}"
        }
        else
        {
            return "${f}"
        }
    }

    private fun downloadImage(imageUrl: String, imagePath: String)
    {
        val inStream: InputStream
        var out: ByteArrayOutputStream? = null
        var count: Int
        try
        {
            val url = URL(imageUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.doInput = true
            conn.connect()
            inStream = conn.inputStream
            out = ByteArrayOutputStream()

            val length = conn.contentLength
            var done = 0
            val data = ByteArray(1024)
            while (done < length)
            {
                count = inStream.read(data)
                done += count

                listener?.notify(done / length.toFloat())
                out.write(data, 0, count)
            }

            // flushing output
            out.flush()

            val bmp = BitmapFactory.decodeByteArray(out.toByteArray(), 0, length)
            val fileOut = FileOutputStream(imagePath)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOut)
        }
        catch (e: MalformedURLException)
        {
            // TODO:: better error handling/checking
            throw RuntimeException(e)
        }
        catch (e: IOException)
        {
            // TODO:: better error handling/checking
            throw RuntimeException(e)
        }
        finally
        {
            try
            {
                out!!.close()
            }
            catch (e: IOException)
            {
                //TODO:: better error handling/checking
                throw RuntimeException(e)
            }
        }
    }

    // TODO:: not a huge fan of this method. Will probably want to future-proof it as much as possible.
    private fun savePageImage(p: Page)
    {
        val heritage = Heritage(resolver.query(p.heritage(), null, null, null, null))

        val root = Environment.getExternalStorageDirectory()
        val chapterPath = root.absolutePath + "/YAMR/" + stripBadCharsForFile(heritage.providerName) + "/" +
                stripBadCharsForFile(heritage.seriesName) + "/" + formatFloat(heritage.chapterNumber)

        val chapterDirectory = File(chapterPath)
        chapterDirectory.mkdirs()
        val pagePath = "$chapterDirectory/${formatFloat(heritage.pageNumber)}.png"

        downloadImage(p.imageUrl!!, pagePath)

        p.imagePath = pagePath
        resolver.update(p.uri(), p.getContentValues(), null, null) // Save the path off
    }

    private fun saveThumbnail(s: Series): String?
    {
        val p = Provider(resolver.query(Provider.uri(s.providerId), null, null, null, null))

        val root = Environment.getExternalStorageDirectory()
        val seriesPath = root.absolutePath + "/YAMR/" + stripBadCharsForFile(p.name) + "/" + stripBadCharsForFile(s.name)
        val chapterDirectory = File(seriesPath)
        chapterDirectory.mkdirs()
        val thumbPath = "$chapterDirectory/thumb.png"

        var out: FileOutputStream? = null
        try
        {
            val url = URL(s.thumbnailUrl)
            val bmp = BitmapFactory.decodeStream(url.openConnection().inputStream)
            out = FileOutputStream(thumbPath)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        catch (e: MalformedURLException)
        {
            // TODO:: better error handling/checking
            throw RuntimeException(e)
        }
        catch (e: IOException)
        {
            // Couldn't get it
            return null
        }
        finally
        {
            try
            {
                if (out != null)
                {
                    out.close()
                }
            }
            catch (e: IOException)
            {
                //TODO:: better error handling/checking
                throw RuntimeException(e)
            }

        }
        return thumbPath
    }
}
