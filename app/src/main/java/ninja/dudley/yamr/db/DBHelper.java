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
    private Context context;

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "YAMR.db";

    public static final String AUTHORITY = "ninja.dudley.yamr.db.DBProvider";

    private enum SQLite3Types
    {
        Text,
        Integer,
        Real
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
        public static final String COLUMN_NEW_URL = "new_url";
        public static final SQLite3Types COLUMN_NEW_URL_TYPE = SQLite3Types.Text;
        public static final String COLUMN_LATEST_CHAPTER_ID = "latest_chapter_id";
        public static final SQLite3Types COLUMN_LATEST_CHAPTER_ID_TYPE = SQLite3Types.Integer;

        public static final String[] projection;

        static
        {
            projection = new String[]{_ID, COLUMN_URL, COLUMN_FULLY_PARSED, COLUMN_NAME, COLUMN_NEW_URL, COLUMN_LATEST_CHAPTER_ID};
        }
    }

    public static abstract class SeriesEntry extends MangaElementEntry
    {
        public static final String TABLE_NAME = "series";

        public static final String COLUMN_NAME = "name";
        public static final SQLite3Types COLUMN_NAME_TYPE = SQLite3Types.Text;
        public static final String COLUMN_DESCRIPTION = "description";
        public static final SQLite3Types COLUMN_DESCRIPTION_TYPE = SQLite3Types.Text;
        public static final String COLUMN_PROVIDER_ID = "provider_id";
        public static final SQLite3Types COLUMN_PROVIDER_ID_TYPE = SQLite3Types.Integer;
        public static final String COLUMN_FAVORITE = "favorite";
        public static final SQLite3Types COLUMN_FAVORITE_TYPE = SQLite3Types.Integer;
        public static final String COLUMN_ALTERNATE_NAME = "alt_name";
        public static final SQLite3Types COLUMN_ALTERNATE_NAME_TYPE = SQLite3Types.Text;
        public static final String COLUMN_COMPLETE = "complete";
        public static final SQLite3Types COLUMN_COMPLETE_TYPE = SQLite3Types.Integer;
        public static final String COLUMN_AUTHOR = "author";
        public static final SQLite3Types COLUMN_AUTHOR_TYPE = SQLite3Types.Text;
        public static final String COLUMN_ARTIST = "artist";
        public static final SQLite3Types COLUMN_ARTIST_TYPE = SQLite3Types.Text;
        public static final String COLUMN_THUMBNAIL_URL = "thumbnail_url";
        public static final SQLite3Types COLUMN_THUMBNAIL_URL_TYPE = SQLite3Types.Text;
        public static final String COLUMN_THUMBNAIL_PATH = "thumbnail_path";
        public static final SQLite3Types COLUMN_THUMBNAIL_PATH_TYPE = SQLite3Types.Text;

        public static final String[] projection;

        static
        {
            projection = new String[]{_ID, COLUMN_URL, COLUMN_FULLY_PARSED, COLUMN_NAME, COLUMN_DESCRIPTION,
                    COLUMN_PROVIDER_ID, COLUMN_FAVORITE, COLUMN_ALTERNATE_NAME, COLUMN_COMPLETE,
                    COLUMN_AUTHOR, COLUMN_ARTIST, COLUMN_THUMBNAIL_URL, COLUMN_THUMBNAIL_PATH
            };
        }
    }

    public static abstract class SeriesGenreEntry implements BaseColumns
    {
        public static final String TABLE_NAME = "series_genres";

        public static final String COLUMN_SERIES_ID = "series_id";
        public static final SQLite3Types COLUMN_SERIES_ID_TYPE = SQLite3Types.Integer;
        public static final String COLUMN_GENRE_ID = "genre_id";
        public static final SQLite3Types COLUMN_GENRE_ID_TYPE = SQLite3Types.Integer;

        public static final String[] projection;

        static
        {
            projection = new String[]{_ID, COLUMN_SERIES_ID, COLUMN_GENRE_ID};
        }
    }

    public static abstract class GenreEntry implements BaseColumns
    {
        public static final String TABLE_NAME = "genres";

        public static final String COLUMN_NAME = "name";
        public static final SQLite3Types COLUMN_NAME_TYPE = SQLite3Types.Text;

        public static final String[] projection;

        static
        {
            projection = new String[]{_ID, COLUMN_NAME};
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

    public static abstract class PageHeritageViewEntry
    {
        public static final String TABLE_NAME="page_heritage";

        public static final String COLUMN_PROVIDER_NAME = "provider_name";
        public static final SQLite3Types COLUMN_PROVIDER_NAME_TYPE = SQLite3Types.Text;
        public static final String COLUMN_SERIES_NAME = "series_name";
        public static final SQLite3Types COLUMN_SERIES_NAME_TYPE = SQLite3Types.Text;
        public static final String COLUMN_CHAPTER_NUMBER = "chapter_number";
        public static final SQLite3Types COLUMN_CHAPTER_NUMBER_TYPE = SQLite3Types.Real;
        public static final String COLUMN_PAGE_NUMBER = "page_number";
        public static final SQLite3Types COLUMN_PAGE_NUMBER_TYPE = SQLite3Types.Real;
        public static final String COLUMN_PAGE_ID = "page_id";
        public static final SQLite3Types COLUMN_PAGE_ID_TYPE = SQLite3Types.Integer;

        public static final String[] projection;

        static
        {
            projection = new String[]{COLUMN_PROVIDER_NAME, COLUMN_SERIES_NAME, COLUMN_CHAPTER_NUMBER, COLUMN_PAGE_NUMBER, COLUMN_PAGE_ID};
        }
    }

    public static abstract class BookmarkEntry implements BaseColumns
    {
        public static final String TABLE_NAME = "bookmark";

        public static final String COLUMN_SERIES_ID = "series_id";
        public static final SQLite3Types COLUMN_SERIES_ID_TYPE = SQLite3Types.Integer;
        public static final String COLUMN_PAGE_ID = "progress_page_id";
        public static final SQLite3Types COLUMN_PAGE_ID_TYPE = SQLite3Types.Integer;
        public static final String COLUMN_NOTE = "note";
        public static final SQLite3Types COLUMN_NOTE_TYPE = SQLite3Types.Text;

        public static final String[] projection;

        static
        {
            projection = new String[]{_ID, COLUMN_SERIES_ID, COLUMN_PAGE_ID, COLUMN_NOTE};
        }
    }

    public static abstract class SeriesGenreViewEntry extends SeriesEntry
    {
        public static final String TABLE_NAME = "series_genre_view";
    }

    public static abstract class GenreSeriesViewEntry extends GenreEntry
    {
        public static final String TABLE_NAME = "genre_series_view";
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
       return " UNIQUE ("+ MangaElementEntry.COLUMN_URL +") ON CONFLICT ABORT";
    }

    private static String joinedAliasedColumn(String tableName, String fieldName, String alias)
    {
        return tableName + "." + fieldName + " as " + alias;
    }

    private static String joinStatement(String lhsTable, String lhsField, String rhsTable, String rhsField)
    {
        return " join " + rhsTable + " ON " + lhsTable + "." + lhsField + " = " + rhsTable + "." + rhsField;
    }


    public DBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String providerCreate = "CREATE TABLE " + ProviderEntry.TABLE_NAME + " (" +
                mangaElementColumns() +
                makeColumn(ProviderEntry.COLUMN_NAME, ProviderEntry.COLUMN_NAME_TYPE) +
                makeColumn(ProviderEntry.COLUMN_NEW_URL, ProviderEntry.COLUMN_NEW_URL_TYPE) +
                makeColumn(ProviderEntry.COLUMN_LATEST_CHAPTER_ID, ProviderEntry.COLUMN_LATEST_CHAPTER_ID_TYPE) +
                makeForeignKey(ProviderEntry.COLUMN_LATEST_CHAPTER_ID, ChapterEntry.TABLE_NAME, ChapterEntry._ID) +
                uniqueConstraint() +
                ")";
        db.execSQL(providerCreate);

        String seriesCreate = "CREATE TABLE " + SeriesEntry.TABLE_NAME + " (" +
                mangaElementColumns() +
                makeColumn(SeriesEntry.COLUMN_NAME, SeriesEntry.COLUMN_NAME_TYPE) +
                makeColumn(SeriesEntry.COLUMN_DESCRIPTION, SeriesEntry.COLUMN_DESCRIPTION_TYPE) +
                makeColumn(SeriesEntry.COLUMN_PROVIDER_ID, SeriesEntry.COLUMN_PROVIDER_ID_TYPE) +
                makeColumn(SeriesEntry.COLUMN_FAVORITE, SeriesEntry.COLUMN_FAVORITE_TYPE) +
                makeColumn(SeriesEntry.COLUMN_ALTERNATE_NAME, SeriesEntry.COLUMN_ALTERNATE_NAME_TYPE) +
                makeColumn(SeriesEntry.COLUMN_COMPLETE, SeriesEntry.COLUMN_COMPLETE_TYPE) +
                makeColumn(SeriesEntry.COLUMN_AUTHOR, SeriesEntry.COLUMN_AUTHOR_TYPE) +
                makeColumn(SeriesEntry.COLUMN_ARTIST, SeriesEntry.COLUMN_ARTIST_TYPE) +
                makeColumn(SeriesEntry.COLUMN_THUMBNAIL_URL, SeriesEntry.COLUMN_THUMBNAIL_URL_TYPE) +
                makeColumn(SeriesEntry.COLUMN_THUMBNAIL_PATH, SeriesEntry.COLUMN_THUMBNAIL_PATH_TYPE) +
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

        String pageHeritageView = "CREATE VIEW " + PageHeritageViewEntry.TABLE_NAME + " AS " +
               "select " + joinedAliasedColumn(PageEntry.TABLE_NAME, PageEntry._ID, PageHeritageViewEntry.COLUMN_PAGE_ID) + ", "
                         + joinedAliasedColumn(ProviderEntry.TABLE_NAME, ProviderEntry.COLUMN_NAME, PageHeritageViewEntry.COLUMN_PROVIDER_NAME) + ", "
                         + joinedAliasedColumn(SeriesEntry.TABLE_NAME, SeriesEntry.COLUMN_NAME, PageHeritageViewEntry.COLUMN_SERIES_NAME) + ", "
                         + joinedAliasedColumn(ChapterEntry.TABLE_NAME, ChapterEntry.COLUMN_NUMBER, PageHeritageViewEntry.COLUMN_CHAPTER_NUMBER) + ", "
                         + joinedAliasedColumn(PageEntry.TABLE_NAME, PageEntry.COLUMN_NUMBER, PageHeritageViewEntry.COLUMN_PAGE_NUMBER) +
               " from " + ProviderEntry.TABLE_NAME + joinStatement(ProviderEntry.TABLE_NAME, ProviderEntry._ID, SeriesEntry.TABLE_NAME, SeriesEntry.COLUMN_PROVIDER_ID)
                       + joinStatement(SeriesEntry.TABLE_NAME, SeriesEntry._ID, ChapterEntry.TABLE_NAME, ChapterEntry.COLUMN_SERIES_ID)
                       + joinStatement(ChapterEntry.TABLE_NAME, ChapterEntry._ID, PageEntry.TABLE_NAME, PageEntry.COLUMN_CHAPTER_ID)
                ;
        db.execSQL(pageHeritageView);

        String bookmarksCreate = "CREATE TABLE " + BookmarkEntry.TABLE_NAME + " (" +
                BookmarkEntry._ID + " INTEGER PRIMARY KEY, " +
                makeColumn(BookmarkEntry.COLUMN_SERIES_ID, BookmarkEntry.COLUMN_SERIES_ID_TYPE) +
                makeColumn(BookmarkEntry.COLUMN_PAGE_ID, BookmarkEntry.COLUMN_PAGE_ID_TYPE) +
                makeColumn(BookmarkEntry.COLUMN_NOTE, BookmarkEntry.COLUMN_NOTE_TYPE) +
                makeForeignKey(BookmarkEntry.COLUMN_SERIES_ID, SeriesEntry.TABLE_NAME, SeriesEntry._ID) +
                makeForeignKey(BookmarkEntry.COLUMN_PAGE_ID, PageEntry.TABLE_NAME, PageEntry._ID, false) +
                ")";
        db.execSQL(bookmarksCreate);

        String genreCreate = "CREATE TABLE " + GenreEntry.TABLE_NAME + " (" +
                GenreEntry._ID + " INTEGER PRIMARY KEY, " +
                makeColumn(GenreEntry.COLUMN_NAME, GenreEntry.COLUMN_NAME_TYPE, false) +
                ")";
        db.execSQL(genreCreate);

        String seriesGenreCreate = "CREATE TABLE " + SeriesGenreEntry.TABLE_NAME + " (" +
                SeriesGenreEntry._ID + " INTEGER PRIMARY KEY, " +
                makeColumn(SeriesGenreEntry.COLUMN_SERIES_ID, SeriesGenreEntry.COLUMN_SERIES_ID_TYPE) +
                makeColumn(SeriesGenreEntry.COLUMN_GENRE_ID, SeriesGenreEntry.COLUMN_GENRE_ID_TYPE) +
                makeForeignKey(SeriesGenreEntry.COLUMN_SERIES_ID, SeriesEntry.TABLE_NAME, SeriesEntry._ID) +
                makeForeignKey(SeriesGenreEntry.COLUMN_GENRE_ID, GenreEntry.TABLE_NAME, GenreEntry._ID, false) +
                ")";
        db.execSQL(seriesGenreCreate);

        String seriesGenreVewCreate = "CREATE VIEW " + SeriesGenreViewEntry.TABLE_NAME + " AS " +
                "select " + SeriesEntry.TABLE_NAME + ".* " +
                "from " + SeriesGenreEntry.TABLE_NAME + joinStatement(SeriesGenreEntry.TABLE_NAME, SeriesGenreEntry.COLUMN_SERIES_ID, SeriesEntry.TABLE_NAME, SeriesEntry._ID);
        db.execSQL(seriesGenreVewCreate);

        String genreSeriesViewCreate = "CREATE VIEW " + GenreSeriesViewEntry.TABLE_NAME + " AS " +
                "select " + GenreEntry.TABLE_NAME + ".* " +
                "from " + SeriesGenreEntry.TABLE_NAME + joinStatement(SeriesGenreEntry.TABLE_NAME, SeriesGenreEntry.COLUMN_GENRE_ID, GenreEntry.TABLE_NAME, GenreEntry._ID);
        db.execSQL(genreSeriesViewCreate);

        db.execSQL("INSERT INTO provider (url, new_url, name) values (\"http://www.mangapanda.com/alphabetical\", \"http://www.mangapanda.com/latest\", \"MangaPanda\")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS " + PageEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ChapterEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SeriesGenreEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + GenreEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SeriesEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ProviderEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + BookmarkEntry.TABLE_NAME);
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
