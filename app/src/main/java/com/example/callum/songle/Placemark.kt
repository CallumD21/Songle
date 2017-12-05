package com.example.callum.songle

//The word is the word the marker is representing, the pos is the position of the word in the lyrics
//the point is the latitude and longitude. The fileName is the name of the file the bitmap of the
//marker is being stored in
data class Placemark(var word: String, val pos: String, val point: Pair<Double,Double>, var fileName: String)