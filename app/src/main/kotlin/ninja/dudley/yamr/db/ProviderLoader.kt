package ninja.dudley.yamr.db

import android.content.ContentValues
import android.content.Context
import ninja.dudley.yamr.model.MangaElement
import ninja.dudley.yamr.model.Provider
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.ArrayList


/**
 * Created by mdudley on 10/10/15.
 */
public class ProviderLoader()
{
    public companion object
    {
        private fun loadFiles(context: Context): List<JSONObject>
        {
            val jsons = ArrayList<JSONObject>()

            val assetManager = context.assets

            assetManager.list("providers").forEach(
                    {
                        val inputStream = assetManager.open("providers/$it")
                        val reader = BufferedReader(InputStreamReader(inputStream));
                        val fileString = StringBuilder();
                        var line: String? = ""

                        while (line != null) {
                            fileString.append(line);
                            line = reader.readLine()
                        }

                        jsons.add(JSONObject(fileString.toString()))
                    }
            )
            return jsons
        }

        public fun loadProviders(context: Context): List<ContentValues>
        {
            val providers = ArrayList<ContentValues>()
            loadFiles(context).forEach(
                    {
                        val provider = ContentValues()
                        provider.put(MangaElement.urlCol, it.getString("url"))
                        provider.put(MangaElement.typeCol, "Provider")
                        provider.put(Provider.nameCol, it.getString("name"))
                        provider.put(Provider.newUrlCol, it.getString("newUrl"))
                        provider.put(Provider.fetchProviderCol, consolidate(it.getJSONArray("fetchProvider")))
                        provider.put(Provider.stubSeriesCol, consolidate(it.getJSONArray("stubSeries")))
                        provider.put(Provider.fetchSeriesCol, consolidate(it.getJSONArray("fetchSeries")))
                        provider.put(Provider.fetchSeriesGenresCol, consolidate(it.getJSONArray("fetchSeriesGenres")))
                        provider.put(Provider.stubChapterCol, consolidate(it.getJSONArray("stubChapter")))
                        provider.put(Provider.fetchChapterCol, consolidate(it.getJSONArray("fetchChapter")))
                        provider.put(Provider.stubPageCol, consolidate(it.getJSONArray("stubPage")))
                        provider.put(Provider.fetchPageCol, consolidate(it.getJSONArray("fetchPage")))
                        provider.put(Provider.fetchNewCol, consolidate(it.getJSONArray("fetchNew")))
                        providers.add(provider)
                    }
            )
            return providers
        }

        private fun consolidate(lines: JSONArray): String
        {
            val builder = StringBuilder()
            for (i in 0..lines.length()-1)
            {
                builder.append(lines.getString(i)).append("\n")
            }
            return builder.toString()
        }
    }
}