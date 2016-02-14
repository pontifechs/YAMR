package ninja.dudley.yamr.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.provider.BaseColumns;

import org.jsoup.helper.StringUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ninja.dudley.yamr.db.util.Column;
import ninja.dudley.yamr.db.util.ForeignKey;
import ninja.dudley.yamr.db.util.Id;
import ninja.dudley.yamr.db.util.Table;
import ninja.dudley.yamr.db.util.Unique;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Genre;
import ninja.dudley.yamr.model.MangaElement;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.model.Series;

/**
 * Created by mdudley on 5/19/15.
 */
public class DBHelper extends SQLiteOpenHelper
{
    private final Context context;

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "YAMR.db";

    public static final String AUTHORITY = "ninja.dudley.yamr.db.DBProvider";

    public static final String ID = "_id";

    public static final Map<String, String[]> projections;

    static
    {
        projections = new HashMap<>();
        projections.put(Provider.tableName, projection(Provider.class));
        projections.put(Series.tableName, projection(Series.class));
        projections.put(Chapter.tableName, projection(Chapter.class));
        projections.put(Page.tableName, projection(Page.class));
        projections.put(Genre.tableName, projection(Genre.class));
    }

    public static abstract class SeriesGenreEntry implements BaseColumns
    {
        public static final String TABLE_NAME = "series_genres";
        public static final String SERIES_GENRES_VIEW = "series_genres_view";
        public static final String GENRE_SERIES_VIEW = "genre_series_view";

        public static final String COLUMN_SERIES_ID = "series_id";
        public static final Column.Type COLUMN_SERIES_ID_TYPE = Column.Type.Integer;
        public static final String COLUMN_GENRE_ID = "genre_id";
        public static final Column.Type COLUMN_GENRE_ID_TYPE = Column.Type.Integer;

        public static final String[] projection;

        static
        {
            projection = new String[]{_ID, COLUMN_SERIES_ID, COLUMN_GENRE_ID};
        }
    }

    public static abstract class PageHeritageViewEntry
    {
        public static final String TABLE_NAME = "page_heritage";

        public static final String COLUMN_PROVIDER_ID = "provider_id";
        public static final String COLUMN_PROVIDER_NAME = "provider_name";
        public static final String COLUMN_SERIES_ID = "series_id";
        public static final String COLUMN_SERIES_NAME = "series_name";
        public static final String COLUMN_CHAPTER_ID = "chapter_id";
        public static final String COLUMN_CHAPTER_NUMBER = "chapter_number";
        public static final String COLUMN_PAGE_NUMBER = "page_number";
        public static final String COLUMN_PAGE_ID = "page_id";

        public static final String[] projection;

        static
        {
            projection = new String[]{COLUMN_PROVIDER_ID, COLUMN_PROVIDER_NAME, COLUMN_SERIES_ID,
                    COLUMN_SERIES_NAME, COLUMN_CHAPTER_ID, COLUMN_CHAPTER_NUMBER,
                    COLUMN_PAGE_NUMBER, COLUMN_PAGE_ID};
        }
    }

    public static abstract class SeriesPageCompleteViewEntry
    {
        public static final String TABLE_NAME = "series_page_complete";

        public static final String COLUMN_TOTAL = "total";
        public static final String COLUMN_FETCHED = "fetched";
        public static final String COLUMN_SERIES_ID = "series_id";
    }

    public static abstract class ChapterPageCompleteViewEntry
    {
        public static final String TABLE_NAME = "chapter_page_complete";

        public static final String COLUMN_TOTAL = "total";
        public static final String COLUMN_FETCHED = "fetched";
        public static final String COLUMN_CHAPTER_ID = "chapter_id";
    }

    private static String makeColumn(String name, Column.Type type)
    {
        if (type == Column.Type.Datetime)
        {
            return name + " " + type.toString() + " DEFAULT CURRENT_TIMESTAMP";
        }
        return name + " " + type.toString();
    }

    private static String makeForeignKey(String keyColumn, String refTable, String refColumn)
    {
        return "FOREIGN KEY(" + keyColumn + ") REFERENCES " + refTable + "(" + refColumn + ") " +
               "ON DELETE CASCADE";
    }

    private static String uniqueColumns(List<String> column)
    {
        if (column.size() <= 0)
        {
            return "";
        }

        String constraint = ", UNIQUE (" + column.get(0);
        for (int i = 1; i < column.size(); ++i)
        {
            constraint += ", " + column.get(i);
        }
        constraint += ") ON CONFLICT ABORT";
        return constraint;
    }

    private static String joinedAliasedColumn(String tableName, String fieldName, String alias)
    {
        return tableName + "." + fieldName + " as " + alias;
    }

    private static String joinStatement(String lhsTable, String lhsField,
                                        String rhsTable, String rhsField)
    {
        return " join " + rhsTable + " ON " + lhsTable + "." + lhsField + " = " +
                                              rhsTable + "." + rhsField;
    }

    public static String schema(Class<?> klass, int version)
    {
        Table t = klass.getAnnotation(Table.class);
        if (t == null)
        {
            throw new AssertionError(klass + " Isn't annotated with Table");
        }

        String schema = "CREATE TABLE " + t.value() + " (";

        Set<Field> fields = new HashSet<>();
        fields.addAll(Arrays.asList(klass.getDeclaredFields()));

        Class<?> superK = klass.getSuperclass();
        while (superK != Object.class)
        {
            fields.addAll(Arrays.asList(superK.getDeclaredFields()));
            superK = superK.getSuperclass();
        }

        // SQL columns
        List<String> definitions = new ArrayList<>();
        for (Field field : fields)
        {
            Column column = field.getAnnotation(Column.class);
            // Skip if we're building for an earlier version
            if (column != null && column.version() > version)
            {
                continue;
            }

            ForeignKey fk = field.getAnnotation(ForeignKey.class);
            Id id = field.getAnnotation(Id.class);

            if (id != null)
            {
                definitions.add(makeColumn(ID, Column.Type.Integer) + " PRIMARY KEY ");
            }
            else if (column != null)
            {
                definitions.add(makeColumn(column.name(), column.type()));
            }
            else if (fk != null)
            {
                definitions.add(makeColumn(fk.name(), Column.Type.Integer));
            }
        }

        // Foreign keys
        for (Field field : fields)
        {
            ForeignKey fk = field.getAnnotation(ForeignKey.class);
            if (fk == null)
            {
                continue;
            }

            Class<?> parent = fk.value();
            Table foreign = parent.getAnnotation(Table.class);
            definitions.add(makeForeignKey(fk.name(), foreign.value(), ID));
        }
        schema += StringUtil.join(definitions, ",");

        // Unique constraints
        List<String> uniqueFields = new ArrayList<>();
        for (Field field : fields)
        {
            Unique unique = field.getAnnotation(Unique.class);
            if (unique == null)
            {
                continue;
            }
            Column column = field.getAnnotation(Column.class);
            uniqueFields.add(column.name());
        }
        schema += uniqueColumns(uniqueFields) + ")";
        return schema;
    }

    private static String[] projection(Class<?> klass)
    {
        List<Field> fields = new ArrayList<>();
        fields.addAll(Arrays.asList(klass.getDeclaredFields()));

        Class<?> superK = klass.getSuperclass();
        while (superK != Object.class)
        {
            fields.addAll(Arrays.asList(superK.getDeclaredFields()));
            superK = superK.getSuperclass();
        }
        List<String> ret = new ArrayList<>();
        for (Field field : fields)
        {
            Column column = field.getAnnotation(Column.class);
            ForeignKey fk = field.getAnnotation(ForeignKey.class);
            Id id = field.getAnnotation(Id.class);
            if (column != null)
            {
                ret.add(column.name());
            }
            else if (fk != null)
            {
                ret.add(fk.name());
            }
            else if (id != null)
            {
                ret.add(ID);
            }
        }
        String[] stupidJava = new String[ret.size()];
        ret.toArray(stupidJava);
        return (stupidJava);
    }

    public DBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        createV1(db);
        createAndUpdateV2(db);
        createAndUpdateV3(db);
    }

    private void createV1(SQLiteDatabase db)
    {
        db.execSQL(schema(Provider.class, 1));
        db.execSQL(schema(Series.class, 1));
        db.execSQL(schema(Chapter.class, 1));
        db.execSQL(schema(Page.class, 1));
        db.execSQL(schema(Genre.class, 1));

        String pageHeritageView =
                "CREATE VIEW " + PageHeritageViewEntry.TABLE_NAME + " AS " +
                        "select " +
                        joinedAliasedColumn(Provider.tableName, ID,
                                PageHeritageViewEntry.COLUMN_PROVIDER_ID) + ", " +
                        joinedAliasedColumn(Provider.tableName, "name",
                                PageHeritageViewEntry.COLUMN_PROVIDER_NAME) + ", " +
                        joinedAliasedColumn(Series.tableName, ID,
                                PageHeritageViewEntry.COLUMN_SERIES_ID) + ", " +
                        joinedAliasedColumn(Series.tableName, "name",
                                PageHeritageViewEntry.COLUMN_SERIES_NAME) + ", " +
                        joinedAliasedColumn(Chapter.tableName, ID,
                                PageHeritageViewEntry.COLUMN_CHAPTER_ID) + ", " +
                        joinedAliasedColumn(Chapter.tableName, "number",
                                PageHeritageViewEntry.COLUMN_CHAPTER_NUMBER) + ", " +
                        joinedAliasedColumn(Page.tableName, ID,
                                PageHeritageViewEntry.COLUMN_PAGE_ID) + ", " +
                        joinedAliasedColumn(Page.tableName, "number",
                                PageHeritageViewEntry.COLUMN_PAGE_NUMBER) +
                        " from " + Provider.tableName +
                        joinStatement(Provider.tableName, ID, Series.tableName, Provider.tableName + ID) +
                        joinStatement(Series.tableName, ID, Chapter.tableName, Series.tableName + ID) +
                        joinStatement(Chapter.tableName, ID, Page.tableName, Chapter.tableName + ID);
        db.execSQL(pageHeritageView);

        String seriesGenreCreate =
                "CREATE TABLE " + SeriesGenreEntry.TABLE_NAME + " " +
                        "(" +
                        SeriesGenreEntry._ID + " INTEGER PRIMARY KEY, " +
                        makeColumn(SeriesGenreEntry.COLUMN_SERIES_ID,
                                SeriesGenreEntry.COLUMN_SERIES_ID_TYPE) + ", " +
                        makeColumn(SeriesGenreEntry.COLUMN_GENRE_ID,
                                SeriesGenreEntry.COLUMN_GENRE_ID_TYPE) + ", " +
                        makeForeignKey(SeriesGenreEntry.COLUMN_SERIES_ID,
                                Series.tableName, DBHelper.ID) + ", " +
                        makeForeignKey(SeriesGenreEntry.COLUMN_GENRE_ID,
                                Genre.tableName, DBHelper.ID) +
                        ")";
        db.execSQL(seriesGenreCreate);

        // Genres in the given series
        String seriesGenreViewCreate =
                "CREATE VIEW " + SeriesGenreEntry.SERIES_GENRES_VIEW + " AS " +
                        "select " + Genre.tableName + ".* , " +
                        SeriesGenreEntry.TABLE_NAME +
                        ".series_id as series_id " +
                        "from " + SeriesGenreEntry.TABLE_NAME +
                        joinStatement(SeriesGenreEntry.TABLE_NAME, SeriesGenreEntry.COLUMN_GENRE_ID,
                                Genre.tableName, ID);
        db.execSQL(seriesGenreViewCreate);

        // Series in the given genre
        String genreSeriesViewCreate =
                "CREATE VIEW " + SeriesGenreEntry.GENRE_SERIES_VIEW + " AS " +
                        "select " + Series.tableName + ".* , " +
                        SeriesGenreEntry.TABLE_NAME +
                        ".genre_id as genre_id " +
                        "from " + SeriesGenreEntry.TABLE_NAME +
                        joinStatement(SeriesGenreEntry.TABLE_NAME, SeriesGenreEntry.COLUMN_SERIES_ID,
                                Series.tableName, ID);
        db.execSQL(genreSeriesViewCreate);
    }

    private void createAndUpdateV2(SQLiteDatabase db)
    {
        // Chapter Page Fetching Status
        String chapterPageComplete =
                "CREATE VIEW " + ChapterPageCompleteViewEntry.TABLE_NAME + " AS " +
                        "select sum(1) as " + ChapterPageCompleteViewEntry.COLUMN_TOTAL + ", " +
                        "sum(case when " + Page.imagePathCol + " is null then 0 else 1 end) as " + ChapterPageCompleteViewEntry.COLUMN_FETCHED + ", " + Page.chapterIdCol + " " +
                        "from " + Chapter.tableName + " as c " +
                        "left join " +  Page.tableName + " as p " +
                        "on  c._id = p." + Page.chapterIdCol + " " +
                        "group by c._id";
        db.execSQL(chapterPageComplete);

        // Series Page complete
        String seriesPageComplete =
                "CREATE VIEW " + SeriesPageCompleteViewEntry.TABLE_NAME  + " AS " +
                        "select sum(" + ChapterPageCompleteViewEntry.COLUMN_TOTAL + ") as " + SeriesPageCompleteViewEntry.COLUMN_TOTAL + ", " +
                        "sum(" + ChapterPageCompleteViewEntry.COLUMN_FETCHED + ") as " + SeriesPageCompleteViewEntry.COLUMN_FETCHED + " , " + Chapter.seriesIdCol + " " +
                        "from  " + Chapter.tableName + " " +
                        "left join " + ChapterPageCompleteViewEntry.TABLE_NAME + " as cpc " +
                        "on " + Chapter.tableName + "._id = cpc." + ChapterPageCompleteViewEntry.COLUMN_CHAPTER_ID + " " +
                        "group by " + Chapter.seriesIdCol;
        db.execSQL(seriesPageComplete);
    }

    private void addFetchDate(Class klass, String tableName, SQLiteDatabase db)
    {
         // Really hate that I have to create/insert/drop
        String move =
                "ALTER TABLE " + tableName +
                        " RENAME TO " + tableName + "temp";

        String recreate = schema(klass, 3);

        String refill =
                "INSERT INTO " + tableName +
                        " SELECT " + tableName + "temp.*, CURRENT_TIMESTAMP " +
                        " FROM " + tableName + "temp";

        String drop =
                "DROP TABLE " + tableName + "temp";

        db.execSQL(move);
        db.execSQL(recreate);
        db.execSQL(refill);
        db.execSQL(drop);
    }

    private void createAndUpdateV3(SQLiteDatabase db)
    {
        // Add fetch date to all the fetched things
        addFetchDate(Provider.class, Provider.tableName, db);
        addFetchDate(Series.class, Series.tableName, db);
        addFetchDate(Chapter.class, Chapter.tableName, db);
        addFetchDate(Page.class, Page.tableName, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        if (oldVersion < 2)
        {
            createAndUpdateV2(db);
        }

        if (oldVersion < 3)
        {
            createAndUpdateV3(db);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db)
    {
        super.onOpen(db);
        if (!db.isReadOnly())
        {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }

        // Load up existing providers.
        Map<String, Provider> existingProviders = new HashMap<>();
        Cursor allProviders = db.query(Provider.tableName, projections.get(Provider.tableName), null, null, null, null, null);
        if (allProviders.getCount() > 0)
        {
            allProviders.moveToFirst();
            do
            {
                Provider provider = new Provider(allProviders, false);
                existingProviders.put(provider.getName(), provider);
            }
            while (allProviders.moveToNext());
        }

        // Go over all the new providers and make sure they're updated to reflect the latest code
        List<ContentValues> newProviders = ProviderLoader.Companion.loadProviders(context);
        for (int i = 0; i < newProviders.size(); ++i)
        {
            ContentValues newProvider = newProviders.get(i);
            // Is this a new one?
            if (!existingProviders.containsKey(newProvider.getAsString(Provider.nameCol)))
            {
                db.insert(Provider.tableName, null, newProvider);
            }
            else
            {
                db.update(Provider.tableName, newProvider, "name = ?", new String[]{newProvider.getAsString("name")});
            }
        }
        allProviders.close();
    }
}
