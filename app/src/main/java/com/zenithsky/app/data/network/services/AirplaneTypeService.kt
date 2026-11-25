package com.zenithsky.app.data.network.services

import com.zenithsky.app.data.model.AirplaneTypeDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AirplaneTypeService {

    @GET("api/airplane-types")
    suspend fun getAirplaneTypes(): Response<List<AirplaneTypeDto>>

    @GET("api/airplane-types/{id}")
    suspend fun getAirplaneTypeById(
        @Path("id") id: Int
    ): Response<AirplaneTypeDto>
}