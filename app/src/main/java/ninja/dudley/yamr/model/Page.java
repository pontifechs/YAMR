package ninja.dudley.yamr.model;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

/**
 * Created by mdudley on 5/19/15.
 */
public class Page implements Parcelable
{
    private File storage;

    public Page(String path)
    {
        storage = new File(path);
    }

    public Page(Parcel in)
    {
        String path = in.readString();
        storage = new File(path);
    }

    public Page(File file)
    {
        this.storage = file;
    }


    public Drawable image()
    {
        return Drawable.createFromPath(storage.getAbsolutePath());
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(storage.getAbsolutePath());
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
    {
        public Page createFromParcel(Parcel in)
        {
            return new Page(in);
        }

        public Page[] newArray(int size)
        {
            return new Page[size];
        }
    };
}
