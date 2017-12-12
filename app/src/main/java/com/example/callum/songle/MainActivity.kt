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
import org.jetbrains.anko.alert
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, DownloadCompleteListener {
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

    companion object {
        var wordsList = ArrayList<String>()
        var readWords = -1
        var guessedSongs = ArrayList<Song>()
        var readSongs = -1
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
            //Set firstRun to false so it doesnt do this again
            editor.putBoolean("firstRun",false)
            editor.apply()
            //Network receiver needs the saved timestamp of Songs
            val timeStamp = preferences.getString("timeStamp","")
            val gson = Gson()
            var json = preferences.getString("Songs","ERROR")
            var songList = ArrayList<Song>()
            if (json!="ERROR"){
                val type = object : TypeToken<ArrayList<Song>>() {}.type
                songList = gson.fromJson<ArrayList<Song>>(json, type)
            }
            //Download the map
            //The number of the song just played is empty string as no song has yet been played
            receiver = NetworkReceiver(this,timeStamp,songList,"")
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            this.registerReceiver(receiver, filter)
            //Open help
            val intent = Intent(this,Help::class.java)
            startActivity(intent)
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
                        //Save the active map
                        saveMap(activeMap)
                        //Reset the maps
                        for (marker in markers){
                            if (marker!=null){
                                marker.remove()
                            }
                        }
                        markers.clear()
                        placemarkers.clear()
                        Log.i("MYAPP","pms:"+placemarkers.size.toString()+", m:"+markers.size.toString())
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
        changeSong()

    }

    private fun changeSong(){
        //Reset the collected words for the song
        wordsList = ArrayList<String>()
        //-2 tells collected words to clear the screen
        readWords = -2
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
        //See if we can access the users location
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onLocationChanged(current : Location?) {
        mLastLocation=current
        if(current == null){
            //DO SOMETHING ELSE
            Log.i("MYAPP","[onLocationChanged] Location is unknown")
        } else {
            val position = LatLng(current.latitude,current.longitude)
            //If first time this function has been called initalize the circle
            if (locationChanged){
                val circleOptions = CircleOptions()
                circle = mMap.addCircle(circleOptions
                        .center(position)
                        .radius(20.0)
                        .strokeColor(Color.parseColor("#673AB7")))
                locationChanged = false
            }
            //Move the circle
            circle.center = position
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
        //DO SOMETHING
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

        try {
            //Show current position
            mMap.isMyLocationEnabled = true
        } catch (se : SecurityException) {
            Log.i("MYAPP","Security exception thrown [onMapReady]")
        }
        //Add ”My location” button to the user interface
        mMap.uiSettings.isMyLocationButtonEnabled = true

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
        //If maps and song are empty an error occured
        if (container.maps.size==0 && container.songs.size==0) {
            Log.i("MYAPP","An error occurred while downloading!")
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
            //Save the current song
            json = gson.toJson(currentSong)
            editor.putString("CurrentSong", json)
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
        //Clear the map
        for (marker in markers){
            if (marker!=null){
                marker.remove()
            }
        }
        //Reset the markers
        markers = ArrayList<Marker?>()
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
    }

    fun load(){
        //Load the saved words from the previous plays
        val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val gson = Gson()
        var json = preferences.getString("CollectedWords","ERROR")
        if (json!="ERROR"){
            wordsList = gson.fromJson(json, ArrayList<String>().javaClass)
        }
        //Load the total distance walked default value 0
        distanceWalked=preferences.getFloat("distanceWalked",0.0f)
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
