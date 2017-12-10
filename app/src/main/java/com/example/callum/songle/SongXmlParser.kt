package com.example.callum.songle

import android.graphics.Bitmap
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

class SongXmlParser {
    private val ns: String? = null

    //Returns a container with an empty map
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(input : InputStream, timeStamp: String):Container{
        input.use {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false)
            parser.setInput(input,null)
            parser.nextTag()
            return readSongs(parser,timeStamp)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readSongs(parser: XmlPullParser, timeStamp: String) : Container{
        val songs = ArrayList<Song>()
        parser.require(XmlPullParser.START_TAG,ns,"Songs")
        val newTimeStamp = parser.getAttributeValue(null,"timestamp")
        if (newTimeStamp!=timeStamp){
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                // Starts by looking for a song tag
                if (parser.name == "Song") {
                    songs.add(readSong(parser))
                } else {
                    skip(parser)
                }
            }
        }
        return Container(ArrayList<ArrayList<Pair<Placemark, Bitmap?>>>(),songs,newTimeStamp,"")
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readSong(parser: XmlPullParser): Song {
        parser.require(XmlPullParser.START_TAG, ns, "Song")
        var number = ""
        var artist = ""
        var title = ""
        var link = ""
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG)
                continue
            when(parser.name){
                    "Number" -> number = readNumber(parser)
                    "Artist" -> artist = readArtist(parser)
                    "Title" -> title = readTitle(parser)
                    "Link" -> link = readLink(parser)
                else -> skip(parser)
            }
        }
        return Song(number, artist, title, link)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readNumber(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "Number")
        val number = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "Number")
        return number
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readArtist(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "Artist")
        val artist = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "Artist")
        return artist
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTitle(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "Title")
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "Title")
        return title
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLink(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "Link")
        val link = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "Link")
        return link
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}