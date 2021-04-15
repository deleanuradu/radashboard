package com.drs.radashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private var weatherBitURL = ""
    private var weatherBitAPIKey = "a83f9f46e44b46d088ef53e9994bb80d"
    private lateinit var resultsTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latestLocation: Location? = null
    private var gpsEnabled: Boolean = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        resultsTextView = findViewById(R.id.resultsTextView)
        statusTextView = findViewById(R.id.statusTextView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

//        val locationRequest = LocationRequest.create()
//            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//            .setInterval((20 * 1000).toLong())
//            .setFastestInterval((1 * 1000).toLong())

        getWeatherButton.setOnClickListener {
            checkGPSEnabled()
            if (gpsEnabled) {
                statusTextView.text = getString(R.string.gps_enabled)
                obtainLocation()
                if (latestLocation != null) {
                    weatherBitURL =
                        "https://api.weatherbit.io/v2.0/current?" + "lat=" + latestLocation!!.latitude +
                                "&lon=" + latestLocation!!.longitude + "&key=" + weatherBitAPIKey
                    loadMeteoInfo()
                } else {
                    resultsTextView.text = getString(R.string.pinpoint_failure)
                }
            } else {
                statusTextView.text = getString(R.string.please_enable_gps)
            }
        }
    }

    private fun checkGPSEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun obtainLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            ActivityCompat.requestPermissions(this, permissions, 0)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                latestLocation = location
            }
    }

    @SuppressLint("SetTextI18n")
    private fun loadMeteoInfo() {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)

        val stringReq = StringRequest(Request.Method.GET, weatherBitURL, { response ->
            Log.d("Response", response.toString())

            // get the JSON object
            val obj = JSONObject(response)

            // get the Array from obj of name - "data"
            val arr = obj.getJSONArray("data")
            Log.d("lat obj1", arr.toString())

            // get the JSON object from the
            // array at index position 0
            val obj2 = arr.getJSONObject(0)
            Log.d("lat obj2", obj2.toString())

            // set the temperature and the city
            // name using getString() function
            resultsTextView.text = obj2.getString("temp") + " °C în " + obj2.getString("city_name")
        },
            // In case of any error
            { })
        queue.add(stringReq)
    }
}
