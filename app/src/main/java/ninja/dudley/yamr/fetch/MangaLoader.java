package ninja.dudley.yamr.fetch;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;

/**
 * Created by mdudley on 6/3/15.
 */
public class MangaLoader extends CursorLoader
{
    public MangaLoader(Context context)
    {
        super(context);
    }

    public MangaLoader(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        super(context, uri, projection, selection, selectionArgs, sortOrder);
    }
}
