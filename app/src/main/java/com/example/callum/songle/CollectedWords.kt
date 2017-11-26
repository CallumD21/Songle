package com.example.callum.songle

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_collected_words.*
import kotlinx.android.synthetic.main.activity_guessed_songs.*

class CollectedWords : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collected_words)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val word = TextView(this)
        word.setText("time")
        word.textSize= 20f
        word.setPadding(50,20,0,0)
        word.setTextColor(Color.parseColor("#673AB7"))
        words.addView(word,0)
        val word1 = TextView(this)
        word1.setText("way")
        word1.textSize= 20f
        word1.setPadding(50,20,0,0)
        word1.setTextColor(Color.parseColor("#673AB7"))
        words.addView(word1,0)
        val word2 = TextView(this)
        word2.setText("Magnifico-o-o-o-o")
        word2.textSize= 20f
        word2.setPadding(50,20,0,0)
        word2.setTextColor(Color.parseColor("#673AB7"))
        words.addView(word2,0)
        val word3 = TextView(this)
        word3.setText("a")
        word3.textSize= 20f
        word3.setPadding(50,20,0,0)
        word3.setTextColor(Color.parseColor("#673AB7"))
        words.addView(word3,0)
        val word4 = TextView(this)
        word4.setText("Beelzebub")
        word4.textSize= 20f
        word4.setPadding(50,20,0,0)
        word4.setTextColor(Color.parseColor("#673AB7"))
        words.addView(word4,0)
        val word5 = TextView(this)
        word5.setText("and")
        word5.textSize= 20f
        word5.setPadding(50,20,0,0)
        word5.setTextColor(Color.parseColor("#673AB7"))
        words.addView(word5,0)
        val word6 = TextView(this)
        word6.setText("you")
        word6.textSize= 20f
        word6.setPadding(50,20,0,0)
        word6.setTextColor(Color.parseColor("#673AB7"))
        words.addView(word6,0)
        val word7 = TextView(this)
        word7.setText("Scaramouche")
        word7.textSize= 20f
        word7.setPadding(50,20,0,0)
        word7.setTextColor(Color.parseColor("#673AB7"))
        words.addView(word7,0)
        val word8 = TextView(this)
        word8.setText("you")
        word8.textSize= 20f
        word8.setPadding(50,20,0,0)
        word8.setTextColor(Color.parseColor("#673AB7"))
        words.addView(word8,0)
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
