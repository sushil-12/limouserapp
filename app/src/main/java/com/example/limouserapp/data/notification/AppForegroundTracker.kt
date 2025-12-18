package com.example.limouserapp.data.notification

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether the app process is in the foreground.
 *
 * Used to decide whether to show in-app banners vs system notifications for Socket.IO events.
 */
@Singleton
class AppForegroundTracker @Inject constructor() : DefaultLifecycleObserver {

    private val _isInForeground = MutableStateFlow(false)
    val isInForeground: StateFlow<Boolean> = _isInForeground.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        _isInForeground.value = true
        Timber.d("AppForegroundTracker: foreground=true")
    }

    override fun onStop(owner: LifecycleOwner) {
        _isInForeground.value = false
        Timber.d("AppForegroundTracker: foreground=false")
    }
}


