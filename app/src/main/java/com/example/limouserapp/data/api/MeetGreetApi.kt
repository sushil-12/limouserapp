package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.booking.MeetGreetResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Meet and Greet API service interface
 */
interface MeetGreetApi {
    
    /**
     * Fetch meet and greet choices
     * Endpoint: /api/mobile-data?only_meet_greet=true
     */
    @GET("api/mobile-data")
    suspend fun getMeetGreetChoices(
        @Query("only_meet_greet") onlyMeetGreet: Boolean = true
    ): MeetGreetResponse
}

