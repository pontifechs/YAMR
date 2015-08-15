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
    public val name: String
    Column(name = newUrlCol)
    public val newUrl: String

    Column(name = fetchProviderCol)
    public val fetchProvider: String
    Column(name = stubSeriesCol)
    public val stubSeries: String
    Column(name = fetchSeriesCol)
    public val fetchSeries: String
    Column(name = fetchSeriesGenresCol)
    public val fetchSeriesGenres: String
    Column(name = stubChapterCol)
    public val stubChapter: String
    Column(name = fetchChapterCol)
    public val fetchChapter: String
    Column(name = stubPageCol)
    public val stubPage: String
    Column(name = fetchPageCol)
    public val fetchPage: String
    Column(name = fetchNewCol)
    public val fetchNew: String

    // Construction / Persistence ------------------------------------------------------------------
    public constructor(c: Cursor) : super(c)
    {
        if (type != MangaElement.UriType.Provider)
        {
            throw IllegalArgumentException("Attempted to make a provider from a ${type}")
        }
        name = getString(c, nameCol)!!
        newUrl = getString(c, newUrlCol)!!
        fetchProvider = getString(c, fetchProviderCol)!!
        stubSeries = getString(c, stubSeriesCol)!!
        fetchSeries = getString(c, fetchSeriesCol)!!
        fetchSeriesGenres = getString(c, fetchSeriesGenresCol)!!
        stubChapter = getString(c, stubChapterCol)!!
        fetchChapter = getString(c, fetchChapterCol)!!
        stubPage = getString(c, stubPageCol)!!
        fetchPage = getString(c, fetchPageCol)!!
        fetchNew = getString(c, fetchNewCol)!!
        c.close()
    }

    override fun getContentValues(): ContentValues
    {
        val values = super.getContentValues()
        values.put(nameCol, name)
        values.put(newUrlCol, newUrl)
        return values
    }

    // Uri Handling --------------------------------------------------------------------------------
    public fun uri(): Uri
    {
        return uri(id)
    }

    public fun series(): Uri
    {
        return series(id)
    }

    companion object
    {
        public val tableName: String = "provider"
        public val nameCol: String = "name"
        public val newUrlCol: String = "new_url"
        public val fetchProviderCol: String = "fetch_provider"
        public val stubSeriesCol: String = "stub_series"
        public val fetchSeriesCol: String = "fetch_series"
        public val fetchSeriesGenresCol: String = "fetch_series_genres"
        public val stubChapterCol: String = "stub_chapter_col"
        public val fetchChapterCol: String = "fetch_chapter"
        public val stubPageCol: String = "stub_page"
        public val fetchPageCol: String = "fetch_page"
        public val fetchNewCol: String = "fetch_new"

        // Uri Handling ----------------------------------------------------------------------------
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
