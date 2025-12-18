package com.example.limouserapp.di

import com.example.limouserapp.data.api.NotificationApi
import com.example.limouserapp.data.notification.channel.NotificationChannelManager
import com.example.limouserapp.data.notification.display.NotificationDisplayManagerImpl
import com.example.limouserapp.data.notification.factory.NotificationIntentFactoryImpl
import com.example.limouserapp.data.notification.handler.BookingNotificationHandler
import com.example.limouserapp.data.notification.handler.ChatNotificationHandler
import com.example.limouserapp.data.notification.handler.DefaultNotificationHandler
import com.example.limouserapp.data.notification.handler.LiveRideNotificationHandler
import com.example.limouserapp.data.notification.handler.NotificationHandlerManager
import com.example.limouserapp.data.notification.interfaces.FcmTokenRepository
import com.example.limouserapp.data.notification.interfaces.NotificationDisplayManager
import com.example.limouserapp.data.notification.interfaces.NotificationIntentFactory
import com.example.limouserapp.data.notification.repository.FcmTokenRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton
import kotlin.jvm.JvmSuppressWildcards

/**
 * Dependency injection module for notification system
 * Provides all notification-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    
    /**
     * Provide NotificationApi
     */
    @Provides
    @Singleton
    fun provideNotificationApi(
        @Named("main") retrofit: Retrofit
    ): NotificationApi {
        return retrofit.create(NotificationApi::class.java)
    }
    
    /**
     * Provide NotificationIntentFactory
     */
    @Provides
    @Singleton
    fun provideNotificationIntentFactory(): NotificationIntentFactory {
        return NotificationIntentFactoryImpl()
    }
    
    /**
     * Provide NotificationDisplayManager
     */
    @Provides
    @Singleton
    fun provideNotificationDisplayManager(
        notificationManager: android.app.NotificationManager,
        intentFactory: NotificationIntentFactory
    ): NotificationDisplayManager {
        return NotificationDisplayManagerImpl(notificationManager, intentFactory)
    }
    
    /**
     * Provide NotificationChannelManager
     */
    @Provides
    @Singleton
    fun provideNotificationChannelManager(
        notificationManager: android.app.NotificationManager
    ): NotificationChannelManager {
        return NotificationChannelManager(notificationManager)
    }
    
    /**
     * Provide set of all NotificationHandlers for NotificationHandlerManager
     * Handlers are automatically injected by Hilt
     * @JvmSuppressWildcards prevents Kotlin from translating Set<T> to Set<? extends T> in Java
     */
    @Provides
    @Singleton
    fun provideNotificationHandlers(
        liveRideHandler: LiveRideNotificationHandler,
        bookingHandler: BookingNotificationHandler,
        chatHandler: ChatNotificationHandler,
        defaultHandler: DefaultNotificationHandler
    ): @JvmSuppressWildcards Set<com.example.limouserapp.data.notification.interfaces.NotificationHandler> {
        return setOf(
            liveRideHandler,
            bookingHandler,
            chatHandler,
            defaultHandler
        )
    }
    
    /**
     * Provide NotificationHandlerManager
     * @JvmSuppressWildcards prevents Kotlin from translating Set<T> to Set<? extends T> in Java
     */
    @Provides
    @Singleton
    fun provideNotificationHandlerManager(
        handlers: @JvmSuppressWildcards Set<com.example.limouserapp.data.notification.interfaces.NotificationHandler>
    ): NotificationHandlerManager {
        return NotificationHandlerManager(handlers)
    }
    
    /**
     * Provide FcmTokenRepository
     */
    @Provides
    @Singleton
    fun provideFcmTokenRepository(
        repository: FcmTokenRepositoryImpl
    ): FcmTokenRepository {
        return repository
    }
}

