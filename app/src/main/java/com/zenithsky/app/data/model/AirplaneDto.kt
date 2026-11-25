package com.zenithsky.app.data.model

data class AirplaneDto(
    val airplane_id: Int,
    val capacity: Int,
    val type_id: Int,
    val airline_id: Int,
    val identifier: String?,
    val description: String?,
    val airlinename: String?
)