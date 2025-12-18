package com.example.limouserapp.ui.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class NavEvent {
    data class ToLiveRide(val bookingId: String? = null) : NavEvent()
    data class ToChat(val bookingId: Int) : NavEvent()
}

/**
 * In-process navigation event bus for reacting to Socket/FCM events while the app is running.
 * This avoids "restart app to navigate" issues.
 */
@Singleton
class NavigationEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<NavEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<NavEvent> = _events.asSharedFlow()

    fun tryEmit(event: NavEvent) {
        _events.tryEmit(event)
    }
}


