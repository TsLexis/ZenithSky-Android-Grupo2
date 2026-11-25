package com.zenithsky.app.data.network.services

import com.zenithsky.app.data.model.AirlineDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AirlineService {

    @GET("api/airlines")
    suspend fun getAirlines(): Response<List<AirlineDto>>

    @GET("api/airlines/{id}")
    suspend fun getAirlineById(
        @Path("id") id: Int
    ): Response<AirlineDto>

    @POST("api/airlines")
    suspend fun createAirline(
        @Body airline: AirlineDto
    ): Response<AirlineDto>

    @PUT("api/airlines/{id}")
    suspend fun updateAirline(
        @Path("id") id: Int,
        @Body airline: AirlineDto
    ): Response<AirlineDto>

    @DELETE("api/airlines/{id}")
    suspend fun deleteAirline(
        @Path("id") id: Int
    ): Response<Unit>
}