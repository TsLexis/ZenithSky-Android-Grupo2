package com.zenithsky.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avion_destacado")
data class AvionDestacado(

    @PrimaryKey
    val id: Int,

    val modelo: String,

    val capacidad: Int
)