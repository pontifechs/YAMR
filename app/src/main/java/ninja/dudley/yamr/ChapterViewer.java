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

import java.util.ArrayList;
import java.util.List;

import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Series;


public class ChapterViewer extends ListActivity
{
    public static final String CHAPTER_INTENT_MESSAGE = "ninja.dudley.yamr.ChapterViewer.series";

    private class ChapterAdapter extends BaseAdapter
    {
        private List<Chapter> chapters;

        private Context context;

        public ChapterAdapter(Context context, List<Chapter> chapters)
        {
            this.context = context;
            this.chapters = chapters;
        }

        @Override
        public int getCount()
        {
            return chapters.size();
        }

        @Override
        public Chapter getItem(int position)
        {
            return chapters.get(position);
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

            final Chapter chapter = getItem(position);
            TextView text = (TextView) view.findViewById(R.id.textView);
            //text.setText(chapter.name());

            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Intent intent = new Intent(context, PageViewer.class);
                    //intent.putExtra(PageViewer.PAGE_INTENT_MESSAGE, chapter.getFirstPage());
                    startActivity(intent);
                }
            });
            return view;
        }
    }

    List<Chapter> chapters = new ArrayList<Chapter>();
    ChapterAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_viewer);

        Intent intent = getIntent();
        Series series = intent.getParcelableExtra(CHAPTER_INTENT_MESSAGE);

        chapters = series.getChapters();
        adapter = new ChapterAdapter(this, chapters);
        setListAdapter(adapter);

        TextView textView = (TextView) findViewById(R.id.textView);
//        textView.setText(series.name());
    }
}
