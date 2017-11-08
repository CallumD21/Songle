package com.example.callum.songle

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_achievement.view.*
import kotlinx.android.synthetic.main.activity_guessed_songs.*

class GuessedSongs : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guessed_songs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val song = TextView(this)
        song.setText("Bohemian Rhapsody")
        song.textSize= 20f
        song.setPadding(50,20,0,0)
        song.setTextColor(Color.parseColor("#673AB7"))
        ll.addView(song)
        val artist = TextView(this)
        artist.setText("By Queen")
        artist.textSize= 16f
        artist.setPadding(50,10,0,0)
        artist.setTextColor(Color.parseColor("#673AB7"))
        ll.addView(artist)
        val link = TextView(this)
        link.setText("https://youtu.be/fJ9rUzIMcZQ")
        link.textSize= 16f
        link.setPadding(50,10,0,100)
        ll.addView(link)

        val song2 = TextView(this)
        song2.setText("Mr. Brightside")
        song2.textSize= 20f
        song2.setPadding(50,20,0,0)
        song2.setTextColor(Color.parseColor("#673AB7"))
        ll.addView(song2)
        val artist2 = TextView(this)
        artist2.setText("By The Killers")
        artist2.textSize= 16f
        artist2.setPadding(50,10,0,0)
        artist2.setTextColor(Color.parseColor("#673AB7"))
        ll.addView(artist2)
        val link2 = TextView(this)
        link2.setText("https://youtu.be/gGdGFtwCNBE")
        link2.textSize= 16f
        link2.setPadding(50,10,0,100)
        ll.addView(link2)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
