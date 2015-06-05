package ninja.dudley.yamr.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by mdudley on 5/19/15.
 */
public class DBHelper extends SQLiteOpenHelper
{
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "YAMR.db";

    public static final String AUTHORITY = "ninja.dudley.yamr.db.DBProvider";

    private enum SQLite3Types
    {
        Text,
        Integer,
        Real,
    }

    public static abstract class MangaElementEntry implements BaseColumns
    {
        public static final String COLUMN_URL = "url";
        public static final SQLite3Types COLUMN_URL_TYPE = SQLite3Types.Text;
        public static final String COLUMN_FULLY_PARSED = "fully_parsed";
        public static final SQLite3Types COLUMN_FULLY_PARSED_TYPE = SQLite3Types.Integer;
    }

    public static abstract class ProviderEntry extends MangaElementEntry
    {
        public static final String TABLE_NAME = "provider";

        public static final String COLUMN_NAME = "name";
        public static final SQLite3Types COLUMN_NAME_TYPE = SQLite3Types.Text;

        public static final String[] projection;

        static
        {
            projection = new String[]{_ID, COLUMN_URL, COLUMN_FULLY_PARSED, COLUMN_NAME};
        }
    }

    public static abstract class SeriesEntry extends MangaElementEntry
    {
        public static final String TABLE_NAME = "series";

        public static final String COLUMN_NAME = "name";
        public static final SQLite3Types COLUMN_NAME_TYPE = SQLite3Types.Text;
        public static final String COLUMN_PROVIDER_ID = "provider_id";
        public static final SQLite3Types COLUMN_PROVIDER_ID_TYPE = SQLite3Types.Integer;

        public static final String[] projection;

        static
        {
            projection = new String[]{_ID, COLUMN_URL, COLUMN_FULLY_PARSED, COLUMN_NAME, COLUMN_PROVIDER_ID};
        }
    }

    public static abstract class ChapterEntry extends MangaElementEntry
    {
        public static final String TABLE_NAME = "chapter";

        public static final String COLUMN_NAME = "name";
        public static final SQLite3Types COLUMN_NAME_TYPE = SQLite3Types.Text;
        public static final String COLUMN_NUMBER = "number";
        public static final SQLite3Types COLUMN_NUMBER_TYPE = SQLite3Types.Real;
        public static final String COLUMN_SERIES_ID = "series_id";
        public static final SQLite3Types COLUMN_SERIES_ID_TYPE = SQLite3Types.Integer;

        public static final String[] projection;

        static
        {
            projection = new String[]{_ID, COLUMN_URL, COLUMN_FULLY_PARSED, COLUMN_NAME, COLUMN_NUMBER, COLUMN_SERIES_ID};
        }
    }

    public static abstract class PageEntry extends MangaElementEntry
    {
        public static final String TABLE_NAME = "page";

        public static final String COLUMN_NUMBER = "number";
        public static final SQLite3Types COLUMN_NUMBER_TYPE = SQLite3Types.Real;
        public static final String COLUMN_IMAGE_URL = "image_url";
        public static final SQLite3Types COLUMN_IMAGE_URL_TYPE = SQLite3Types.Text;
        public static final String COLUMN_IMAGE_PATH = "path";
        public static final SQLite3Types COLUMN_IMAGE_PATH_TYPE = SQLite3Types.Text;
        public static final String COLUMN_CHAPTER_ID = "chapter_id";
        public static final SQLite3Types COLUMN_CHAPTER_ID_TYPE = SQLite3Types.Integer;

        public static final String[] projection;

        static
        {
            projection = new String[]{_ID, COLUMN_URL, COLUMN_FULLY_PARSED, COLUMN_NUMBER, COLUMN_IMAGE_URL, COLUMN_IMAGE_PATH, COLUMN_CHAPTER_ID};
        }
    }

    private static String mangaElementColumns()
    {
        return MangaElementEntry._ID + " INTEGER PRIMARY KEY, " +
                makeColumn(MangaElementEntry.COLUMN_URL, MangaElementEntry.COLUMN_URL_TYPE) +
                makeColumn(MangaElementEntry.COLUMN_FULLY_PARSED, MangaElementEntry.COLUMN_FULLY_PARSED_TYPE);
    }


    private static String makeColumn(String name, SQLite3Types type)
    {
        return makeColumn(name, type, true);
    }

    private static String makeColumn(String name, SQLite3Types type, boolean addComma)
    {
        return name + " " + type.toString() + (addComma ? "," : "");
    }

    private static String makeForeignKey(String keyColumn, String refTable, String refColumn)
    {
        return makeForeignKey(keyColumn, refTable, refColumn, true);
    }

    private static String makeForeignKey(String keyColumn, String refTable, String refColumn, boolean addComma)
    {
        return "FOREIGN KEY(" + keyColumn + ") REFERENCES " + refTable + "(" + refColumn + ") ON DELETE CASCADE" + (addComma ? "," : "");
    }

    private static String uniqueConstraint()
    {
       return " UNIQUE (" + MangaElementEntry._ID + ", " + MangaElementEntry.COLUMN_URL +") ON CONFLICT ABORT";
    }

    public DBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String providerCreate = "CREATE TABLE " + ProviderEntry.TABLE_NAME + " (" +
                mangaElementColumns() +
                makeColumn(ProviderEntry.COLUMN_NAME, ProviderEntry.COLUMN_NAME_TYPE) +
                uniqueConstraint() +
                ")";
        db.execSQL(providerCreate);

        String seriesCreate = "CREATE TABLE " + SeriesEntry.TABLE_NAME + " (" +
               mangaElementColumns() +
                makeColumn(SeriesEntry.COLUMN_NAME, SeriesEntry.COLUMN_NAME_TYPE) +
                makeColumn(SeriesEntry.COLUMN_PROVIDER_ID, SeriesEntry.COLUMN_PROVIDER_ID_TYPE) +
                makeForeignKey(SeriesEntry.COLUMN_PROVIDER_ID, ProviderEntry.TABLE_NAME, ProviderEntry._ID) +
                uniqueConstraint() +
                ")";
        db.execSQL(seriesCreate);

        String chapterCreate = "CREATE TABLE " + ChapterEntry.TABLE_NAME + " (" +
                mangaElementColumns() +
                makeColumn(ChapterEntry.COLUMN_NAME, ChapterEntry.COLUMN_NAME_TYPE) +
                makeColumn(ChapterEntry.COLUMN_NUMBER, ChapterEntry.COLUMN_NUMBER_TYPE) +
                makeColumn(ChapterEntry.COLUMN_SERIES_ID, ChapterEntry.COLUMN_SERIES_ID_TYPE) +
                makeForeignKey(ChapterEntry.COLUMN_SERIES_ID, SeriesEntry.TABLE_NAME, SeriesEntry._ID) +
                uniqueConstraint() +
                ")";
        db.execSQL(chapterCreate);

        String pageCreate = "CREATE TABLE " + PageEntry.TABLE_NAME + " (" +
                mangaElementColumns() +
                makeColumn(PageEntry.COLUMN_NUMBER, PageEntry.COLUMN_NUMBER_TYPE) +
                makeColumn(PageEntry.COLUMN_IMAGE_URL, PageEntry.COLUMN_IMAGE_URL_TYPE) +
                makeColumn(PageEntry.COLUMN_IMAGE_PATH, PageEntry.COLUMN_IMAGE_PATH_TYPE) +
                makeColumn(PageEntry.COLUMN_CHAPTER_ID, PageEntry.COLUMN_CHAPTER_ID_TYPE) +
                makeForeignKey(PageEntry.COLUMN_CHAPTER_ID, ChapterEntry.TABLE_NAME, ChapterEntry._ID) +
                uniqueConstraint() +
                ")";
        db.execSQL(pageCreate);

        db.execSQL("INSERT INTO provider (url, name) values (\"http://www.mangapanda.com/alphabetical\", \"MangaPanda\")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS " + PageEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ChapterEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SeriesEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ProviderEntry.TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db)
    {
        super.onOpen(db);
        if (!db.isReadOnly())
        {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }
}
