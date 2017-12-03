package com.example.callum.songle

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
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
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast
import java.lang.reflect.Type
import java.net.URL

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, DownloadCompleteListener {
    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    var mLocationPermissionGranted = false
    private var mLastLocation: Location? = null
    val TAG = "MainActivity"
    private var placemarkers = ArrayList<Placemark>()
    private var markers = ArrayList<Marker>()
    //Loaded is true when the placemarkers and markers have been loaded
    private var loaded = false
    //True if the map need drawing when the map is ready
    private var draw = false
    //A BroadcastReeceiver that monitors network connectivity changes
    private lateinit var receiver : NetworkReceiver

    companion object {
        var wordsList = ArrayList<String>()
        var readWords = -1
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
            val word=placemarkers[index].name
            toast("You collected: "+word)
            wordsList.add(word)
            //Remove the marker from the map
            markers[index].remove()
            //Remove the markers from the lists
            markers.removeAt(index)
            placemarkers.removeAt(index)
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
        //Use true as the default value as if it doesnt exist it means it is the apps first run so a
        //map needs downloading
        val downloadMap=preferences.getBoolean("downloadMap",true)
        //If it is the first run of the app open the help activity
        if(downloadMap){
            //Set downloadMap to false so it doesnt download the map again
            val editor=preferences.edit()
            editor.putBoolean("downloadMap",false)
            editor.apply()
            receiver = NetworkReceiver(this)
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            this.registerReceiver(receiver, filter)
        }

        //Use true as the default value as if it doesnt exist it means it is the apps first run
        val firstRun=preferences.getBoolean("firstRun",true)
        //If it is the first run of the app open the help activity
        if(firstRun){
            //Set firstRun to false so it doesnt do this again
            val editor=preferences.edit()
            editor.putBoolean("firstRun",false)
            editor.apply()
            val intent = Intent(this,Help::class.java)
            startActivity(intent)
        }




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
                val dialog = AlertDialog.Builder(this).create()
                dialog.setView(View.inflate(this,R.layout.guess_dialog,null))
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
                        toast("You clicked on Yes Button")
                    }
                    negativeButton("No") {
                        toast("You clicked on No Button")
                    }
                }.show()
            }
            R.id.nav_diff -> {
                val difficulties = listOf("Easy", "Medium", "Hard", "Really Hard", "Impossible")
                selector("Please choose a difficulty:", difficulties, { dialogInterface, i ->
                    toast("You are playing ${difficulties[i]} difficulty!")
                })
            }
            R.id.nav_walk -> {
                val dialog = AlertDialog.Builder(this).create()
                dialog.setView(View.inflate(this,R.layout.incorrect,null))
                dialog.show()
            }
        }
        return true
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
            Log.d("MYAPP","IllegalStateException thrown [onConnected]")
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
            Log.d("MYAPP","[onLocationChanged] Location is unknown")
        } else {
            val position = LatLng(current.latitude,current.longitude)
            val circleOptions = CircleOptions()
            val circle = mMap.addCircle(circleOptions
                    .center(position)
                    .radius(20.0)
                    .strokeColor(Color.parseColor("#673AB7")))
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
        Log.d("MYAPP","[onConnectionSuspended]")
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        //DO SOMETHING
        Log.d("MYAPP","[onConnectionFailed]")
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
        Log.d("MYAPP","mAPrEADY")
        mMap = googleMap

        try {
            //Show current position
            mMap.isMyLocationEnabled = true
        } catch (se : SecurityException) {
            Log.d("MYAPP","Security exception thrown [onMapReady]")
        }
        //Add ”My location” button to the user interface
        mMap.uiSettings.isMyLocationButtonEnabled = true

        if (draw){
                drawMap()
        }
    }

    override fun downloadComplete(result: ArrayList<Placemark>) {
        placemarkers = result
        //If the icon is null an error occured
        if (placemarkers[0].icon == null) {
            placemarkers[0].name
        } else {
            drawMap()
        }
        //Placemarkers and markers have now been loaded
        loaded=true
    }

    //Return the index of the marker closest to the currentLocation
    fun nearestMarker(currentLocation: Location?): Int{
        var indexOfClosest = -1
        //currentLocation cannot be null as if it is the collect button wont be displayed
        if (currentLocation==null){
            Log.d("MYAPP","Location can not be found")
        }
        else{
            val location = Location("")
            location.longitude=placemarkers[0].point.first
            location.latitude=placemarkers[0].point.second
            indexOfClosest = 0
            var shortestDistance = currentLocation.distanceTo(location)
            var distance: Float
            for (i in markers.indices){
                location.longitude=placemarkers[i].point.first
                location.latitude=placemarkers[i].point.second
                distance=currentLocation.distanceTo(location)
                if(distance<shortestDistance){
                    shortestDistance=distance
                    indexOfClosest=i
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
            Log.d("MYAPP","Location can not be found")
        }
        else if (loaded){
            val location = Location("")
            var i=0
            var distance: Float
            while (!found && i<placemarkers.size){
                location.longitude=placemarkers[i].point.first
                location.latitude=placemarkers[i].point.second
                distance=currentLocation.distanceTo(location)
                if (distance<=20){
                    found=true
                }
                i++
            }
        }
        return found
    }

    fun drawMap(){
        //Draw the map
        for (placemark in placemarkers) {
            var point = LatLng(placemark.point.second, placemark.point.first)
            var marker = MarkerOptions().position(point)
            //marker.icon(BitmapDescriptorFactory.fromBitmap(placemark.icon))
            markers.add(mMap.addMarker(marker))
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
        //Load the placemarkers
        json = preferences.getString("Placemarkers","ERROR")
        if (json!="ERROR"){
            val type = object : TypeToken<ArrayList<Placemark>>() {}.type
            placemarkers = gson.fromJson<ArrayList<Placemark>>(json, type)
            //The map need drawing
            draw = true
        }
        loaded=true
    }

    fun save(){
        //Save the collected words
        val preferences = getSharedPreferences("MainFile",Context.MODE_PRIVATE)
        val editor = preferences.edit()
        val gson = Gson()
        var json = gson.toJson(wordsList)
        editor.putString("CollectedWords", json)
        json = gson.toJson(placemarkers)
        editor.putString("Placemarkers",json)
        editor.apply()
    }
}
