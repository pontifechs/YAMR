package ninja.dudley.yamr.svc

import android.content.ContentResolver
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import ninja.dudley.yamr.BuildConfig
import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.model.js.JsChapter
import ninja.dudley.yamr.model.js.JsPage
import ninja.dudley.yamr.model.js.JsSeries
import ninja.dudley.yamr.ui.activities.Settings
import org.acra.ACRA
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList

/**
* Created by mdudley on 5/19/15. Yup.
*/
open class FetcherSync
{
    private var cx: Context? = null
    private var androidContext: android.content.Context? = null
    private var scope: ScriptableObject? = null
    private val resolver: ContentResolver
    private var provider: Provider? = null

    constructor(context: android.content.Context)
    {
        this.androidContext = context
        this.resolver = context.contentResolver
    }

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

    interface NotifyStatus
    {
        fun notify(status: Float): Boolean
    }

    protected var listener: NotifyStatus? = null
    fun register(listener: NotifyStatus)
    {
        this.listener = listener
    }

    enum class Behavior
    {
        LazyFetch,
        ForceRefresh;
    }

    fun fetchProvider(provider: Provider, behavior: Behavior = Behavior.LazyFetch): Provider
    {
        if (behavior === FetcherSync.Behavior.LazyFetch && provider.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return provider
        }

        init(provider)
        Log.d("Fetch", "Starting a Provider fetch")

        val doc = fetchUrl(provider.url)
        ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
        ScriptableObject.putProperty(scope, "provider", Context.javaToJS(provider, scope))
        val result = cx!!.evaluateString(scope, "fetchProvider(doc, provider);", "fetchProvider", 100, null);
        val elements = Context.jsToJava(result, Elements::class.java) as Elements
        Log.d("RHINO", "${elements.size}")
        provider.fullyParsed = true
        resolver.update(provider.uri(), provider.getContentValues(), null, null)
        Log.d("Fetch", "Iteration complete. Provider Fetched.")

        val statusStride = Math.ceil((elements.size / 100.0f).toDouble()).toInt()
        var index = 0
        for (e in elements)
        {
            index++
            if (index % statusStride == 0 && listener != null)
            {
                val progress = index / elements.size.toFloat()
                Log.d("Fetch", "ProviderFetch progress: $progress")
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

        return provider
    }

    fun fetchSeries(series: Series, behavior: Behavior = Behavior.LazyFetch): Series
    {
        if (behavior === FetcherSync.Behavior.LazyFetch && series.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return series
        }

        init(series)
        Log.d("Fetch", "Starting Series fetch: ${series.id}: ${series.url}")
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
        val statusStride = Math.ceil((elements.size / 100.0f).toDouble()).toInt()
        var index = 0
        for (e in elements)
        {
            index++
            if (index % statusStride == 0 && listener != null)
            {
                val progress = index / elements.size.toFloat()
                Log.d("Fetch", "SeriesFetch progress: $progress")
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

        return series
    }

    fun fetchChapter(chapter: Chapter, behavior: Behavior = Behavior.LazyFetch): Chapter
    {
        if (behavior === FetcherSync.Behavior.LazyFetch && chapter.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return chapter
        }

        init(chapter)
        val doc = fetchUrl(chapter.url)
        ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
        ScriptableObject.putProperty(scope, "chapter", Context.javaToJS(chapter, scope))
        val result = cx!!.evaluateString(scope, "fetchChapter(doc, chapter);", "fetchProvider", 500, null);
        val elements = Context.jsToJava(result, Elements::class.java) as Elements

        val statusStride = Math.ceil((elements.size / 100.0f).toDouble()).toInt()
        var index = 0
        for (e in elements)
        {
            index++
            if (index % statusStride == 0 && listener != null)
            {
                val progress = index / elements.size.toFloat()
                Log.d("Fetch", "Chapter fetch progress: $progress")
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

        return chapter
    }

    fun fetchPage(page: Page, behavior: Behavior = Behavior.LazyFetch): Page
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
        page.imagePath = savePageImage(page)
        Log.d("FetchPage", "Done")
        resolver.update(page.uri(), page.getContentValues(), null, null)
        return page
    }

    fun fetchNew(provider: Provider): List<Uri>
    {
        init(provider)
        Log.d("FetchStarter", "Starting")
        val newChapters = ArrayList<Uri>()
        val doc = fetchUrl(provider.newUrl)
        ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
        val result = cx!!.evaluateString(scope, "fetchNew(doc);", "fetchProvider", 1000, null);
        val seriesChapterPairs = Context.jsToJava(result, List::class.java) as List<List<Any>>

        for (pair in seriesChapterPairs)
        {
            val jsSeries = pair[0] as JsSeries;
            val jsChapter = pair[1] as JsChapter;

            var series = jsSeries.unJS(provider.id) ;
            if (seriesExists(series.url))
            {
                series = Series(resolver.query(Series.baseUri(), null, null, arrayOf(series.url), null))
            }
            else
            {
                Log.d("Fetch", "Completely New Series!!")
                val inserted = resolver.insert(Series.baseUri(), series.getContentValues())
                series = Series(resolver.query(inserted, null, null, null, null))
                fetchSeries(series)  // FYI, this will always make the newly fetched chapter exist already.
            }

            val chapter = jsChapter.unJS(series.id);
            if (!chapterExists(chapter.url))
            {
                Log.d("Fetch", "Haven't seen this one.")
                val newChapterUri = resolver.insert(Chapter.baseUri(), chapter.getContentValues())
                if (series.favorite)
                {
                    series.updated = true
                    resolver.update(series.uri(), series.getContentValues(), null, null)
                    newChapters.add(newChapterUri)
                }
            }
        }

        return newChapters
    }

    fun fetchAllNew(): List<Uri>
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

    fun fetchEntireChapter(chapter: Chapter, behavior: Behavior = FetcherSync.Behavior.LazyFetch): Chapter
    {
        fetchChapter(chapter, behavior)
        if (chapterPageComplete(chapter) && behavior != FetcherSync.Behavior.ForceRefresh)
        {
            Log.d("FetchEntire", "${chapter.url} is Page Complete. Skipping ")
            return chapter
        }
        val pages = resolver.query(chapter.pages(), null, null, null, null)

        // Only send status updates between pages, not status for each page
        val localListener = listener
        listener = null

        var i = 1.0f
        localListener?.notify(0.0f);
        while (pages.moveToNext())
        {
            Log.d("FetchAll", "page $i / ${pages.count}")
            val page = Page(pages, false)
            fetchPage(page, behavior)

            localListener?.notify(i++ / pages.count.toFloat())
        }
        listener = localListener
        return chapter
    }

    fun fetchEntireSeries(series: Series, behavior: Behavior = FetcherSync.Behavior.LazyFetch): Series
    {
        fetchSeries(series, behavior)
        if (seriesPageComplete(series) && behavior != FetcherSync.Behavior.ForceRefresh)
        {
            Log.d("FetchEntire", "${series.url} is Page Complete. Skipping")
            return series
        }
        val chapters = resolver.query(series.chapters(), null, null, null, null)

        // Only send status updates between chapters, not status for each chapter
        val localListener = listener
        listener = null

        var i = 1.0f
        localListener?.notify(0.0f)
        while (chapters.moveToNext())
        {
            Log.d("FetchAll", "chapter $i / ${chapters.count}")
            val chapter = Chapter(chapters, false)
            fetchEntireChapter(chapter, behavior)

            localListener?.notify(i++ / chapters.count.toFloat())
        }
        listener = localListener
        return series
    }

    fun fetchAllSeries()
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
                    if (!BuildConfig.DEBUG)
                    {
                        val reporter = ACRA.getErrorReporter()
                        reporter.handleSilentException(Exception("Stale Series? ${series.url}"))
                    }
                }
                localListener?.notify(i++ / totalSeries.toFloat())
            }
        }
        listener = localListener
    }

    private fun fetchUrl(url: String): Document
    {
        val response = Jsoup.connect(url)
                            .maxBodySize(0)    //// KEKEKEKEKEKEK. MangaHere's List page is over 1MB
                            .timeout(10000)
                            .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                            .referrer("http://www.google.com").method(Connection.Method.GET)
                            .execute()
        return response.parse()
    }

    private fun seriesPageComplete(series: Series): Boolean
    {
        val c = resolver.query(series.pageComplete(), null, null, null, null)
        c.moveToFirst()
        val totalIndex = c.getColumnIndex(DBHelper.SeriesPageCompleteViewEntry.COLUMN_TOTAL);
        val fetchedIndex = c.getColumnIndex(DBHelper.SeriesPageCompleteViewEntry.COLUMN_FETCHED);
        val total = c.getInt(totalIndex);
        val fetched = c.getInt(fetchedIndex);
        c.close()
        return total != 0 && total == fetched;
    }

    private fun chapterPageComplete(chapter: Chapter): Boolean
    {
        val c = resolver.query(chapter.pageComplete(), null, null, null, null)
        c.moveToFirst()
        val totalIndex = c.getColumnIndex(DBHelper.ChapterPageCompleteViewEntry.COLUMN_TOTAL);
        val fetchedIndex = c.getColumnIndex(DBHelper.ChapterPageCompleteViewEntry.COLUMN_FETCHED);
        val total = c.getInt(totalIndex);
        val fetched = c.getInt(fetchedIndex);
        c.close()
        return total != 0 && total == fetched;
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
            return "$f"
        }
    }

    private fun downloadImage(imageUrl: String, outputStream: FileOutputStream): Boolean
    {
        val inStream: InputStream
        var count: Int
        val url = URL(imageUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 10000
        conn.connect()
        try
        {
            inStream = conn.inputStream
        }
        catch (fnfe: FileNotFoundException)
        {
            return false
        }

        val length = conn.contentLength
        var done = 0
        val data = ByteArray(1024)
        while (done < length)
        {
            count = inStream.read(data)
            done += count

            listener?.notify(done / length.toFloat())
            outputStream.write(data, 0, count)
        }
        outputStream.flush()
        return true
    }

    private fun getRootDirectory(): String
    {
        if (Settings.useExternalStorage(androidContext!!))
        {
            return Environment.getExternalStorageDirectory().absolutePath
        }
        else
        {
            return androidContext!!.filesDir.absolutePath
        }
    }

    private fun buildChapterPath(p: Page): String
    {
        val heritage = Heritage(resolver.query(p.heritage(), null, null, null, null))

        return getRootDirectory() + "/YAMR/" + stripBadCharsForFile(heritage.providerName) + "/" +
                stripBadCharsForFile(heritage.seriesName) + "/" + formatFloat(heritage.chapterNumber)
    }

    private fun buildPagePath(p: Page): String
    {
        val extension = p.imageUrl!!.substring(p.imageUrl!!.lastIndexOf("."))
        return buildChapterPath(p) + "/" + formatFloat(p.number) + extension
    }

    private fun getPageOutputStream(p: Page): FileOutputStream
    {
        val chapterPath = buildChapterPath(p)
        val pagePath = buildPagePath(p)
        val chapterDirectory = File(chapterPath)
        chapterDirectory.mkdirs()
        return FileOutputStream(pagePath);
    }

    private fun savePageImage(p: Page): String?
    {
        if (downloadImage(p.imageUrl!!, getPageOutputStream(p)))
        {
            val path = buildPagePath(p)
            // Check for 0B files
            val file = File(path)
            if (file.length() == 0L)
            {
                file.delete();
                return null;
            }
            resize(path)
            return path
        }
        else
        {
            return null
        }
    }

    private fun saveThumbnail(s: Series): String?
    {
        val p = Provider(resolver.query(Provider.uri(s.providerId), null, null, null, null))

        val seriesPath = getRootDirectory() + "/YAMR/" + stripBadCharsForFile(p.name) + "/" + stripBadCharsForFile(s.name)
        val chapterDirectory = File(seriesPath)
        chapterDirectory.mkdirs()
        val thumbPath = "$chapterDirectory/thumb.png"
        val stream = FileOutputStream(thumbPath)

        if (downloadImage(s.thumbnailUrl!!, stream))
        {
            return thumbPath
        }
        else
        {
            return null
        }
    }

    private companion object
    {

        private fun resize(path: String)
        {
            var bitmap = BitmapFactory.decodeFile(path)
            if (bitmap.width < 8192 && bitmap.height < 8192)
            {
                return
            }
            bitmap = clampToSize(bitmap, 8192, 8192)
            val out = FileOutputStream(path)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.close()
        }

        private fun clampToSize(image: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap
        {
            if (image.width <= maxWidth && image.height <= maxHeight)
            {
                return image
            }

            // Figure out how far out of bounds we are.
            val widthOver = image.width - maxWidth
            val heightOver = image.height - maxHeight

            val currentAspectRatio = image.width.toFloat() / image.height.toFloat()
            // Need to scale in width
            if (widthOver > heightOver)
            {
                val newWidth = maxWidth
                val newHeight = (maxWidth * currentAspectRatio).toInt()
                return Bitmap.createScaledBitmap(image, newWidth, newHeight, true)
            }

            // Need to scale in height
            if (heightOver > widthOver)
            {
                val newWidth = (maxHeight * currentAspectRatio).toInt()
                val newHeight = maxHeight
                return Bitmap.createScaledBitmap(image, newWidth, newHeight, true)
            }

            throw AssertionError("Should Never Happen")
        }
    }
}
