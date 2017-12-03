package com.example.callum.songle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import android.webkit.DownloadListener

class NetworkReceiver(private val caller: DownloadCompleteListener) : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        val Lyrics = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/01/lyrics.txt"
        val Map1 = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/01/map1.kml"
        val Map2 = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/01/map2.kml"
        val Map3 = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/01/map3.kml"
        val Map4 = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/01/map4.kml"
        val Map5 = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/01/map5.kml"
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        //Use wifi if it is available
        if(networkInfo?.type==ConnectivityManager.TYPE_WIFI){
            Download(caller).execute(Map1,Map2,Map3,Map4,Map5,Lyrics)

        }
        //Else use whatever connection is available
        else if(networkInfo !=null){
            Download(caller).execute(Map1,Map2,Map3,Map4,Map5,Lyrics)
        }
        else{
            //No connection
            Log.d("MYAPP","No network connection!")
        }
    }
}