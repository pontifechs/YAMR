package ninja.dudley.yamr.db

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import ninja.dudley.yamr.model.*

/**
* Created by mdudley on 5/21/15. Yup.
*/
public class DBProvider : ContentProvider()
{
    private var dbh: DBHelper? = null

    private enum class MatchCode(private val code: Int)
    {
        NoMatch(UriMatcher.NO_MATCH),
        ProviderMatch(lastCode++),
        ProviderByID(lastCode++),
        ProviderSeries(lastCode++),
        ProviderAll(lastCode++),
        SeriesMatch(lastCode++),
        SeriesByID(lastCode++),
        SeriesChapters(lastCode++),
        SeriesFavorites(lastCode++),
        SeriesGenres(lastCode++),
        ChapterMatch(lastCode++),
        ChapterByID(lastCode++),
        PrevChapterInSeries(lastCode++),
        NextChapterInSeries(lastCode++),
        ChapterPages(lastCode++),
        PageMatch(lastCode++),
        PageByID(lastCode++),
        PrevPagesInChapter(lastCode++),
        NextPagesInChapter(lastCode++),
        PageHeritage(lastCode++),
        GenreMatch(lastCode++),
        GenreSeries(lastCode++),
        GenreAll(lastCode++),
        SeriesGenreRelator(lastCode++);

        override fun toString(): String
        {
            return Integer.toString(code)
        }

        public fun value(): Int
        {
            return code
        }

        companion object
        {
            public fun from(code: Int): MatchCode
            {
                for (matchCode in values)
                {
                    if (matchCode.value() == code)
                    {
                        return matchCode
                    }
                }
                throw AssertionError("Invalid MatchCode " + code)
            }
        }
    }

    override fun onCreate(): Boolean
    {
        dbh = DBHelper(context)
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor
    {
        val db = dbh!!.readableDatabase
        val code = matcher.match(uri)
        val matchCode = MatchCode.from(code)
        when (matchCode)
        {
            DBProvider.MatchCode.ProviderMatch -> return db.query(Provider.tableName, DBHelper.projections[Provider.tableName], "${MangaElement.urlCol}=?", selectionArgs, null, null, sortOrder)
            DBProvider.MatchCode.ProviderByID -> return db.query(Provider.tableName, DBHelper.projections[Provider.tableName], "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))), null, null, null, "1")
            DBProvider.MatchCode.ProviderSeries ->
            {
                var querySelection = Series.providerIdCol + "=?"
                if (selection != null)
                {
                    querySelection += " and " + selection
                }
                val querySelectionArgs: Array<String>
                if (selectionArgs == null)
                {
                    querySelectionArgs = arrayOf(Integer.toString(getId(code, uri)))
                }
                else
                {
                    querySelectionArgs = arrayOf(Integer.toString(getId(code, uri)), selectionArgs[0])
                }
                return db.query(Series.tableName, DBHelper.projections[Series.tableName], querySelection, querySelectionArgs, null, null, sortOrder ?: Series.nameCol)
            }
            DBProvider.MatchCode.ProviderAll -> return db.query(Provider.tableName, DBHelper.projections[Provider.tableName], null, null, null, null, sortOrder)
            DBProvider.MatchCode.PrevChapterInSeries ->
            {
                val chapterNumberStringForPrev = uri.pathSegments[uri.pathSegments.size - 2]
                val chapterNumberForPrev = java.lang.Float.parseFloat(chapterNumberStringForPrev)
                return db.query(Chapter.tableName, DBHelper.projections[Chapter.tableName], "${Chapter.seriesIdCol}=? and ${Chapter.numberCol}<?", arrayOf(Integer.toString(getId(code, uri)), java.lang.Float.toString(chapterNumberForPrev)), null, null, Chapter.numberCol + " desc", "1")
            }
            DBProvider.MatchCode.NextChapterInSeries ->
            {
                val chapterNumberStringForNext = uri.pathSegments[uri.pathSegments.size - 2]
                val chapterNumberForNext = java.lang.Float.parseFloat(chapterNumberStringForNext)
                return db.query(Chapter.tableName, DBHelper.projections[Chapter.tableName], "${Chapter.seriesIdCol}=? and ${Chapter.numberCol}>?", arrayOf(Integer.toString(getId(code, uri)), java.lang.Float.toString(chapterNumberForNext)), null, null, Chapter.numberCol + " asc", "1")
            }
            DBProvider.MatchCode.SeriesMatch -> return db.query(Series.tableName, DBHelper.projections[Series.tableName], "${MangaElement.urlCol}=?", selectionArgs, null, null, sortOrder)
            DBProvider.MatchCode.SeriesByID -> return db.query(Series.tableName, DBHelper.projections[Series.tableName], "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))), null, null, null, "1")
            DBProvider.MatchCode.SeriesChapters -> return db.query(Chapter.tableName, DBHelper.projections[Chapter.tableName], "${Chapter.seriesIdCol}=?", arrayOf(Integer.toString(getId(code, uri))), null, null, sortOrder ?: Chapter.numberCol)
            DBProvider.MatchCode.SeriesFavorites -> return db.query(Series.tableName, DBHelper.projections[Series.tableName], "${Series.favoriteCol} > 0", null, null, null, sortOrder)
            DBProvider.MatchCode.SeriesGenres ->
            {
                return db.query(DBHelper.SeriesGenreEntry.SERIES_GENRES_VIEW, DBHelper.projections[Genre.tableName], "${DBHelper.SeriesGenreEntry.COLUMN_SERIES_ID} =?", arrayOf(Integer.toString(getId(code, uri))), null, null, null)
            }
            DBProvider.MatchCode.GenreSeries ->
            {
                return db.query(DBHelper.SeriesGenreEntry.GENRE_SERIES_VIEW, DBHelper.projections[Series.tableName], "${DBHelper.SeriesGenreEntry.COLUMN_GENRE_ID} =?", arrayOf(Integer.toString(getId(code, uri))), null, null, null)
            }
            DBProvider.MatchCode.ChapterMatch -> return db.query(Chapter.tableName, DBHelper.projections[Chapter.tableName], "${MangaElement.urlCol}=?", selectionArgs, null, null, sortOrder)
            DBProvider.MatchCode.ChapterByID -> return db.query(Chapter.tableName, DBHelper.projections[Chapter.tableName], "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))), null, null, null, "1")
            DBProvider.MatchCode.ChapterPages -> return db.query(Page.tableName, DBHelper.projections[Page.tableName], "${Page.chapterIdCol}=?", arrayOf(Integer.toString(getId(code, uri))), null, null, sortOrder ?: Page.numberCol)
            DBProvider.MatchCode.PrevPagesInChapter ->
            {
                val pageNumberStringForPrev = uri.pathSegments[uri.pathSegments.size - 2]
                val pageNumberForPrev = java.lang.Float.parseFloat(pageNumberStringForPrev)
                return db.query(Page.tableName, DBHelper.projections[Page.tableName], "${Page.chapterIdCol}=? and ${Page.numberCol}<?", arrayOf(Integer.toString(getId(code, uri)), java.lang.Float.toString(pageNumberForPrev)), null, null, Page.numberCol + " desc")
            }
            DBProvider.MatchCode.NextPagesInChapter ->
            {
                val pageNumberStringNext = uri.pathSegments[uri.pathSegments.size - 2]
                val pageNumberNext = java.lang.Float.parseFloat(pageNumberStringNext)
                val cursor = db.query(Page.tableName, DBHelper.projections[Page.tableName], "${Page.chapterIdCol}=? and ${Page.numberCol}>?", arrayOf(Integer.toString(getId(code, uri)), java.lang.Float.toString(pageNumberNext)), null, null, Page.numberCol + " asc")
                return cursor
            }
            DBProvider.MatchCode.PageMatch -> return db.query(Page.tableName, DBHelper.projections[Page.tableName], "${MangaElement.urlCol}=?", selectionArgs, null, null, sortOrder)
            DBProvider.MatchCode.PageByID -> return db.query(Page.tableName, DBHelper.projections[Page.tableName], "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))), null, null, null, "1")
            DBProvider.MatchCode.PageHeritage -> return db.query(DBHelper.PageHeritageViewEntry.TABLE_NAME, DBHelper.PageHeritageViewEntry.projection, "${DBHelper.PageHeritageViewEntry.COLUMN_PAGE_ID}=?", arrayOf(Integer.toString(getId(code, uri))), null, null, null, "1")
            DBProvider.MatchCode.GenreMatch -> return db.query(Genre.tableName, DBHelper.projections[Genre.tableName], "${Genre.nameCol}=?", selectionArgs, null, null, null, "1")
            DBProvider.MatchCode.GenreAll -> return db.query(Genre.tableName, DBHelper.projections[Genre.tableName], null, null, null, null, sortOrder)
            else -> throw IllegalArgumentException("Invalid query uri: " + uri.toString())
        }
    }

    override fun getType(uri: Uri): String
    {
        val code = matcher.match(uri)
        val matchCode = MatchCode.from(code)
        when (matchCode)
        {
            DBProvider.MatchCode.ProviderMatch, DBProvider.MatchCode.ProviderByID ->
                return "vnd.android.cursor.item/vnd.dudley.ninja.ninja.dudley.yamr.db.DBProvider." + Provider.tableName
            DBProvider.MatchCode.ProviderSeries, DBProvider.MatchCode.SeriesFavorites, DBProvider.MatchCode.GenreSeries ->
                return "vnd.android.cursor.dir/vnd.dudley.ninja.ninja.dudley.yamr.db.DBProvider." + Series.tableName
            DBProvider.MatchCode.SeriesMatch, DBProvider.MatchCode.SeriesByID ->
                return "vnd.android.cursor.item/vnd.dudley.ninja.ninja.dudley.yamr.db.DBProvider." + Series.tableName
            DBProvider.MatchCode.SeriesChapters ->
                return "vnd.android.cursor.dir/vnd.dudley.ninja.ninja.dudley.yamr.db.DBProvider." + Chapter.tableName
            DBProvider.MatchCode.ChapterMatch, DBProvider.MatchCode.ChapterByID ->
                return "vnd.android.cursor.item/vnd.dudley.ninja.ninja.dudley.yamr.db.DBProvider." + Chapter.tableName
            DBProvider.MatchCode.ChapterPages ->
                return "vnd.android.cursor.dir/vnd.dudley.ninja.ninja.dudley.yamr.db.DBProvider." + Page.tableName
            DBProvider.MatchCode.PageMatch, DBProvider.MatchCode.PageByID ->
                return "vnd.android.cursor.item/vnd.dudley.ninja.ninja.dudley.yamr.db.DBProvider." + Page.tableName
            DBProvider.MatchCode.GenreMatch ->
                return "vnd.android.cursor.item/vnd.dudley.ninja.ninja.dudley.yamr.db.DBProvider." + Genre.tableName
            DBProvider.MatchCode.SeriesGenres ->
                return "vnd.android.cursor.dir/vnd.dudley.ninja.ninja.dudley.yamr.db.DBProvider." + Genre.tableName
            else -> throw IllegalArgumentException("Invalid uri: " + uri.toString())
        }
    }

    override fun insert(uri: Uri, values: ContentValues): Uri?
    {
        val db = dbh!!.writableDatabase
        val id: Long
        val inserted: Uri
        val code = matcher.match(uri)
        val matchCode = MatchCode.from(code)
        when (matchCode)
        {
            DBProvider.MatchCode.ProviderMatch ->
            {
                id = db.insert(Provider.tableName, null, values)
                inserted = Provider.uri(id.toInt())
                return inserted
            }
            DBProvider.MatchCode.SeriesMatch ->
            {
                id = db.insert(Series.tableName, null, values)
                inserted = Series.uri(id.toInt())
                val providerSeries = Provider.series(getId(code, uri))
                context.contentResolver.notifyChange(providerSeries, null)
                return inserted
            }
            DBProvider.MatchCode.ChapterMatch ->
            {
                id = db.insert(Chapter.tableName, null, values)
                inserted = Chapter.uri(id.toInt())
                val seriesChapters = Series.chapters(getId(code, uri))
                context.contentResolver.notifyChange(seriesChapters, null)
                return inserted
            }
            DBProvider.MatchCode.PageMatch ->
            {
                id = db.insert(Page.tableName, null, values)
                inserted = Page.uri(id.toInt())
                val chapterPages = Chapter.pages(getId(code, uri))
                context.contentResolver.notifyChange(chapterPages, null)
                return inserted
            }
            DBProvider.MatchCode.GenreMatch ->
            {
                id = db.insert(Genre.tableName, null, values)
                inserted = Genre.uri(id.toInt())
                return inserted
            }
            DBProvider.MatchCode.SeriesGenreRelator ->
            {
                db.insert(DBHelper.SeriesGenreEntry.TABLE_NAME, null, values)
                return null
            }
            else -> throw IllegalArgumentException("Invalid insert uri: " + uri.toString())
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int
    {
        val db = dbh!!.writableDatabase
        val code = matcher.match(uri)
        val matchCode = MatchCode.from(code)
        when (matchCode)
        {
            DBProvider.MatchCode.ProviderByID -> return db.delete(Provider.tableName, "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))))
            DBProvider.MatchCode.SeriesByID -> return db.delete(Series.tableName, "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))))
            DBProvider.MatchCode.ChapterByID -> return db.delete(Chapter.tableName, "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))))
            DBProvider.MatchCode.PageByID -> return db.delete(Page.tableName, "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))))
            DBProvider.MatchCode.SeriesGenres -> return db.delete(DBHelper.SeriesGenreEntry.TABLE_NAME, "${DBHelper.SeriesGenreEntry.COLUMN_SERIES_ID}=?", arrayOf(Integer.toString(getId(code, uri))))
            else -> throw IllegalArgumentException("Invalid delete uri : " + uri.toString())
        }
    }

    override fun update(uri: Uri, values: ContentValues, selection: String?, selectionArgs: Array<String>?): Int
    {
        val db = dbh!!.writableDatabase
        val code = matcher.match(uri)
        val matchCode = MatchCode.from(code)
        when (matchCode)
        {
            DBProvider.MatchCode.ProviderByID -> return db.update(Provider.tableName, values, "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))))
            DBProvider.MatchCode.SeriesByID -> return db.update(Series.tableName, values, "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))))
            DBProvider.MatchCode.ChapterByID -> return db.update(Chapter.tableName, values, "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))))
            DBProvider.MatchCode.PageByID -> return db.update(Page.tableName, values, "${DBHelper.ID}=?", arrayOf(Integer.toString(getId(code, uri))))
            else -> throw IllegalArgumentException("Invalid update uri: " + uri.toString())
        }
    }

    companion object
    {

        private var matcher: UriMatcher

        // Java you are the fucking dumbest. I hate you with a passion. You never let me do what I want to do.
        // Android you're almost as bad! I don't give a flying fuck what the number I put in here is. All I care is that it's unique.
        var lastCode: Int = 0

        init
        {
            matcher = UriMatcher(UriMatcher.NO_MATCH)
            matcher.addURI(DBHelper.AUTHORITY, "/provider", MatchCode.ProviderMatch.value())
            matcher.addURI(DBHelper.AUTHORITY, "/provider/all", MatchCode.ProviderAll.value())
            matcher.addURI(DBHelper.AUTHORITY, "/provider/#", MatchCode.ProviderByID.value())
            matcher.addURI(DBHelper.AUTHORITY, "/provider/#/series", MatchCode.ProviderSeries.value())
            matcher.addURI(DBHelper.AUTHORITY, "/series", MatchCode.SeriesMatch.value())
            matcher.addURI(DBHelper.AUTHORITY, "/series/favorites", MatchCode.SeriesFavorites.value())
            matcher.addURI(DBHelper.AUTHORITY, "/series/#", MatchCode.SeriesByID.value())
            matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters", MatchCode.SeriesChapters.value())
            matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters/*/prev", MatchCode.PrevChapterInSeries.value())
            matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters/*/next", MatchCode.NextChapterInSeries.value())
            matcher.addURI(DBHelper.AUTHORITY, "/series/#/genres", MatchCode.SeriesGenres.value())
            matcher.addURI(DBHelper.AUTHORITY, "/chapter", MatchCode.ChapterMatch.value())
            matcher.addURI(DBHelper.AUTHORITY, "/chapter/#", MatchCode.ChapterByID.value())
            matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages", MatchCode.ChapterPages.value())
            matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages/*/prev", MatchCode.PrevPagesInChapter.value())
            matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages/*/next", MatchCode.NextPagesInChapter.value())
            matcher.addURI(DBHelper.AUTHORITY, "/page", MatchCode.PageMatch.value())
            matcher.addURI(DBHelper.AUTHORITY, "/page/#", MatchCode.PageByID.value())
            matcher.addURI(DBHelper.AUTHORITY, "/page/#/heritage", MatchCode.PageHeritage.value())
            matcher.addURI(DBHelper.AUTHORITY, "/genre", MatchCode.GenreMatch.value())
            matcher.addURI(DBHelper.AUTHORITY, "/genre/all", MatchCode.GenreAll.value())
            matcher.addURI(DBHelper.AUTHORITY, "/genre/relator", MatchCode.SeriesGenreRelator.value())
            matcher.addURI(DBHelper.AUTHORITY, "/genre/#/series", MatchCode.GenreSeries.value())
        }

        private fun getId(code: Int, uri: Uri): Int
        {
            val segments: List<String>
            val idStr: String
            val matchCode = MatchCode.from(code)
            when (matchCode)
            {
                DBProvider.MatchCode.ProviderByID, DBProvider.MatchCode.SeriesByID, DBProvider.MatchCode.ChapterByID, DBProvider.MatchCode.PageByID -> return Integer.parseInt(uri.lastPathSegment)
                DBProvider.MatchCode.ProviderSeries, DBProvider.MatchCode.SeriesChapters, DBProvider.MatchCode.ChapterPages, DBProvider.MatchCode.PageHeritage, DBProvider.MatchCode.SeriesGenres,
                DBProvider.MatchCode.GenreSeries ->
                {
                    segments = uri.pathSegments
                    idStr = segments[segments.size - 2]
                    return Integer.parseInt(idStr)
                }
                DBProvider.MatchCode.PrevChapterInSeries, DBProvider.MatchCode.NextChapterInSeries, DBProvider.MatchCode.PrevPagesInChapter, DBProvider.MatchCode.NextPagesInChapter ->
                {
                    segments = uri.pathSegments
                    idStr = segments[segments.size - 4]
                    return Integer.parseInt(idStr)
                }
                else -> return -1
            }
        }
    }
}
