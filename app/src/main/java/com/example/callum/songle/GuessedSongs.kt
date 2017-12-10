package com.example.callum.songle

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_guessed_songs.*

class GuessedSongs : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guessed_songs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        val listOfSongs = MainActivity.guessedSongs
        for (i in listOfSongs.indices){
            if (i>MainActivity.readSongs){
                //Add the song
                var text = listOfSongs[i].title
                val song = TextView(this)
                song.setText(text)
                song.textSize= 20f
                song.setPadding(50,20,0,0)
                song.setTextColor(Color.parseColor("#673AB7"))
                songs.addView(song)
                //Add the song
                text = listOfSongs[i].artist
                val artist = TextView(this)
                artist.setText(text)
                artist.textSize= 16f
                artist.setPadding(50,10,0,0)
                artist.setTextColor(Color.parseColor("#673AB7"))
                songs.addView(artist)
                //Add the youtube link
                text = listOfSongs[i].link
                val link = TextView(this)
                link.setText(text)
                link.textSize= 16f
                link.setPadding(50,10,0,100)
                songs.addView(link)
                //When the link is clicked open youtube
                link.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW,Uri.parse(link.text.toString()))
                    startActivity(intent)
                }
                MainActivity.readSongs++
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val intent = Intent(this,MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivityIfNeeded(intent,0)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
