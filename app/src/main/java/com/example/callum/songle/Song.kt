package com.example.callum.songle

//The word is the word the marker is representing, the pos is the position of the word in the lyrics
//the point is the latitude and longitude. The fileName is the name of the file the bitmap of the
//marker is being stored in
data class Song(var number: String, val artist: String, val title: String, var link: String)