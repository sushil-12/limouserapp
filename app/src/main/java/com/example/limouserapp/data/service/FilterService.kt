package com.example.limouserapp.data.service

import com.example.limouserapp.data.api.FilterApi
import com.example.limouserapp.data.model.booking.FilterData
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags

@Singleton
class FilterService @Inject constructor(
    private val api: FilterApi
) {
    private var cachedFilterData: FilterData? = null
    
    suspend fun fetchFilters(): FilterData {
        return try {
            Log.d(DebugTags.BookingProcess, "Fetching filters from API")
            val response = api.getFilters()
            if (response.success) {
                cachedFilterData = response.data
                Log.d(DebugTags.BookingProcess, "Filters fetched successfully")
                response.data
            } else {
                Log.e(DebugTags.BookingProcess, "Filter API returned success=false: ${response.message}")
                cachedFilterData ?: FilterData()
            }
        } catch (e: Exception) {
            Log.e(DebugTags.BookingProcess, "Error fetching filters", e)
            cachedFilterData ?: FilterData()
        }
    }
    
    fun getCachedFilters(): FilterData? = cachedFilterData
}

