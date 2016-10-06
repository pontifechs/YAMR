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
@Table(Series.tableName)
class Series : MangaElement
{
    @ForeignKey(value = Provider::class, name = providerIdCol)
    val providerId: Int
    @Column(name = nameCol)
    var name: String
    @Column(name = descriptionCol)
    var description: String? = null
    @Column(name = favoriteCol, type = Column.Type.Integer)
    var favorite: Boolean = false
    @Column(name = alternateNameCol)
    var alternateName: String? = null
    @Column(name = completeCol, type = Column.Type.Integer)
    var complete: Boolean = false
    @Column(name = authorCol)
    var author: String? = null
    @Column(name = artistCol)
    var artist: String? = null
    @Column(name = thumbnailUrlCol)
    var thumbnailUrl: String? = null
    @Column(name = thumbnailPathCol)
    var thumbnailPath: String? = null
    @ForeignKey(value = Chapter::class, name = progressChapterIdCol)
    var progressChapterId: Int? = -1
    @ForeignKey(value = Page::class, name = progressPageIdCol)
    var progressPageId: Int? = -1
    @Column(name = updatedCol, type = Column.Type.Integer)
    var updated: Boolean = false



    // Construction / Persistence ------------------------------------------------------------------
    constructor(providerId: Int, url: String, name: String) : super(url, MangaElement.UriType.Series)
    {
        this.providerId = providerId
        this.url = url
        this.name = name
    }

    constructor(c: Cursor, close: Boolean = true) : super(c)
    {
        if (type != MangaElement.UriType.Series)
        {
            throw IllegalArgumentException("Attempted to make a series from a $type")
        }
        providerId = getInt(c, providerIdCol)
        name = getString(c, nameCol)!!
        description = getString(c, descriptionCol)
        favorite = getBool(c, favoriteCol)
        alternateName = getString(c, alternateNameCol)
        complete = getBool(c, completeCol)
        author = getString(c, authorCol)
        artist = getString(c, artistCol)
        thumbnailUrl = getString(c, thumbnailUrlCol)
        thumbnailPath = getString(c, thumbnailPathCol)
        progressChapterId = getInt(c, progressChapterIdCol)
        progressPageId = getInt(c, progressPageIdCol)
        updated = getBool(c, updatedCol)
        if (close)
        {
            c.close()
        }
    }

    override fun getContentValues(): ContentValues
    {
        val values = super.getContentValues()
        values.put(providerIdCol, providerId)
        values.put(nameCol, name)
        values.put(descriptionCol, description)
        values.put(favoriteCol, favorite)
        values.put(alternateNameCol, alternateName)
        values.put(completeCol, complete)
        values.put(authorCol, author)
        values.put(artistCol, artist)
        values.put(thumbnailUrlCol, thumbnailUrl)
        values.put(thumbnailPathCol, thumbnailPath)
        if (progressChapterId != -1)
        {
            values.put(progressChapterIdCol, progressChapterId)
        }
        else
        {
            values.putNull(progressChapterIdCol)
        }
        if (progressPageId != -1)
        {
            values.put(progressPageIdCol, progressPageId)
        }
        else
        {
            values.putNull(progressPageIdCol)
        }
        values.put(updatedCol, updated)
        return values
    }

    // Uri Handling --------------------------------------------------------------------------------
    fun uri(): Uri
    {
        return uri(id)
    }

    fun chapters(): Uri
    {
        return chapters(id)
    }

    fun prevChapter(num: Float): Uri
    {
        return prevChapter(id, num)
    }

    fun nextChapter(num: Float): Uri
    {
        return nextChapter(id, num)
    }

    fun genres(): Uri
    {
        return genres(id)
    }

    fun pageComplete(): Uri
    {
       return pageComplete(id)
    }

    companion object
    {
        const val tableName: String = "series"
        const val providerIdCol: String = Provider.tableName + DBHelper.ID
        const val nameCol: String = "name"
        const val descriptionCol: String = "description"
        const val favoriteCol: String = "favorite"
        const val alternateNameCol: String = "alternate_name"
        const val completeCol: String = "complete"
        const val authorCol: String = "author"
        const val artistCol: String = "artist"
        const val thumbnailUrlCol: String = "thumbnail_url"
        const val thumbnailPathCol: String = "thumbnail_path"
        const val progressChapterIdCol: String = "progress_" + Chapter.tableName + DBHelper.ID
        const val progressPageIdCol: String = "progress_" + Page.tableName + DBHelper.ID
        const val updatedCol: String = "updated"

        // Uri Handling ----------------------------------------------------------------------------
        fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/series")
        }

        fun uri(id: Int): Uri
        {
            return baseUri().buildUpon().appendPath(Integer.toString(id)).build()
        }

        fun favorites(): Uri
        {
            return baseUri().buildUpon().appendPath("favorites").build()
        }

        fun chapters(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("chapters").build()
        }

        fun prevChapter(id: Int, num: Float): Uri
        {
            return chapters(id).buildUpon()
                    .appendPath(java.lang.Float.toString(num))
                    .appendPath("prev").build()
        }

        fun nextChapter(id: Int, num: Float): Uri
        {
            return chapters(id).buildUpon()
                    .appendPath(java.lang.Float.toString(num))
                    .appendPath("next").build()
        }

        fun genres(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("genres").build()
        }

        fun pageComplete(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("pageComplete").build()
        }
    }
}
