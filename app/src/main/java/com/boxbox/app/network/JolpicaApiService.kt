package com.boxbox.app.network

import com.boxbox.app.model.ConstructorStandingsApiResponse
import com.boxbox.app.model.DriverStandingsApiResponse
import com.boxbox.app.model.RaceApiResponse
import com.boxbox.app.model.ResultsApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface JolpicaApiService {
    @GET("ergast/f1/{season}/races.json")
    suspend fun getRaces(@Path("season") season: Int): RaceApiResponse

    @GET("ergast/f1/{season}/{round}/results.json")
    suspend fun getRaceResults(
        @Path("season") season: Int,
        @Path("round") round: Int
    ): ResultsApiResponse

    @GET("ergast/f1/{season}/driverstandings.json")
    suspend fun getDriverStandings(@Path("season") season: Int): DriverStandingsApiResponse

    @GET("ergast/f1/{season}/constructorstandings.json")
    suspend fun getConstructorStandings(@Path("season") season: Int): ConstructorStandingsApiResponse
}
