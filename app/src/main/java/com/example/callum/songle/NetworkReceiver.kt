package com.example.callum.songle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import android.webkit.DownloadListener

class NetworkReceiver(private val caller: DownloadCompleteListener, val timestamp : String, val songList : ArrayList<Song>) : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        val Songs = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml"
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        //Use wifi if it is available
        if(networkInfo?.type==ConnectivityManager.TYPE_WIFI){
            Download(caller,songList).execute(Songs,timestamp)

        }
        //Else use whatever connection is available
        else if(networkInfo !=null){
            Download(caller,songList).execute(Songs,timestamp)
        }
        else{
            //No connection
            Log.d("MYAPP","No network connection!")
        }
    }
}