package com.example.limouserapp.di

import com.example.limouserapp.data.storage.TokenManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint to access TokenManager from non-injectable contexts (e.g. Composables)
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TokenManagerEntryPoint {
    fun tokenManager(): TokenManager
}
