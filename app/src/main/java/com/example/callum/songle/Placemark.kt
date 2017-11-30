package com.example.callum.songle

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

data class Placemark(val name: String, val icon: Bitmap?, val point: Pair<Double,Double>)