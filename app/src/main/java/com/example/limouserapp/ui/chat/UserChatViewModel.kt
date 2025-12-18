package com.example.limouserapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.socket.ActiveRide
import com.example.limouserapp.data.socket.SocketService
import com.example.limouserapp.data.notification.interfaces.NotificationDisplayManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

data class UserChatUiState(
    val bookingId: Int = 0,
    val driverId: String? = null,
    val driverName: String = "Driver",
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<UserChatMessage> = emptyList()
)

data class UserChatMessage(
    val id: String,
    val bookingId: Int,
    val text: String,
    val createdAtIso: String?,
    val isFromDriver: Boolean
)

@HiltViewModel
class UserChatViewModel @Inject constructor(
    private val socketService: SocketService,
    private val notificationDisplayManager: NotificationDisplayManager
) : ViewModel() {

    private val client = OkHttpClient()

    private val _bookingId = MutableStateFlow(0)
    private val _driverId = MutableStateFlow<String?>(null)
    private val _driverName = MutableStateFlow("Driver")
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _history = MutableStateFlow<List<UserChatMessage>>(emptyList())
    private val _pendingMessages = MutableStateFlow<List<UserChatMessage>>(emptyList())

    val uiState: StateFlow<UserChatUiState> = combine(
        _bookingId,
        _driverId,
        _driverName,
        socketService.connectionStatus,
        socketService.chatMessages,
        _history,
        _pendingMessages,
        _isLoading,
        _error
    ) { values: Array<Any?> ->
        val bookingId = values[0] as Int
        val driverId = values[1] as String?
        val driverName = values[2] as String
        val conn = values[3] as com.example.limouserapp.data.model.dashboard.ConnectionStatus
        val socketMsgs = values[4] as List<com.example.limouserapp.data.socket.ChatMessage>
        val history = values[5] as List<UserChatMessage>
        val pendingMsgs = values[6] as List<UserChatMessage>
        val loading = values[7] as Boolean
        val error = values[8] as String?

        val liveMsgs = socketMsgs
            .filter { it.bookingId == bookingId }
            .map {
                UserChatMessage(
                    id = it.id,
                    bookingId = it.bookingId,
                    text = it.message,
                    createdAtIso = it.timestamp.takeIf { ts -> ts > 0L }?.let { ts ->
                        Instant.ofEpochMilli(ts).toString()
                    },
                    isFromDriver = it.isFromDriver
                )
            }

        // Merge by id (history + realtime + pending)
        val merged = (history + liveMsgs + pendingMsgs)
            .distinctBy { it.id }
            .sortedBy { it.createdAtIso ?: "" }

        UserChatUiState(
            bookingId = bookingId,
            driverId = driverId,
            driverName = driverName,
            isConnected = conn.isConnected,
            isLoading = loading,
            errorMessage = error,
            messages = merged
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserChatUiState()
    )

    fun start(bookingId: Int, activeRide: ActiveRide?) {
        _bookingId.value = bookingId
        _error.value = null

        // Set current chat booking ID to suppress notifications for this chat
        (notificationDisplayManager as? com.example.limouserapp.data.notification.display.NotificationDisplayManagerImpl)
            ?.setCurrentChatBookingId(bookingId)
        Timber.d("Chat opened for booking $bookingId - suppressing notifications")

        // Ensure socket is connected + join booking room for chat stream.
        socketService.connect()
        socketService.joinBookingRoom(bookingId)

        val ride = activeRide?.takeIf { it.bookingId.toIntOrNull() == bookingId }
        _driverId.value = ride?.driverId?.ifBlank { null }
        _driverName.value = ride?.driverName?.ifBlank { null } ?: "Driver"

        fetchHistory()
    }

    fun stop() {
        // Clear current chat booking ID when leaving chat
        (notificationDisplayManager as? com.example.limouserapp.data.notification.display.NotificationDisplayManagerImpl)
            ?.setCurrentChatBookingId(null)

        val bookingId = _bookingId.value
        Timber.d("Chat closed for booking $bookingId - notifications will resume")
        if (bookingId > 0) {
            socketService.leaveBookingRoom(bookingId)
        }
    }

    fun sendMessage(text: String) {
        val bookingId = _bookingId.value
        val receiverId = _driverId.value
        val trimmed = text.trim()
        if (bookingId <= 0 || receiverId.isNullOrBlank() || trimmed.isBlank()) return

        // Add message immediately to pending messages for immediate UI feedback
        val pendingMessage = UserChatMessage(
            id = "pending_${System.currentTimeMillis()}",
            bookingId = bookingId,
            text = trimmed,
            createdAtIso = java.time.Instant.now().toString(),
            isFromDriver = false // User messages are not from driver
        )

        _pendingMessages.value = _pendingMessages.value + pendingMessage

        // Send via socket
        socketService.sendChatMessage(bookingId = bookingId, receiverId = receiverId, message = trimmed)
    }

    fun fetchHistory(page: Int = 1, limit: Int = 50) {
        val bookingId = _bookingId.value
        if (bookingId <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                val url = "https://limortservice.infodevbox.com/api/messages/$bookingId?page=$page&limit=$limit"
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .header("x-secret", "limoapi_notifications_secret_2024_xyz789")
                    .header("Content-Type", "application/json")
                    .build()

                val resp = client.newCall(req).execute()
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    _error.value = "Failed to load chat history"
                    Timber.w("Chat history HTTP ${resp.code}: $body")
                    return@launch
                }

                val root = JSONObject(body)
                if (!root.optBoolean("success", false)) {
                    _error.value = "Failed to load chat history"
                    return@launch
                }

                val arr: JSONArray = root.optJSONArray("data") ?: JSONArray()
                val parsed = buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("_id", obj.optString("id", ""))
                        val senderRole = obj.optString("senderRole", obj.optString("sender_role", ""))
                        val msg = obj.optString("message", "")
                        val createdAt = obj.optString("createdAt", obj.optString("created_at", null))
                            ?: obj.optString("createdAt", null)
                        val bId = obj.optInt("bookingId", obj.optInt("booking_id", bookingId))
                        add(
                            UserChatMessage(
                                id = id.ifBlank { "msg_$i" },
                                bookingId = bId,
                                text = msg,
                                createdAtIso = createdAt,
                                isFromDriver = senderRole == "driver"
                            )
                        )
                    }
                }
                _history.value = parsed
            } catch (e: Exception) {
                Timber.e(e, "Failed to load chat history")
                _error.value = e.message ?: "Failed to load chat history"
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}


