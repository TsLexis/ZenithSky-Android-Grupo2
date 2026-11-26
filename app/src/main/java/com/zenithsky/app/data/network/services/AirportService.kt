package com.zenithsky.app.data.network.services

import com.zenithsky.app.data.model.AirportDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AirportService {

    @GET("api/airports")
    suspend fun getAirports(
        @Query("busqueda") busqueda: String? = null
    ): Response<List<AirportDto>>

    @GET("api/airports/{id}")
    suspend fun getAirportById(
        @Path("id") id: Int
    ): Response<AirportDto>

    @POST("api/airports")
    suspend fun createAirport(
        @Body airport: AirportDto
    ): Response<AirportDto>

    @PUT("api/airports/{id}")
    suspend fun updateAirport(
        @Path("id") id: Int,
        @Body airport: AirportDto
    ): Response<String>

    @DELETE("api/airports/{id}")
    suspend fun deleteAirport(
        @Path("id") id: Int
    ): Response<Unit>
}