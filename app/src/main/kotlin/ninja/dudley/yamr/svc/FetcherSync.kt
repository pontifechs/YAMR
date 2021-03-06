package ninja.dudley.yamr.svc

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import ninja.dudley.yamr.BuildConfig
import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.ui.activities.Settings
import org.acra.ACRA
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList

/**
* Created by mdudley on 5/19/15. Yup.
*/
abstract class FetcherSync
{
    private var androidContext: android.content.Context? = null
    private val resolver: ContentResolver

    constructor(context: android.content.Context)
    {
        this.androidContext = context
        this.resolver = context.contentResolver
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

    abstract fun enumerateSeries(): List<Series>

    abstract fun fillSeries(series: Series): Series

    abstract fun enumerateGenres(series: Series): List<Genre>

    abstract fun enumerateChapters(series: Series): List<Chapter>

    abstract fun fillChapter(chapter: Chapter): Chapter

    abstract fun enumeratePages(chapter: Chapter): List<Page>

    abstract fun fillPage(page: Page): Page

    abstract fun enumerateNew(): List<Pair<Series, Chapter>>

    fun fetchProvider(provider: Provider, behavior: Behavior = Behavior.LazyFetch): Provider
    {
        if (behavior === FetcherSync.Behavior.LazyFetch && provider.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return provider
        }

        Log.d("Fetch", "Starting a Provider fetch")

        val seriesStubs = enumerateSeries()

        val statusStride = Math.ceil((seriesStubs.size / 100.0f).toDouble()).toInt()
        var index = 0
        for (series in seriesStubs)
        {
            index++
            if (index % statusStride == 0 && listener != null)
            {
                val progress = index / seriesStubs.size.toFloat()
                Log.d("Fetch", "ProviderFetch progress: $progress")
                listener?.notify(progress)
            }

            if (seriesExists(series.url))
            {
                continue
            }
            resolver.insert(Series.baseUri(), series.getContentValues())
        }

        Log.d("Fetch", "Iteration complete. Provider Fetched.")
        provider.fullyParsed = true
        resolver.update(provider.uri(), provider.getContentValues(), null, null)

        return provider
    }

    fun fetchSeries(series: Series, behavior: Behavior = Behavior.LazyFetch): Series
    {
        if (behavior === FetcherSync.Behavior.LazyFetch && series.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return series
        }

        Log.d("Fetch", "Starting Series fetch: ${series.id}: ${series.url}")
        val fullSeries = fillSeries(series)
        val genres = enumerateGenres(fullSeries)

        // Delete all existing relations (Only really relevant with a refresh.)
        resolver.delete(Series.genres(fullSeries.id), null, null)

        for (genre in genres)
        {
            if (!genreExists(genre.name))
            {
                resolver.insert(Genre.baseUri(), genre.getContentValues())
            }
            val g = Genre(resolver.query(Genre.baseUri(), null, null, arrayOf(genre.name), null))

            // Now that we have the genre for sure, add the relation.
            resolver.insert(Genre.relator(), Genre.SeriesGenreRelator(series.id, g.id))
        }

        val chapterStubs = enumerateChapters(fullSeries)

        // Parse chapters
        val statusStride = Math.ceil((chapterStubs.size / 100.0f).toDouble()).toInt()
        var index = 0
        for (chapter in chapterStubs)
        {
            index++
            if (index % statusStride == 0 && listener != null)
            {
                val progress = index / chapterStubs.size.toFloat()
                Log.d("Fetch", "SeriesFetch progress: $progress")
                listener?.notify(progress)
            }

            if (chapterExists(chapter.url))
            {
                continue
            }
            resolver.insert(Chapter.baseUri(), chapter.getContentValues())
        }
        fullSeries.fullyParsed = true
        fullSeries.thumbnailPath = saveThumbnail(fullSeries)
        resolver.update(fullSeries.uri(), fullSeries.getContentValues(), null, null)
        Log.d("Fetch", "Iteration complete. Series Fetched.")

        return fullSeries
    }

    fun fetchChapter(chapter: Chapter, behavior: Behavior = Behavior.LazyFetch): Chapter
    {
        if (behavior === FetcherSync.Behavior.LazyFetch && chapter.fullyParsed)
        {
            Log.d("Fetch", "Already parsed. Ignoring")
            return chapter
        }

        val fullChapter = fillChapter(chapter)
        val pageStubs = enumeratePages(fullChapter)

        val statusStride = Math.ceil((pageStubs.size / 100.0f).toDouble()).toInt()
        var index = 0
        for (page in pageStubs)
        {
            index++
            if (index % statusStride == 0 && listener != null)
            {
                val progress = index / pageStubs.size.toFloat()
                Log.d("Fetch", "Chapter fetch progress: $progress")
                listener?.notify(progress)
            }

            if (pageExists(page.url))
            {
                continue
            }

            resolver.insert(Page.baseUri(), page.getContentValues())
        }
        fullChapter.fullyParsed = true
        resolver.update(fullChapter.uri(), fullChapter.getContentValues(), null, null)
        Log.d("Fetch", "Iteration complete. Chapter fetched")

        return fullChapter
    }

    fun fetchPage(page: Page, behavior: Behavior = Behavior.LazyFetch): Page
    {
        if (behavior === FetcherSync.Behavior.LazyFetch && page.fullyParsed && (page.imagePath != null))
        {
            Log.d("FetchPage", "Already parsed. Ignoring")
            return page
        }

        val fullPage = fillPage(page)
        fullPage.fullyParsed = true
        fullPage.imagePath = savePageImage(fullPage)
        Log.d("FetchPage", "Done")
        resolver.update(fullPage.uri(), fullPage.getContentValues(), null, null)
        return fullPage
    }

    fun fetchNew(): List<Uri>
    {
        Log.d("FetchStarter", "Starting")
        val newChapters = ArrayList<Uri>()

        val seriesChapterPairs = enumerateNew()

        for (pair in seriesChapterPairs)
        {
            var series = pair.first
            val chapter = pair.second

            if (seriesExists(series.url))
            {
                series = Series(resolver.query(Series.baseUri(), null, null, arrayOf(series.url), null))
            }
            else
            {
                // We really only care about series we have favorited. And if it doesn't exists, it can't be favorited.
                continue
            }

            if (!chapterExists(chapter.url))
            {
                Log.d("Fetch", "Haven't seen this one.")
                val chapterWithSeriesId = Chapter(series.id, chapter.url, chapter.number)
                val newChapterUri = resolver.insert(Chapter.baseUri(), chapterWithSeriesId.getContentValues())
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
        localListener?.notify(0.0f)
        while (pages.moveToNext())
        {
            Log.d("FetchAll", "page $i / ${pages.count}")
            val page = Page(pages, false)
            fetchPage(page, behavior)

            localListener?.notify(i++ / pages.count.toFloat())
        }
        pages.close()
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
        chapters.close()
        listener = localListener
        return series
    }

    private fun seriesPageComplete(series: Series): Boolean
    {
        val c = resolver.query(series.pageComplete(), null, null, null, null)
        c.moveToFirst()
        val totalIndex = c.getColumnIndex(DBHelper.SeriesPageCompleteViewEntry.COLUMN_TOTAL)
        val fetchedIndex = c.getColumnIndex(DBHelper.SeriesPageCompleteViewEntry.COLUMN_FETCHED)
        val total = c.getInt(totalIndex)
        val fetched = c.getInt(fetchedIndex)
        c.close()
        return total != 0 && total == fetched
    }

    private fun chapterPageComplete(chapter: Chapter): Boolean
    {
        val c = resolver.query(chapter.pageComplete(), null, null, null, null)
        c.moveToFirst()
        val totalIndex = c.getColumnIndex(DBHelper.ChapterPageCompleteViewEntry.COLUMN_TOTAL)
        val fetchedIndex = c.getColumnIndex(DBHelper.ChapterPageCompleteViewEntry.COLUMN_FETCHED)
        val total = c.getInt(totalIndex)
        val fetched = c.getInt(fetchedIndex)
        c.close()
        return total != 0 && total == fetched
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

        if (conn.responseCode != 200)
        {
            return false
        }
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
        return FileOutputStream(pagePath)
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
                file.delete()
                return null
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

    companion object
    {

        fun fetchUrl(url: String): Document
        {
            val response = Jsoup.connect(url)
                    .maxBodySize(0)    //// KEKEKEKEKEKEK. MangaHere's List page is over 1MB
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com").method(Connection.Method.GET)
                    .execute()
            return response.parse()
        }

        private fun resize(path: String)
        {
            var bitmap = BitmapFactory.decodeFile(path)
            if (bitmap == null || bitmap.width < 8192 && bitmap.height < 8192)
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
