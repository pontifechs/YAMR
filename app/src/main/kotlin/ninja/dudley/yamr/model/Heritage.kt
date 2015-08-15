package ninja.dudley.yamr.model;

import android.database.Cursor
import ninja.dudley.yamr.db.DBHelper

/**
 * Created by mdudley on 7/19/15.
 */
public class Heritage
{
    public val providerId: Int
    public val providerName: String
    public val seriesId: Int
    public val seriesName: String
    public val chapterId: Int
    public val chapterNumber: Float
    public val pageId: Int
    public val pageNumber: Float

    constructor(c: Cursor)
    {
        c.moveToFirst()

        val providerIdCol = c.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_PROVIDER_ID)
        val providerNameCol = c.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_PROVIDER_NAME)
        val seriesIdCol = c.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_SERIES_ID)
        val seriesNameCol = c.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_SERIES_NAME)
        val chapterIdCol = c.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_CHAPTER_ID)
        val chapterNumberCol = c.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_CHAPTER_NUMBER)
        val pageIdCol = c.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_PAGE_ID)
        val pageNumberCol = c.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_PAGE_NUMBER)

        providerId = c.getInt(providerIdCol)
        providerName = c.getString(providerNameCol)
        seriesId = c.getInt(seriesIdCol)
        seriesName = c.getString(seriesNameCol)
        chapterId = c.getInt(chapterIdCol)
        chapterNumber = c.getFloat(chapterNumberCol)
        pageId = c.getInt(pageIdCol)
        pageNumber = c.getFloat(pageNumberCol)

        c.close()
    }
}
