package com.example.weatherapp.models

import java.io.Serializable

data class Sys (
    val country:String,
    val sunrise:Long,
    val sunset:Long
        ):Serializable