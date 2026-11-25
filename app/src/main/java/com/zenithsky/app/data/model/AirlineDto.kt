package com.zenithsky.app.data.model

data class AirlineDto(
    val airline_id: Int,
    val iata: String,
    val airlinename: String,
    val base_airport: Int,
    val base_airport_iata: String?,
    val base_airport_name: String?
)