package com.example.callum.songle

interface DownloadCompleteListener {
    fun downloadXmlComplete(result: ArrayList<Placemark>)
    fun downloadTxtComplete(result: ArrayList<ArrayList<String>>)
}