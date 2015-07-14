package ninja.dudley.yamr.model

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.db.util.Column
import ninja.dudley.yamr.db.util.ForeignKey
import ninja.dudley.yamr.db.util.Table

/**
 * Created by mdudley on 5/19/15.
 */
Table(Page.tableName)
public class Page : MangaElement
{
    ForeignKey(value = Chapter::class, name = chapterIdCol)
    public val chapterId: Int
    Column(name = numberCol, type = Column.Type.Real)
    public val number: Float
    Column(name = imageUrlCol)
    public var imageUrl: String? = null
    Column(name = imagePathCol)
    public var imagePath: String? = null

    public fun uri(): Uri
    {
        return uri(id)
    }

    public fun heritage(): Uri
    {
        return heritage(id)
    }

    // Construction / Persistence ------------------------------------------------------------------
    public constructor(chapterId: Int, url: String, number: Float) : super(url)
    {
        this.chapterId = chapterId
        this.number = number
    }

    public constructor(c: Cursor) : super(c)
    {
        chapterId = getInt(c, chapterIdCol)
        number = getFloat(c, numberCol)
        imageUrl = getString(c, imageUrlCol)
        imagePath = getString(c, imagePathCol)
        c.close()
    }

    override fun getContentValues(): ContentValues
    {
        val values = super.getContentValues()
        values.put(chapterIdCol, chapterId)
        values.put(numberCol, number)
        values.put(imageUrlCol, imageUrl)
        values.put(imagePathCol, imagePath)
        return values
    }

    companion object
    {
        public val tableName: String = "page"
        public val chapterIdCol: String = Chapter.tableName + DBHelper.ID
        public val numberCol: String = "number"
        public val imageUrlCol: String = "image_url"
        public val imagePathCol: String = "image_path"


        // Uri Handling --------------------------------------------------------------------------------
        public fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/page")
        }

        public fun uri(id: Int): Uri
        {
            return baseUri().buildUpon().appendPath(Integer.toString(id)).build()
        }

        public fun heritage(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("heritage").build()
        }
    }
}
