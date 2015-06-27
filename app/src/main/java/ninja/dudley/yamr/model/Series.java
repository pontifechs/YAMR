package ninja.dudley.yamr.model;


import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 5/19/15.
 */
public class Series extends MangaElement
{
    private int providerId;

    private String name;
    private String description;
    private boolean favorite = false;
    private String alternateName;
    private boolean complete = false;
    private String author;
    private String artist;
    private String thumbnailUrl;
    private String thumbnailPath;

    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/series");
    }

    public static Uri uri(int id)
    {
        Uri base = baseUri();
        return base.buildUpon().appendPath(Integer.toString(id)).build();
    }

    public Series() {}

    public Series(Cursor c)
    {
        super(c);
        providerId = getInt(c, DBHelper.SeriesEntry.COLUMN_PROVIDER_ID);
        name = getString(c, DBHelper.SeriesEntry.COLUMN_NAME);
        description = getString(c, DBHelper.SeriesEntry.COLUMN_DESCRIPTION);
        favorite = getBool(c, DBHelper.SeriesEntry.COLUMN_FAVORITE);
        alternateName = getString(c, DBHelper.SeriesEntry.COLUMN_ALTERNATE_NAME);
        complete = getBool(c, DBHelper.SeriesEntry.COLUMN_COMPLETE);
        author = getString(c, DBHelper.SeriesEntry.COLUMN_AUTHOR);
        artist = getString(c, DBHelper.SeriesEntry.COLUMN_ARTIST);
        thumbnailUrl = getString(c, DBHelper.SeriesEntry.COLUMN_THUMBNAIL_URL);
        thumbnailPath = getString(c, DBHelper.SeriesEntry.COLUMN_THUMBNAIL_PATH);
        c.close();
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(DBHelper.SeriesEntry.COLUMN_PROVIDER_ID, providerId);
        values.put(DBHelper.SeriesEntry.COLUMN_NAME, name);
        values.put(DBHelper.SeriesEntry.COLUMN_DESCRIPTION, description);
        values.put(DBHelper.SeriesEntry.COLUMN_FAVORITE, favorite);
        values.put(DBHelper.SeriesEntry.COLUMN_ALTERNATE_NAME, alternateName);
        values.put(DBHelper.SeriesEntry.COLUMN_COMPLETE, complete);
        values.put(DBHelper.SeriesEntry.COLUMN_AUTHOR, author);
        values.put(DBHelper.SeriesEntry.COLUMN_ARTIST, artist);
        values.put(DBHelper.SeriesEntry.COLUMN_THUMBNAIL_URL, thumbnailUrl);
        values.put(DBHelper.SeriesEntry.COLUMN_THUMBNAIL_PATH, thumbnailPath);
        return values;
    }

    public Uri uri()
    {
        return uri(_id);
    }

    public int getProviderId()
    {
        return providerId;
    }

    public void setProviderId(int providerId)
    {
        this.providerId = providerId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean isFavorite()
    {
        return favorite;
    }

    public void setFavorite(boolean favorite)
    {
        this.favorite = favorite;
    }

    public String getAlternateName()
    {
        return alternateName;
    }

    public void setAlternateName(String alternateName)
    {
        this.alternateName = alternateName;
    }

    public boolean isComplete()
    {
        return complete;
    }

    public void setComplete(boolean complete)
    {
        this.complete = complete;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public String getArtist()
    {
        return artist;
    }

    public void setArtist(String artist)
    {
        this.artist = artist;
    }

    public String getThumbnailUrl()
    {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl)
    {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getThumbnailPath()
    {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath)
    {
        this.thumbnailPath = thumbnailPath;
    }
}
