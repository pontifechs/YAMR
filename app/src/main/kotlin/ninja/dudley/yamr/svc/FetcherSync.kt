package ninja.dudley.yamr.svc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.model.js.JsChapter
import ninja.dudley.yamr.model.js.JsPage
import ninja.dudley.yamr.model.js.JsSeries
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.ScriptableObject
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.ArrayList

/**
 * Created by mdudley on 5/19/15.
 */
public class FetcherSync(protected var context: android.content.Context)
{
    private val cx: Context
    private val scope: ScriptableObject
    init
    {
        cx = Context.enter()
        cx.setOptimizationLevel(-1)
        scope = cx.initStandardObjects()

        try
        {
            ScriptableObject.defineClass(scope, javaClass<JsSeries>())
            ScriptableObject.defineClass(scope, javaClass<JsChapter>())
            ScriptableObject.defineClass(scope, javaClass<JsPage>())
        }
        catch (e: Exception)
        {
            Log.e("RHINO", e.toString())
            // Gotta catch 'em all!
            throw RuntimeException(e)
        }

        // TODO:: Make generic, rather than just grab MangaPanda (id 1)
        val provider = Provider(context.getContentResolver().query(Provider.uri(1), null, null, null, null));

        cx.evaluateString(scope, provider.fetchProvider, "fetchProvider", 1, null);
        cx.evaluateString(scope, provider.stubSeries, "stubSeries", 1, null);
        cx.evaluateString(scope, provider.fetchSeries, "fetchSeries", 1, null);
        cx.evaluateString(scope, provider.fetchSeriesGenres, "fetchSeriesGenres", 1, null);
        cx.evaluateString(scope, provider.stubChapter, "stubChapter", 1, null);
        cx.evaluateString(scope, provider.fetchChapter, "fetchChapter", 1, null);
        cx.evaluateString(scope, provider.stubPage, "stubPage", 1, null);
        cx.evaluateString(scope, provider.fetchPage, "fetchPage", 1, null);
        cx.evaluateString(scope, provider.fetchNew, "fetchNew", 1, null);
    }

    public interface NotifyStatus
    {
        public fun notifyProviderStatus(status: Float)

        public fun notifySeriesStatus(status: Float)

        public fun notifyChapterStatus(status: Float)

        public fun notifyPageStatus(status: Float)
    }

    protected var listener: NotifyStatus? = null
    public fun register(listener: NotifyStatus)
    {
        this.listener = listener
    }

    public enum class FetchBehavior(private val value: String)
    {
        LazyFetch("LazyFetch"),
        ForceRefresh("ForceRefresh");

        override fun toString(): String
        {
            return value
        }
    }

    public fun fetchProvider(provider: Provider, behavior: FetchBehavior = FetchBehavior.LazyFetch): Provider
    {
        if (behavior === FetcherSync.FetchBehavior.LazyFetch && provider.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return provider
        }

        Log.d("Fetch", "Starting a Provider fetch")
        try
        {
            val doc = fetchUrl(provider.url)
            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            ScriptableObject.putProperty(scope, "provider", Context.javaToJS(provider, scope))
            val result = cx.evaluateString(scope, "fetchProvider(doc, provider);", "fetchProvider", 100, null);
            val elements = Context.jsToJava(result, javaClass<Elements>()) as Elements
            Log.d("RHINO", "${elements.size()}")
            provider.fullyParsed = true
            context.getContentResolver().update(provider.uri(), provider.getContentValues(), null, null)
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
                    listener?.notifyProviderStatus(progress)
                }
                ScriptableObject.putProperty(scope, "element", Context.javaToJS(e, scope))
                val seriesResult = cx.evaluateString(scope, "stubSeries(element);", "fetchProvider", 200, null);
                val jsSeries = Context.jsToJava(seriesResult, javaClass<JsSeries>()) as JsSeries
                val s = jsSeries.unJS(provider.id)

                if (seriesExists(s.url))
                {
                    continue
                }
                context.getContentResolver().insert(Series.baseUri(), s.getContentValues())
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

    public fun fetchSeries(series: Series, behavior: FetchBehavior = FetchBehavior.LazyFetch): Series
    {
        if (behavior === FetcherSync.FetchBehavior.LazyFetch && series.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return series
        }

        Log.d("Fetch", "Starting Series fetch")
        try
        {
            val doc = fetchUrl(series.url)
            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            ScriptableObject.putProperty(scope, "series", Context.javaToJS(series, scope))

            val genreResult = cx.evaluateString(scope, "fetchSeriesGenres(doc, series);", "fetchProvider", 100, null);
            val genres = Context.jsToJava(genreResult, javaClass<List<String>>()) as List<String>

            for (genre in genres)
            {
                if (!genreExists(genre))
                {
                    val g = Genre(genre)
                    context.getContentResolver().insert(Genre.baseUri(), g.getContentValues())
                }
                val g = Genre(context.getContentResolver().query(Genre.baseUri(), null, null, arrayOf(genre), null))

                // Now that we have the genre for sure, add the relation.
                context.getContentResolver().insert(Genre.relator(), Genre.SeriesGenreRelator(series.id, g.id))
            }

            val result = cx.evaluateString(scope, "fetchSeries(doc, series);", "fetchProvider", 300, null);
            val elements = Context.jsToJava(result, javaClass<Elements>()) as Elements

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
                    listener?.notifySeriesStatus(progress)
                }
                ScriptableObject.putProperty(scope, "element", Context.javaToJS(e, scope))
                val chapterResult = cx.evaluateString(scope, "stubChapter(element);", "fetchProvider", 400, null);
                val jsChapter = Context.jsToJava(chapterResult, javaClass<JsChapter>()) as JsChapter
                val chapter = jsChapter.unJS(series.id)

                if (chapterExists(chapter.url))
                {
                    continue
                }
                context.getContentResolver().insert(Chapter.baseUri(), chapter.getContentValues())
            }
            series.fullyParsed = true
            series.thumbnailPath = saveThumbnail(series)
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

    public fun fetchChapter(chapter: Chapter, behavior: FetchBehavior = FetchBehavior.LazyFetch): Chapter
    {
        if (behavior === FetcherSync.FetchBehavior.LazyFetch && chapter.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return chapter
        }

        try
        {
            val doc = fetchUrl(chapter.url)
            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            ScriptableObject.putProperty(scope, "chapter", Context.javaToJS(chapter, scope))
            val result = cx.evaluateString(scope, "fetchChapter(doc, chapter);", "fetchProvider", 500, null);
            val elements = Context.jsToJava(result, javaClass<Elements>()) as Elements

            val statusStride = Math.ceil((elements.size() / 100.0f).toDouble()).toInt()
            var index = 0
            for (e in elements)
            {
                index++
                if (index % statusStride == 0 && listener != null)
                {
                    val progress = index / elements.size().toFloat()
                    Log.d("Fetch", "Chapter fetch progress: ${progress}")
                    listener?.notifyChapterStatus(progress)
                }

                ScriptableObject.putProperty(scope, "element", Context.javaToJS(e, scope))
                val pageResult = cx.evaluateString(scope, "stubPage(element);", "fetchProvider", 600, null);
                val jsPage = Context.jsToJava(pageResult, javaClass<JsPage>()) as JsPage
                val page = jsPage.unJS(chapter.id)

                if (pageExists(page.url))
                {
                    continue
                }

                context.getContentResolver().insert(Page.baseUri(), page.getContentValues())
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

    public fun fetchPage(page: Page, behavior: FetchBehavior = FetchBehavior.LazyFetch): Page
    {
        try
        {
            if (behavior === FetcherSync.FetchBehavior.LazyFetch && page.fullyParsed)
            {
                Log.d("FetchPage", "Already parsed. Ignoring")
                return page
            }
            val doc = fetchUrl(page.url)

            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            ScriptableObject.putProperty(scope, "page", Context.javaToJS(page, scope))
            val result = cx.evaluateString(scope, "fetchPage(doc, page);", "fetchProvider", 700, null);
            val p = Context.jsToJava(result, javaClass<String>()) as String

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


        Log.d("FetchStarter", "Starting")
        val newChapters = ArrayList<Uri>()
        try
        {
            val doc = fetchUrl(provider.newUrl)
            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            val result = cx.evaluateString(scope, "fetchNew(doc);", "fetchProvider", 1000, null);
            val seriesChapterPairs = Context.jsToJava(result, javaClass<List<List<Object>>>()) as List<List<Object>>

            for (pair in seriesChapterPairs)
            {
                val jsSeries = pair.get(0) as JsSeries;
                val jsChapter = pair.get(1) as JsChapter;

                var series = jsSeries.unJS(provider.id);
                if (seriesExists(series.url))
                {
                    series = Series(context.getContentResolver().query(Series.baseUri(), null, null, arrayOf(series.url), null))
                }
                else
                {
                    Log.d("Fetch", "Completely New Series!!")
                    val inserted = context.getContentResolver().insert(Series.baseUri(), series.getContentValues())
                    series = Series(context.getContentResolver().query(inserted, null, null, null, null))
                    fetchSeries(series)
                    continue
                }

                val chapter = jsChapter.unJS(series.id);
                if (!chapterExists(chapter.url))
                {
                    Log.d("Fetch", "Haven't seen this one.")
                    context.getContentResolver().insert(Chapter.baseUri(), chapter.getContentValues())
                    if (series.favorite)
                    {
                        series.updated = true
                        context.getContentResolver().update(series.uri(), series.getContentValues(), null, null)
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

    throws(IOException::class)
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
        val c = context.getContentResolver().query(Provider.baseUri(), null, null, arrayOf(url), null)
        val ret = c.getCount() > 0
        c.close()
        return ret
    }

    private fun seriesExists(url: String): Boolean
    {
        val c = context.getContentResolver().query(Series.baseUri(), null, null, arrayOf(url), null)
        val ret = c.getCount() > 0
        c.close()
        return ret
    }

    private fun chapterExists(url: String): Boolean
    {
        val c = context.getContentResolver().query(Chapter.baseUri(), null, null, arrayOf(url), null)
        val ret = c.getCount() > 0
        c.close()
        return ret
    }

    private fun pageExists(url: String): Boolean
    {
        val c = context.getContentResolver().query(Page.baseUri(), null, null, arrayOf(url), null)
        val ret = c.getCount() > 0
        c.close()
        return ret
    }

    private fun genreExists(name: String): Boolean
    {
        val c = context.getContentResolver().query(Genre.baseUri(), null, null, arrayOf(name), null)
        val ret = c.getCount() > 0
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
        var count: Int = 0
        try
        {
            val url = URL(imageUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.setDoInput(true)
            conn.connect()
            inStream = conn.getInputStream()
            out = ByteArrayOutputStream()

            val length = conn.getContentLength()
            var done = 0
            val data = ByteArray(1024)
            while (done < length)
            {
                count = inStream.read(data)
                done += count

                listener?.notifyPageStatus(done / length.toFloat())
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
        val heritage = Heritage(context.getContentResolver().query(p.heritage(), null, null, null, null))

        val root = Environment.getExternalStorageDirectory()
        val chapterPath = root.getAbsolutePath() + "/." + stripBadCharsForFile(heritage.providerName) + "/" +
                stripBadCharsForFile(heritage.seriesName) + "/" + formatFloat(heritage.chapterNumber)

        val chapterDirectory = File(chapterPath)
        chapterDirectory.mkdirs()
        val pagePath = "${chapterDirectory}/${formatFloat(heritage.pageNumber)}.png"

        downloadImage(p.imageUrl!!, pagePath)

        p.imagePath = pagePath
        context.getContentResolver().update(p.uri(), p.getContentValues(), null, null) // Save the path off
    }

    private fun saveThumbnail(s: Series): String?
    {
        val p = Provider(context.getContentResolver().query(Provider.uri(s.providerId), null, null, null, null))

        val root = Environment.getExternalStorageDirectory()
        val seriesPath = root.getAbsolutePath() + "/" + stripBadCharsForFile(p.name!!) + "/" + stripBadCharsForFile(s.name!!)
        val chapterDirectory = File(seriesPath)
        chapterDirectory.mkdirs()
        val thumbPath = "${chapterDirectory}/thumb.png"

        var out: FileOutputStream? = null
        try
        {
            val url = URL(s.thumbnailUrl)
            val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
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
