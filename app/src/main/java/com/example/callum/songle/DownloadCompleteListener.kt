package com.example.callum.songle

import android.graphics.Bitmap

interface DownloadCompleteListener {
    fun downloadComplete(result: ArrayList<ArrayList<Pair<Placemark, Bitmap?>>>)
}