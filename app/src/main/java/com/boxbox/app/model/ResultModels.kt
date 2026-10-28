package com.boxbox.app.model

import com.google.gson.annotations.SerializedName

data class ResultsApiResponse(
    @SerializedName("MRData") val mrData: ResultsMrData
)

data class ResultsMrData(
    @SerializedName("RaceTable") val raceTable: ResultsRaceTable
)

data class ResultsRaceTable(
    @SerializedName("Races") val races: List<RaceResultData>
)

data class RaceResultData(
    @SerializedName("Results") val results: List<DriverResult>
)

data class DriverResult(
    @SerializedName("position") val position: String,
    @SerializedName("points") val points: String,
    @SerializedName("Driver") val driver: DriverInfo,
    @SerializedName("Constructor") val constructor: ConstructorInfo,
    @SerializedName("status") val status: String,
    @SerializedName("Time") val time: ResultTime? = null
)

data class DriverInfo(
    @SerializedName("code") val code: String?,
    @SerializedName("givenName") val givenName: String,
    @SerializedName("familyName") val familyName: String
)

data class ConstructorInfo(
    @SerializedName("constructorId") val constructorId: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("nationality") val nationality: String? = null
)

data class ResultTime(
    @SerializedName("time") val time: String
)
