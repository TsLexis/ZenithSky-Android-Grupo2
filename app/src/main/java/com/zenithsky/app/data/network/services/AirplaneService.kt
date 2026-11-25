package com.zenithsky.app.data.network.services

import com.zenithsky.app.data.model.AirplaneDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AirplaneService {

    @GET("api/airplanes")
    suspend fun getAirplanes(
        @Query("typeId") typeId: Int? = null,
        @Query("airlineId") airlineId: Int? = null
    ): Response<List<AirplaneDto>>

    @GET("api/airplanes/{id}")
    suspend fun getAirplaneById(
        @Path("id") id: Int
    ): Response<AirplaneDto>

    @POST("api/airplanes")
    suspend fun createAirplane(
        @Body airplane: AirplaneDto
    ): Response<AirplaneDto>

    @PUT("api/airplanes/{id}")
    suspend fun updateAirplane(
        @Path("id") id: Int,
        @Body airplane: AirplaneDto
    ): Response<AirplaneDto>

    @DELETE("api/airplanes/{id}")
    suspend fun deleteAirplane(
        @Path("id") id: Int
    ): Response<Unit>
}