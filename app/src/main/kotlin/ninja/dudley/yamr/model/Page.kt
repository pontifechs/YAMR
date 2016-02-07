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
@Table(Page.tableName)
class Page : MangaElement
{
    @ForeignKey(value = Chapter::class, name = chapterIdCol)
    val chapterId: Int
    @Column(name = numberCol, type = Column.Type.Real)
    val number: Float
    @Column(name = imageUrlCol)
    var imageUrl: String? = null
    @Column(name = imagePathCol)
    var imagePath: String? = null

    // Construction / Persistence ------------------------------------------------------------------
    constructor(chapterId: Int, url: String, number: Float) :
        super(url, MangaElement.UriType.Page)
    {
        this.chapterId = chapterId
        this.number = number
    }

    constructor(c: Cursor, close: Boolean = true) : super(c)
    {
        if (type != MangaElement.UriType.Page)
        {
            throw IllegalArgumentException("Attempted to make a page from a $type")
        }
        chapterId = getInt(c, chapterIdCol)
        number = getFloat(c, numberCol)
        imageUrl = getString(c, imageUrlCol)
        imagePath = getString(c, imagePathCol)
        if (close)
        {
            c.close()
        }
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

    // Uri Handling --------------------------------------------------------------------------------
    fun uri(): Uri
    {
        return uri(id)
    }

    fun heritage(): Uri
    {
        return heritage(id)
    }

    companion object
    {
        const val tableName: String = "page"
        const val chapterIdCol: String = Chapter.tableName + DBHelper.ID
        const val numberCol: String = "number"
        const val imageUrlCol: String = "image_url"
        const val imagePathCol: String = "image_path"


        // Uri Handling ----------------------------------------------------------------------------
        fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/page")
        }

        fun uri(id: Int): Uri
        {
            return baseUri().buildUpon().appendPath(Integer.toString(id)).build()
        }

        fun heritage(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("heritage").build()
        }
    }
}
