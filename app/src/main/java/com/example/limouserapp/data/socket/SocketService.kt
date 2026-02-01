package com.example.limouserapp.data.socket

import android.annotation.SuppressLint
import android.util.Log
import com.example.limouserapp.data.model.dashboard.ConnectionStatus
import com.example.limouserapp.data.notification.AppForegroundTracker
import com.example.limouserapp.data.notification.handler.NotificationHandlerManager
import com.example.limouserapp.data.notification.model.NotificationPayload
import com.example.limouserapp.data.notification.model.NotificationPriority
import com.example.limouserapp.data.notification.model.NotificationType
import com.example.limouserapp.data.storage.TokenManager
import com.example.limouserapp.ui.navigation.NavEvent
import com.example.limouserapp.ui.navigation.NavigationEventBus
import dagger.Lazy
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import timber.log.Timber
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketService @Inject constructor(
    private val tokenManager: TokenManager,
    private val notificationHandlerManager: Lazy<NotificationHandlerManager>,
    private val appForegroundTracker: AppForegroundTracker,
    private val navigationEventBus: NavigationEventBus
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var socket: Socket? = null
    private val _connectionStatus = MutableStateFlow(ConnectionStatus(isConnected = false))
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _driverLocations = MutableStateFlow<List<DriverLocationUpdate>>(emptyList())
    val driverLocations: StateFlow<List<DriverLocationUpdate>> = _driverLocations.asStateFlow()
    
    private val _bookingUpdates = MutableStateFlow<List<BookingUpdate>>(emptyList())
    val bookingUpdates: StateFlow<List<BookingUpdate>> = _bookingUpdates.asStateFlow()
    
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    
    private val _userNotifications = MutableStateFlow<List<UserNotification>>(emptyList())
    val userNotifications: StateFlow<List<UserNotification>> = _userNotifications.asStateFlow()
    
    private val _activeRide = MutableStateFlow<ActiveRide?>(null)
    val activeRide: StateFlow<ActiveRide?> = _activeRide.asStateFlow()
    
    private var connectionAttempts = 0
    private var lastConnectionTime: Long? = null
    private var isReconnecting = false
    private var reconnectJob: Job? = null
    private var isManualDisconnect = false
    
    // Store room IDs for rejoin after reconnect
    private var currentCustomerId: String? = null
    private var currentBookingId: String? = null
    
    // Throttling for driver location updates (1-2 seconds for UI)
    private var lastDriverLocationUpdateTime = 0L
    private val DRIVER_LOCATION_THROTTLE_MS = 1500L
    
    companion object {
        private const val TAG = "SocketService"
        // private const val SOCKET_URL = "http://10.10.60.196:3000"
        private const val SOCKET_URL = "https://limortservice.infodevbox.com"

        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val INITIAL_RECONNECT_DELAY = 1000L
        private const val MAX_RECONNECT_DELAY = 30000L
        private const val BACKOFF_MULTIPLIER = 1.5
        private const val CONNECTION_TIMEOUT = 30000L
        private const val PING_TIMEOUT = 60000L
        private const val PING_INTERVAL = 25000L
    }
    
    fun connect() {
        scope.launch {
            try {   
                val token = tokenManager.getAccessToken()
                if (token == null) {
                    Timber.e("No authentication token available for Socket.IO connection")
                    updateConnectionStatus(false, "No authentication token")
                    return@launch
                }
                
                val userId = extractUserIdFromToken(token) ?: "unknown"
                disconnectInternal()
                
                val options = IO.Options().apply {
                    forceNew = true
                    reconnection = false
                    timeout = CONNECTION_TIMEOUT
                    
                    // CORRECTED: Match server expectations exactly
                    auth = mutableMapOf(
                        "userId" to userId,
                        "userType" to "customer",
                        "secret" to "limoapi_notifications_secret_2024_xyz789"
                    )
                    
                    // IMPORTANT: For local HTTP server, use polling only to avoid websocket errors
                    // WebSocket connections to HTTP (non-HTTPS) servers are blocked by Android
                    // Try polling first which works with both HTTP and HTTPS
                    transports = arrayOf("polling", "websocket")
                    // Try to force polling to work
                    upgrade = false
                }
                
                val uri = URI.create(SOCKET_URL)
                Timber.d("Creating Socket.IO connection with URI: $uri")
                socket = IO.socket(uri, options)
                
                setupEventListeners()
                
                isManualDisconnect = false
                
                // Log before connecting
                Timber.d("Socket.IO connecting to $SOCKET_URL with userId=$userId")
                Timber.d("Transport options: ${options.transports.contentToString()}")
                Timber.d("Options - timeout: ${options.timeout}, reconnection: ${options.reconnection}")
                
                socket?.connect()
                
            } catch (e: Exception) {
                Timber.e(e, "Error connecting to Socket.IO server")
                updateConnectionStatus(false, e.message ?: "Connection failed")
                scheduleReconnect()
            }
        }
    }
    
    private fun extractUserIdFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                val jsonObject = org.json.JSONObject(payload)
                jsonObject.optString("sub", null)
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Error parsing JWT token")
            null
        }
    }
    
    fun disconnect() {
        isManualDisconnect = true
        reconnectJob?.cancel()
        disconnectInternal()
    }
    
    private fun disconnectInternal() {
        try {
            socket?.disconnect()
            socket = null
            updateConnectionStatus(false, "Disconnected")
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting from Socket.IO server")
        }
    }
    
    private fun setupEventListeners() {
        // CORRECTED: Listen for server's actual events
        socket?.on(Socket.EVENT_CONNECT) {
            Timber.d("Socket.IO connected successfully")
            lastConnectionTime = System.currentTimeMillis()
            connectionAttempts = 0
            isReconnecting = false
            updateConnectionStatus(true)
            
            // CRITICAL: Join user room with user ID (for general notifications)
            // Backend contract: Authentication does NOT subscribe to updates, must join room
            joinUserRoom()
            
            // CRITICAL: Rejoin ride room if we have an active ride
            // Backend contract: Room membership is LOST on reconnect, must rejoin
            // This ensures driver.location.update continues to arrive after reconnect
            rejoinRideRoomIfNeeded()
        }
        
        socket?.on(Socket.EVENT_DISCONNECT) { args ->
            Timber.d("Socket.IO disconnected: ${args.joinToString()}")
            updateConnectionStatus(false, "Disconnected")
        }
        
        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Timber.e("Socket.IO connection error: ${args.joinToString()}")
            connectionAttempts++
            isReconnecting = true
            updateConnectionStatus(false, "Connection error")
            
            if (!isManualDisconnect) {
                scheduleReconnect()
            }
        }
        
        // CORRECTED: Listen for server's actual events
        socket?.on("connected") { args ->
            try {
                val data = args[0] as? JSONObject
                data?.let { json ->
                    Timber.d("Received connection confirmation: ${json.toString()}")
                }
                
                // Join user room after connection confirmation (matching iOS behavior)
                joinUserRoom()
                
                // CRITICAL: Rejoin ride room if we have an active ride
                // Backend contract: Room membership is LOST on reconnect, must rejoin
                rejoinRideRoomIfNeeded()
            } catch (e: Exception) {
                Timber.e(e, "Error processing connected event")
            }
        }
        
        // CORRECTED: Main notification event from server
        socket?.on("user.notifications") { args ->
            try {
                val data = args[0] as? JSONObject
                Timber.d("Received user notification: ${data.toString()}")
                data?.let { json ->
                    val notification = UserNotification(
                        id = json.optString("id", ""),
                        title = json.optString("title", ""),
                        message = json.optString("message", ""),
                        type = json.optString("type", ""),
                        priority = json.optString("priority", "normal"),
                        data = json.optJSONObject("data"),
                        timestamp = json.optString("timestamp", ""),
                        replayed = json.optBoolean("replayed", false)
                    )
                    addUserNotification(notification)

                    // Bugfix (1): If app is backgrounded, show a *system* notification too.
                    // In foreground, we keep using the in-app NotificationContainer banner.
                    if (!appForegroundTracker.isInForeground.value) {
                        scope.launch {
                            runCatching {
                                notificationHandlerManager.get().processNotification(notification.toNotificationPayload())
                            }.onFailure { e ->
                                Timber.e(e, "Failed to display system notification for socket event")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing user notification")
            }
        }
        
        // Listen for room join confirmation from backend
        socket?.on("room-joined") { args ->
            try {
                val data = args[0] as? JSONObject
                data?.let { json ->
                    val room = json.optString("room", "")
                    Timber.d("✅ Room joined successfully: $room")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing room-joined event")
            }
        }
        
        // CORRECTED: Driver location updates (with dots)
        // Backend contract: io.to(customerId).emit("driver.location.update", payload)
        // This event is ONLY received if client is in the correct room
        // Backend code: this.io.to(customerId).emit('driver.location.update', payload)
        socket?.on("driver.location.update") { args ->
            try {
                Timber.d("📡 [DRIVER_LOCATION] ✅ RECEIVED driver.location.update event!")
                Timber.d("📡 [DRIVER_LOCATION] Args: ${args.contentToString()}")
                Timber.d("📡 [DRIVER_LOCATION] Args count: ${args.size}, types: ${args.map { it?.javaClass?.simpleName }}")
                
                // Handle both array and single object formats
                val data = when {
                    args.isNotEmpty() && args[0] is JSONObject -> args[0] as JSONObject
                    args.isNotEmpty() && args[0] is String -> JSONObject(args[0] as String)
                    else -> null
                }
                
                data?.let { json ->
                    Timber.d("📡 Parsing driver location update: ${json.toString()}")
                    
                    // Extract bookingId with comprehensive handling
                    val bookingIdStr = try {
                        val bookingIdValue = json.opt("bookingId") ?: json.opt("booking_id")
                        when {
                            bookingIdValue is String -> bookingIdValue.ifBlank { null }
                            bookingIdValue is Int -> bookingIdValue.toString()
                            bookingIdValue is Double -> bookingIdValue.toInt().toString()
                            bookingIdValue is Long -> bookingIdValue.toString()
                            bookingIdValue != null -> bookingIdValue.toString().ifBlank { null }
                            else -> {
                                val str1 = json.optString("bookingId", "")
                                val str2 = json.optString("booking_id", "")
                                (if (str1.isNotBlank()) str1 else str2).ifBlank { null }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error extracting bookingId")
                        null
                    }
                    
                    Timber.d("📡 Extracted bookingId: '$bookingIdStr'")
                    
                    val driverUpdate = DriverLocationUpdate(
                        driverId = json.optString("userId") ?: json.optString("driver_id", ""),
                        latitude = json.optDouble("latitude", 0.0),
                        longitude = json.optDouble("longitude", 0.0),
                        heading = json.optDouble("heading", 0.0),
                        speed = json.optDouble("speed", 0.0),
                        timestamp = json.optLong("timestamp", 0L),
                        bookingId = bookingIdStr
                    )
                    updateDriverLocation(driverUpdate)
                } ?: Timber.w("📡 driver.location.update event received but data is null or not JSONObject. Args: ${args.contentToString()}")
            } catch (e: Exception) {
                Timber.e(e, "Error processing driver location update")
            }
        }
        
        // CORRECTED: Active ride event
        socket?.on("active_ride") { args ->
            try {
                Timber.d("Active ride started or found")
                val data = args[0] as? JSONObject
                Timber.d("Received Active Ride: ${data.toString()}")

                data?.let { json ->
                    // Extract the nested data object
                    val rideData = json.optJSONObject("data") ?: json
                    Timber.d("Extracted ride data: ${rideData.toString()}")
                    
                    // Extract nested objects
                    val locations = rideData.optJSONObject("locations")
                    val pickup = locations?.optJSONObject("pickup")
                    val dropoff = locations?.optJSONObject("dropoff")
                    val driver = rideData.optJSONObject("driver")
                    val customer = rideData.optJSONObject("customer")

                    val driverName = driver?.optString("name")
                        ?.ifBlank { null }
                        ?: listOfNotNull(
                            driver?.optString("first_name")?.ifBlank { null },
                            driver?.optString("last_name")?.ifBlank { null }
                        ).joinToString(" ").ifBlank { null }
                    val driverPhone = driver?.optString("phone")?.ifBlank { null }
                        ?: driver?.optString("mobile")?.ifBlank { null }
                        ?: driver?.optString("cell")?.ifBlank { null }

                    val vehicleObj = driver?.optJSONObject("vehicle") ?: rideData.optJSONObject("vehicle")
                    val vehicleType = vehicleObj?.optString("vehicle_type")?.ifBlank { null }
                        ?: vehicleObj?.optString("type")?.ifBlank { null }
                    val plate = vehicleObj?.optString("plate_number")?.ifBlank { null }
                        ?: vehicleObj?.optString("plate")?.ifBlank { null }
                    
                    val activeRide = ActiveRide(
                        bookingId = rideData.optString("booking_id") ?: rideData.optString("bookingId", ""),
                        driverId = driver?.optString("id") ?: rideData.optString("driver_id") ?: rideData.optString("driverId", ""),
                        customerId = customer?.optString("id") ?: rideData.optString("customer_id") ?: rideData.optString("customerId", ""),
                        status = rideData.optString("status", ""),
                        driverLatitude = rideData.optDouble("driver_latitude", 0.0).let { if (it == 0.0) rideData.optDouble("driverLatitude", 0.0) else it },
                        driverLongitude = rideData.optDouble("driver_longitude", 0.0).let { if (it == 0.0) rideData.optDouble("driverLongitude", 0.0) else it },
                        pickupLatitude = pickup?.optDouble("latitude", 0.0) ?: rideData.optDouble("pickup_latitude", 0.0).let { if (it == 0.0) rideData.optDouble("pickupLatitude", 0.0) else it },
                        pickupLongitude = pickup?.optDouble("longitude", 0.0) ?: rideData.optDouble("pickup_longitude", 0.0).let { if (it == 0.0) rideData.optDouble("pickupLongitude", 0.0) else it },
                        dropoffLatitude = dropoff?.optDouble("latitude", 0.0) ?: rideData.optDouble("dropoff_latitude", 0.0).let { if (it == 0.0) rideData.optDouble("dropoffLatitude", 0.0) else it },
                        dropoffLongitude = dropoff?.optDouble("longitude", 0.0) ?: rideData.optDouble("dropoff_longitude", 0.0).let { if (it == 0.0) rideData.optDouble("dropoffLongitude", 0.0) else it },
                        pickupAddress = pickup?.optString("address") ?: rideData.optString("pickup_address") ?: rideData.optString("pickupAddress", ""),
                        dropoffAddress = dropoff?.optString("address") ?: rideData.optString("dropoff_address") ?: rideData.optString("dropoffAddress", ""),
                        timestamp = json.optString("timestamp", System.currentTimeMillis().toString()),
                        driverName = driverName,
                        driverPhone = driverPhone,
                        vehicleType = vehicleType,
                        plateNumber = plate
                    )
                    
                    Timber.d("Created ActiveRide: bookingId=${activeRide.bookingId}, status=${activeRide.status}")
                    _activeRide.value = activeRide

                    // Backend contract: driver.location.update is ONLY emitted to room members
                    // Room priority: customerId (PRIMARY) > bookingId (FALLBACK)
                    // Join customerId room (PRIMARY) - backend emits to this room primarily
                    joinRideRoom(activeRide.customerId, activeRide.bookingId)
                    
                    // SAFETY: Also join bookingId room as fallback (matching Angular behavior)
                    // Backend may emit to bookingId room in some cases, so join both for safety
                    if (activeRide.bookingId.isNotBlank()) {
                        activeRide.bookingId.toIntOrNull()?.let { bookingId ->
                            joinBookingRoom(bookingId)
                            Timber.d("🔷 Also joined bookingId room (FALLBACK): $bookingId")
                            
                            // Request driver location after a short delay to ensure room join is processed
                            // Some backends require explicit request to start emitting location updates
                            scope.launch {
                                delay(500) // Small delay to ensure room join is processed
                                requestDriverLocation(bookingId)
                                Timber.d("📡 Requested driver location for booking: $bookingId")
                            }
                        }
                    }
                    
                    // Store room IDs for rejoin on reconnect
                    currentCustomerId = activeRide.customerId.ifBlank { null }
                    currentBookingId = activeRide.bookingId.ifBlank { null }

                    // Auto-navigation (bug #3): if app is foregrounded, jump to live ride immediately.
                    if (appForegroundTracker.isInForeground.value) {
                        navigationEventBus.tryEmit(NavEvent.ToLiveRide(bookingId = activeRide.bookingId))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing active ride")
            }
        }
        
        // CORRECTED: Chat message event (with dots)
        socket?.on("chat.message") { args ->
            try {
                val data = args[0] as? JSONObject
                data?.let { json ->
                    val chatMessage = ChatMessage(
                        id = json.optString("_id") ?: json.optString("id", ""),
                        bookingId = json.optInt("booking_id", 0).let { if (it == 0) json.optInt("bookingId", 0) else it },
                        senderId = json.optString("sender_id") ?: json.optString("senderId", ""),
                        senderName = json.optString("sender_name") ?: json.optString("senderName", ""),
                        message = json.optString("message", ""),
                        timestamp = json.optLong("timestamp", 0L),
                        isFromDriver = (json.optString("sender_role") ?: json.optString("senderRole", "")) == "driver"
                    )
                    addChatMessage(chatMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing chat message")
            }
        }
        
        // Heartbeat handling
        socket?.on("heartbeat") {
            Timber.d("Received heartbeat from server")
        }
    }
    
    // CORRECTED: Send chat message with proper event name
    fun sendChatMessage(bookingId: Int, receiverId: String, message: String) {
        try {
            val data = JSONObject().apply {
                put("bookingId", bookingId)
                put("receiverId", receiverId)
                put("message", message)
            }
            socket?.emit("chat.message", data)
        } catch (e: Exception) {
            Timber.e(e, "Error sending chat message")
        }
    }
    
    /**
     * Join user room with user ID (matching iOS behavior)
     * Backend contract: Authentication validates identity but does NOT subscribe to updates
     * This joins the user's general notification room
     */
    private fun joinUserRoom() {
        try {
            val token = tokenManager.getAccessToken()
            if (token == null) {
                Timber.w("Cannot join user room - no authentication token available")
                return
            }
            
            val userId = extractUserIdFromToken(token) ?: "unknown"
            if (userId == "unknown") {
                Timber.w("Cannot join user room - user ID is unknown")
                return
            }
            
            if (socket?.connected() != true) {
                Timber.w("Cannot join user room - socket is not connected")
                return
            }
            
            // Emit join-room event with user ID (matching iOS: {"room": userId})
            socket?.emit("join-room", JSONObject().apply {
                put("room", userId)
            })
            Timber.d("🔷 Joined user room: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Error joining user room")
        }
    }
    
    /**
     * Join ride room for driver location updates
     * Backend contract: driver.location.update is ONLY emitted to room members
     * Priority: customerId (PRIMARY) > bookingId (FALLBACK)
     * 
     * CRITICAL: Backend emits driver.location.update ONLY to the room specified
     * If client is not in the room, events are silently dropped (no error)
     * 
     * @param customerId Preferred room ID (customer's user ID) - PRIMARY
     * @param bookingId Fallback room ID if customerId is unavailable - FALLBACK
     */
    private fun joinRideRoom(customerId: String, bookingId: String) {
        try {
            if (socket?.connected() != true) {
                Timber.w("❌ Cannot join ride room - socket is not connected")
                return
            }
            
            // Backend contract: customerId is PRIMARY, bookingId is FALLBACK
            // Backend logic: io.to(customerId).emit(...) OR io.to(bookingId).emit(...)
            // IMPORTANT: Backend expects room as STRING (see backend handleJoinRoom: room: string)
            val roomId = when {
                customerId.isNotBlank() -> {
                    Timber.d("🔷 Joining ride room with customerId (PRIMARY): $customerId")
                    customerId // Use as string
                }
                bookingId.isNotBlank() -> {
                    Timber.d("🔷 Joining ride room with bookingId (FALLBACK): $bookingId")
                    bookingId // Use as string
                }
                else -> {
                    Timber.w("❌ Cannot join ride room - both customerId and bookingId are empty")
                    return
                }
            }
            
            // Emit join-room event (backend expects: socket.emit("join-room", { room: "<room_id>" }))
            // Backend handleJoinRoom accepts room: string, so always send as string
            socket?.emit("join-room", JSONObject().apply {
                put("room", roomId) // Always send as string to match backend contract
            })
            Timber.d("📡 Now listening for driver.location.update events in room: $roomId")
        } catch (e: Exception) {
            Timber.e(e, "❌ Error joining ride room")
        }
    }
    
    /**
     * Rejoin ride room after reconnect
     * Backend contract: Room membership is LOST on reconnect, must rejoin
     * This ensures driver.location.update continues to arrive after reconnect
     */
    private fun rejoinRideRoomIfNeeded() {
        val activeRide = _activeRide.value
        if (activeRide != null) {
            // Rejoin using stored room IDs or active ride data
            val customerId = currentCustomerId ?: activeRide.customerId
            val bookingId = currentBookingId ?: activeRide.bookingId
            
            // Rejoin customerId room (PRIMARY)
            joinRideRoom(customerId, bookingId)
            
            // SAFETY: Also rejoin bookingId room as fallback (matching Angular behavior)
            if (bookingId.isNotBlank()) {
                bookingId.toIntOrNull()?.let { bookingIdInt ->
                    joinBookingRoom(bookingIdInt)
                    // Request driver location after rejoin to ensure updates resume
                    scope.launch {
                        delay(500) // Small delay to ensure room join is processed
                        requestDriverLocation(bookingIdInt)
                        Timber.d("📡 Requested driver location after reconnect for booking: $bookingIdInt")
                    }
                }
            }
            
            Timber.d("🔄 Rejoined ride rooms after reconnect (customerId=$customerId, bookingId=$bookingId)")
        }
    }
    
    // CORRECTED: Join room with proper event name
    // NOTE: This is a legacy method - prefer joinRideRoom() which handles customerId/bookingId priority
    fun joinBookingRoom(bookingId: Int) {
        try {
            if (socket?.connected() != true) {
                Timber.w("Cannot join booking room - socket is not connected")
                return
            }
            val roomId = bookingId.toString()
            socket?.emit("join-room", JSONObject().apply {
                put("room", roomId) // Backend expects string
            })
            Timber.d("🔷 Joined booking room: $roomId (bookingId: $bookingId)")
        } catch (e: Exception) {
            Timber.e(e, "Error joining booking room")
        }
    }
    
    fun leaveBookingRoom(bookingId: Int) {
        try {
            socket?.emit("leave-room", JSONObject().apply {
                put("room", bookingId.toString())
            })
            Timber.d("Left booking room: $bookingId")
        } catch (e: Exception) {
            Timber.e(e, "Error leaving booking room")
        }
    }
    
    // Send heartbeat to server
    fun sendHeartbeat() {
        try {
            socket?.emit("heartbeat")
        } catch (e: Exception) {
            Timber.e(e, "Error sending heartbeat")
        }
    }
    
    // Request driver location for a specific booking
    fun requestDriverLocation(bookingId: Int) {
        try {
            if (socket?.connected() != true) {
                Timber.w("❌ Cannot request driver location - socket is not connected")
                return
            }
            val requestData = JSONObject().apply {
                put("bookingId", bookingId)
            }
            socket?.emit("request_driver_location", requestData)
            Timber.d("📡 Emitted request_driver_location event for bookingId: $bookingId")
        } catch (e: Exception) {
            Timber.e(e, "Error requesting driver location")
        }
    }
    
    private fun updateConnectionStatus(
        isConnected: Boolean,
        error: String? = null
    ) {
        _connectionStatus.value = ConnectionStatus(
            isConnected = isConnected,
            lastConnected = lastConnectionTime,
            connectionAttempts = connectionAttempts,
            isReconnecting = isReconnecting,
            error = error
        )
    }
    
    private fun updateDriverLocation(driverUpdate: DriverLocationUpdate) {
        // Throttle updates to prevent excessive processing
        val now = System.currentTimeMillis()
        val timeSinceLastUpdate = now - lastDriverLocationUpdateTime
        
        if (timeSinceLastUpdate < DRIVER_LOCATION_THROTTLE_MS) {
            Timber.d("📡 Throttling driver location update (${timeSinceLastUpdate}ms < ${DRIVER_LOCATION_THROTTLE_MS}ms)")
            return
        }
        
        lastDriverLocationUpdateTime = now
        
        try {
            val currentLocations = _driverLocations.value.toMutableList()
            val existingIndex = currentLocations.indexOfFirst { it.driverId == driverUpdate.driverId }
            
            if (existingIndex >= 0) {
                currentLocations[existingIndex] = driverUpdate
            } else {
                currentLocations.add(driverUpdate)
            }
            
            _driverLocations.value = currentLocations
            Timber.d("📡 Driver location updated: ${driverUpdate.driverId}, lat=${driverUpdate.latitude}, lng=${driverUpdate.longitude}, bookingId=${driverUpdate.bookingId}")
        } catch (e: Exception) {
            Timber.e(e, "Error updating driver location")
        }
    }

    private fun UserNotification.toNotificationPayload(): NotificationPayload {
        // Check if this is a chat message notification
        val isChatMessage = data?.optString("type") == "chat_message"
        val typeEnum = if (isChatMessage) {
            Timber.d("Converting notification to CHAT_MESSAGE type")
            NotificationType.CHAT_MESSAGE
        } else {
            NotificationType.fromString(type)
        }

        val bookingId = data?.optString("booking_id")
            ?.ifBlank { null }
            ?: data?.optString("bookingId")?.ifBlank { null }
        Timber.d("NotificationPayload created - type: $typeEnum, isChatMessage: $isChatMessage, bookingId: $bookingId")

        val priorityEnum = when (priority.lowercase()) {
            "max" -> NotificationPriority.MAX
            "high" -> NotificationPriority.HIGH
            "low" -> NotificationPriority.LOW
            else -> NotificationPriority.DEFAULT
        }

        return NotificationPayload(
            type = typeEnum,
            title = title.ifBlank { "Notification" },
            body = message.ifBlank { "You have a new update" },
            bookingId = bookingId,
            priority = priorityEnum
        )
    }
    
    @SuppressLint("SuspiciousIndentation")
    private fun addUserNotification(notification: UserNotification) {
        // First, add to notifications list
        val currentNotifications = _userNotifications.value.toMutableList()
        currentNotifications.add(notification)
        _userNotifications.value = currentNotifications

                    // Check if notification data contains a chat message
                    notification.data?.let { data ->
                        if (data.optString("type") == "chat_message") {
                            Timber.d("📨 Processing chat message from notification: ${notification.type}")
                            val chatMessage = ChatMessage(
                                id = data.optString("_id") ?: data.optString("id", ""),
                                bookingId = data.optInt("bookingId", data.optInt("booking_id", 0)),
                                senderId = data.optString("senderId") ?: data.optString("sender_id", ""),
                                senderName = data.optString("sender_name") ?: "Driver",
                                message = data.optString("message", ""),
                                timestamp = try {
                                    java.time.Instant.parse(data.optString("createdAt")).toEpochMilli()
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                },
                                isFromDriver = data.optString("senderRole") == "driver" ||
                                              data.optString("sender_role") == "driver"
                            )
                            addChatMessage(chatMessage)
                            Timber.d("✅ Chat message added from notification: ${chatMessage.message}")

                            // Always process chat notifications (even in foreground) so suppression logic can work
                            scope.launch {
                                runCatching {
                                    notificationHandlerManager.get().processNotification(notification.toNotificationPayload())
                                }.onFailure { e ->
                                    Timber.e(e, "Failed to process chat notification")
                                }
                            }
                            return
                        }
                    }

        // Check if this is a live_ride notification (similar to iOS implementation)
        if (notification.type == "live_ride" || notification.type == "live_ride_do" || notification.type == "on_location" || notification.type == "ended") {
            Timber.d("Processing live_ride notification: ${notification.type}")
            handleLiveRideNotification(notification)
        } else {
            Timber.d("Notification type ${notification.type} not processed for live ride navigation")
        }
    }
    
    /**
     * Handle live_ride notification from user.notifications event
     * This is the main event that triggers the live ride screen (similar to iOS)
     */
    private fun handleLiveRideNotification(notification: UserNotification) {
        Timber.d("🚗 Processing live_ride notification: ${notification.type}")

        try {
            val data = notification.data
            Timber.d("🚗 Notification data is null: ${data == null}, type: ${data?.javaClass?.simpleName}")
            if (data != null) {
                Timber.d("🚗 Notification data keys: ${data.keys()?.asSequence()?.joinToString()}")
                Timber.d("🚗 Notification data: ${data.toString()}")

                // Extract driver information from nested metadata
                val metadata = data.optJSONObject("metadata")
                val driverObject = metadata?.optJSONObject("driver_object")

                val driverName = driverObject?.let {
                    val firstName = it.optString("first_name")?.ifBlank { null }
                    val lastName = it.optString("last_name")?.ifBlank { null }
                    if (firstName != null && lastName != null) "$firstName $lastName" else firstName ?: lastName
                } ?: data.optString("driver_name")?.ifBlank { null }

                val driverPhone = driverObject?.optString("mobile")?.ifBlank { null }
                    ?: data.optString("driver_phone")?.ifBlank { null }

                // Extract location information from nested structures
                val pickupInfo = data.optJSONObject("pickup_info")
                val dropoffInfo = data.optJSONObject("dropoff_info")
                val driverLocation = data.optJSONObject("location")

                val pickupLatitude = pickupInfo?.optDouble("latitude", 0.0) ?: data.optDouble("pickup_latitude", 0.0)
                val pickupLongitude = pickupInfo?.optDouble("longitude", 0.0) ?: data.optDouble("pickup_longitude", 0.0)
                val dropoffLatitude = dropoffInfo?.optDouble("latitude", 0.0) ?: data.optDouble("dropoff_latitude", 0.0)
                val dropoffLongitude = dropoffInfo?.optDouble("longitude", 0.0) ?: data.optDouble("dropoff_longitude", 0.0)

                val driverLatitude = driverLocation?.optDouble("latitude", 0.0) ?: data.optDouble("driver_latitude", 0.0)
                val driverLongitude = driverLocation?.optDouble("longitude", 0.0) ?: data.optDouble("driver_longitude", 0.0)

                val pickupAddress = pickupInfo?.optString("address") ?: data.optString("pickup_address", "")
                val dropoffAddress = dropoffInfo?.optString("address") ?: data.optString("dropoff_address", "")

                val bookingId = data.optString("booking_id") ?: data.optString("bookingId", "")
                val status = data.optString("status", "")
                Timber.d("🚗 Extracted bookingId: '$bookingId' (from key 'booking_id': '${data.optString("booking_id", "NOT_FOUND")}', from key 'bookingId': '${data.optString("bookingId", "NOT_FOUND")}'), status: '$status'")

                val activeRide = ActiveRide(
                    bookingId = bookingId,
                    driverId = data.optString("driver_id") ?: data.optString("driverId", ""),
                    customerId = data.optString("customer_id") ?: data.optString("customerId", ""),
                    status = status,
                    driverLatitude = driverLatitude,
                    driverLongitude = driverLongitude,
                    pickupLatitude = pickupLatitude,
                    pickupLongitude = pickupLongitude,
                    dropoffLatitude = dropoffLatitude,
                    dropoffLongitude = dropoffLongitude,
                    pickupAddress = pickupAddress,
                    dropoffAddress = dropoffAddress,
                    timestamp = notification.timestamp,
                    title = notification.title,
                    message = notification.message,
                    driverName = driverName,
                    driverPhone = driverPhone
                )
                
                _activeRide.value = activeRide
                
                // Backend contract: driver.location.update is ONLY emitted to room members
                // Join customerId room (PRIMARY)
                joinRideRoom(activeRide.customerId, activeRide.bookingId)
                
                // SAFETY: Also join bookingId room as fallback (matching Angular behavior)
                if (activeRide.bookingId.isNotBlank()) {
                    activeRide.bookingId.toIntOrNull()?.let { bookingId ->
                        joinBookingRoom(bookingId)
                        Timber.d("🔷 Also joined bookingId room (FALLBACK): $bookingId")
                        
                        // Request driver location after a short delay to ensure room join is processed
                        // Some backends require explicit request to start emitting location updates
                        scope.launch {
                            delay(500) // Small delay to ensure room join is processed
                            requestDriverLocation(bookingId)
                            Timber.d("📡 Requested driver location for booking: $bookingId")
                        }
                    }
                }
                
                // Store room IDs for rejoin on reconnect
                currentCustomerId = activeRide.customerId.ifBlank { null }
                currentBookingId = activeRide.bookingId.ifBlank { null }
                
                Timber.d("🚗 Active ride data updated from live_ride notification - bookingId: $bookingId, driver: ${driverName}, phone: ${driverPhone}, status: $status")

                // Auto-navigation (bug #3): if app is foregrounded, jump to live ride immediately.
                if (appForegroundTracker.isInForeground.value) {
                    navigationEventBus.tryEmit(NavEvent.ToLiveRide(bookingId = activeRide.bookingId))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing live_ride notification")
        }
    }
    
    private fun addChatMessage(chatMessage: ChatMessage) {
        val currentMessages = _chatMessages.value.toMutableList()
        currentMessages.add(chatMessage)
        _chatMessages.value = currentMessages
    }
    
    private fun scheduleReconnect() {
        if (isManualDisconnect || connectionAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Timber.w("Max reconnection attempts reached or manual disconnect")
            isReconnecting = false
            updateConnectionStatus(false, "Max reconnection attempts reached")
            return
        }
        
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = calculateReconnectDelay()
            Timber.d("Scheduling reconnection in ${delay}ms (attempt ${connectionAttempts + 1})")
            
            delay(delay)
            connect()
        }
    }
    
    private fun calculateReconnectDelay(): Long {
        val baseDelay = INITIAL_RECONNECT_DELAY * Math.pow(BACKOFF_MULTIPLIER, connectionAttempts.toDouble()).toLong()
        return minOf(baseDelay, MAX_RECONNECT_DELAY)
    }
    
    fun isConnected(): Boolean {
        return socket?.connected() == true
    }
    
    fun getConnectionInfo(): String {
        return "Connected: ${isConnected()}, Attempts: $connectionAttempts, Last: $lastConnectionTime"
    }
    
    fun forceReconnect() {
        Timber.d("Force reconnection requested")
        connectionAttempts = 0
        isManualDisconnect = false
        connect()
    }
    
    fun cleanup() {
        isManualDisconnect = true
        reconnectJob?.cancel()
        disconnectInternal()
        scope.cancel()
    }
    
    /**
     * Set active ride from UserBooking (for API-based initial check)
     * This sets basic info; socket will provide detailed location data
     */
    fun setActiveRideFromBooking(booking: com.example.limouserapp.data.model.dashboard.UserBooking) {
        // UserBooking doesn't have location data - we only set booking info here
        // The socket connection will provide the full ActiveRide data with locations
        Timber.d("📱 Active ride booking found: ${booking.bookingId}, status: ${booking.bookingStatus}")
        
        // Don't set active ride yet - wait for socket to provide full data
        // Just trigger socket connection which will emit the active_ride event
        if (!isConnected()) {
            connect()
        }
    }
    
    /**
     * Set active ride directly (for API-based initialization)
     * Public method to set active ride from external sources
     */
    fun setActiveRide(ride: ActiveRide) {
        _activeRide.value = ride
        Timber.d("📱 Active ride set: ${ride.bookingId}")
    }
}

// CORRECTED: Updated data classes to match server expectations
data class DriverLocationUpdate(
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    val heading: Double = 0.0,
    val speed: Double = 0.0,
    val timestamp: Long,
    val bookingId: String? = null
)

data class BookingUpdate(
    val bookingId: Int,
    val status: String,
    val message: String,
    val timestamp: Long,
    val driverInfo: DriverInfo? = null
)

data class DriverInfo(
    val driverId: String,
    val name: String,
    val phone: String,
    val vehicleInfo: String
)

data class ChatMessage(
    val id: String,
    val bookingId: Int,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isFromDriver: Boolean
)

data class UserNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val priority: String,
    val data: JSONObject?,
    val timestamp: String,
    val replayed: Boolean
)

data class ActiveRide(
    val bookingId: String,
    val driverId: String,
    val customerId: String,
    val status: String,
    val driverLatitude: Double,
    val driverLongitude: Double,
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val dropoffLatitude: Double,
    val dropoffLongitude: Double,
    val pickupAddress: String,
    val dropoffAddress: String,
    val timestamp: String,
    val title: String = "",
    val message: String = "",
    // Driver details (used for contact + UI)
    val driverName: String? = null,
    val driverPhone: String? = null,
    val vehicleType: String? = null,
    val plateNumber: String? = null
)