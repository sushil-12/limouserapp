package com.example.limouserapp.data.model.notification

import com.google.gson.annotations.SerializedName

/**
 * Details object within each audit event
 */
data class AuditEventDetails(
    @SerializedName("sender")
    val sender: String?,
    
    @SerializedName("receiver")
    val receiver: String?,
    
    @SerializedName("receiver_type")
    val receiverType: String?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("current_status")
    val currentStatus: String?,
    
    @SerializedName("status_display")
    val statusDisplay: String?
)

