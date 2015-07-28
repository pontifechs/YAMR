package ninja.dudley.yamr.svc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.model.js.JsPage
import ninja.dudley.yamr.model.js.JsSeries
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
public class FetcherSync(protected var context: android.content.Context)
{

    public interface NotifyStatus
    {
        public fun notifyProviderStatus(status: Float)

        public fun notifySeriesStatus(status: Float)

        public fun notifyChapterStatus(status: Float)

        public fun notifyPageStatus(status: Float)

        public fun notifyNewStatus(status: Float)
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

        val cx = Context.enter()
        cx.setOptimizationLevel(-1)
        val scope = cx.initStandardObjects()

        try
        {
            ScriptableObject.defineClass(scope, javaClass<JsSeries>())
        }
        catch (e: Exception)
        {
            Log.e("RHINO", e.toString())
            // Gotta catch 'em all!
            throw RuntimeException(e)
        }
        val fillProvider =
                "function fetchProvider(doc, provider) { " +
                        "   return doc.select('.series_alpha li a[href]');" +
                        "};"
        val stubSeries =
                "function stubSeries(element) {" +
                        "   var url = element.absUrl('href');" +
                        "   var name = element.ownText();" +
                        "   var jsSeries = new JsSeries();" +
                        "   jsSeries.url = url;" +
                        "   jsSeries.name = name;" +
                        "   return jsSeries" +
                        "};"
        cx.evaluateString(scope, fillProvider, "fetchProvider", 1, null);
        cx.evaluateString(scope, stubSeries, "fetchProvider", 2, null);

        Log.d("Fetch", "Starting a Provider fetch")
        try
        {

            val doc = fetchUrl(provider.url)
            ScriptableObject.putProperty(scope, "doc", Context.javaToJS(doc, scope))
            ScriptableObject.putProperty(scope, "provider", Context.javaToJS(provider, scope))
            val result = cx.evaluateString(scope, "fetchProvider(doc, provider);", "fetchProvider", 3, null);
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
                    Log.d("Fetch", "ProviderFetch progress: " + progress)
                    listener?.notifyProviderStatus(progress)
                }
                ScriptableObject.putProperty(scope, "element", Context.javaToJS(e, scope))
                val seriesResult = cx.evaluateString(scope, "stubSeries(element);", "fetchProvider", 4, null);
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

    public fun fetchChapter(chapter: Chapter, behavior: FetchBehavior = FetchBehavior.LazyFetch): Chapter
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

            val cx = org.mozilla.javascript.Context.enter()
            cx.setOptimizationLevel(-1)
            val scope = cx.initStandardObjects()

            try
            {
                ScriptableObject.defineClass(scope, javaClass<JsPage>())
            }
            catch (e: Exception)
            {
                Log.e("RHINO", e.toString());
                // Gotta catch 'em all!
            }

            val script =
            "function parse(doc, page) { " +
                    "   var element = doc.select('img[src]').first();" +
                    "   page.imageUrl = element.absUrl('src');" +
                    "};";
            try
            {
                val fct = cx.compileFunction(scope, script, "script", 1, null);
                fct.call(cx, scope, scope, arrayOf(doc, page))
            }
            finally
            {
                org.mozilla.javascript.Context.exit();
            }
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


    throws(IOException::class)
    private fun fetchUrl(url: String): Document
    {
        val response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0").referrer("http://www.google.com").method(Connection.Method.GET).execute()
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
