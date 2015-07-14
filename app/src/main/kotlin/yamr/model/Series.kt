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
Table(Series.tableName)
public class Series : MangaElement
{
    ForeignKey(value = Provider::class, name = providerIdCol)
    public val providerId: Int
    Column(name = nameCol)
    public var name: String
    Column(name = descriptionCol)
    public var description: String? = null
    Column(name = favoriteCol, type = Column.Type.Integer)
    public var favorite: Boolean = false
    Column(name = alternateNameCol)
    public var alternateName: String? = null
    Column(name = completeCol, type = Column.Type.Integer)
    public var complete: Boolean = false
    Column(name = authorCol)
    public var author: String? = null
    Column(name = artistCol)
    public var artist: String? = null
    Column(name = thumbnailUrlCol)
    public var thumbnailUrl: String? = null
    Column(name = thumbnailPathCol)
    public var thumbnailPath: String? = null
    ForeignKey(value = Chapter::class, name = progressChapterIdCol)
    public var progressChapterId: Int = -1
    ForeignKey(value = Page::class, name = progressPageIdCol)
    public var progressPageId: Int = -1
    Column(name = updatedCol, type = Column.Type.Integer)
    public var updated: Boolean = false

    public fun uri(): Uri
    {
        return uri(id)
    }

    public fun chapters(): Uri
    {
        return chapters(id)
    }

    public fun prevChapter(num: Float): Uri
    {
        return prevChapter(id, num)
    }

    public fun nextChapter(num: Float): Uri
    {
        return nextChapter(id, num)
    }

    public fun genres(): Uri
    {
        return genres(id)
    }

    // Construction / Persistence ------------------------------------------------------------------
    public constructor(providerId: Int, url: String, name: String) : super(url)
    {
        this.providerId = providerId
        this.url = url
        this.name = name
    }

    public constructor(c: Cursor, close: Boolean = true) : super(c)
    {
        providerId = getInt(c, providerIdCol)
        name = getString(c, nameCol)!!
        parse(c, close)
    }

    private fun parse(c: Cursor, close: Boolean)
    {
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
        if (progressPageId != -1)
        {
            values.put(progressPageIdCol, progressPageId)
        }
        values.put(updatedCol, updated)
        return values
    }

    companion object
    {
        public val tableName: String = "series"
        public val providerIdCol: String = Provider.tableName + DBHelper.ID
        public val nameCol: String = "name"
        public val descriptionCol: String = "description"
        public val favoriteCol: String = "favorite"
        public val alternateNameCol: String = "alternate_name"
        public val completeCol: String = "complete"
        public val authorCol: String = "author"
        public val artistCol: String = "artist"
        public val thumbnailUrlCol: String = "thumbnail_url"
        public val thumbnailPathCol: String = "thumbnail_path"
        public val progressChapterIdCol: String = "progress_" + Chapter.tableName + DBHelper.ID
        public val progressPageIdCol: String = "progress_" + Page.tableName + DBHelper.ID
        public val updatedCol: String = "updated"

        // Uri Handling --------------------------------------------------------------------------------
        public fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/series")
        }

        public fun uri(id: Int): Uri
        {
            return baseUri().buildUpon().appendPath(Integer.toString(id)).build()
        }

        public fun favorites(): Uri
        {
            return baseUri().buildUpon().appendPath("favorites").build()
        }

        public fun chapters(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("chapters").build()
        }

        public fun prevChapter(id: Int, num: Float): Uri
        {
            return chapters(id).buildUpon().appendPath(java.lang.Float.toString(num)).appendPath("prev").build()
        }

        public fun nextChapter(id: Int, num: Float): Uri
        {
            return chapters(id).buildUpon().appendPath(java.lang.Float.toString(num)).appendPath("next").build()
        }

        public fun genres(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("genres").build()
        }
    }
}
