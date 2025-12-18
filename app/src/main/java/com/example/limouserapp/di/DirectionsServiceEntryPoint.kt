package com.example.limouserapp.di

import com.example.limouserapp.data.service.DirectionsService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DirectionsServiceEntryPoint {
    fun directionsService(): DirectionsService
}

