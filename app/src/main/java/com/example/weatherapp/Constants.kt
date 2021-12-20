package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants{
    const val APP_ID:String ="18dfff90b0a70ba9a5b59b8c0bafcd4f"
    const val BASE_URL:String="http://api.openweathermap.org/data/"
    const val METRIC_UNIT:String="metric"
    const val PRFERENCE_NAME="WeatherPreference"
    const val WEATHER_RESPONSE_DATA="weather_response_data"
    fun isNetworkAvailable(context:Context):Boolean{
        val connectivityManager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            val networkInfo=connectivityManager.activeNetwork ?:return false
            val activeNetwork=connectivityManager.getNetworkCapabilities(networkInfo)?:return false
            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)-> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)-> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)-> true
                else -> false
            }
        }
        else
        {
            val networkInfo=connectivityManager.activeNetworkInfo
            return networkInfo!=null && networkInfo.isConnectedOrConnecting
        }
    }
}