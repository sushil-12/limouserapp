package com.example.limouserapp.data.model.dashboard

import com.google.gson.annotations.SerializedName

/**
 * User Tutorials API Response Models
 * API: GET api/tutorials?type=user_tutorials
 */
data class TutorialData(
    @SerializedName("type")
    val type: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("contents")
    val contents: List<TutorialContent>
)

data class TutorialContent(
    @SerializedName("type")
    val type: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("image")
    val image: String,

    @SerializedName("link")
    val link: String,

    @SerializedName("content")
    val content: String
)
