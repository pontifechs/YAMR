package ninja.dudley.yamr.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

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

    // Matcher codes ------------------------------------------------------
    private static final int ProviderMatch = 0;
    private static final int ProviderByID = 1;
    private static final int ProviderSeries = 5;
    private static final int ProviderAll = 9;
    private static final int SeriesMatch = 10;
    private static final int SeriesByID = 11;
    private static final int SeriesChapters = 15;
    private static final int SeriesFavorites = 16;
    private static final int SeriesGenres = 17;
    private static final int ChapterMatch = 20;
    private static final int ChapterByID = 21;
    private static final int PrevChapterInSeries = 22;
    private static final int NextChapterInSeries = 23;
    private static final int ChapterPages = 25;
    private static final int PageMatch = 30;
    private static final int PageByID = 31;
    private static final int PrevPageInChapter = 32;
    private static final int NextPageInChapter = 33;
    private static final int PageHeritage = 39;
    private static final int GenreMatch = 50;
    private static final int GenreSeries = 51;
    private static final int SeriesGenreRelator = 60;

    static
    {
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(DBHelper.AUTHORITY, "/provider", ProviderMatch);
        matcher.addURI(DBHelper.AUTHORITY, "/provider/all", ProviderAll);
        matcher.addURI(DBHelper.AUTHORITY, "/provider/#", ProviderByID);
        matcher.addURI(DBHelper.AUTHORITY, "/provider/#/series", ProviderSeries);
        matcher.addURI(DBHelper.AUTHORITY, "/series", SeriesMatch);
        matcher.addURI(DBHelper.AUTHORITY, "/series/favorites", SeriesFavorites);
        matcher.addURI(DBHelper.AUTHORITY, "/series/#", SeriesByID);
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters", SeriesChapters);
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters/*/prev", PrevChapterInSeries);
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters/*/next", NextChapterInSeries);
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/genres", SeriesGenres);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter", ChapterMatch);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#", ChapterByID);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages", ChapterPages);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages/*/prev", PrevPageInChapter);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages/*/next", NextPageInChapter);
        matcher.addURI(DBHelper.AUTHORITY, "/page", PageMatch);
        matcher.addURI(DBHelper.AUTHORITY, "/page/#", PageByID);
        matcher.addURI(DBHelper.AUTHORITY, "/page/#/heritage", PageHeritage);
        matcher.addURI(DBHelper.AUTHORITY, "/genre", GenreMatch);
        matcher.addURI(DBHelper.AUTHORITY, "/genre/relator", SeriesGenreRelator);
        matcher.addURI(DBHelper.AUTHORITY, "/genre/#/series", GenreSeries);
    }

    private static int getId(int code, Uri uri)
    {
        List<String> segments;
        String idStr;
        switch (code)
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
        switch (code)
        {
            case ProviderMatch:
                Log.d("Query", "ProviderMatch " + code + " : " + ProviderMatch);
                return db.query(Provider.tableName,
                        DBHelper.projections.get(Provider.tableName),
                        Provider.urlCol + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case ProviderByID:
                Log.d("Query", "ProviderByID " + code + " : " + ProviderByID);
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
                Log.d("Query", "ProviderSeries " + code + " : " + ProviderSeries);
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
                Log.d("Query", "ProviderAll " + code + " : " + ProviderAll);
                return db.query(Provider.tableName,
                        DBHelper.projections.get(Provider.tableName),
                        null,
                        null,
                        null,
                        null,
                        sortOrder
                );
            case PrevChapterInSeries:
                Log.d("Query", "PrevChapterInSeries " + code + " : " + PrevChapterInSeries);
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
                Log.d("Query", "NextChapterInSeries " + code + " : " + NextChapterInSeries);
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
                Log.d("Query", "SeriesMatch " + code + " : " + SeriesMatch);
                return db.query(Series.tableName,
                        DBHelper.projections.get(Series.tableName),
                        Series.urlCol + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case SeriesByID:
                Log.d("Query", "SeriesByID " + code + " : " + SeriesByID);
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
                Log.d("Query", "SeriesChapters " + code + " : " + SeriesChapters);
                return db.query(Chapter.tableName,
                        DBHelper.projections.get(Chapter.tableName),
                        Chapter.seriesIdCol + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        sortOrder == null ? Chapter.numberCol : sortOrder
                );
            case SeriesFavorites:
                Log.d("Query", "SeriesFavorites " + code + " : " + SeriesFavorites);
                return db.query(Series.tableName,
                        DBHelper.projections.get(Series.tableName),
                        Series.favoriteCol + " > 0",
                        null,
                        null,
                        null,
                        sortOrder
                );
            case SeriesGenres:
                Log.d("Query", "SeriesGenres " + code + " : " + SeriesGenres);
                return db.query(DBHelper.SeriesGenreEntry.TABLE_NAME,
                        DBHelper.SeriesGenreEntry.projection,
                        DBHelper.SeriesGenreEntry.COLUMN_SERIES_ID + " =?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null
                );
            case GenreSeries:
                Log.d("Query", "GenreSeries " + code + " : " + GenreSeries);
                return db.query(DBHelper.SeriesGenreEntry.TABLE_NAME,
                        DBHelper.SeriesGenreEntry.projection,
                        DBHelper.SeriesGenreEntry.COLUMN_GENRE_ID + " =?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null
                );
            case ChapterMatch:
                Log.d("Query", "ChapterMatch " + code + " : " + ChapterMatch);
                return db.query(Chapter.tableName,
                        DBHelper.projections.get(Chapter.tableName),
                        Chapter.urlCol + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case ChapterByID:
                Log.d("Query", "ChapterByID " + code + " : " + ChapterByID);
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
                Log.d("Query", "ChapterPages " + code + " : " + ChapterPages );
                return db.query(Page.tableName,
                        DBHelper.projections.get(Page.tableName),
                        Page.chapterIdCol + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        sortOrder == null ? Page.numberCol : sortOrder
                );
            case PrevPageInChapter:
                Log.d("Query", "PrevPageInChapter " + code + " : " + PrevPageInChapter);
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
                Log.d("Query", "NextPageInChapter " + code + " : " + NextPageInChapter);
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
                Log.d("Query", "PageMatch " + code + " : " + PageMatch);
                return db.query(Page.tableName,
                        DBHelper.projections.get(Page.tableName),
                        Page.urlCol + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case PageByID:
                Log.d("Query", "PageByID " + code + " : " + PageByID);
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
                Log.d("Query", "PageHeritage " + code + " : " + PageHeritage);
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
                Log.d("Query", "GenreMatch " + code + " : " + GenreMatch);
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
        switch (code)
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
        switch (code)
        {
            case ProviderMatch:
                id = db.insert(Provider.tableName, null, values);
                inserted = ninja.dudley.yamr.model.Provider.uri((int) id);
                return inserted;
            case SeriesMatch:
                id = db.insert(Series.tableName, null, values);
                inserted = ninja.dudley.yamr.model.Series.uri((int) id);
                Uri providerSeries = ninja.dudley.yamr.model.Provider.uri(getId(code, uri)).buildUpon().appendPath("series").build();
                getContext().getContentResolver().notifyChange(providerSeries, null);
                return inserted;
            case ChapterMatch:
                id = db.insert(Chapter.tableName, null, values);
                inserted = ninja.dudley.yamr.model.Chapter.uri((int) id);
                Uri seriesChapters = ninja.dudley.yamr.model.Series.uri(getId(code, uri)).buildUpon().appendPath("chapters").build();
                getContext().getContentResolver().notifyChange(seriesChapters, null);
                return inserted;
            case PageMatch:
                id = db.insert(Page.tableName, null, values);
                inserted = ninja.dudley.yamr.model.Page.uri((int) id);
                Uri chapterPages = ninja.dudley.yamr.model.Chapter.uri(getId(code, uri)).buildUpon().appendPath("pages").build();
                getContext().getContentResolver().notifyChange(chapterPages, null);
                return inserted;
            case GenreMatch:
                id = db.insert(Genre.tableName, null, values);
                inserted = ninja.dudley.yamr.model.Genre.uri((int) id);
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
        switch (code)
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
        switch (code)
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
