package com.boxbox.app.model

import com.google.gson.annotations.SerializedName

data class DriverStandingsApiResponse(
    @SerializedName("MRData") val mrData: DriverStandingsMrData
)

data class DriverStandingsMrData(
    @SerializedName("StandingsTable") val standingsTable: DriverStandingsTable
)

data class DriverStandingsTable(
    @SerializedName("StandingsLists") val standingsLists: List<DriverStandingsList>
)

data class DriverStandingsList(
    @SerializedName("DriverStandings") val driverStandings: List<DriverStanding>
)

data class DriverStanding(
    @SerializedName("position") val position: String,
    @SerializedName("points") val points: String,
    @SerializedName("wins") val wins: String,
    @SerializedName("Driver") val driver: DriverInfo,
    @SerializedName("Constructors") val constructors: List<ConstructorInfo>
)

data class ConstructorStandingsApiResponse(
    @SerializedName("MRData") val mrData: ConstructorStandingsMrData
)

data class ConstructorStandingsMrData(
    @SerializedName("StandingsTable") val standingsTable: ConstructorStandingsTable
)

data class ConstructorStandingsTable(
    @SerializedName("StandingsLists") val standingsLists: List<ConstructorStandingsList>
)

data class ConstructorStandingsList(
    @SerializedName("ConstructorStandings") val constructorStandings: List<ConstructorStanding>
)

data class ConstructorStanding(
    @SerializedName("position") val position: String,
    @SerializedName("points") val points: String,
    @SerializedName("wins") val wins: String,
    @SerializedName("Constructor") val constructor: ConstructorInfo
)
