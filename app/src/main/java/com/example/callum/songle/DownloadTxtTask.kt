package com.example.callum.songle

import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.xmlpull.v1.XmlPullParserException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


class DownloadTxtTask(private val caller: DownloadCompleteListener) : AsyncTask<String,Void,ArrayList<ArrayList<String>>>(){
    override fun doInBackground(vararg urls: String): ArrayList<ArrayList<String>> {
        var error = ArrayList<ArrayList<String>>()
        return try{
            loadTxtFromNetwork(urls[0])
        }catch (e:IOException){
            //Unable to load content
            val list = ArrayList<String>()
            val str = "Check your network connection"
            list.add(str)
            error.add(list)
            error
        }catch (e:XmlPullParserException){
            val list = ArrayList<String>()
            val str = "Error parsing XML"
            list.add(str)
            error.add(list)
            error
            error
        }
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

    override fun onPostExecute(result: ArrayList<ArrayList<String>>) {
        super.onPostExecute(result)
        caller.downloadTxtComplete(result)
    }

    fun readTxt(input: InputStream): ArrayList<ArrayList<String>>{
        val scanner =Scanner(input)
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

}