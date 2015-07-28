package ninja.dudley.yamr.model

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.db.util.Column
import ninja.dudley.yamr.db.util.Table

/**
 * Created by mdudley on 6/2/15.
 */
Table(Provider.tableName)
public class Provider : MangaElement
{
    Column(name = nameCol)
    public var name: String
    Column(name = newUrlCol)
    public var newUrl: String
//    Column(name = fetchProviderCol)
//    public var fetchProvider: String
//    Column(name = fetchSeriesCol)
//    public var fetchSeries: String
//    Column(name = fetchChapterCol)
//    public var fetchChapter: String
//    Column(name = fetchPageCol)
//    public var fetchPage: String
//    Column(name = fetchNewCol)
//    public var fetchNew: String


    public fun uri(): Uri
    {
        return uri(id)
    }

    public fun series(): Uri
    {
        return series(id)
    }

    // Construction / Persistence ------------------------------------------------------------------

    public constructor(c: Cursor) : super(c)
    {
        if (type != MangaElement.UriType.Provider)
        {
            throw IllegalArgumentException("Attempted to make a provider from a ${type}")
        }
        name = getString(c, nameCol)!!
        newUrl = getString(c, newUrlCol)!!
//        fetchProvider = getString(c, fetchProviderCol)!!
//        fetchSeries = getString(c, fetchSeriesCol)!!
//        fetchChapter = getString(c, fetchChapterCol)!!
//        fetchPage = getString(c, fetchPageCol)!!
//        fetchNew = getString(c, fetchNewCol)!!
        c.close()
    }

    override fun getContentValues(): ContentValues
    {
        val values = super.getContentValues()
        values.put(nameCol, name)
        values.put(newUrlCol, newUrl)
        return values
    }

    companion object
    {
        public val tableName: String = "provider"
        public val nameCol: String = "name"
        public val newUrlCol: String = "new_url"
        public val fetchProviderCol: String = "fetch_provider"
        public val fetchSeriesCol: String = "fetch_series"
        public val fetchChapterCol: String = "fetch_chapter"
        public val fetchPageCol: String = "fetch_page"
        public val fetchNewCol: String = "fetch_new"

        // Uri Handling --------------------------------------------------------------------------------
        public fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/provider")
        }

        public fun uri(id: Int): Uri
        {
            val base = baseUri()
            return base.buildUpon().appendPath(Integer.toString(id)).build()
        }

        public fun series(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("series").build()
        }

        public fun all(): Uri
        {
            return baseUri().buildUpon().appendPath("all").build()
        }
    }
}
