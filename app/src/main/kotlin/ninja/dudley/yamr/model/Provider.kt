package ninja.dudley.yamr.model

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.db.util.Column
import ninja.dudley.yamr.db.util.Table
import ninja.dudley.yamr.db.util.Unique

/**
* Created by mdudley on 6/2/15. Yup.
*/
@Table(Provider.tableName)
class Provider : MangaElement
{
    @Unique
    @Column(name = nameCol)
    val name: String
    @Column(name = newUrlCol)
    val newUrl: String

    @Column(name = fetchProviderCol)
    val fetchProvider: String
    @Column(name = stubSeriesCol)
    val stubSeries: String
    @Column(name = fetchSeriesCol)
    val fetchSeries: String
    @Column(name = fetchSeriesGenresCol)
    val fetchSeriesGenres: String
    @Column(name = stubChapterCol)
    val stubChapter: String
    @Column(name = fetchChapterCol)
    val fetchChapter: String
    @Column(name = stubPageCol)
    val stubPage: String
    @Column(name = fetchPageCol)
    val fetchPage: String
    @Column(name = fetchNewCol)
    val fetchNew: String

    // Construction / Persistence ------------------------------------------------------------------
    constructor(c: Cursor, closeAfter: Boolean = true) : super(c)
    {
        if (type != MangaElement.UriType.Provider)
        {
            throw IllegalArgumentException("Attempted to make a provider from a $type")
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
        if (closeAfter)
        {
            c.close()
        }
    }

    override fun getContentValues(): ContentValues
    {
        val values = super.getContentValues()
        values.put(nameCol, name)
        values.put(newUrlCol, newUrl)
        return values
    }

    // Uri Handling --------------------------------------------------------------------------------
    fun uri(): Uri
    {
        return uri(id)
    }

    fun series(): Uri
    {
        return series(id)
    }

    companion object
    {
        const val tableName: String = "provider"
        const val nameCol: String = "name"
        const val newUrlCol: String = "new_url"
        const val fetchProviderCol: String = "fetch_provider"
        const val stubSeriesCol: String = "stub_series"
        const val fetchSeriesCol: String = "fetch_series"
        const val fetchSeriesGenresCol: String = "fetch_series_genres"
        const val stubChapterCol: String = "stub_chapter_col"
        const val fetchChapterCol: String = "fetch_chapter"
        const val stubPageCol: String = "stub_page"
        const val fetchPageCol: String = "fetch_page"
        const val fetchNewCol: String = "fetch_new"

        // Uri Handling ----------------------------------------------------------------------------
        fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/provider")
        }

        fun uri(id: Int): Uri
        {
            val base = baseUri()
            return base.buildUpon().appendPath(Integer.toString(id)).build()
        }

        fun series(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("series").build()
        }

        fun all(): Uri
        {
            return baseUri().buildUpon().appendPath("all").build()
        }
    }
}
