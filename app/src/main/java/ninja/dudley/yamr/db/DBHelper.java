package ninja.dudley.yamr.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.jsoup.helper.StringUtil;

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
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.model.Series;

/**
 * Created by mdudley on 5/19/15.
 */
public class DBHelper extends SQLiteOpenHelper
{
    private static final int DATABASE_VERSION = 1;
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
        public static final String TABLE_NAME="page_heritage";

        public static final String COLUMN_PROVIDER_NAME = "provider_name";
        public static final Column.Type COLUMN_PROVIDER_NAME_TYPE = Column.Type.Text;
        public static final String COLUMN_SERIES_NAME = "series_name";
        public static final Column.Type COLUMN_SERIES_NAME_TYPE = Column.Type.Text;
        public static final String COLUMN_CHAPTER_NUMBER = "chapter_number";
        public static final Column.Type COLUMN_CHAPTER_NUMBER_TYPE = Column.Type.Real;
        public static final String COLUMN_PAGE_NUMBER = "page_number";
        public static final Column.Type COLUMN_PAGE_NUMBER_TYPE = Column.Type.Real;
        public static final String COLUMN_PAGE_ID = "page_id";
        public static final Column.Type COLUMN_PAGE_ID_TYPE = Column.Type.Integer;

        public static final String[] projection;

        static
        {
            projection = new String[]{COLUMN_PROVIDER_NAME, COLUMN_SERIES_NAME, COLUMN_CHAPTER_NUMBER, COLUMN_PAGE_NUMBER, COLUMN_PAGE_ID};
        }
    }

    private static String makeColumn(String name, Column.Type type)
    {
        return name + " " + type.toString();
    }

    private static String makeForeignKey(String keyColumn, String refTable, String refColumn)
    {
        return "FOREIGN KEY(" + keyColumn + ") REFERENCES " + refTable + "(" + refColumn + ") ON DELETE CASCADE";
    }

    private static String uniqueColumns(List<String> column)
    {
        if (column.size() <= 0)
        {
            return "";
        }

        String constraint = " UNIQUE (" + column.get(0);
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

    private static String joinStatement(String lhsTable, String lhsField, String rhsTable, String rhsField)
    {
        return " join " + rhsTable + " ON " + lhsTable + "." + lhsField + " = " + rhsTable + "." + rhsField;
    }

    public static String schema(Class<?> klass)
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
                Class<?> foreign = fk.value();
                Table parent = foreign.getAnnotation(Table.class);
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
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(schema(Provider.class));
        db.execSQL(schema(Series.class));
        db.execSQL(schema(Chapter.class));
        db.execSQL(schema(Page.class));
        db.execSQL(schema(Genre.class));

        String pageHeritageView = "CREATE VIEW " + PageHeritageViewEntry.TABLE_NAME + " AS " +
                "select " + joinedAliasedColumn(Page.tableName, ID, PageHeritageViewEntry.COLUMN_PAGE_ID) + ", "
                + joinedAliasedColumn(Provider.tableName, "name", PageHeritageViewEntry.COLUMN_PROVIDER_NAME) + ", "
                + joinedAliasedColumn(Series.tableName, "name", PageHeritageViewEntry.COLUMN_SERIES_NAME) + ", "
                + joinedAliasedColumn(Chapter.tableName, "number", PageHeritageViewEntry.COLUMN_CHAPTER_NUMBER) + ", "
                + joinedAliasedColumn(Page.tableName, "number", PageHeritageViewEntry.COLUMN_PAGE_NUMBER) +
                " from " + Provider.tableName + joinStatement(Provider.tableName, ID, Series.tableName, Provider.tableName + ID)
                + joinStatement(Series.tableName, ID, Chapter.tableName, Series.tableName + ID)
                + joinStatement(Chapter.tableName, ID, Page.tableName, Chapter.tableName + ID)
                ;
        db.execSQL(pageHeritageView);

        String seriesGenreCreate = "CREATE TABLE " + SeriesGenreEntry.TABLE_NAME + " (" +
                SeriesGenreEntry._ID + " INTEGER PRIMARY KEY, " +
                makeColumn(SeriesGenreEntry.COLUMN_SERIES_ID, SeriesGenreEntry.COLUMN_SERIES_ID_TYPE) + ", " +
                makeColumn(SeriesGenreEntry.COLUMN_GENRE_ID, SeriesGenreEntry.COLUMN_GENRE_ID_TYPE) + ", " +
                makeForeignKey(SeriesGenreEntry.COLUMN_SERIES_ID, Series.tableName, DBHelper.ID) + ", " +
                makeForeignKey(SeriesGenreEntry.COLUMN_GENRE_ID, Genre.tableName, DBHelper.ID) +
                ")";
        db.execSQL(seriesGenreCreate);

        String seriesGenreVewCreate = "CREATE VIEW series_genre_view AS " +
                "select " + Series.tableName + ".* " +
                "from " + SeriesGenreEntry.TABLE_NAME + joinStatement(SeriesGenreEntry.TABLE_NAME, SeriesGenreEntry.COLUMN_SERIES_ID, Series.tableName, ID);
        db.execSQL(seriesGenreVewCreate);

        String genreSeriesViewCreate = "CREATE VIEW genre_series_view AS " +
                "select " + Genre.tableName+ ".* " +
                "from " + SeriesGenreEntry.TABLE_NAME + joinStatement(SeriesGenreEntry.TABLE_NAME, SeriesGenreEntry.COLUMN_GENRE_ID, Genre.tableName, ID);
        db.execSQL(genreSeriesViewCreate);

        db.execSQL("INSERT INTO provider (url, new_url, name) values (\"http://www.mangapanda.com/alphabetical\", \"http://www.mangapanda.com/latest\", \"MangaPanda\")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {}

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
