package com.example.callum.songle

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.design.widget.Snackbar
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.guess_dialog.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    var mLocationPermissionGranted = false
    private var mLastLocation: Location? = null
    val TAG = "MainActivity"
    //A BroadcastReeceiver that monitors network connectivity changes
    private var receiver = NetworkReceiver()


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

        fab.setOnClickListener { view ->
            toast("You collected: Oh,")
            fab.visibility = View.GONE
            fab_text.visibility = View.GONE
        }

        fun onDestroy() {
            super.onDestroy()
            // Unregisters BroadcastReceiver when app is destroyed.
            if (receiver != null) {
                this.unregisterReceiver(receiver);
            }
        }


        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        // Register BroadcastReceiver to track connection changes.
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        this.registerReceiver(receiver, filter)

    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
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
                startActivity(intent)
                return true
            }
            R.id.action_help -> {
                //Open the Help Activity
                val intent = Intent(this,Help::class.java)
                startActivity(intent)
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
                //Open the Achievement Activity
                val intent = Intent(this,CollectedWords::class.java)
                startActivity(intent)
            }
            R.id.nav_songs -> {
                //Open the Achievement Activity
                val intent = Intent(this,GuessedSongs::class.java)
                startActivity(intent)
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
        try { createLocationRequest(); }
        catch (ise : IllegalStateException){
            Log.d("MYAPP","IllegalStateException thrown [onConnected]")
        }
        //See if we can access the users location
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    override fun onLocationChanged(current : Location?) {
        if(current == null){
            //DO SOMETHING ELSE
            Log.d("MYAPP","[onLocationChanged] Location is unknown")
        } else {
            //MAYBE CHANGE THIS
            Log.d("MYAPP","""[onLocationChanged] Lat/Long now (${current.getLatitude()},${current.getLongitude()})""")
            val position = LatLng(current.latitude,current.longitude)
            val circleOptions = CircleOptions();
            val circle = mMap.addCircle(circleOptions
                    .center(position)
                    .radius(20.0)
                    .strokeColor(Color.parseColor("#673AB7")))
            circle.center = position
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
        mMap = googleMap

        try {
            //Show current position
            mMap.isMyLocationEnabled = true
        } catch (se : SecurityException) {
            Log.d("MYAPP","Security exception thrown [onMapReady]")
        }
        //Add ”My location” button to the user interface
        mMap.uiSettings.isMyLocationButtonEnabled = true
    }
}
