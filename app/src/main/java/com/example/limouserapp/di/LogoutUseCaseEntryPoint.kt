package com.example.limouserapp.di

import com.example.limouserapp.domain.usecase.auth.LogoutUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint to access LogoutUseCase from non-injectable contexts (like Composables)
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LogoutUseCaseEntryPoint {
    fun logoutUseCase(): LogoutUseCase
}

