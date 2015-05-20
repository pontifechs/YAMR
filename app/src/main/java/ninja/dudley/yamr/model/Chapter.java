package ninja.dudley.yamr.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by mdudley on 5/19/15.
 */
public class Chapter implements Parcelable
{
    private File storage;

    public Chapter(String path)
    {
        storage = new File(path);
        if (!storage.isDirectory())
        {
            throw new RuntimeException("Incorrect path for chapter: " + path);
        }
    }

    public Chapter(File file)
    {
        this.storage = file;
    }


    public Chapter(Parcel in)
    {
        String path = in.readString();
        storage = new File(path);
    }


    public String name()
    {
        return storage.getName();
    }

    public String id()
    {
        return storage.getAbsolutePath();
    }

    public Page getFirstPage()
    {
        File[] files = storage.listFiles();

        Arrays.sort(files, new Comparator<File>()
        {
            @Override
            public int compare(File lhs, File rhs)
            {
                return lhs.getName().compareTo(rhs.getName());
            }
        });

        return new Page(files[0].getAbsolutePath());
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
        public Chapter createFromParcel(Parcel in)
        {
            return new Chapter(in);
        }

        public Chapter[] newArray(int size)
        {
            return new Chapter[size];
        }
    };
}
