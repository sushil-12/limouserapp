package com.example.limouserapp.data.model.notification

import com.google.gson.annotations.SerializedName

/**
 * Individual audit event model
 */
data class AuditEvent(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("event_type")
    val eventType: String,
    
    @SerializedName("event_category")
    val eventCategory: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("timestamp_formatted")
    val timestampFormatted: String,
    
    @SerializedName("details")
    val details: AuditEventDetails?,
    
    @SerializedName("user")
    val user: AuditEventUser?,
    
    @SerializedName("booking_id")
    val bookingId: Int?
)

