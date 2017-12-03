package com.example.callum.songle

import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


class Download(private val caller: DownloadCompleteListener) : AsyncTask<String,Void,ArrayList<ArrayList<Placemark>>>(){
    override fun doInBackground(vararg urls: String): ArrayList<ArrayList<Placemark>> {
        var error = ArrayList<ArrayList<Placemark>>()
        var point = Pair(0.0,0.0)
        return try{
            //Load the placemarkers and lyrics
            var lyrics = loadTxtFromNetwork(urls[5])
            var i = 0
            var maps = ArrayList<ArrayList<Placemark>>()
            while (i<5){
                var placemarkers = loadXmlFromNetwork(urls[i])
                //Replace the name in the placemarker with the correct word
                for (placemark in placemarkers){
                    //Split the name to get the indices
                    val indices = placemark.name.split(":")
                    //-1 because of zero indexing in the arraylist
                    placemark.name=lyrics[indices[0].toInt()-1][indices[1].toInt()-1]
                }
                maps.add(placemarkers)
                i++
            }
            maps
        }catch (e:IOException){
            //Unable to load content
            val placemarker = Placemark("Check your network connection",null,point)
            val placemarkers = ArrayList<Placemark>()
            placemarkers.add(placemarker)
            error.add(placemarkers)
            error
        }catch (e:XmlPullParserException){
            val placemarker = Placemark("Error parsing XML",null,point)
            val placemarkers = ArrayList<Placemark>()
            placemarkers.add(placemarker)
            error.add(placemarkers)
            error
        }
    }

    private fun loadXmlFromNetwork(urlString : String): ArrayList<Placemark>{
        var stream: InputStream? = null
        // Instantiate the parser
        val parser = MapXmlParser()
        val placmarkers: ArrayList<Placemark>

        try {
            stream = downloadUrl(urlString)
            placmarkers = parser.parse(stream)
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close()
            }
        }
        return placmarkers
    }

    private fun loadTxtFromNetwork(urlString : String): ArrayList<ArrayList<String>>{
        var stream: InputStream? = null
        val lyrics: ArrayList<ArrayList<String>>

        try {
            stream = downloadUrl(urlString)
            lyrics = readTxt(stream)
            // Makes sure that the InputStream is closed after the app is finished using it.
        } finally {
            if (stream != null) {
                stream.close()
            }
        }
        return lyrics
    }

    fun readTxt(input: InputStream): ArrayList<ArrayList<String>>{
        val scanner = Scanner(input)
        var lyrics = ArrayList<ArrayList<String>>()
        var listOfWords = ArrayList<String>();
        var line: String
        while (scanner.hasNextLine()) {
            line = scanner.nextLine()
            listOfWords=ArrayList(line.split(" "))
            lyrics.add(listOfWords)
        }
        return lyrics
    }

    @Throws(IOException::class)
    private fun downloadUrl(urlString : String):InputStream{
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.doInput = true

        //Start the query
        conn.connect()
        return  conn.inputStream
    }

    override fun onPostExecute(result: ArrayList<ArrayList<Placemark>>) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }

}