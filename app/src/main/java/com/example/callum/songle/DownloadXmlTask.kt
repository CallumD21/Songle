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



class DownloadXmlTask(private val caller: DownloadCompleteListener) : AsyncTask<String,Void,ArrayList<Placemark>>(){
    override fun doInBackground(vararg urls: String): ArrayList<Placemark> {
        var error = ArrayList<Placemark>()
        var point = Pair(0.0,0.0)
        return try{
            loadXmlFromNetwork(urls[0])
        }catch (e:IOException){
            //Unable to load content
            val placemarker = Placemark("Check your network connection",null,point)
            error.add(placemarker)
            error
        }catch (e:XmlPullParserException){
            val placemarker = Placemark("Error parsing XML",null,point)
            error.add(placemarker)
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

    override fun onPostExecute(result: ArrayList<Placemark>) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }

}