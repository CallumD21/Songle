package com.example.callum.songle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import android.webkit.DownloadListener

public class NetworkReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        val URL = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/01/map1.kml"
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        //Use wifi if it is available
        if(networkInfo?.type==ConnectivityManager.TYPE_WIFI){
            DownloadXmlTask().execute(URL)
        }
        //Else use whatever connection is available
        else if(networkInfo !=null){
            DownloadXmlTask().execute(URL)
        }
        else{
            //No connection
            Log.d("MYAPP","No network connection!")
        }
    }
}