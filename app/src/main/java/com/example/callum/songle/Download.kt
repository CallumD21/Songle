package com.example.callum.songle

import android.content.Context
import android.graphics.Bitmap
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


class Download(private val caller: DownloadCompleteListener) : AsyncTask<String,Void,Container>(){
    override fun doInBackground(vararg urls: String): Container {
        //if an error return an empty container
        var container = Container(ArrayList<ArrayList<Pair<Placemark,Bitmap?>>>(),ArrayList<Song>(),"")
        return try{
            //Load the songs
            var cont = loadSongsFromNetwork(urls[0],urls[7])
            //Load the placemarkers and lyrics
            var lyrics = loadTxtFromNetwork(urls[6])
            var i = 1
            var maps = ArrayList<ArrayList<Pair<Placemark,Bitmap?>>>()
            while (i<6){
                var placemarkers = loadMapFromNetwork(urls[i])
                //Replace the name in the placemarker with the correct word
                for (pair in placemarkers){
                    var placemark = pair.first
                    //Split the pos to get the indices
                    val indices = placemark.pos.split(":")
                    //-1 because of zero indexing in the arraylist
                    placemark.word=lyrics[indices[0].toInt()-1][indices[1].toInt()-1]
                }
                maps.add(placemarkers)
                i++
            }
            var container = Container(maps,cont.songs,cont.timeStamp)
            container
        }catch (e:IOException){
            container
        }catch (e:XmlPullParserException){
            container
        }
    }

    private fun loadMapFromNetwork(urlString : String): ArrayList<Pair<Placemark,Bitmap?>>{
        var stream: InputStream? = null
        // Instantiate the parser
        val parser = MapXmlParser()
        val placmarkers: ArrayList<Pair<Placemark,Bitmap?>>

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

    private fun loadSongsFromNetwork(urlString : String, timeStamp: String): Container{
        var stream: InputStream? = null
        // Instantiate the parser
        val parser = SongXmlParser()
        val songs: Container

        try {
            stream = downloadUrl(urlString)
            songs = parser.parse(stream,timeStamp)
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close()
            }
        }
        return songs
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

    override fun onPostExecute(result: Container) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }

}