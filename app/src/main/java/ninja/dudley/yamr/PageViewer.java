package ninja.dudley.yamr;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.util.TouchImageView;

public class PageViewer extends Activity
{
    public static final String PAGE_INTENT_MESSAGE = "ninja.dudley.yamr.PageViewer.page";

    private Page page;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_viewer);

        TouchImageView imageView = (TouchImageView) findViewById(R.id.imageView);
        Intent intent = getIntent();
        page = intent.getParcelableExtra(PAGE_INTENT_MESSAGE);
        //imageView.setImageDrawable(page.image());
    }
}



