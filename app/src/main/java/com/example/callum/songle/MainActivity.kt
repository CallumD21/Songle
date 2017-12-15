package com.example.callum.songle

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_collected_words.view.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.correct.view.*
import kotlinx.android.synthetic.main.guess_dialog.*
import kotlinx.android.synthetic.main.guess_dialog.view.*
import kotlinx.android.synthetic.main.incorrect.view.*
import kotlinx.android.synthetic.main.activity_main.nav_view
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, DownloadCompleteListener{
    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    var mLocationPermissionGranted = false
    private var mLastLocation: Location? = null
    private var placemarkers = ArrayList<Pair<Placemark,Bitmap?>>()
    private var markers = ArrayList<Marker?>()
    //ArrayList of the pos of the collected words, used when the user collects "55:1" in map 1 for
    //example to mark "55:1" in map5 as also collected
    private var collectedPos = ArrayList<String>()
    //Loaded is true when the placemarkers and markers have been loaded
    private var loaded = false
    //True if the map need drawing when the map is ready
    private var draw = false
    //True if the map is ready and can be drawn on
    private var mapReady = false
    //A BroadcastReeceiver that monitors network connectivity changes
    private var receiver : NetworkReceiver? = null
    //The number of the active map 1..5. Defaults to 0 but it is always changed in onCreate
    private var activeMap = 0
    //Circle of radius 20 centered on the user
    private lateinit var circle : Circle
    //True if the first time the location has been changed
    private var locationChanged = true
    //The current song being played
    private lateinit var currentSong: Song
    //The total distance the user has walked while playing the game. Is used to calculate steps made
    private var distanceWalked = 0.0f
    //The number of achievements
    private val numAch = 20
    //Marker for the users location
    private lateinit var myLocation : Marker
    //The total number of words in the song
    private var totalWords = 0
    //True if the user has not yet had a guess of the song
    private var firstGuess = true

    companion object {
        //List of collected words for this song
        var wordsList = ArrayList<String>()
        var readWords = -1
        //List of the guessed songs
        var guessedSongs = ArrayList<Song>()
        var readSongs = -1
        //The progress made on each achievement
        var achievements = ArrayList<Double>()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Create an instance of GoogleApiClient
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()

        collect.setOnClickListener {
            val index=nearestMarker(mLastLocation)
            val word=placemarkers[index].first.word
            toast("You collected: "+word)
            wordsList.add(word)
            //This has now been collected
            collectedPos.add(placemarkers[index].first.pos)
            //Remove the marker from the map
            markers[index]!!.remove()
            if( markers[index] == null ) Log.e("PANIC","PANIC")
            markers[index]=null
            //If no more markers in the circle then remove the button
            if (!inCircle(mLastLocation)){
                collect.visibility = View.GONE
                collect_text.visibility = View.GONE
            }

            //The second achievement is to collect a word
            if (achievements[1]!=100.0){
                achievements[1]=100.0
                toast(R.string.ach)
            }
            //The eighth achievement is to collect all the words to the song
            if (achievements[7]!=100.0){
                if (wordsList.size==totalWords){
                    achievements[7]=100.0
                    toast(R.string.ach)
                }
                else{
                    //The percentage of the achievement completed
                    achievements[7]=(wordsList.size/totalWords.toDouble())*100
                }
            }

        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        //Restore preferences
        val preferences=getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val editor=preferences.edit()

        //Use true as the default value as if it doesnt exist it means it is the apps first run
        val firstRun=preferences.getBoolean("firstRun",true)
        //If it is the first run of the app open the help activity and download the new song
        if(firstRun){
            //Open help
            val intent = Intent(this,Help::class.java)
            startActivity(intent)
            //Set firstRun to false so it doesnt do this again
            editor.putBoolean("firstRun",false)
            editor.apply()
        }
        val downloadMap = preferences.getBoolean("downloadMap",true)
        if (downloadMap){
            //Network receiver needs the saved timestamp of Songs
            val timeStamp = preferences.getString("timeStamp","")
            val gson = Gson()
            var json = preferences.getString("Songs","ERROR")
            var songList = ArrayList<Song>()
            if (json!="ERROR"){
                val type = object : TypeToken<ArrayList<Song>>() {}.type
                songList = gson.fromJson<ArrayList<Song>>(json, type)
            }
            //load the current song
            json = preferences.getString("CurrentSong","ERROR")
            //If currentSong has not been saved then default number to the empty string
            var numSong = ""
            if (json!="ERROR"){
                val type = object : TypeToken<Song>() {}.type
                currentSong = gson.fromJson<Song>(json, type)
                numSong = currentSong.number
            }
            //Download the map
            receiver = NetworkReceiver(this,timeStamp,songList,numSong)
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            this.registerReceiver(receiver, filter)
        }

        //Use 3 as the default value as the difficulty defaults to medium(4)
        activeMap=preferences.getInt("activeMap",4)

        //Load all the saved data
        load()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_achievement -> {
                //Open the Achievement Activity
                val intent = Intent(this,AchievementActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivityIfNeeded(intent,0)
                return true
            }
            R.id.action_help -> {
                //Open the Help Activity
                val intent = Intent(this,Help::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivityIfNeeded(intent,0)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Close the bar unless they choose walk
        if (item.itemId != R.id.nav_walk){
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        when (item.itemId) {
            R.id.nav_guess -> {
                //If the user presses guess open a dialog designed in guess_dialog.xml
                val builder = AlertDialog.Builder(this)
                val layout = layoutInflater.inflate(R.layout.guess_dialog,null)
                builder.setView(layout)
                val dialog = builder.create()
                //Get the button
                val button = layout.submit
                button.setOnClickListener {
                    //Get the user inputted song title and artist name
                    //Put them to lowercase and compare with the current song name and title to
                    //lowercase
                    val title = layout.findViewById<EditText>(R.id.songTitle).text.toString().toLowerCase()
                    val artist = layout.findViewById<EditText>(R.id.artist).text.toString().toLowerCase()
                    if (title==currentSong.title.toLowerCase() && artist==currentSong.artist.toLowerCase()){
                        //Close this dialog
                        dialog.dismiss()
                        correct()
                    }
                    else{
                        //Close this dialog
                        dialog.dismiss()
                        incorrect()
                    }
                }
                dialog.show()
            }
            R.id.nav_words -> {
                //Open the CollectedWords Activity
                val intent = Intent(this,CollectedWords::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivityIfNeeded(intent,0)
            }
            R.id.nav_songs -> {
                //Open the Songs Activity
                val intent = Intent(this,GuessedSongs::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivityIfNeeded(intent,0)
            }
            R.id.nav_giveup -> {
                //If the user presses give up open an anko dialog
                alert("Are you sure you want to give up on this song?", "Give up!") {
                    positiveButton("Yes") {
                        //The 12th achievement is to give up on a song
                        if (achievements[11]!=100.0){
                            achievements[11]= 100.0
                            toast(R.string.ach)
                        }
                        changeSong()
                    }
                    negativeButton("No") {
                    }
                }.show()
            }
            R.id.nav_diff -> {
                val difficulties = listOf("Easy", "Medium", "Hard", "Really Hard", "Impossible")
                selector("Please choose a difficulty:", difficulties, { _, i ->
                    //i starts at 0 the difficulties start at one
                    //Easiest difficulty ois in Map 5
                    val difficulty = 5-i
                    if (activeMap!=difficulty){
                        //Remove the button
                        collect.visibility = View.GONE
                        collect_text.visibility = View.GONE
                        //Save the active map
                        saveMap(activeMap)
                        //Clear the map
                        for (marker in markers){
                            if (marker!=null){
                                marker.remove()
                            }
                        }
                        markers.clear()
                        placemarkers.clear()
                        //Load and draw the new map
                        loadMap(difficulty)
                        activeMap=difficulty
                        //Save activeMap
                        val preferences=getSharedPreferences("MainFile",Context.MODE_PRIVATE)
                        val editor=preferences.edit()
                        editor.putInt("activeMap",activeMap)
                        editor.apply()
                        toast("You have changed to ${difficulties[i]} difficulty!")
                    }
                    else{
                        toast("You are already on ${difficulties[i]} difficulty!")
                    }
                })
            }
        }
        return true
    }


    //Open the correct dialog and download the next song
    private fun correct(){
        val builder = AlertDialog.Builder(this)
        var layout = layoutInflater.inflate(R.layout.correct,null)
        builder.setView(layout)
        val dialog = builder.create()
        //Get the button
        val button = layout.correctButton
        //When the button is pressed close the dialog
        button.setOnClickListener { dialog.dismiss() }
        dialog.show()
        //If the song has not already been guessed then add it to guessed songs
        var found = false
        for (song in guessedSongs){
            if (song.number==currentSong.number){
                found=true
                break
            }
        }
        if (!found){
            guessedSongs.add(currentSong)
        }
        //The third achievement is to correctly guess a song
        if (achievements[2]!=100.0){
            achievements[2]=100.0
            toast(R.string.ach)
        }
        //The seventh achievement is to correctly guess a song having only collected one word
        if (achievements[6]!=100.0 && wordsList.size==1){
            achievements[6]=100.0
            toast(R.string.ach)
        }
        //The sixth achievement is to correctly guess the song first try
        if (achievements[5]!=100.0 && firstGuess){
            achievements[5]=100.0
            toast(R.string.ach)
        }

        changeSong()
    }

    private fun changeSong(){
        //Reset the progress on the eighth achievement if it has not yet been completed
        if (achievements[7]!=100.0){
            achievements[7]=0.0
        }
        //Reset the collected words for the song
        wordsList = ArrayList<String>()
        //-2 tells collected words to clear the screen
        readWords = -2
        //Clear the map
        for (marker in markers){
            if (marker!=null){
                marker.remove()
            }
        }
        //Reset the placemarkers and markers
        markers = ArrayList<Marker?>()
        placemarkers = ArrayList<Pair<Placemark,Bitmap?>>()
        //Clear the map files by saving empty maps
        val maps = ArrayList<ArrayList<Pair<Placemark,Bitmap?>>>()
        val map = ArrayList<Pair<Placemark,Bitmap?>>()
        //Add five empty maps
        var i=0
        while (i<5){
            maps.add(map)
            i++
        }
        saveAllMaps(maps)
        //Hide the button
        collect.visibility = View.GONE
        collect_text.visibility = View.GONE
        //Download the new song
        //Network receiver needs the saved timestamp of Songs
        val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val timeStamp = preferences.getString("timeStamp","")
        val gson = Gson()
        var json = preferences.getString("Songs","ERROR")
        var songList = ArrayList<Song>()
        if (json!="ERROR"){
            val type = object : TypeToken<ArrayList<Song>>() {}.type
            songList = gson.fromJson<ArrayList<Song>>(json, type)
        }
        //Set download map to true so if there is an error changing map it will download the map the
        //next time the app is opened
        val editor=preferences.edit()
        editor.putBoolean("downloadMap", true)
        editor.apply()
        receiver = NetworkReceiver(this,timeStamp,songList,currentSong.number)
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        this.registerReceiver(receiver, filter)
    }

    //Open the incorrect dialog
    private fun incorrect(){
        val builder = AlertDialog.Builder(this)
        val layout = layoutInflater.inflate(R.layout.incorrect,null)
        builder.setView(layout)
        val dialog = builder.create()
        //Get the button
        val button = layout.incorrectButton
        //When the button is pressed close the dialog
        button.setOnClickListener { dialog.dismiss() }
        //First guess is not not true
        firstGuess=false
        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient.connect()
    }

    override fun onStop() {
        super.onStop()
        //Save the data
        save()
        if(mGoogleApiClient.isConnected){
            mGoogleApiClient.disconnect()
        }
    }

    fun createLocationRequest(){
        //Set the paramaters for the location request
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 5000
        mLocationRequest.fastestInterval = 1000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        //See if we can access the users location
        val permissionCheck = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
        if(permissionCheck == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)
        }
    }

    override fun onConnected(connectionHint : Bundle?) {
        try { createLocationRequest() }
        catch (ise : IllegalStateException){
            Log.i("MYAPP","IllegalStateException thrown [onConnected]")
        }
        //If we cant access the users location then ask for permission
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onLocationChanged(current : Location?) {
        if(current == null){
            toast("Cannot determine your location!")
        } else {
            //Update the distance walked
            if (mLastLocation!=null){
                distanceWalked = distanceWalked + current.distanceTo(mLastLocation)
                //Round to the nearest integer
                //0.762 is the average step length in meters
                val steps = distanceWalked.div(0.762).toInt()
                this.nav_view.menu.findItem(R.id.nav_walk).title = steps.toString()+" steps"
                if (steps>0){
                    //If the walking achievements have not been completed update their progress
                    //The first achievement is to do a step
                    if (achievements[0]!=100.0){
                        achievements[0]= 100.0
                        toast(R.string.ach)
                    }
                    //The fourth achievement is to do 1,000 step
                    if (achievements[3]!=100.0){
                        if (steps>=1000){
                            achievements[3]=100.0
                            toast(R.string.ach)
                        }
                        else{
                            //The percentage of the achievement completed
                            achievements[3]=(steps/1000.0)*100
                        }
                    }
                    //The fifth achievement is to do 10,000 step
                    if (achievements[4]!=100.0){
                        if (steps>=10000){
                            achievements[4]=100.0
                            toast(R.string.ach)
                        }
                        else{
                            //The percentage of the achievement completed
                            achievements[4]=(steps/10000.0)*100
                        }
                    }
                }
                //The location of the castle's gatehouse
                val castle = Location("Castle")
                castle.latitude = 55.94853
                castle.longitude = -3.19868
                //The ninth achievement is to go to the castle so be within 40 meters of the castle gate
                if (achievements[8]!=100.0 && castle.distanceTo(current)<=40){
                    achievements[8]=100.0
                    toast(R.string.ach)
                }
                //The location of Waverly
                val waverly = Location("Waverly")
                waverly.latitude = 55.95200
                waverly.longitude = -3.18997
                //The tenth achievement is to go to waverly so be within 70 meters it
                if (achievements[9]!=100.0 && waverly.distanceTo(current)<=70){
                    achievements[9]=100.0
                    toast(R.string.ach)
                }
                //The location of Arthur's seat
                val arthurs = Location("Arthur's")
                arthurs.latitude = 55.94403
                arthurs.longitude = -3.16209
                //The 11th achievement is to go to Arthur's seat so be within 40 meters it
                if (achievements[10]!=100.0 && arthurs.distanceTo(current)<=40){
                    achievements[10]=100.0
                    toast(R.string.ach)
                }
            }
            val position = LatLng(current.latitude,current.longitude)
            //If first time this function has been called initialize the circle and myLocation
            if (locationChanged){
                val circleOptions = CircleOptions()
                circle = mMap.addCircle(circleOptions
                        .center(position)
                        .radius(20.0)
                        .strokeColor(Color.parseColor("#673AB7")))
                val marker = MarkerOptions().position(LatLng(current.latitude,current.longitude))
                //Set the marker icon as the location image and anchor centers the image to the point
                //zIndex brings the location marker above the word markers so it is clear where the user is
                marker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.location))
                        .anchor(0.5f,0.5f)
                        .zIndex(1f)
                myLocation = mMap.addMarker(marker)
                locationChanged = false
            }
            //Move the circle and location marker
            circle.center = position
            myLocation.position = LatLng(current.latitude,current.longitude)
            mLastLocation=current
            //If a point is within the circle of radius 20 centered at the users location then
            //display the collect button
            if (inCircle(mLastLocation)){
                collect.visibility = View.VISIBLE
                collect_text.visibility = View.VISIBLE
            }
            else{
                collect.visibility = View.GONE
                collect_text.visibility = View.GONE
            }
        }
    }

    override fun onConnectionSuspended(flag : Int) {
        //DO SOMETHING
        Log.i("MYAPP","[onConnectionSuspended]")
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        //DO SOMETHING
        Log.i("MYAPP","[onConnectionFailed]")
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mapReady = true
        mMap = googleMap

        //Zoom in on the play area
        val zoomLevel = 15.5f;
        //This is the centre of the play area
        val pos = LatLng(55.944425,-3.188396)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, zoomLevel));

        if (draw){
                drawMap()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (receiver!=null){
            this.unregisterReceiver(receiver)
        }
    }

    override fun downloadComplete(container: Container) {
        //If maps are empty an error occured
        if (container.maps.size==0) {
            toast("An error occurred while downloading! \nReopen the app to try again!")
        } else {
            //Save the timeStamp and songs list
            val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
            val editor=preferences.edit()
            editor.putString("timeStamp",container.timeStamp)
            val gson = Gson()
            var json = gson.toJson(container.songs)
            editor.putString("Songs", json)
            placemarkers = container.maps[activeMap-1]
            if (mapReady){
                drawMap()
            }
            else{
                //So when the map is ready it will draw the map
                draw = true
            }
            //Save all the maps
            saveAllMaps(container.maps)

            //Initialize the current song
            for (song in container.songs){
                if (song.number==container.songNumber){
                    currentSong=song
                    Log.i("MYAPP",currentSong.number+" "+currentSong.artist+" "+currentSong.title)
                    break
                }
            }
            //The song has now changed so firstGuess is true
            firstGuess = true
            //Save the current song
            json = gson.toJson(currentSong)
            editor.putString("CurrentSong", json)
            //The total number of words in the song is the number of placemarkers in map5
            totalWords = container.maps[4].size
            editor.putInt("totalWords",totalWords)
            //downloadMap is now false
            editor.putBoolean("downloadMap", false)
            editor.apply()
        }
        //Placemarkers and markers have now been loaded
        loaded=true
    }

    //Return the index of the marker closest to the currentLocation
    fun nearestMarker(currentLocation: Location?): Int{
        var indexOfClosest = -1
        //currentLocation cannot be null as if it is the collect button wont be displayed
        if (currentLocation==null){
            Log.i("MYAPP","Location can not be found")
        }
        else{
            val location = Location("")
            var shortestDistance = 9000f
            var distance: Float
            for (i in placemarkers.indices){
                var placemarker = placemarkers[i].first
                if (!collectedPos.contains(placemarker.pos)){
                    location.longitude=placemarkers[i].first.point.first
                    location.latitude=placemarkers[i].first.point.second
                    distance=currentLocation.distanceTo(location)
                    if(distance<shortestDistance){
                        shortestDistance=distance
                        indexOfClosest=i
                    }
                }
            }
        }
        return indexOfClosest
    }

    //Return true if a marker is within 20 meters of the user
    fun inCircle(currentLocation: Location?) : Boolean{
        var found = false
        //If the currentLocation is null then report an error and return false so the button is not
        //displayed
        if (currentLocation==null){
            Log.i("MYAPP","Location can not be found")
        }
        else if (loaded){
            val location = Location("")
            var i=0
            var distance: Float
            while (!found && i<placemarkers.size){
                var placemark = placemarkers[i].first
                if (!collectedPos.contains(placemark.pos)){
                    location.longitude=placemarkers[i].first.point.first
                    location.latitude=placemarkers[i].first.point.second
                    distance=currentLocation.distanceTo(location)
                    if (distance<=20){
                        found=true
                    }
                }
                i++
            }
        }
        return found
    }

    fun drawMap(){
        //Draw the map
        for (i in placemarkers.indices) {
            var placemark = placemarkers[i].first
            //Only draw markers that are not collected
            if (!collectedPos.contains(placemark.pos)){
                val point = LatLng(placemark.point.second, placemark.point.first)
                val marker = MarkerOptions().position(point)
                marker.icon(BitmapDescriptorFactory.fromBitmap(placemarkers[i].second))
                //Placemarkrs in postition i match with markers in postition i
                markers.add(i,mMap.addMarker(marker))
            }
            else{
                markers.add(i,null)
            }
        }
        //Make the button appear if a marker is in the circle
        if (inCircle(mLastLocation)){
            collect.visibility = View.VISIBLE
            collect_text.visibility = View.VISIBLE
        }
    }

    fun load(){
        //Load the saved words from the previous plays
        val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val gson = Gson()
        var json = preferences.getString("CollectedWords","ERROR")
        if (json!="ERROR"){
            wordsList = gson.fromJson(json, ArrayList<String>().javaClass)
        }
        //Load the distanceWalked
        distanceWalked = preferences.getFloat("distanceWalked",0.0f)
        //Load the totalWords
        totalWords = preferences.getInt("totalWords",0)
        //Load firstGuess
        firstGuess = preferences.getBoolean("firstGuess",true)
        //Round to the nearest integer
        //0.762 is the average step length in meters
        this.nav_view.menu.findItem(R.id.nav_walk).title = distanceWalked.div(0.762).toInt().toString()+" steps"
        //Load the guessed songs
        json = preferences.getString("GuessedSongs","ERROR")
        if (json!="ERROR"){
            val type = object : TypeToken<ArrayList<Song>>() {}.type
            guessedSongs = gson.fromJson<ArrayList<Song>>(json, type)
        }
        //load the collected pos
        json = preferences.getString("CollectedPos","ERROR")
        if (json!="ERROR"){
            collectedPos = gson.fromJson(json, ArrayList<String>().javaClass)
        }
        //load the current song
        json = preferences.getString("CurrentSong","ERROR")
        if (json!="ERROR"){
            val type = object : TypeToken<Song>() {}.type
            currentSong = gson.fromJson<Song>(json, type)
        }
        //load the achievement progress
        json = preferences.getString("AchievementProgress","INIT")
        if (json=="INIT"){
            //If progress has not yet been saved the set all progress to 0
            var i = 0
            while(i<numAch){
                achievements.add(i,0.0)
                i++
            }
        }
        else{
            achievements = gson.fromJson(json, ArrayList<Double>().javaClass)
        }
        //Load the map
        loadMap(activeMap)
    }

    fun loadMap(map : Int){
        //Load the placemarkers
        val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val gson = Gson()
        var json = preferences.getString("Map"+map.toString(),"ERROR")
        if (json!="ERROR"){
            //Load placemarkers
            val type = object : TypeToken<ArrayList<Placemark>>() {}.type
            var pms = gson.fromJson<ArrayList<Placemark>>(json, type)
            //Load the bitmaps
            var bitmaps = HashMap<String,Bitmap>()
            for (placemark in pms){
                if (!bitmaps.containsKey(placemark.fileName)){
                    //Load the bitmap from the file
                    var fis: FileInputStream? = null
                    try {
                        fis = this.openFileInput(placemark.fileName)
                        bitmaps.put(placemark.fileName,BitmapFactory.decodeStream(fis))
                    } catch (e: FileNotFoundException) {
                        Log.i("MYAPP", "file not found")
                        e.printStackTrace()
                    } catch (e: IOException) {
                        Log.i("MYAPP", "io exception")
                        e.printStackTrace()
                    } finally {
                        if (fis!=null){
                            fis.close()
                        }
                    }
                }
                var icon = bitmaps.get(placemark.fileName)
                placemarkers.add(Pair(placemark,icon))
            }
            //The map needs drawing and has been loaded
            if (mapReady){
                drawMap()
            }
            else{
                draw=true
            }
            loaded=true
        }
    }

    fun save(){
        //Save the collected words
        val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val editor = preferences.edit()
        val gson = Gson()
        var json = gson.toJson(wordsList)
        editor.putString("CollectedWords", json)
        //Save the guessed words
        json = gson.toJson(guessedSongs)
        editor.putString("GuessedSongs", json)
        //Save the collectedPos
        json = gson.toJson(collectedPos)
        editor.putString("CollectedPos", json)
        //Save the distance walked
        editor.putFloat("distanceWalked",distanceWalked)
        //Save the achievement progress
        json = gson.toJson(achievements)
        editor.putString("AchievementProgress", json)
        //Save firstGuess
        editor.putBoolean("firstGuess",firstGuess)
        editor.apply()
        //Save the current map to the activeMap folder
        saveMap(activeMap)
    }

    fun saveMap(map : Int){
        val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val editor = preferences.edit()
        val gson = Gson()
        //Extract the placemarkers and save an ArrayList<Placemarker>
        var pms = ArrayList<Placemark>()
        for (pair in placemarkers){
            pms.add(pair.first)
        }
        var json = gson.toJson(pms)
        editor.putString("Map"+map.toString(),json)
        editor.apply()
    }

    fun saveAllMaps(maps : ArrayList<ArrayList<Pair<Placemark,Bitmap?>>>){
        val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val editor = preferences.edit()
        val gson = Gson()
        //bitmaps stores the bitmap and it corresponding file name
        var bitmaps = HashMap<Bitmap,String>()
        //The file names are "0","1","2","3","4"
        var fileName = 0;
        var icon : Bitmap?
        //Save the maps to "Map"+i
        var i = 1
        //Save the maps
        for (index in maps.indices){
            var map = maps[index]
            var pms = ArrayList<Placemark>()
            //Extract the placemarkers to be saved as ArrayList<Placemarker> and save the bitmaps
            for (pair in map){
                icon=pair.second
                if (!bitmaps.containsKey(icon) && icon!=null){
                    bitmaps.put(icon,fileName.toString())
                    fileName++
                }
                //Get the file name of the bitmap for this placemarker
                var file = bitmaps.get(pair.second)
                //File is never null
                if (file!=null){
                    pair.first.fileName = file
                    pms.add(pair.first)
                }
            }
            //Bitmaps cannot be saved in shared preferences so save the bitmaps and the placemarkers in
            //shared preferences
            var json = gson.toJson(pms)
            editor.putString("Map"+i.toString(),json)
            i++
        }




        editor.apply()
        //Save the bitmaps
        for (bitmap in bitmaps.keys){
            var fos : FileOutputStream? = null
            try {
                fos = this.openFileOutput(bitmaps.get(bitmap), Context.MODE_PRIVATE)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            } catch (e: FileNotFoundException) {
                Log.i("MYAPP", "file not found")
            } catch (e: IOException) {
                Log.i("MYAPP", "io exception")
            } finally {
                if (fos!=null){
                    fos.close()
                }
            }
        }


    }
}
