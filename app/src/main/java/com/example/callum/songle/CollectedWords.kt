package com.example.callum.songle

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_collected_words.*
import kotlinx.android.synthetic.main.activity_guessed_songs.*

class CollectedWords : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collected_words)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        val listOfWords = MainActivity.wordsList
        for (i in listOfWords.indices){
            if (i>MainActivity.readWords){
                val text = listOfWords[i]
                val word = TextView(this)
                word.setText(text)
                word.textSize= 20f
                word.setPadding(50,20,0,0)
                word.setTextColor(Color.parseColor("#673AB7"))
                words.addView(word,0)
                MainActivity.readWords++
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            intent=Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivityIfNeeded(intent,0)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
