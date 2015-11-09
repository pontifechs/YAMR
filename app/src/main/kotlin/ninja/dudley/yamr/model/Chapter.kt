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
public class Chapter : MangaElement
{
    @ForeignKey(value = Series::class, name = seriesIdCol)
    public val seriesId: Int
    @Column(name = nameCol)
    public var name: String? = null
    @Column(name = numberCol, type = Column.Type.Real)
    public val number: Float

    // Construction / Persistence ------------------------------------------------------------------
    public constructor(seriesId: Int, url: String, number: Float) : super(url, MangaElement.UriType.Chapter)
    {
        this.seriesId = seriesId
        this.number = number
    }

    public constructor(c: Cursor, close: Boolean = true) : super(c)
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
    public fun uri(): Uri
    {
        return uri(id)
    }

    public fun pages(): Uri
    {
        return pages(id)
    }

    public fun prevPage(num: Float): Uri
    {
        return prevPage(id, num)
    }

    public fun nextPage(num: Float): Uri
    {
        return nextPage(id, num)
    }

    companion object
    {
        const public val tableName: String = "chapter"
        const public val seriesIdCol: String = Series.tableName + DBHelper.ID
        const public val nameCol: String = "name"
        const public val numberCol: String = "number"

        // Uri Handling ----------------------------------------------------------------------------
        public fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/chapter")
        }

        public fun uri(id: Int): Uri
        {
            return baseUri().buildUpon().appendPath(Integer.toString(id)).build()
        }

        public fun pages(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("pages").build()
        }

        public fun prevPage(id: Int, num: Float): Uri
        {
            return pages(id).buildUpon()
                    .appendPath(java.lang.Float.toString(num))
                    .appendPath("prev").build()
        }

        public fun nextPage(id: Int, num: Float): Uri
        {
            return pages(id).buildUpon()
                    .appendPath(java.lang.Float.toString(num))
                    .appendPath("next").build()
        }
    }
}
