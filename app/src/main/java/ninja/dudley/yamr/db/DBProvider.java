package ninja.dudley.yamr.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.List;

import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Genre;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.model.Series;

/**
 * Created by mdudley on 5/21/15.
 */
public class DBProvider extends ContentProvider
{
    private DBHelper dbh;

    private static final UriMatcher matcher;

    // Java you are the fucking dumbest. I hate you with a passion. You never let me do what I want to do.
    // Android you're almost as bad! I don't give a flying fuck what the number I put in here is. All I care is that it's unique.
    private static int lastCode = 0;
    private enum MatchCode
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
        PrevPageInChapter(lastCode++),
        NextPageInChapter(lastCode++),
        PageHeritage(lastCode++),
        GenreMatch(lastCode++),
        GenreSeries(lastCode++),
        SeriesGenreRelator(lastCode++);

        private final int code;

        MatchCode(int code)
        {
            this.code = code;
        }

        @Override
        public String toString()
        {
            return Integer.toString(code);
        }

        public int val()
        {
            return code;
        }

        public static MatchCode from(int code)
        {
            for (MatchCode matchCode : MatchCode.values())
            {
                if (matchCode.val() == code)
                {
                    return matchCode;
                }
            }
            throw new AssertionError("Invalid MatchCode " + code);
        }
    }


    // Matcher codes ------------------------------------------------------

    static
    {
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(DBHelper.AUTHORITY, "/provider", MatchCode.ProviderMatch.val());
        matcher.addURI(DBHelper.AUTHORITY, "/provider/all", MatchCode.ProviderAll.val());
        matcher.addURI(DBHelper.AUTHORITY, "/provider/#", MatchCode.ProviderByID.val());
        matcher.addURI(DBHelper.AUTHORITY, "/provider/#/series", MatchCode.ProviderSeries.val());
        matcher.addURI(DBHelper.AUTHORITY, "/series", MatchCode.SeriesMatch.val());
        matcher.addURI(DBHelper.AUTHORITY, "/series/favorites", MatchCode.SeriesFavorites.val());
        matcher.addURI(DBHelper.AUTHORITY, "/series/#", MatchCode.SeriesByID.val());
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters", MatchCode.SeriesChapters.val());
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters/*/prev", MatchCode.PrevChapterInSeries.val());
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters/*/next", MatchCode.NextChapterInSeries.val());
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/genres", MatchCode.SeriesGenres.val());
        matcher.addURI(DBHelper.AUTHORITY, "/chapter", MatchCode.ChapterMatch.val());
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#", MatchCode.ChapterByID.val());
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages", MatchCode.ChapterPages.val());
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages/*/prev", MatchCode.PrevPageInChapter.val());
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages/*/next", MatchCode.NextPageInChapter.val());
        matcher.addURI(DBHelper.AUTHORITY, "/page", MatchCode.PageMatch.val());
        matcher.addURI(DBHelper.AUTHORITY, "/page/#", MatchCode.PageByID.val());
        matcher.addURI(DBHelper.AUTHORITY, "/page/#/heritage", MatchCode.PageHeritage.val());
        matcher.addURI(DBHelper.AUTHORITY, "/genre", MatchCode.GenreMatch.val());
        matcher.addURI(DBHelper.AUTHORITY, "/genre/relator", MatchCode.SeriesGenreRelator.val());
        matcher.addURI(DBHelper.AUTHORITY, "/genre/#/series", MatchCode.GenreSeries.val());
    }

    private static int getId(int code, Uri uri)
    {
        List<String> segments;
        String idStr;
        MatchCode matchCode = MatchCode.from(code);
        switch (matchCode)
        {
            case ProviderByID:
            case SeriesByID:
            case ChapterByID:
            case PageByID:
                return Integer.parseInt(uri.getLastPathSegment());
            case ProviderSeries:
            case SeriesChapters:
            case ChapterPages:
            case PageHeritage:
                segments = uri.getPathSegments();
                idStr = segments.get(segments.size() - 2);
                return Integer.parseInt(idStr);
            case PrevChapterInSeries:
            case NextChapterInSeries:
            case PrevPageInChapter:
            case NextPageInChapter:
                segments = uri.getPathSegments();
                idStr = segments.get(segments.size() - 4);
                return Integer.parseInt(idStr);
            default:
                return -1;
        }
    }

    @Override
    public boolean onCreate()
    {
        dbh = new DBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        SQLiteDatabase db = dbh.getReadableDatabase();
        int code = matcher.match(uri);
        MatchCode matchCode = MatchCode.from(code);
        switch (matchCode)
        {
            case ProviderMatch:
                return db.query(Provider.tableName,
                        DBHelper.projections.get(Provider.tableName),
                        Provider.urlCol + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case ProviderByID:
                return db.query(Provider.tableName,
                        DBHelper.projections.get(Provider.tableName),
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null,
                        "1"
                );
            case ProviderSeries:
                String querySelection = Series.providerIdCol + "=?";
                if (selection != null)
                {
                    querySelection += " and " + selection;
                }
                String[] querySelectionArgs;
                if (selectionArgs == null)
                {
                    querySelectionArgs = new String[]{Integer.toString(getId(code, uri))};
                }
                else
                {
                    querySelectionArgs = new String[]{Integer.toString(getId(code, uri)), selectionArgs[0]};
                }
                return db.query(Series.tableName,
                        DBHelper.projections.get(Series.tableName),
                        querySelection,
                        querySelectionArgs,
                        null,
                        null,
                        sortOrder == null ? Series.nameCol : sortOrder
                );
            case ProviderAll:
                return db.query(Provider.tableName,
                        DBHelper.projections.get(Provider.tableName),
                        null,
                        null,
                        null,
                        null,
                        sortOrder
                );
            case PrevChapterInSeries:
                String chapterNumberStringForPrev = uri.getPathSegments().get(uri.getPathSegments().size() - 2);
                float chapterNumberForPrev = Float.parseFloat(chapterNumberStringForPrev);
                return db.query(Chapter.tableName,
                        DBHelper.projections.get(Chapter.tableName),
                        Chapter.seriesIdCol + "=? and " + Chapter.numberCol + "<?",
                        new String[]{Integer.toString(getId(code, uri)), Float.toString(chapterNumberForPrev)},
                        null,
                        null,
                        Chapter.numberCol + " desc",
                        "1"
                );
            case NextChapterInSeries:
                String chapterNumberStringForNext = uri.getPathSegments().get(uri.getPathSegments().size() - 2);
                float chapterNumberForNext = Float.parseFloat(chapterNumberStringForNext);
                return db.query(Chapter.tableName,
                        DBHelper.projections.get(Chapter.tableName),
                        Chapter.seriesIdCol + "=? and " + Chapter.numberCol + ">?",
                        new String[]{Integer.toString(getId(code, uri)), Float.toString(chapterNumberForNext)},
                        null,
                        null,
                        Chapter.numberCol + " asc",
                        "1"
                );
            case SeriesMatch:
                return db.query(Series.tableName,
                        DBHelper.projections.get(Series.tableName),
                        Series.urlCol + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case SeriesByID:
                return db.query(Series.tableName,
                        DBHelper.projections.get(Series.tableName),
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null,
                        "1"
                );
            case SeriesChapters:
                return db.query(Chapter.tableName,
                        DBHelper.projections.get(Chapter.tableName),
                        Chapter.seriesIdCol + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        sortOrder == null ? Chapter.numberCol : sortOrder
                );
            case SeriesFavorites:
                return db.query(Series.tableName,
                        DBHelper.projections.get(Series.tableName),
                        Series.favoriteCol + " > 0",
                        null,
                        null,
                        null,
                        sortOrder
                );
            case SeriesGenres:
                return db.query(DBHelper.SeriesGenreEntry.TABLE_NAME,
                        DBHelper.SeriesGenreEntry.projection,
                        DBHelper.SeriesGenreEntry.COLUMN_SERIES_ID + " =?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null
                );
            case GenreSeries:
                return db.query(DBHelper.SeriesGenreEntry.TABLE_NAME,
                        DBHelper.SeriesGenreEntry.projection,
                        DBHelper.SeriesGenreEntry.COLUMN_GENRE_ID + " =?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null
                );
            case ChapterMatch:
                return db.query(Chapter.tableName,
                        DBHelper.projections.get(Chapter.tableName),
                        Chapter.urlCol + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case ChapterByID:
                return db.query(Chapter.tableName,
                        DBHelper.projections.get(Chapter.tableName),
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null,
                        "1"
                );
            case ChapterPages:
                return db.query(Page.tableName,
                        DBHelper.projections.get(Page.tableName),
                        Page.chapterIdCol + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        sortOrder == null ? Page.numberCol : sortOrder
                );
            case PrevPageInChapter:
                String pageNumberStringForPrev = uri.getPathSegments().get(uri.getPathSegments().size() - 2);
                float pageNumberForPrev = Float.parseFloat(pageNumberStringForPrev);
                return db.query(Page.tableName,
                        DBHelper.projections.get(Page.tableName),
                        Page.chapterIdCol + "=? and " + Page.numberCol + "<?",
                        new String[]{Integer.toString(getId(code, uri)), Float.toString(pageNumberForPrev)},
                        null,
                        null,
                        Page.numberCol + " desc",
                        "1"
                );
            case NextPageInChapter:
                String pageNumberStringNext = uri.getPathSegments().get(uri.getPathSegments().size() - 2);
                float pageNumberNext = Float.parseFloat(pageNumberStringNext);
                return db.query(Page.tableName,
                        DBHelper.projections.get(Page.tableName),
                        Page.chapterIdCol + "=? and " + Page.numberCol + ">?",
                        new String[]{Integer.toString(getId(code, uri)), Float.toString(pageNumberNext)},
                        null,
                        null,
                        Page.numberCol + " asc",
                        "1"
                );
            case PageMatch:
                return db.query(Page.tableName,
                        DBHelper.projections.get(Page.tableName),
                        Page.urlCol + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case PageByID:
                return db.query(Page.tableName,
                        DBHelper.projections.get(Page.tableName),
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null,
                        "1"
                );
            case PageHeritage:
                return db.query(DBHelper.PageHeritageViewEntry.TABLE_NAME,
                        DBHelper.PageHeritageViewEntry.projection,
                        DBHelper.PageHeritageViewEntry.COLUMN_PAGE_ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null,
                        "1"
                );
            case GenreMatch:
                return db.query(Genre.tableName,
                        DBHelper.projections.get(Genre.tableName),
                        Genre.nameCol + "=?",
                        selectionArgs,
                        null,
                        null,
                        null,
                        "1"
                );
            default:
                throw new IllegalArgumentException("Invalid query uri: " + uri.toString());
        }
    }

    @Override
    public String getType(Uri uri)
    {
        int code = matcher.match(uri);
        MatchCode matchCode = MatchCode.from(code);
        switch (matchCode)
        {
            case ProviderMatch:
            case ProviderByID:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + Provider.tableName;
            case ProviderSeries:
            case SeriesFavorites:
            case GenreSeries:
                return "vnd.android.cursor.dir/vnd.dudley.ninja.yamr.db.DBProvider." + Series.tableName;
            case SeriesMatch:
            case SeriesByID:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + Series.tableName;
            case SeriesChapters:
                return "vnd.android.cursor.dir/vnd.dudley.ninja.yamr.db.DBProvider." + Chapter.tableName;
            case ChapterMatch:
            case ChapterByID:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + Chapter.tableName;
            case ChapterPages:
                return "vnd.android.cursor.dir/vnd.dudley.ninja.yamr.db.DBProvider." + Page.tableName;
            case PageMatch:
            case PageByID:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + Page.tableName;
            case GenreMatch:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + Genre.tableName;
            case SeriesGenres:
                return "vnd.android.cursor.dir/vnd.dudley.ninja.yamr.db.DBProvider." + Genre.tableName;
            default:
                throw new IllegalArgumentException("Invalid uri: " + uri.toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        SQLiteDatabase db = dbh.getWritableDatabase();
        long id;
        Uri inserted;
        int code = matcher.match(uri);
        MatchCode matchCode = MatchCode.from(code);
        switch (matchCode)
        {
            case ProviderMatch:
                id = db.insert(Provider.tableName, null, values);
                inserted = Provider.uri((int) id);
                return inserted;
            case SeriesMatch:
                id = db.insert(Series.tableName, null, values);
                inserted = Series.uri((int) id);
                Uri providerSeries = Provider.series(getId(code, uri));
                getContext().getContentResolver().notifyChange(providerSeries, null);
                return inserted;
            case ChapterMatch:
                id = db.insert(Chapter.tableName, null, values);
                inserted = Chapter.uri((int) id);
                Uri seriesChapters = Series.chapters(getId(code, uri));
                getContext().getContentResolver().notifyChange(seriesChapters, null);
                return inserted;
            case PageMatch:
                id = db.insert(Page.tableName, null, values);
                inserted = Page.uri((int) id);
                Uri chapterPages = Chapter.pages(getId(code, uri));
                getContext().getContentResolver().notifyChange(chapterPages, null);
                return inserted;
            case GenreMatch:
                id = db.insert(Genre.tableName, null, values);
                inserted = Genre.uri((int) id);
                return inserted;
            case SeriesGenreRelator:
                db.insert(DBHelper.SeriesGenreEntry.TABLE_NAME, null, values);
                return null;
            default:
                throw new IllegalArgumentException("Invalid insert uri: " + uri.toString());
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        SQLiteDatabase db = dbh.getWritableDatabase();
        int code = matcher.match(uri);
        MatchCode matchCode = MatchCode.from(code);
        switch (matchCode)
        {
            case ProviderByID:
                return db.delete(Provider.tableName,
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case SeriesByID:
                return db.delete(Series.tableName,
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case ChapterByID:
                return db.delete(Chapter.tableName,
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case PageByID:
                return db.delete(Page.tableName,
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            default:
                throw new IllegalArgumentException("Invalid delete uri : " + uri.toString());
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        SQLiteDatabase db = dbh.getWritableDatabase();
        int code = matcher.match(uri);
        MatchCode matchCode = MatchCode.from(code);
        switch (matchCode)
        {
            case ProviderByID:
                return db.update(Provider.tableName,
                        values,
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case SeriesByID:
                return db.update(Series.tableName,
                        values,
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case ChapterByID:
                return db.update(Chapter.tableName,
                        values,
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case PageByID:
                return db.update(Page.tableName,
                        values,
                        DBHelper.ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            default:
                throw new IllegalArgumentException("Invalid update uri: " + uri.toString());
        }
    }
}
