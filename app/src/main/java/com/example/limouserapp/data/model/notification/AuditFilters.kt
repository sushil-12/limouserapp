package com.example.limouserapp.data.model.notification

import com.google.gson.annotations.SerializedName

/**
 * Filters applied to the audit records query
 */
data class AuditFilters(
    @SerializedName("from")
    val from: String?,
    
    @SerializedName("to")
    val to: String?,
    
    @SerializedName("event_type")
    val eventType: String?,
    
    @SerializedName("event_category")
    val eventCategory: String?,
    
    @SerializedName("search")
    val search: String?,
    
    @SerializedName("booking_id")
    val bookingId: Int?
)

