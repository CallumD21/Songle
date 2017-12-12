package com.example.callum.songle

import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


class Download(private val caller: DownloadCompleteListener, private val songList: ArrayList<Song>) : AsyncTask<String,Void,Container>(){
    override fun doInBackground(vararg args: String): Container {
        //if an error return an empty container
        val container = Container(ArrayList<ArrayList<Pair<Placemark,Bitmap?>>>(),ArrayList<Song>(),"","")
        return try{
            //Load the songs
            val cont = loadSongsFromNetwork(args[0],args[1])
            //If loadSongsFromNetwork doesn't load any songs then the song list is the one passed in
            val listOfSongs : ArrayList<Song>
            if (cont.songs.size==0){
                listOfSongs=songList
            }
            else{
                listOfSongs=cont.songs
            }
            val newSong = pickASong(listOfSongs,MainActivity.guessedSongs,args[2])
            val urls = ArrayList<String>()
            urls.add("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"+newSong+"/lyrics.txt")
            urls.add("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"+newSong+"/map1.kml")
            urls.add("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"+newSong+"/map2.kml")
            urls.add("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"+newSong+"/map3.kml")
            urls.add("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"+newSong+"/map4.kml")
            urls.add("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"+newSong+"/map5.kml")
            //Load the placemarkers and lyrics
            var lyrics = loadTxtFromNetwork(urls[0])
            var i = 1
            var maps = ArrayList<ArrayList<Pair<Placemark,Bitmap?>>>()
            while (i<6){
                var placemarkers = loadMapFromNetwork(urls[i])
                //Replace the name in the placemarker with the correct word
                for (pair in placemarkers){
                    var placemark = pair.first
                    //Split the pos to get the indices
                    val indices = placemark.pos.split(":")
                    //-1 because of zero indexing in the arraylist
                    placemark.word=lyrics[indices[0].toInt()-1][indices[1].toInt()-1]
                }
                maps.add(placemarkers)
                i++
            }
            var container = Container(maps,listOfSongs,cont.timeStamp,newSong)
            container
        }catch (e:IOException){
            container
        }catch (e:XmlPullParserException){
            container
        }
    }

    private fun loadMapFromNetwork(urlString : String): ArrayList<Pair<Placemark,Bitmap?>>{
        var stream: InputStream? = null
        // Instantiate the parser
        val parser = MapXmlParser()
        val placmarkers: ArrayList<Pair<Placemark,Bitmap?>>

        try {
            stream = downloadUrl(urlString)
            placmarkers = parser.parse(stream)
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close()
            }
        }
        return placmarkers
    }

    private fun loadTxtFromNetwork(urlString : String): ArrayList<ArrayList<String>>{
        var stream: InputStream? = null
        val lyrics: ArrayList<ArrayList<String>>

        try {
            stream = downloadUrl(urlString)
            lyrics = readTxt(stream)
            // Makes sure that the InputStream is closed after the app is finished using it.
        } finally {
            if (stream != null) {
                stream.close()
            }
        }
        return lyrics
    }

    private fun loadSongsFromNetwork(urlString : String, timeStamp: String): Container{
        var stream: InputStream? = null
        // Instantiate the parser
        val parser = SongXmlParser()
        val songs: Container

        try {
            stream = downloadUrl(urlString)
            songs = parser.parse(stream,timeStamp)
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close()
            }
        }
        return songs
    }

    fun readTxt(input: InputStream): ArrayList<ArrayList<String>>{
        val scanner = Scanner(input)
        var lyrics = ArrayList<ArrayList<String>>()
        var listOfWords = ArrayList<String>();
        var line: String
        while (scanner.hasNextLine()) {
            line = scanner.nextLine()
            listOfWords=ArrayList(line.split(" "))
            lyrics.add(listOfWords)
        }
        return lyrics
    }

    @Throws(IOException::class)
    private fun downloadUrl(urlString : String):InputStream{
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.doInput = true

        //Start the query
        conn.connect()
        return  conn.inputStream
    }

    override fun onPostExecute(result: Container) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }

    //Randomly pick a non guessed song that does not have number numSong and return the number
    //of this song. When the user gives up on a song or all songs have been guessed it should not
    //pick the same song twice in a row. If all songs have been guessed just randomly pick a song
    //from the song list
    fun pickASong(songList : ArrayList<Song>, guessedSongs: ArrayList<Song>, numSong: String) : String{
        //The number of the new song
        val newSong : String
        //Remove the song numSong from both lists
        for (song in songList){
            if (song.number==numSong){
                songList.remove(song)
                break
            }
        }
        for (song in guessedSongs){
            if (song.number==numSong){
                songList.remove(song)
                break
            }
        }
        if (songList.size==guessedSongs.size){
            val index = Random().nextInt(songList.size)
            newSong = songList[index].number
        }
        else{
            //Create a list of non guessed songs
            var notGuessed = ArrayList<Song>()
            for (song in songList){
                if (!contains(guessedSongs,song)){
                    notGuessed.add(song)
                }
            }
            //Randomly pick a song
            val index = Random().nextInt(notGuessed.size)
            newSong = notGuessed[index].number
        }
        return newSong
    }

    //See if the song is in the list if it is then return true
    fun contains(list: ArrayList<Song>, song: Song): Boolean{
        var found = false
        for (item in list){
            //Two songs are the same if their numbers are the same
            if (song.number==item.number){
                found=true
                //Can now exit the loop
                break
            }
        }
        return found
    }


}