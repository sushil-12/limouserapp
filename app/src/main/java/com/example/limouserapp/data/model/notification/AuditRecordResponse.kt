package com.example.limouserapp.data.model.notification

import com.google.gson.annotations.SerializedName

/**
 * Main response wrapper for booking audit records API
 */
data class AuditRecordResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: AuditRecordData?,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("code")
    val code: Int
)

/**
 * Data container for audit records
 */
data class AuditRecordData(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("total_events")
    val totalEvents: Int,
    
    @SerializedName("total_bookings")
    val totalBookings: Int,
    
    @SerializedName("events")
    val events: List<AuditEvent>,
    
    @SerializedName("pagination")
    val pagination: AuditPagination,
    
    @SerializedName("filters_applied")
    val filtersApplied: AuditFilters
)

