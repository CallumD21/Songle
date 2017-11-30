package com.example.callum.songle

interface DownloadCompleteListener {
    fun downloadComplete(result: ArrayList<Placemark>)
}