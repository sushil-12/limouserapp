package com.example.limouserapp.data.model.dashboard

import com.google.gson.annotations.SerializedName

/**
 * Paginated response wrapper for API endpoints that return paginated data
 */
data class PaginatedResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: PaginatedData<T>,
    
    @SerializedName("currency")
    val currency: CurrencyInfo? = null
)

/**
 * Paginated data structure
 */
data class PaginatedData<T>(
    @SerializedName("current_page")
    val currentPage: Int,
    
    @SerializedName("data")
    val data: List<T>,
    
    @SerializedName("first_page_url")
    val firstPageUrl: String?,
    
    @SerializedName("from")
    val from: Int?,
    
    @SerializedName("last_page")
    val lastPage: Int,
    
    @SerializedName("last_page_url")
    val lastPageUrl: String?,
    
    @SerializedName("links")
    val links: List<PageLink>,
    
    @SerializedName("next_page_url")
    val nextPageUrl: String?,
    
    @SerializedName("path")
    val path: String,
    
    @SerializedName("per_page")
    val perPage: Int,
    
    @SerializedName("prev_page_url")
    val prevPageUrl: String?,
    
    @SerializedName("to")
    val to: Int?,
    
    @SerializedName("total")
    val total: Int
)

/**
 * Page link for pagination
 */
data class PageLink(
    @SerializedName("url")
    val url: String?,
    
    @SerializedName("label")
    val label: String,
    
    @SerializedName("active")
    val active: Boolean
)

/**
 * Currency information
 */
data class CurrencyInfo(
    @SerializedName("countryName")
    val countryName: String,
    
    @SerializedName("currency")
    val currency: String,
    
    @SerializedName("currencyCountry")
    val currencyCountry: String,
    
    @SerializedName("symbol")
    val symbol: String,
    
    @SerializedName("dateFormat")
    val dateFormat: String
)
