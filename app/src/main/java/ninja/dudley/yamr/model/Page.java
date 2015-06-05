package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 5/19/15.
 */
public class Page extends MangaElement
{
    private int chapterId;

    private float number;
    private String imageUrl;
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
        int chapterIdCol = c.getColumnIndex(DBHelper.PageEntry.COLUMN_CHAPTER_ID);
        int numberCol = c.getColumnIndex(DBHelper.PageEntry.COLUMN_NUMBER);
        int imageUrlCol = c.getColumnIndex(DBHelper.PageEntry.COLUMN_IMAGE_URL);
        int imagePathCol = c.getColumnIndex(DBHelper.PageEntry.COLUMN_IMAGE_PATH);
        chapterId = c.getInt(chapterIdCol);
        number = c.getFloat(numberCol);
        imageUrl = c.getString(imageUrlCol);
        imagePath = c.getString(imagePathCol);
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(DBHelper.PageEntry.COLUMN_CHAPTER_ID, chapterId);
        values.put(DBHelper.PageEntry.COLUMN_NUMBER, number);
        values.put(DBHelper.PageEntry.COLUMN_IMAGE_URL, imageUrl);
        values.put(DBHelper.PageEntry.COLUMN_IMAGE_PATH, imagePath);
        return values;
    }

    public Uri uri()
    {
        return uri(_id);
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
