package ninja.dudley.yamr;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ninja.dudley.yamr.fetch.Fetcher;
import ninja.dudley.yamr.fetch.impl.MangaPandaFetcher;
import ninja.dudley.yamr.model.Series;

public class SeriesViewer extends ListActivity
{

    private SeriesAdapter adapter;
    private class SeriesAdapter extends BaseAdapter
    {
        private List<Series> series;

        private Context context;

        public SeriesAdapter(Context context, List<Series> series)
        {
            this.context = context;
            this.series = series;
        }

        @Override
        public int getCount()
        {
            return series.size();
        }

        @Override
        public Series getItem(int position)
        {
            return series.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View view = convertView;

            if (view == null)
            {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.chapter_item, parent, false);
            }

            final Series series = getItem(position);
            TextView text = (TextView) view.findViewById(R.id.textView);
            text.setText(series.getName());

            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Intent intent = new Intent(context, ChapterViewer.class);
//                    intent.putExtra(ChapterViewer.CHAPTER_INTENT_MESSAGE, series);
                    startActivity(intent);
                }
            });
            return view;
        }
    }

    private List<Series> series;

    private Fetcher fetcher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_viewer);

        Intent i = new Intent(this, MangaPandaFetcher.class);
        startService(i);
    }
}
