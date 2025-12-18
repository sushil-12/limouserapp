package com.example.limouserapp.data.model.notification

import com.google.gson.annotations.SerializedName

/**
 * User object within each audit event
 */
data class AuditEventUser(
    @SerializedName("id")
    val id: Int?,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("role")
    val role: String
)

