package ninja.dudley.yamr.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by mdudley on 5/19/15.
 */
public class Series implements Parcelable
{
    private File storage;

    public Series(String path)
    {
        storage = new File(path);
    }

    public Series(Parcel in)
    {
        String path = in.readString();
        storage = new File(path);
    }

    public Series(File file)
    {
        this.storage = file;
    }

    public String name()
    {
        return this.storage.getName();
    }

    public List<Chapter> getChapters()
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

        List<Chapter> chapters = new ArrayList<Chapter>();
        for (File f : files)
        {
            chapters.add(new Chapter(f));
        }
        return chapters;
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
        public Series createFromParcel(Parcel in)
        {
            return new Series(in);
        }

        public Series[] newArray(int size)
        {
            return new Series[size];
        }
    };
}
