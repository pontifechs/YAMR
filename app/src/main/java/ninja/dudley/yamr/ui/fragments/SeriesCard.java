package ninja.dudley.yamr.ui.fragments;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.List;

import ninja.dudley.yamr.R;
import ninja.dudley.yamr.model.Series;

public class SeriesCard extends Fragment
{
    public static final String ArgumentKey = "series_arg";

    private Series series;

    private ImageView thumbnail;
    private TextView seriesName;
    private TextView firstSlot;
    private TextView secondSlot;
    private TextView thirdSlot;
    private TextView fourthSlot;

    public static SeriesCard newInstance(Series s)
    {
        SeriesCard fragment = new SeriesCard();
        Bundle args = new Bundle();
        args.putString(ArgumentKey, s.uri().toString());
        fragment.setArguments(args);
        return fragment;
    }

    public SeriesCard() {}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            Uri seriesUri = Uri.parse(getArguments().getString(ArgumentKey));
            series = new Series(getActivity().getContentResolver().query(seriesUri, null, null, null, null));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_series_card, container, false);
        thumbnail = (ImageView) layout.findViewById(R.id.seriesThumbnail);
        seriesName = (TextView) layout.findViewById(R.id.seriesName);
        firstSlot = (TextView) layout.findViewById(R.id.firstSlot);
        secondSlot = (TextView) layout.findViewById(R.id.secondSlot);
        thirdSlot = (TextView) layout.findViewById(R.id.thirdSlot);
        fourthSlot = (TextView) layout.findViewById(R.id.fourthSlot);

        Drawable d;
        if (series.getThumbnailPath() != null)
        {
            d = Drawable.createFromPath(series.getThumbnailPath());
        }
        else
        {
            d = getResources().getDrawable(R.drawable.panic);
        }

        if (d != null)
        {
            thumbnail.setImageDrawable(d);
        }

        seriesName.setText(series.getName());

        List<String> info = new ArrayList<>();
        if (!StringUtil.isBlank(series.getAuthor()))
        {
            info.add("<b>Author: </b>" + series.getAuthor());
        }
        if (!StringUtil.isBlank(series.getArtist()))
        {
            info.add("<b>Artist: </b>" + series.getArtist());
        }
        info.add("<b>Status: </b>" + (series.isComplete() ? "Complete" : "Ongoing"));
        if (!StringUtil.isBlank(series.getDescription()))
        {
            info.add(series.getDescription());
        }

        if (info.size() >= 1)
        {
            firstSlot.setText(Html.fromHtml(info.get(0)));
        }
        if (info.size() >= 2)
        {
            secondSlot.setText(Html.fromHtml(info.get(1)));
        }
        if (info.size() >= 3)
        {
            thirdSlot.setText(Html.fromHtml(info.get(2)));
        }
        if (info.size() >= 4)
        {
            fourthSlot.setText(Html.fromHtml(info.get(3)));
        }

        // Inflate the layout for this fragment
        return layout;
    }

}
