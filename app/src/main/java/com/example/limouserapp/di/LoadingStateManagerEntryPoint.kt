package com.example.limouserapp.di

import com.example.limouserapp.data.network.LoadingStateManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint to access LoadingStateManager from non-injectable contexts
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LoadingStateManagerEntryPoint {
    fun loadingStateManager(): LoadingStateManager
}

