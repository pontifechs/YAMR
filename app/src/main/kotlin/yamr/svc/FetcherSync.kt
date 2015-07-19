package ninja.dudley.yamr.svc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.model.*
import yamr.model.Heritage
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * Created by mdudley on 5/19/15.
 */
public abstract class FetcherSync(protected var context: Context)
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

    public fun fetchProvider(provider: Provider): Provider
    {
        return fetchProvider(provider, FetchBehavior.LazyFetch)
    }

    public abstract fun fetchProvider(provider: Provider, behavior: FetchBehavior = FetchBehavior.LazyFetch): Provider

    public abstract fun fetchSeries(series: Series, behavior: FetchBehavior = FetchBehavior.LazyFetch): Series

    public abstract fun fetchChapter(chapter: Chapter, behavior: FetchBehavior = FetchBehavior.LazyFetch): Chapter

    public abstract fun fetchPage(page: Page, behavior: FetchBehavior = FetchBehavior.LazyFetch): Page

    public abstract fun fetchNew(provider: Provider): List<Uri>

    protected fun providerExists(url: String): Boolean
    {
        val c = context.getContentResolver().query(Provider.baseUri(), null, null, arrayOf(url), null)
        val ret = c.getCount() > 0
        c.close()
        return ret
    }

    protected fun seriesExists(url: String): Boolean
    {
        val c = context.getContentResolver().query(Series.baseUri(), null, null, arrayOf(url), null)
        val ret = c.getCount() > 0
        c.close()
        return ret
    }

    protected fun chapterExists(url: String): Boolean
    {
        val c = context.getContentResolver().query(Chapter.baseUri(), null, null, arrayOf(url), null)
        val ret = c.getCount() > 0
        c.close()
        return ret
    }

    protected fun pageExists(url: String): Boolean
    {
        val c = context.getContentResolver().query(Page.baseUri(), null, null, arrayOf(url), null)
        val ret = c.getCount() > 0
        c.close()
        return ret
    }

    protected fun genreExists(name: String): Boolean
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
    protected fun savePageImage(p: Page)
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

    protected fun saveThumbnail(s: Series): String?
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
