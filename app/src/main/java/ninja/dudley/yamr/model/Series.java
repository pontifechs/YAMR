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
    public final int providerId;

    public static final String nameCol = "name";
    @Column(name=nameCol)
    public String name;

    public static final String descriptionCol = "description";
    @Column(name=descriptionCol)
    public String description;

    public static final String favoriteCol = "favorite";
    @Column(name=favoriteCol, type=Column.Type.Integer)
    public boolean favorite = false;

    public static final String alternateNameCol = "alternate_name";
    @Column(name=alternateNameCol)
    public String alternateName;

    public static final String completeCol = "complete";
    @Column(name=completeCol, type=Column.Type.Integer)
    public boolean complete = false;

    public static final String authorCol = "author";
    @Column(name=authorCol)
    public String author;

    public static final String artistCol = "artist";
    @Column(name=artistCol)
    public String artist;

    public static final String thumbnailUrlCol = "thumbnail_url";
    @Column(name=thumbnailUrlCol)
    public String thumbnailUrl;

    public static final String thumbnailPathCol = "thumbnail_path";
    @Column(name=thumbnailPathCol)
    public String thumbnailPath;

    public static final String progressChapterIdCol = "progress_" + Chapter.tableName + DBHelper.ID;
    @ForeignKey(value=Chapter.class, name=progressChapterIdCol)
    public int progressChapterId = -1;

    public static final String progressPageIdCol = "progress_" + Page.tableName + DBHelper.ID;
    @ForeignKey(value=Page.class, name=progressPageIdCol)
    public int progressPageId = -1;

    public static final String updatedCol = "updated";
    @Column(name=updatedCol, type=Column.Type.Integer)
    public boolean updated = false;


    // Uri Handling --------------------------------------------------------------------------------
    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/series");
    }
    public static Uri uri(int id)
    {
        return baseUri().buildUpon().appendPath(Integer.toString(id)).build();
    }
    public Uri uri()
    {
        return uri(id);
    }

    public static Uri favorites()
    {
        return baseUri().buildUpon().appendPath("favorites").build();
    }

    public static Uri chapters(int id)
    {
        return uri(id).buildUpon().appendPath("chapters").build();
    }
    public Uri chapters()
    {
        return chapters(id);
    }

    public static Uri prevChapter(int id, float num)
    {
        return uri(id).buildUpon().appendPath(Float.toString(num)).appendPath("prev").build();
    }
    public Uri prevChapter(float num)
    {
        return prevChapter(id, num);
    }

    public static Uri nextChapter(int id, float num)
    {
        return uri(id).buildUpon().appendPath(Float.toString(num)).appendPath("next").build();
    }
    public Uri nextChapter(float num)
    {
        return nextChapter(id, num);
    }

    public static Uri genres(int id)
    {
        return uri(id).buildUpon().appendPath("genres").build();
    }
    public Uri genres()
    {
        return genres(id);
    }


    // Construction / Persistence ------------------------------------------------------------------
    public Series() { providerId = -1; }
    public Series(int providerId) { this.providerId = providerId; }
    public Series(Cursor c)
    {
        super(c);
        providerId = getInt(c, providerIdCol);
        parse(c,true);
    }

    public Series(Cursor c, boolean close)
    {
        super(c);
        providerId = getInt(c, providerIdCol);
        parse(c,close);
    }

    private void parse(Cursor c, boolean close)
    {
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
}
