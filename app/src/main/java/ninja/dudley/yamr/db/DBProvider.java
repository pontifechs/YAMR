package ninja.dudley.yamr.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

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
    private static final int Chapter = 20;
    private static final int ChapterByID = 21;
    private static final int ChapterPages = 25;
    private static final int Page = 30;
    private static final int PageByID = 31;

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
        matcher.addURI(DBHelper.AUTHORITY, "/chapter", Chapter);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#", ChapterByID);
        matcher.addURI(DBHelper.AUTHORITY, "/chapter/#/pages", ChapterPages);
        matcher.addURI(DBHelper.AUTHORITY, "/page", Page);
        matcher.addURI(DBHelper.AUTHORITY, "/page/#", PageByID);
    }

    private static final int getId(int code, Uri uri)
    {
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
                List<String> segments = uri.getPathSegments();
                String idStr = segments.get(segments.size() - 2);
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
            case ProviderByID:
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
                return db.query(DBHelper.SeriesEntry.TABLE_NAME,
                        DBHelper.SeriesEntry.projection,
                        DBHelper.SeriesEntry.COLUMN_PROVIDER_ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null
                );
            case ProviderAll:
                return db.query(DBHelper.ProviderEntry.TABLE_NAME,
                        DBHelper.ProviderEntry.projection,
                        null,
                        null,
                        null,
                        null,
                        null
                );
            case SeriesByID:
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
                return db.query(DBHelper.ChapterEntry.TABLE_NAME,
                        DBHelper.ChapterEntry.projection,
                        DBHelper.ChapterEntry.COLUMN_SERIES_ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null
                );
            case ChapterByID:
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
                return db.query(DBHelper.PageEntry.TABLE_NAME,
                        DBHelper.PageEntry.projection,
                        DBHelper.PageEntry.COLUMN_CHAPTER_ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
                        null,
                        null,
                        null
                );
            case PageByID:
                return db.query(DBHelper.PageEntry.TABLE_NAME,
                        DBHelper.PageEntry.projection,
                        DBHelper.PageEntry._ID + "=?",
                        new String[]{Integer.toString(getId(code, uri))},
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
            default:
                throw new IllegalArgumentException("Invalid uri: " + uri.toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        SQLiteDatabase db = dbh.getWritableDatabase();
        long id;
        int code = matcher.match(uri);
        switch (code)
        {
            case Provider:
                id = db.insert(DBHelper.ProviderEntry.TABLE_NAME, null, values);
                return Uri.parse("content://" + DBHelper.AUTHORITY + "/provider/" + id);
            case Series:
                id = db.insert(DBHelper.SeriesEntry.TABLE_NAME, null, values);
                return Uri.parse("content://" + DBHelper.AUTHORITY + "/series/" + id);
            case Chapter:
                id = db.insert(DBHelper.ChapterEntry.TABLE_NAME, null, values);
                return Uri.parse("content://" + DBHelper.AUTHORITY + "/chapter/" + id);
            case Page:
                id = db.insert(DBHelper.PageEntry.TABLE_NAME, null, values);
                return Uri.parse("content://" + DBHelper.AUTHORITY + "/page/" + id);
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
            default:
                throw new IllegalArgumentException("Invalid update uri: " + uri.toString());
        }
    }
}
