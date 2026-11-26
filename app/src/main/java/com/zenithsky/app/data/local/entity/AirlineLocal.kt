package com.zenithsky.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "airline_local")
data class AirlineLocal(

    @PrimaryKey
    val id: Int,

    val iata: String,

    val nombre: String,

    val aeropuertoBaseIata: String,

    val aeropuertoBaseNombre: String
)