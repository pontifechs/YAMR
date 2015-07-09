package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.db.util.Column;
import ninja.dudley.yamr.db.util.ForeignKey;
import ninja.dudley.yamr.db.util.Table;

/**
 * Created by mdudley on 5/19/15.
 */
@Table(Page.tableName)
public class Page extends MangaElement
{
    public static final String tableName = "page";

    public static final String chapterIdCol = Chapter.tableName + DBHelper.ID;
    @ForeignKey(value = Chapter.class, name = chapterIdCol)
    public final int chapterId;

    public static final String numberCol = "number";
    @Column(name = numberCol, type = Column.Type.Real)
    public float number;

    public static final String imageUrlCol = "image_url";
    @Column(name = imageUrlCol)
    public String imageUrl;

    public static final String imagePathCol = "image_path";
    @Column(name = imagePathCol)
    public String imagePath;


    // Uri Handling --------------------------------------------------------------------------------
    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/page");
    }

    public static Uri uri(int id)
    {
        return baseUri().buildUpon().appendPath(Integer.toString(id)).build();
    }

    public Uri uri()
    {
        return uri(id);
    }

    public static Uri heritage(int id)
    {
        return uri(id).buildUpon().appendPath("heritage").build();
    }

    public Uri heritage()
    {
        return heritage(id);
    }

    // Construction / Persistence ------------------------------------------------------------------
    public Page() { chapterId = -1; }
    public Page(int chapterId) { this.chapterId = chapterId; }
    public Page(Cursor c)
    {
        super(c);
        chapterId = getInt(c, chapterIdCol);
        number = getFloat(c, numberCol);
        imageUrl = getString(c, imageUrlCol);
        imagePath = getString(c, imagePathCol);
        c.close();
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(chapterIdCol, chapterId);
        values.put(numberCol, number);
        values.put(imageUrlCol, imageUrl);
        values.put(imagePathCol, imagePath);
        return values;
    }
}
