package com.zenithsky.app.data.model

import com.google.gson.annotations.SerializedName

data class AirplaneDto(
    val airplane_id: Int,
    val capacity: Int,
    val type_id: Int,
    val airline_id: Int,
    val identifier: String?,
    val description: String?,
    @SerializedName("airline_name")
    val airlinename: String?
)