package com.zenithsky.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operacion_log")
data class OperacionLog(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val operacion: String,

    val entidad: String,

    val detalle: String,

    val fecha: Long = System.currentTimeMillis()
)