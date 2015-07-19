package yamr.model;

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

    constructor(heritage: Cursor)
    {
        heritage.moveToFirst()

        val providerIdCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_PROVIDER_ID)
        val providerNameCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_PROVIDER_NAME)
        val seriesIdCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_SERIES_ID)
        val seriesNameCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_SERIES_NAME)
        val chapterIdCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_CHAPTER_ID)
        val chapterNumberCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_CHAPTER_NUMBER)
        val pageIdCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_PAGE_ID)
        val pageNumberCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_PAGE_NUMBER)

        providerId = heritage.getInt(providerIdCol)
        providerName = heritage.getString(providerNameCol)
        seriesId = heritage.getInt(seriesIdCol)
        seriesName = heritage.getString(seriesNameCol)
        chapterId = heritage.getInt(chapterIdCol)
        chapterNumber = heritage.getFloat(chapterNumberCol)
        pageId = heritage.getInt(pageIdCol)
        pageNumber = heritage.getFloat(pageNumberCol)

        heritage.close()
    }
}
