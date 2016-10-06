package ninja.dudley.yamr.model

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.db.util.Column
import ninja.dudley.yamr.db.util.ForeignKey
import ninja.dudley.yamr.db.util.Table

/**
* Created by mdudley on 5/19/15. Yup.
*/
@Table(Chapter.tableName)
class Chapter : MangaElement
{
    @ForeignKey(value = Series::class, name = seriesIdCol)
    val seriesId: Int
    @Column(name = nameCol)
    var name: String? = null
    @Column(name = numberCol, type = Column.Type.Real)
    val number: Float

    // Construction / Persistence ------------------------------------------------------------------
    constructor(seriesId: Int, url: String, number: Float) : super(url, MangaElement.UriType.Chapter)
    {
        this.seriesId = seriesId
        this.number = number
    }

    constructor(c: Cursor, close: Boolean = true) : super(c)
    {
        if (type != MangaElement.UriType.Chapter)
        {
            throw IllegalArgumentException("Attempted to make a chapter from a $type")
        }
        seriesId = getInt(c, seriesIdCol)
        name = getString(c, nameCol)
        number = getFloat(c, numberCol)
        if (close)
        {
            c.close()
        }
    }

    override fun getContentValues(): ContentValues
    {
        val values = super.getContentValues()
        values.put(seriesIdCol, seriesId)
        values.put(nameCol, name)
        values.put(numberCol, number)
        return values
    }

    // Uri Handling --------------------------------------------------------------------------------
    fun uri(): Uri
    {
        return uri(id)
    }

    fun pages(): Uri
    {
        return pages(id)
    }

    fun prevPage(num: Float): Uri
    {
        return prevPage(id, num)
    }

    fun nextPage(num: Float): Uri
    {
        return nextPage(id, num)
    }

    fun pageComplete(): Uri
    {
       return pageComplete(id)
    }

    companion object
    {
        const val tableName: String = "chapter"
        const val seriesIdCol: String = Series.tableName + DBHelper.ID
        const val nameCol: String = "name"
        const val numberCol: String = "number"

        // Uri Handling ----------------------------------------------------------------------------
        fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/chapter")
        }

        fun uri(id: Int): Uri
        {
            return baseUri().buildUpon().appendPath(Integer.toString(id)).build()
        }

        fun pages(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("pages").build()
        }

        fun prevPage(id: Int, num: Float): Uri
        {
            return pages(id).buildUpon()
                    .appendPath(java.lang.Float.toString(num))
                    .appendPath("prev").build()
        }

        fun nextPage(id: Int, num: Float): Uri
        {
            return pages(id).buildUpon()
                    .appendPath(java.lang.Float.toString(num))
                    .appendPath("next").build()
        }

        fun pageComplete(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("pageComplete").build()
        }
    }
}
