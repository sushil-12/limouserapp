package com.example.limouserapp.di

import android.app.NotificationManager
import android.content.Context
import com.example.limouserapp.data.api.AuthApi
import com.example.limouserapp.data.api.AirportApi
import com.example.limouserapp.data.api.AirlineApi
import com.example.limouserapp.data.api.MeetGreetApi
import com.example.limouserapp.data.api.DashboardApi
import com.example.limouserapp.data.api.LocationApi
import com.example.limouserapp.data.api.DirectionsApi
import com.example.limouserapp.data.api.DistanceMatrixApi
import com.example.limouserapp.data.api.RegistrationApi
import com.example.limouserapp.data.api.QuoteApi
import com.example.limouserapp.data.api.BookingApi
import com.example.limouserapp.data.api.FilterApi
import com.example.limouserapp.data.model.auth.BooleanIntTypeAdapter
import com.example.limouserapp.data.model.booking.AirportOptionTypeAdapter
import com.example.limouserapp.data.model.booking.AirlineOptionTypeAdapter
import com.example.limouserapp.data.model.booking.IntTypeAdapter
import com.example.limouserapp.data.model.booking.DoubleTypeAdapter
import com.example.limouserapp.data.model.booking.AirportOption
import com.example.limouserapp.data.model.booking.AirlineOption
import com.example.limouserapp.data.network.NetworkConfig
import com.example.limouserapp.data.network.interceptors.AuthInterceptor
import com.example.limouserapp.data.network.interceptors.LoggingInterceptor
import com.example.limouserapp.data.network.interceptors.RetryInterceptor
import com.example.limouserapp.data.network.interceptors.LoadingInterceptor
import com.example.limouserapp.data.network.LoadingStateManager
import com.example.limouserapp.data.storage.TokenManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Network module for dependency injection
 * Provides network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Provide custom Gson instance with BooleanIntTypeAdapter and custom serializers
     * serializeNulls() ensures all fields are included in JSON (matches web format)
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .serializeNulls() // Include null fields in JSON (matches web format)
            .registerTypeAdapter(Boolean::class.java, BooleanIntTypeAdapter())
            .registerTypeAdapter(Boolean::class.javaObjectType, BooleanIntTypeAdapter())
            .registerTypeAdapter(Int::class.javaObjectType, IntTypeAdapter()) // Handle nullable Int with empty strings
            .registerTypeAdapter(Double::class.javaObjectType, DoubleTypeAdapter()) // Handle nullable Double with empty strings
            .registerTypeAdapter(AirportOption::class.java, AirportOptionTypeAdapter())
            .registerTypeAdapter(AirlineOption::class.java, AirlineOptionTypeAdapter())
            .create()
    }
    
    // LoadingStateManager is provided via @Inject constructor, no need for manual provider
    
    /**
     * Provide main API OkHttp client
     */
    @Provides
    @Singleton
    @Named("main")
    fun provideMainOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: LoggingInterceptor,
        retryInterceptor: RetryInterceptor,
        loadingInterceptor: LoadingInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.example.limouserapp.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loadingInterceptor) // Add loading interceptor first to track all requests
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(logging)
            .addInterceptor(retryInterceptor)
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * Provide Google Places API OkHttp client
     */
    @Provides
    @Singleton
    @Named("places")
    fun providePlacesOkHttpClient(
        loggingInterceptor: LoggingInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.example.limouserapp.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(logging)
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * Provide main API Retrofit instance
     */
    @Provides
    @Singleton
    @Named("main")
    fun provideMainRetrofit(
        @Named("main") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Provide Google Places API Retrofit instance
     */
    @Provides
    @Singleton
    @Named("places")
    fun providePlacesRetrofit(
        @Named("places") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.GOOGLE_PLACES_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Provide Google Roads API (snapToRoads)
     * Roads API is hosted on the same base URL as Places
     */
    @Provides
    @Singleton
    fun provideGoogleRoadsApi(
        @Named("places") retrofit: Retrofit
    ): com.example.limouserapp.data.api.GoogleRoadsApi {
        return retrofit.create(com.example.limouserapp.data.api.GoogleRoadsApi::class.java)
    }
    
    /**
     * Provide AuthApi
     */
    @Provides
    @Singleton
    fun provideAuthApi(
        @Named("main") retrofit: Retrofit
    ): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }
    
    /**
     * Provide RegistrationApi
     */
    @Provides
    @Singleton
    fun provideRegistrationApi(
        @Named("main") retrofit: Retrofit
    ): RegistrationApi {
        return retrofit.create(RegistrationApi::class.java)
    }
    
    /**
     * Provide LocationApi
     */
    @Provides
    @Singleton
    fun provideLocationApi(
        @Named("places") retrofit: Retrofit
    ): LocationApi {
        return retrofit.create(LocationApi::class.java)
    }
    
    /**
     * Provide DirectionsApi
     */
    @Provides
    @Singleton
    fun provideDirectionsApi(
        @Named("places") retrofit: Retrofit
    ): DirectionsApi {
        return retrofit.create(DirectionsApi::class.java)
    }
    
    /**
     * Provide DistanceMatrixApi
     */
    @Provides
    @Singleton
    fun provideDistanceMatrixApi(
        @Named("places") retrofit: Retrofit
    ): DistanceMatrixApi {
        return retrofit.create(DistanceMatrixApi::class.java)
    }
    
    /**
     * Provide DashboardApi
     */
    @Provides
    @Singleton
    fun provideDashboardApi(
        @Named("main") retrofit: Retrofit
    ): DashboardApi {
        return retrofit.create(DashboardApi::class.java)
    }

    /**
     * Provide QuoteApi
     */
    @Provides
    @Singleton
    fun provideQuoteApi(
        @Named("main") retrofit: Retrofit
    ): QuoteApi {
        return retrofit.create(QuoteApi::class.java)
    }

    /**
     * Provide BookingApi
     */
    @Provides
    @Singleton
    fun provideBookingApi(
        @Named("main") retrofit: Retrofit
    ): BookingApi {
        return retrofit.create(BookingApi::class.java)
    }
    
    /**
     * Provide AirportApi
     */
    @Provides
    @Singleton
    fun provideAirportApi(
        @Named("main") retrofit: Retrofit
    ): AirportApi {
        return retrofit.create(AirportApi::class.java)
    }
    
    /**
     * Provide AirlineApi
     */
    @Provides
    @Singleton
    fun provideAirlineApi(
        @Named("main") retrofit: Retrofit
    ): AirlineApi {
        return retrofit.create(AirlineApi::class.java)
    }
    
    /**
     * Provide MeetGreetApi
     */
    @Provides
    @Singleton
    fun provideMeetGreetApi(
        @Named("main") retrofit: Retrofit
    ): MeetGreetApi {
        return retrofit.create(MeetGreetApi::class.java)
    }
    
    /**
     * Provide FilterApi
     */
    @Provides
    @Singleton
    fun provideFilterApi(
        @Named("main") retrofit: Retrofit
    ): FilterApi {
        return retrofit.create(FilterApi::class.java)
    }
    
    /**
     * Provide NotificationManager
     */
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}
