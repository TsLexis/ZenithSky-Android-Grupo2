package com.zenithsky.app.data.network

import com.zenithsky.app.data.network.services.AirlineService
import com.zenithsky.app.data.network.services.AirplaneService
import com.zenithsky.app.data.network.services.AirplaneTypeService
import com.zenithsky.app.data.network.services.AirportService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Funciona como el puente entre Android Studio y el Backend
object ApiClient {

    //permite al emulador acceder al localhost de la computadora
    //donde se ejecuta el backend en 5000
    //Cambiar la IP por la de cada uno para una conexión directa
    //IP Alex: 192.168.50.130
    //IP Evans:
    //IP Bryan:
    //IP Eduardo:
    private const val BASE_URL = "http://192.168.50.130:5000/"

    //Retrofit se encarga de realizar las peticiones HTTP entre Android Studio y el Backend
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Servicio de Aeropuertos
    val airportService: AirportService by lazy {
        retrofit.create(AirportService::class.java)
    }

    // Servicio de Aerolíneas
    val airlineService: AirlineService by lazy {
        retrofit.create(AirlineService::class.java)
    }

    // Servicio de Aviones
    val airplaneService: AirplaneService by lazy {
        retrofit.create(AirplaneService::class.java)
    }

    // Servicio de Tipos de Avión
    val airplaneTypeService: AirplaneTypeService by lazy {
        retrofit.create(AirplaneTypeService::class.java)
    }
}