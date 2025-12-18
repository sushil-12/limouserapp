package com.example.limouserapp.di

import com.example.limouserapp.data.local.UserStateManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint to access UserStateManager from non-injectable contexts (like Composables)
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserStateManagerEntryPoint {
    fun userStateManager(): UserStateManager
}

