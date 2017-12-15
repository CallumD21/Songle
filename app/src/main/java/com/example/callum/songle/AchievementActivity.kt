package com.example.callum.songle

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_achievement.*
import org.jetbrains.anko.find
import java.util.ArrayList

class AchievementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievement)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        //Set the progress bars progress rounding the doubles
        pb1.progress=MainActivity.achievements[0].toInt()
        pb2.progress=MainActivity.achievements[1].toInt()
        pb3.progress=MainActivity.achievements[2].toInt()
        pb4.progress=MainActivity.achievements[3].toInt()
        pb5.progress=MainActivity.achievements[4].toInt()
        pb6.progress=MainActivity.achievements[5].toInt()
        pb7.progress=MainActivity.achievements[6].toInt()
        pb8.progress=MainActivity.achievements[7].toInt()
        pb9.progress=MainActivity.achievements[8].toInt()
        pb10.progress=MainActivity.achievements[9].toInt()
        pb11.progress=MainActivity.achievements[10].toInt()
        pb12.progress=MainActivity.achievements[11].toInt()
        pb13.progress=MainActivity.achievements[12].toInt()
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
