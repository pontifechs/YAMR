package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 5/19/15.
 */
public class Page extends MangaElement
{
    private int chapterId;

    private String imageUrl;
    private String imagePath;

    public Page() {}

    public Page(Cursor c)
    {
        super(c);
        int chapterIdCol = c.getColumnIndex(DBHelper.PageEntry.COLUMN_CHAPTER_ID);
        int imageUrlCol = c.getColumnIndex(DBHelper.PageEntry.COLUMN_IMAGE_URL);
        int imagePathCol = c.getColumnIndex(DBHelper.PageEntry.COLUMN_IMAGE_PATH);
        chapterId = c.getInt(chapterIdCol);
        imageUrl = c.getString(imageUrlCol);
        imagePath = c.getString(imagePathCol);
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(DBHelper.PageEntry.COLUMN_CHAPTER_ID, chapterId);
        values.put(DBHelper.PageEntry.COLUMN_IMAGE_URL, imageUrl);
        values.put(DBHelper.PageEntry.COLUMN_IMAGE_PATH, imagePath);
        return values;
    }

    public int getChapterId()
    {
        return chapterId;
    }

    public void setChapterId(int chapterId)
    {
        this.chapterId = chapterId;
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
