package com.example.callum.songle

import android.graphics.Bitmap

//A container class for the output of Download
data class Container(val maps: ArrayList<ArrayList<Pair<Placemark, Bitmap?>>>, val songs: ArrayList<Song>, val timeStamp: String,val songNumber: String)