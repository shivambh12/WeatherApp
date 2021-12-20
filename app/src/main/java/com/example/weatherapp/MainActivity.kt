package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter

import com.karumi.dexter.PermissionToken

import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.listener.PermissionRequest

import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {


    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog ?=null

    private lateinit var mSharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSharedPreferences= getSharedPreferences(Constants.PRFERENCE_NAME,Context.MODE_PRIVATE)


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this)
                .withPermissions(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            getLastLocation()
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
            // END
        }
    }
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {

        mFusedLocationClient?.lastLocation!!.addOnCompleteListener(this) { task ->
            task.addOnFailureListener(this)
            {
                Toast.makeText(
                    this@MainActivity,
                    "${it.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            if (task.isSuccessful && task.result != null) {
               val lastLocation = task.result
                getLocationWeatherDetails(lastLocation.latitude,lastLocation.longitude)

            }
            else {
                Toast.makeText(
                    this@MainActivity,
                    "getLastLocation:exception",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("find123", "getLastLocation:exception", task.exception)

            }
        }.addOnFailureListener(this)
        {
            Toast.makeText(
                this@MainActivity,
                "${it.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun isLocationEnabled(): Boolean {

        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude:Double)
    {
        if(Constants.isNetworkAvailable(this))
        {
            showCustomProgressDialog()
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherService=retrofit
                .create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getweather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            listCall.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>?
                ) {
                    if(response!!.isSuccessful)
                    {
                        hideCustomProgressDialog()
                        val weatherList: WeatherResponse? = response.body()
                        Log.i("Response Result","$weatherList")

                        val weatherResponseJsonString= Gson().toJson(weatherList)
                        val editor=mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                        editor.apply()
                        setupUI()
                    }
                    else
                    {
                        val rc=response.code()
                        when(rc)
                        {
                            400 -> {
                                Log.e("Error 400","Bad Connection")
                            }
                            404 -> {
                                Log.e("Error 404","Not Found")
                            }
                            else ->
                            {
                                Log.e("Error","Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable?) {
                    hideCustomProgressDialog()
                    Log.e("Errorrrr",t!!.message.toString())
                }
            })
        }
        else
        {
            Toast.makeText(
                this@MainActivity,
                "You have not connected to internet.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }
    private  fun showCustomProgressDialog()
    {
        mProgressDialog= Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId)
        {
            R.id.action_refresh -> {
                getLastLocation()
                true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    private fun hideCustomProgressDialog()
    {
        if(mProgressDialog!=null)
            mProgressDialog!!.dismiss()
    }

    @SuppressLint("NewApi")
    private fun setupUI() {
        val weatherResponseJsonString=mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA,"")
        if(!weatherResponseJsonString.isNullOrEmpty())
        {
            val weatherList = Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)
            // For loop to get the required data. And all are populated in the UI.
            if(weatherList==null)
                return;
            for (z in weatherList.weather.indices) {
                Log.i("NAMEEEEEEEE", weatherList.weather[z].main)

                //Declaration
                val tv_main:TextView = findViewById(R.id.tv_main)
                val tv_main_description:TextView = findViewById(R.id.tv_main_description)
                val tv_temp:TextView = findViewById(R.id.tv_temp)
                val tv_humidity:TextView = findViewById(R.id.tv_humidity)
                val tv_min:TextView = findViewById(R.id.tv_min)
                val tv_max:TextView = findViewById(R.id.tv_max)
                val tv_speed:TextView = findViewById(R.id.tv_speed)
                val tv_name:TextView = findViewById(R.id.tv_name)
                val tv_country:TextView = findViewById(R.id.tv_country)
                val tv_sunrise_time:TextView = findViewById(R.id.tv_sunrise_time)
                val tv_sunset_time:TextView = findViewById(R.id.tv_sunset_time)
                val iv_main:ImageView=findViewById(R.id.iv_main)

                tv_main.text = weatherList.weather[z].main
                tv_main_description.text = weatherList.weather[z].description
                tv_temp.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                tv_humidity.text = weatherList.main.humidity.toString() + " %"
                tv_min.text = weatherList.main.temp_min.toString() + " min"
                tv_max.text = weatherList.main.temp_max.toString() + " max"
                tv_speed.text = weatherList.wind.speed.toString()
                tv_name.text = weatherList.name
                tv_country.text = weatherList.sys.country
                tv_sunrise_time.text = unixTime(weatherList.sys.sunrise)
                tv_sunset_time.text = unixTime(weatherList.sys.sunset)

                // Here we update the main icon
                when (weatherList.weather[z].icon) {
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_main.setImageResource(R.drawable.cloud)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10n" -> iv_main.setImageResource(R.drawable.cloud)
                    "11n" -> iv_main.setImageResource(R.drawable.rain)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
                }
            }
        }


    }

    /**
     * Function is used to get the temperature unit value.
     */
    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    /**
     * The function is used to get the formatted time based on the Format and the LOCALE we pass to it.
     */
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat")
        val sdf = SimpleDateFormat("HH:mm", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
    // END
}


