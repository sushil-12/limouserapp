package com.example.limouserapp.data.model.notification

import com.google.gson.annotations.SerializedName

/**
 * Pagination information for audit records
 */
data class AuditPagination(
    @SerializedName("current_page")
    val currentPage: Int,
    
    @SerializedName("per_page")
    val perPage: Int,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("last_page")
    val lastPage: Int,
    
    @SerializedName("from")
    val from: Int?,
    
    @SerializedName("to")
    val to: Int?
)

