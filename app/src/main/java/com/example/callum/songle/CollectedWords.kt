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
        //Load the saved words from the previous plays
        val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val gson = Gson()
        val json = preferences.getString("CollectedWords","ERROR")
        //json is "ERROR" if no words have yet been saved so initialize wordsList to the empty Array
        if (json=="ERROR"){
            Log.d("MYAPP","ERROR")
            MainActivity.wordsList = ArrayList()
        }
        else{
            MainActivity.wordsList = gson.fromJson(json, ArrayList<String>().javaClass)
        }
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

    override fun onStop() {
        super.onStop()
        //Save the collected words
        val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val editor = preferences.edit()
        val gson = Gson()
        val json = gson.toJson(MainActivity.wordsList)
        editor.putString("CollectedWords", json)
        editor.apply()
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
