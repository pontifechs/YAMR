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
@Table(Series.tableName)
public class Series extends MangaElement
{
    public static final String tableName = "series";

    public static final String providerIdCol = Provider.tableName + DBHelper.ID;
    @ForeignKey(value=Provider.class, name=providerIdCol)
    private int providerId;

    public static final String nameCol = "name";
    @Column(name=nameCol)
    private String name;

    public static final String descriptionCol = "description";
    @Column(name=descriptionCol)
    private String description;

    public static final String favoriteCol = "favorite";
    @Column(name=favoriteCol, type=Column.Type.Integer)
    private boolean favorite = false;

    public static final String alternateNameCol = "alternate_name";
    @Column(name=alternateNameCol)
    private String alternateName;

    public static final String completeCol = "complete";
    @Column(name=completeCol, type=Column.Type.Integer)
    private boolean complete = false;

    public static final String authorCol = "author";
    @Column(name=authorCol)
    private String author;

    public static final String artistCol = "artist";
    @Column(name=artistCol)
    private String artist;

    public static final String thumbnailUrlCol = "thumbnail_url";
    @Column(name=thumbnailUrlCol)
    private String thumbnailUrl;

    public static final String thumbnailPathCol = "thumbnail_path";
    @Column(name=thumbnailPathCol)
    private String thumbnailPath;

    public static final String progressChapterIdCol = "progress_" + Chapter.tableName + DBHelper.ID;
    @ForeignKey(value=Chapter.class, name=progressChapterIdCol)
    private int progressChapterId = -1;

    public static final String progressPageIdCol = "progress_" + Page.tableName + DBHelper.ID;
    @ForeignKey(value=Page.class, name=progressPageIdCol)
    private int progressPageId = -1;

    public static final String updatedCol = "updated";
    @Column(name=updatedCol, type=Column.Type.Integer)
    private boolean updated = false;

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
        parse(c,true);
    }

    public Series(Cursor c, boolean close)
    {
        super(c);
        parse(c,close);
    }

    private void parse(Cursor c, boolean close)
    {
        providerId = getInt(c, providerIdCol);
        name = getString(c, nameCol);
        description = getString(c, descriptionCol);
        favorite = getBool(c, favoriteCol);
        alternateName = getString(c, alternateNameCol);
        complete = getBool(c, completeCol);
        author = getString(c, authorCol);
        artist = getString(c, artistCol);
        thumbnailUrl = getString(c, thumbnailUrlCol);
        thumbnailPath = getString(c, thumbnailPathCol);
        progressChapterId = getInt(c, progressChapterIdCol);
        progressPageId = getInt(c, progressPageIdCol);
        updated = getBool(c, updatedCol);
        if (close)
        {
            c.close();
        }
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(providerIdCol, providerId);
        values.put(nameCol, name);
        values.put(descriptionCol, description);
        values.put(favoriteCol, favorite);
        values.put(alternateNameCol, alternateName);
        values.put(completeCol, complete);
        values.put(authorCol, author);
        values.put(artistCol, artist);
        values.put(thumbnailUrlCol, thumbnailUrl);
        values.put(thumbnailPathCol, thumbnailPath);
        if (progressChapterId != -1)
        {
            values.put(progressChapterIdCol, progressChapterId);
        }
        if (progressPageId != -1)
        {
            values.put(progressPageIdCol, progressPageId);
        }
        values.put(updatedCol, updated);
        return values;
    }

    public Uri uri()
    {
        return uri(id);
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

    public int getProgressChapterId()
    {
        return progressChapterId;
    }

    public void setProgressChapterId(int progressChapterId)
    {
        this.progressChapterId = progressChapterId;
    }

    public int getProgressPageId()
    {
        return progressPageId;
    }

    public void setProgressPageId(int progressPageId)
    {
        this.progressPageId = progressPageId;
    }

    public boolean isUpdated()
    {
        return updated;
    }

    public void setUpdated(boolean updated)
    {
        this.updated = updated;
    }
}
