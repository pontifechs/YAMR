package ninja.dudley.yamr.model.mangapanda;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

/**
 * Created by mdudley on 5/29/15.
 */
public class MangaPandaPage
{

    protected void scrapePage()
    {
        try
        {
            Document doc = Jsoup.connect("url").get();
            Element element = doc.select("img[src]").first();
            String ans = element.absUrl("src");
        }
        catch (IOException e)
        {
            // Shrug?
            throw new RuntimeException(e);
        }
    }
}
