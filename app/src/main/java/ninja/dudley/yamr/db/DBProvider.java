package ninja.dudley.yamr.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.util.List;

/**
 * Created by mdudley on 5/21/15.
 */
public class DBProvider extends ContentProvider
{
    private DBHelper dbh;

    private static final UriMatcher matcher;

    // Matcher codes ------------------------------------------------------
    private static final int Provider = 0;
    private static final int ProviderByID = 1;
    private static final int ProviderSeries = 5;
    private static final int ProviderAll = 9;
    private static final int Series = 10;
    private static final int SeriesByID = 11;
    private static final int SeriesChapters = 15;
    private static final int SeriesFavorites = 16;
    private static final int SeriesGenres = 17;
    private static final int Chapter = 20;
    private static final int ChapterByID = 21;
    private static final int PrevChapterInSeries = 22;
    private static final int NextChapterInSeries = 23;
    private static final int ChapterPages = 25;
    private static final int Page = 30;
    private static final int PageByID = 31;
    private static final int PrevPageInChapter = 32;
    private static final int NextPageInChapter = 33;
    private static final int PageHeritage = 39;
    private static final int Bookmark = 40;
    private static final int BookmarkBySeriesId = 41;
    private static final int Bookmarks = 42;
    private static final int Genre = 50;
    private static final int GenreSeries = 51;
    private static final int SeriesGenreRelator = 60;

    static
    {
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(DBHelper.AUTHORITY, "/provider", Provider);
        matcher.addURI(DBHelper.AUTHORITY, "/provider/all", ProviderAll);
        matcher.addURI(DBHelper.AUTHORITY, "/provider/#", ProviderByID);
        matcher.addURI(DBHelper.AUTHORITY, "/provider/#/series", ProviderSeries);
        matcher.addURI(DBHelper.AUTHORITY, "/series", Series);
        matcher.addURI(DBHelper.AUTHORITY, "/series/#", SeriesByID);
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters", SeriesChapters);
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters/*/prev", PrevChapterInSeries);
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/chapters/*/next", NextChapterInSeries);
        matcher.addURI(DBHelper.AUTHORITY, "/series/favorites", SeriesFavorites);
        matcher.addURI(DBHelper.AUTHORITY, "/series/#/genres", SeriesGenres);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter", Chapter);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#", ChapterByID);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages", ChapterPages);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages/*/prev", PrevPageInChapter);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages/*/next", NextPageInChapter);
        matcher.addURI(DBHelper.AUTHORITY, "/page", Page);
        matcher.addURI(DBHelper.AUTHORITY, "/page/#", PageByID);
        matcher.addURI(DBHelper.AUTHORITY, "/page/#/heritage", PageHeritage);
        matcher.addURI(DBHelper.AUTHORITY, "/bookmark", Bookmark);
        matcher.addURI(DBHelper.AUTHORITY, "/bookmark/all", Bookmarks);
        matcher.addURI(DBHelper.AUTHORITY, "/bookmark/#", BookmarkBySeriesId);
        matcher.addURI(DBHelper.AUTHORITY, "/genre", Genre);
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
            case BookmarkBySeriesId:
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
            case Provider:
                Log.d("Query", "Provider " + code + " : " + Provider);
                return db.query(DBHelper.ProviderEntry.TABLE_NAME,
                        DBHelper.ProviderEntry.projection,
                        DBHelper.ProviderEntry.COLUMN_URL + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case ProviderByID:
                Log.d("Query", "ProviderByID " + code + " : " + ProviderByID);
                return db.query(DBHelper.ProviderEntry.TABLE_NAME,
                        DBHelper.ProviderEntry.projection,
                        DBHelper.ProviderEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null,
                        "1"
                );
            case ProviderSeries:
                Log.d("Query", "ProviderSeries " + code + " : " + ProviderSeries);
                String querySelection = DBHelper.SeriesEntry.COLUMN_PROVIDER_ID + "=?";
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

                return db.query(DBHelper.SeriesEntry.TABLE_NAME,
                        DBHelper.SeriesEntry.projection,
                        querySelection,
                        querySelectionArgs,
                        null,
                        null,
                        sortOrder == null ? DBHelper.SeriesEntry.COLUMN_NAME : sortOrder
                );
            case ProviderAll:
                Log.d("Query", "ProviderAll " + code + " : " + ProviderAll);
                return db.query(DBHelper.ProviderEntry.TABLE_NAME,
                        DBHelper.ProviderEntry.projection,
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
                return db.query(DBHelper.ChapterEntry.TABLE_NAME,
                        DBHelper.ChapterEntry.projection,
                        DBHelper.ChapterEntry.COLUMN_SERIES_ID + "=? and " + DBHelper.ChapterEntry.COLUMN_NUMBER + "<?",
                        new String[]{Integer.toString(getId(code, uri)), Float.toString(chapterNumberForPrev)},
                        null,
                        null,
                        DBHelper.ChapterEntry.COLUMN_NUMBER + " desc",
                        "1"
                );
            case NextChapterInSeries:
                Log.d("Query", "NextChapterInSeries " + code + " : " + NextChapterInSeries);
                String chapterNumberStringForNext = uri.getPathSegments().get(uri.getPathSegments().size() - 2);
                float chapterNumberForNext = Float.parseFloat(chapterNumberStringForNext);
                return db.query(DBHelper.ChapterEntry.TABLE_NAME,
                        DBHelper.ChapterEntry.projection,
                        DBHelper.ChapterEntry.COLUMN_SERIES_ID + "=? and " + DBHelper.ChapterEntry.COLUMN_NUMBER + ">?",
                        new String[]{Integer.toString(getId(code, uri)), Float.toString(chapterNumberForNext)},
                        null,
                        null,
                        DBHelper.ChapterEntry.COLUMN_NUMBER + " asc",
                        "1"
                );
            case Series:
                Log.d("Query", "Series " + code + " : " + Series);
                return db.query(DBHelper.SeriesEntry.TABLE_NAME,
                        DBHelper.SeriesEntry.projection,
                        DBHelper.SeriesEntry.COLUMN_URL + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case SeriesByID:
                Log.d("Query", "SeriesByID " + code + " : " + SeriesByID);
                return db.query(DBHelper.SeriesEntry.TABLE_NAME,
                        DBHelper.SeriesEntry.projection,
                        DBHelper.SeriesEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null,
                        "1"
                );
            case SeriesChapters:
                Log.d("Query", "SeriesChapters " + code + " : " + SeriesChapters);
                return db.query(DBHelper.ChapterEntry.TABLE_NAME,
                        DBHelper.ChapterEntry.projection,
                        DBHelper.ChapterEntry.COLUMN_SERIES_ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        sortOrder == null ? DBHelper.ChapterEntry.COLUMN_NUMBER : sortOrder
                );
            case SeriesFavorites:
                Log.d("Query", "SeriesFavorites " + code + " : " + SeriesFavorites);
                return db.query(DBHelper.SeriesEntry.TABLE_NAME,
                        DBHelper.SeriesEntry.projection,
                        DBHelper.SeriesEntry.COLUMN_FAVORITE + " > 0",
                        null,
                        null,
                        null,
                        sortOrder
                );
            case SeriesGenres:
                Log.d("Query", "SeriesGenres " + code + " : " + SeriesGenres);
                return db.query(DBHelper.SeriesGenreEntry.TABLE_NAME,
                        DBHelper.GenreEntry.projection,
                        DBHelper.SeriesGenreEntry.COLUMN_SERIES_ID + " =?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null
                );
            case GenreSeries:
                Log.d("Query", "GenreSeries " + code + " : " + GenreSeries);
                return db.query(DBHelper.SeriesGenreEntry.TABLE_NAME,
                        DBHelper.SeriesEntry.projection,
                        DBHelper.GenreEntry._ID + " =?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null
                );
            case Chapter:
                Log.d("Query", "Chapter " + code + " : " + Chapter);
                return db.query(DBHelper.ChapterEntry.TABLE_NAME,
                        DBHelper.ChapterEntry.projection,
                        DBHelper.ChapterEntry.COLUMN_URL + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case ChapterByID:
                Log.d("Query", "ChapterByID " + code + " : " + ChapterByID);
                return db.query(DBHelper.ChapterEntry.TABLE_NAME,
                        DBHelper.ChapterEntry.projection,
                        DBHelper.ChapterEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null,
                        "1"
                );
            case ChapterPages:
                Log.d("Query", "ChapterPages " + code + " : " + ChapterPages );
                return db.query(DBHelper.PageEntry.TABLE_NAME,
                        DBHelper.PageEntry.projection,
                        DBHelper.PageEntry.COLUMN_CHAPTER_ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        sortOrder == null ? DBHelper.PageEntry.COLUMN_NUMBER : sortOrder
                );
            case PrevPageInChapter:
                Log.d("Query", "PrevPageInChapter " + code + " : " + PrevPageInChapter);
                String pageNumberStringForPrev = uri.getPathSegments().get(uri.getPathSegments().size() - 2);
                float pageNumberForPrev = Float.parseFloat(pageNumberStringForPrev);
                return db.query(DBHelper.PageEntry.TABLE_NAME,
                        DBHelper.PageEntry.projection,
                        DBHelper.PageEntry.COLUMN_CHAPTER_ID + "=? and " + DBHelper.PageEntry.COLUMN_NUMBER + "<?",
                        new String[]{Integer.toString(getId(code, uri)), Float.toString(pageNumberForPrev)},
                        null,
                        null,
                        DBHelper.PageEntry.COLUMN_NUMBER + " desc",
                        "1"
                );
            case NextPageInChapter:
                Log.d("Query", "NextPageInChapter " + code + " : " + NextPageInChapter);
                String pageNumberStringNext = uri.getPathSegments().get(uri.getPathSegments().size() - 2);
                float pageNumberNext = Float.parseFloat(pageNumberStringNext);
                return db.query(DBHelper.PageEntry.TABLE_NAME,
                        DBHelper.PageEntry.projection,
                        DBHelper.PageEntry.COLUMN_CHAPTER_ID + "=? and " + DBHelper.PageEntry.COLUMN_NUMBER + ">?",
                        new String[]{Integer.toString(getId(code, uri)), Float.toString(pageNumberNext)},
                        null,
                        null,
                        DBHelper.PageEntry.COLUMN_NUMBER + " asc",
                        "1"
                );
            case Page:
                Log.d("Query", "Page " + code + " : " + Page);
                return db.query(DBHelper.PageEntry.TABLE_NAME,
                        DBHelper.PageEntry.projection,
                        DBHelper.PageEntry.COLUMN_URL + "=?",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
            case PageByID:
                Log.d("Query", "PageByID " + code + " : " + PageByID);
                return db.query(DBHelper.PageEntry.TABLE_NAME,
                        DBHelper.PageEntry.projection,
                        DBHelper.PageEntry._ID + "=?",
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
            case BookmarkBySeriesId:
                Log.d("Query", "BookmarkBySeriesId " + code + " : " + BookmarkBySeriesId);
                return db.query(DBHelper.BookmarkEntry.TABLE_NAME,
                        DBHelper.BookmarkEntry.projection,
                        DBHelper.BookmarkEntry.COLUMN_SERIES_ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null
                );
            case Bookmarks:
                Log.d("Query", "Bookmarks " + code + " : " + Bookmarks);
                return db.query(DBHelper.BookmarkEntry.TABLE_NAME,
                        DBHelper.BookmarkEntry.projection,
                        null,
                        null,
                        null,
                        null,
                        sortOrder
                );
            case Genre:
                Log.d("Query", "Genre " + code + " : " + Genre);
                return db.query(DBHelper.GenreEntry.TABLE_NAME,
                        DBHelper.GenreEntry.projection,
                        DBHelper.GenreEntry.COLUMN_NAME + "=?",
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
            case Provider:
            case ProviderByID:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.ProviderEntry.TABLE_NAME;
            case ProviderSeries:
            case SeriesFavorites:
            case GenreSeries:
                return "vnd.android.cursor.dir/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.SeriesEntry.TABLE_NAME;
            case Series:
            case SeriesByID:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.SeriesEntry.TABLE_NAME;
            case SeriesChapters:
                return "vnd.android.cursor.dir/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.ChapterEntry.TABLE_NAME;
            case Chapter:
            case ChapterByID:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.ChapterEntry.TABLE_NAME;
            case ChapterPages:
                return "vnd.android.cursor.dir/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.PageEntry.TABLE_NAME;
            case Page:
            case PageByID:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.PageEntry.TABLE_NAME;
            case Bookmark:
            case BookmarkBySeriesId:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.BookmarkEntry.TABLE_NAME;
            case Bookmarks:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.BookmarkEntry.TABLE_NAME;
            case Genre:
                return "vnd.android.cursor.item/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.GenreEntry.TABLE_NAME;
            case SeriesGenres:
                return "vnd.android.cursor.dir/vnd.dudley.ninja.yamr.db.DBProvider." + DBHelper.GenreEntry.TABLE_NAME;
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
            case Provider:
                id = db.insert(DBHelper.ProviderEntry.TABLE_NAME, null, values);
                inserted = ninja.dudley.yamr.model.Provider.uri((int) id);
                return inserted;
            case Series:
                id = db.insert(DBHelper.SeriesEntry.TABLE_NAME, null, values);
                inserted = ninja.dudley.yamr.model.Series.uri((int) id);
                Uri providerSeries = ninja.dudley.yamr.model.Provider.uri(getId(code, uri)).buildUpon().appendPath("series").build();
                getContext().getContentResolver().notifyChange(providerSeries, null);
                return inserted;
            case Chapter:
                id = db.insert(DBHelper.ChapterEntry.TABLE_NAME, null, values);
                inserted = ninja.dudley.yamr.model.Chapter.uri((int) id);
                Uri seriesChapters = ninja.dudley.yamr.model.Series.uri(getId(code, uri)).buildUpon().appendPath("chapters").build();
                getContext().getContentResolver().notifyChange(seriesChapters, null);
                return inserted;
            case Page:
                id = db.insert(DBHelper.PageEntry.TABLE_NAME, null, values);
                inserted = ninja.dudley.yamr.model.Page.uri((int) id);
                Uri chapterPages = ninja.dudley.yamr.model.Chapter.uri(getId(code, uri)).buildUpon().appendPath("pages").build();
                getContext().getContentResolver().notifyChange(chapterPages, null);
                return inserted;
            case BookmarkBySeriesId:
                id = db.insert(DBHelper.BookmarkEntry.TABLE_NAME, null, values);
                inserted = ninja.dudley.yamr.model.Bookmark.uri((int) id);
                Uri bookmarks = ninja.dudley.yamr.model.Bookmark.uri(getId(code, uri)).buildUpon().appendPath("s").build();
                getContext().getContentResolver().notifyChange(bookmarks, null);
                return inserted;
            case Genre:
                id = db.insert(DBHelper.GenreEntry.TABLE_NAME, null, values);
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
                return db.delete(DBHelper.ProviderEntry.TABLE_NAME,
                        DBHelper.ProviderEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case SeriesByID:
                return db.delete(DBHelper.SeriesEntry.TABLE_NAME,
                        DBHelper.SeriesEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case ChapterByID:
                return db.delete(DBHelper.ChapterEntry.TABLE_NAME,
                        DBHelper.ChapterEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case PageByID:
                return db.delete(DBHelper.PageEntry.TABLE_NAME,
                        DBHelper.PageEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case BookmarkBySeriesId:
                return db.delete(DBHelper.BookmarkEntry.TABLE_NAME,
                        DBHelper.BookmarkEntry.COLUMN_SERIES_ID + "=?",
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
                return db.update(DBHelper.ProviderEntry.TABLE_NAME,
                        values,
                        DBHelper.ProviderEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case SeriesByID:
                return db.update(DBHelper.SeriesEntry.TABLE_NAME,
                        values,
                        DBHelper.SeriesEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case ChapterByID:
                return db.update(DBHelper.ChapterEntry.TABLE_NAME,
                        values,
                        DBHelper.ChapterEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case PageByID:
                return db.update(DBHelper.PageEntry.TABLE_NAME,
                        values,
                        DBHelper.PageEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            case BookmarkBySeriesId:
                return db.update(DBHelper.BookmarkEntry.TABLE_NAME,
                        values,
                        DBHelper.BookmarkEntry.COLUMN_SERIES_ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))}
                );
            default:
                throw new IllegalArgumentException("Invalid update uri: " + uri.toString());
        }
    }
}
