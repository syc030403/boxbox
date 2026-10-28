package com.boxbox.app.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class RaceApiResponse(
    @SerializedName("MRData") val mrData: MrData
)

data class MrData(
    @SerializedName("RaceTable") val raceTable: RaceTable
)

data class RaceTable(
    @SerializedName("season") val season: String,
    @SerializedName("Races") val races: List<Race>
)

@Parcelize
data class Race(
    @SerializedName("round") val round: String,
    @SerializedName("raceName") val raceName: String,
    @SerializedName("Circuit") val circuit: Circuit,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String? = null,
    @SerializedName("FirstPractice") val firstPractice: Session? = null,
    @SerializedName("SecondPractice") val secondPractice: Session? = null,
    @SerializedName("ThirdPractice") val thirdPractice: Session? = null,
    @SerializedName("Qualifying") val qualifying: Session? = null,
    @SerializedName("Sprint") val sprint: Session? = null,
    @SerializedName("SprintQualifying") val sprintQualifying: Session? = null
) : Parcelable

@Parcelize
data class Circuit(
    @SerializedName("circuitName") val circuitName: String,
    @SerializedName("Location") val location: Location? = null
) : Parcelable

@Parcelize
data class Location(
    @SerializedName("locality") val locality: String? = null,
    @SerializedName("country") val country: String? = null
) : Parcelable

@Parcelize
data class Session(
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String? = null
) : Parcelable
