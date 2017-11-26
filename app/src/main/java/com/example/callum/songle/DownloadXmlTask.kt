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



class DownloadXmlTask() : AsyncTask<String,Void,String>(){
    override fun doInBackground(vararg urls: String): String {
        return try{
            loadXmlFromNetwork(urls[0])
        }catch (e:IOException){
            //Unable to load content
            "Unable to load content"
        }catch (e:XmlPullParserException){
            "Error parsing XML"
        }
    }

    private fun loadXmlFromNetwork(urlString : String): String{
        val result = StringBuilder()
        var stream: InputStream? = null
        // Instantiate the parser
        val parser = MapXmlParser()
        var placmarkers: List<Placemark>? = null

        try {
            stream = downloadUrl(urlString)
            placmarkers = parser.parse(stream)
            Log.d("MYAPP",placmarkers.size.toString())
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close()
            }
        }
        return result.toString()
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

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
    }

}