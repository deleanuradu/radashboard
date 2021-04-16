package com.drs.radashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
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
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private var weatherBitURL = ""
    private var weatherBitAPIKey = "a83f9f46e44b46d088ef53e9994bb80d"
    private lateinit var resultsTextView: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latestLocation: Location? = null

    companion object {
        const val REQUEST_CHECK_SETTINGS = 999
    }

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        resultsTextView = findViewById(R.id.resultsTextView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        obtainLocation()

        getWeatherButton.setOnClickListener {
            // if no location, try to get one
            if (latestLocation == null) {
                resultsTextView.text = getString(R.string.pinpoint_failure)
                obtainLocation()
            }
            // if now you got location, get meto info
            if (latestLocation != null) {
                weatherBitURL =
                    "https://api.weatherbit.io/v2.0/current?" + "lat=" + latestLocation!!.latitude +
                            "&lon=" + latestLocation!!.longitude + "&key=" + weatherBitAPIKey
                loadMeteoInfo()
            }
            // if you still don't have a location
            else {
                resultsTextView.text = getString(R.string.retry_failure)
            }
        }
    }

    private fun obtainLocation() {
        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        // GPS is on
        task.addOnSuccessListener {
            // if any permissions missing, request them
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
            }
            // else load latest location
            else {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        latestLocation = location
                    }
                Log.e("Latest location: ", latestLocation.toString())
            }
        }
        // if GPS is off ask user to enable it
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) { }
            }
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
            resultsTextView.text = obj2.getString("temp") + " °C in " + obj2.getString("city_name")
        },
            // In case of any error
            { })
        queue.add(stringReq)
    }
}
