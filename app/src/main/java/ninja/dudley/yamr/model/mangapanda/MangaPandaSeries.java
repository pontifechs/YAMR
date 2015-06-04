package ninja.dudley.yamr.model.mangapanda;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Iterator;

import ninja.dudley.yamr.fetch.Fetcher;

/**
 * Created by mdudley on 5/29/15.
 */
public class MangaPandaSeries
{


    public MangaPandaSeries(Fetcher parent, String url)
    {
        try
        {
            Document doc = Jsoup.connect("url").get();
            Element e = doc.select(".aname").first();
            String name = e.text();
        }
        catch (IOException e)
        {
            //Shrug?
            throw new RuntimeException(e);
        }
    }

    public void scrapeChapters()
    {
        try
        {
            Document doc = Jsoup.connect("url").get();
            Elements elements = doc.select("td .chico_manga ~ a[href]");

            for (Iterator<Element> it = elements.iterator(); it.hasNext(); )
            {
                Element e = it.next();
                String chapterUrl = e.attr("abs:href");
            }
        }
        catch (IOException e)
        {
            // Panic? IDK what might cause this.
            throw new RuntimeException(e);
        }
    }
}
