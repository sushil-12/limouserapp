package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.limouserapp.data.service.AirportService
import com.example.limouserapp.data.service.RecentLocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ScheduleRideViewModel @Inject constructor(
    val airportService: AirportService,
    val recentLocationService: RecentLocationService
) : ViewModel()

