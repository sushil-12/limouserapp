package com.example.limouserapp.data.service

import com.example.limouserapp.data.api.QuoteApi
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.data.model.booking.VehicleListingRequest
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags

@Singleton
class QuoteService @Inject constructor(
    private val api: QuoteApi
) {
    suspend fun fetchVehicles(request: VehicleListingRequest): List<Vehicle> {
        return try {
            Log.d(DebugTags.BookingProcess, "Fetching vehicles with request=$request")
            val res = api.getVehicleListing(request)
            Log.d(DebugTags.BookingProcess, "Vehicle listing fetched, count=${res.data?.size ?: 0}")
            res.data ?: emptyList()
        } catch (e: Exception) {
            Log.e(DebugTags.BookingProcess, "Vehicle listing failed", e)
            emptyList()
        }
    }
}



