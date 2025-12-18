package com.example.limouserapp.data.model.booking

import com.google.gson.annotations.SerializedName

/**
 * Meet and Greet Choice - matches iOS MeetGreetChoice
 */
data class MeetGreetChoice(
    @SerializedName("id") val id: Int,
    @SerializedName("message") val message: String
)

/**
 * Meet and Greet Data - matches iOS MeetGreetData
 */
data class MeetGreetData(
    @SerializedName("meetGreets") val meetGreets: List<MeetGreetChoice>
)

/**
 * Meet and Greet Response - matches iOS MeetGreetResponse
 */
data class MeetGreetResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: MeetGreetData,
    @SerializedName("message") val message: String
)

