package ninja.dudley.yamr.model.mangapanda;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by mdudley on 5/29/15.
 */
public class MangaPandaChapter
{


    public void scrapePages()
    {
        try
        {
            Document doc = Jsoup.connect("url").get();
            Elements elements = doc.select("#pageMenu option[value]");

            for (Iterator<Element> it = elements.iterator(); it.hasNext();)
            {
                Element e = it.next();
                String pageUrl= e.absUrl("value");
            }
        }
        catch (IOException e)
        {
            // Shrug?
            throw new RuntimeException(e);
        }
    }
}
