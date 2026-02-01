package com.example.limouserapp.data.model.dashboard

import com.google.gson.annotations.SerializedName

/**
 * User FAQ API Response Models
 * API: GET api/user-faq
 */
data class FaqData(
    @SerializedName("faq_type")
    val faqType: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("sections")
    val sections: List<FaqSection>
)

data class FaqSection(
    @SerializedName("title")
    val title: String,

    @SerializedName("items")
    val items: List<FaqItem>
)

data class FaqItem(
    @SerializedName("question")
    val question: String,

    @SerializedName("answer")
    val answer: String
)
