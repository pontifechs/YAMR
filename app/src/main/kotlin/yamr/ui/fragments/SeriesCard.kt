package ninja.dudley.yamr.ui.fragments

import android.app.Fragment
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

import org.jsoup.helper.StringUtil

import java.util.ArrayList

import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Series

public class SeriesCard : Fragment()
{

    private var series: Series? = null

    private var thumbnail: ImageView? = null
    private var seriesName: TextView? = null
    private var firstSlot: TextView? = null
    private var secondSlot: TextView? = null
    private var thirdSlot: TextView? = null
    private var fourthSlot: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        if (getArguments() != null)
        {
            val seriesUri = Uri.parse(getArguments().getString(ArgumentKey))
            series = Series(getActivity().getContentResolver().query(seriesUri, null, null, null, null))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val layout = inflater.inflate(R.layout.fragment_series_card, container, false) as RelativeLayout
        thumbnail = layout.findViewById(R.id.seriesThumbnail) as ImageView
        seriesName = layout.findViewById(R.id.seriesName) as TextView
        firstSlot = layout.findViewById(R.id.firstSlot) as TextView
        secondSlot = layout.findViewById(R.id.secondSlot) as TextView
        thirdSlot = layout.findViewById(R.id.thirdSlot) as TextView
        fourthSlot = layout.findViewById(R.id.fourthSlot) as TextView

        val d: Drawable?
        if (series!!.thumbnailPath != null)
        {
            d = Drawable.createFromPath(series!!.thumbnailPath)
        }
        else
        {
            d = getResources().getDrawable(R.drawable.panic, null)
        }

        if (d != null)
        {
            thumbnail!!.setImageDrawable(d)
        }

        seriesName!!.setText(series!!.name)

        val info = ArrayList<String>()
        if (!StringUtil.isBlank(series!!.author))
        {
            info.add("<b>Author: </b>" + series!!.author!!)
        }
        if (!StringUtil.isBlank(series!!.artist))
        {
            info.add("<b>Artist: </b>" + series!!.artist!!)
        }
        info.add("<b>Status: </b>" + (if (series!!.complete) "Complete" else "Ongoing"))
        if (!StringUtil.isBlank(series!!.description))
        {
            info.add(series!!.description)
        }

        if (info.size() >= 1)
        {
            firstSlot!!.setText(Html.fromHtml(info.get(0)))
        }
        if (info.size() >= 2)
        {
            secondSlot!!.setText(Html.fromHtml(info.get(1)))
        }
        if (info.size() >= 3)
        {
            thirdSlot!!.setText(Html.fromHtml(info.get(2)))
        }
        if (info.size() >= 4)
        {
            fourthSlot!!.setText(Html.fromHtml(info.get(3)))
        }

        // Inflate the layout for this fragment
        return layout
    }

    companion object
    {
        public val ArgumentKey: String = "series_arg"

        public fun newInstance(s: Series): SeriesCard
        {
            val fragment = SeriesCard()
            val args = Bundle()
            args.putString(ArgumentKey, s.uri().toString())
            fragment.setArguments(args)
            return fragment
        }
    }

}
