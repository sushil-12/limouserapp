package com.example.limouserapp.ui.booking.comprehensivebooking

import java.util.UUID

/**
 * Extra Stop Model - matches iOS ExtraStop class
 */
data class ExtraStop(
    val id: String = UUID.randomUUID().toString(),
    var address: String = "",
    var latitude: Double? = null,
    var longitude: Double? = null,
    var isLocationSelected: Boolean = false,
    var bookingInstructions: String = ""
)

