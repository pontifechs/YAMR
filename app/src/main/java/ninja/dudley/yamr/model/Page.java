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
    @ForeignKey(value=Chapter.class, name=chapterIdCol)
    private int chapterId;

    public static final String numberCol = "number";
    @Column(name=numberCol, type=Column.Type.Real)
    private float number;

    public static final String imageUrlCol = "image_url";
    @Column(name=imageUrlCol)
    private String imageUrl;

    public static final String imagePathCol = "image_path";
    @Column(name=imagePathCol)
    private String imagePath;

    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/page");
    }

    public static Uri uri(int id)
    {
        Uri base = baseUri();
        return base.buildUpon().appendPath(Integer.toString(id)).build();
    }

    public Page() {}

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

    public Uri uri()
    {
        return uri(id);
    }

    public int getChapterId()
    {
        return chapterId;
    }

    public void setChapterId(int chapterId)
    {
        this.chapterId = chapterId;
    }

    public float getNumber()
    {
        return number;
    }

    public void setNumber(float number)
    {
        this.number = number;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public String getImagePath()
    {
        return imagePath;
    }

    public void setImagePath(String imagePath)
    {
        this.imagePath = imagePath;
    }
}
