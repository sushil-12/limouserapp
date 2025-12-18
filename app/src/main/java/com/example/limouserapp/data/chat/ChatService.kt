package com.example.limouserapp.data.chat

import com.example.limouserapp.data.socket.SocketService
import com.example.limouserapp.data.storage.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chat service for real-time communication with drivers
 * Manages chat messages and real-time updates
 */
@Singleton
class ChatService @Inject constructor(
    private val socketService: SocketService,
    private val tokenManager: TokenManager
) {
    
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _currentBookingId = MutableStateFlow<Int?>(null)
    val currentBookingId: StateFlow<Int?> = _currentBookingId.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    init {
        // Observe socket connection status
        observeSocketConnection()
        // Observe incoming chat messages
        observeIncomingMessages()
    }
    
    /**
     * Start chat for a specific booking
     */
    fun startChat(bookingId: Int) {
        try {
            _currentBookingId.value = bookingId
            _chatMessages.value = emptyList()
            _unreadCount.value = 0
            
            // Join booking room for chat
            socketService.joinBookingRoom(bookingId)
            
            Timber.d("Started chat for booking: $bookingId")
            
        } catch (e: Exception) {
            Timber.e(e, "Error starting chat for booking: $bookingId")
        }
    }
    
    /**
     * Stop chat
     */
    fun stopChat() {
        try {
            _currentBookingId.value?.let { bookingId ->
                socketService.leaveBookingRoom(bookingId)
            }
            
            _currentBookingId.value = null
            _chatMessages.value = emptyList()
            _unreadCount.value = 0
            
            Timber.d("Stopped chat")
            
        } catch (e: Exception) {
            Timber.e(e, "Error stopping chat")
        }
    }
    
    /**
     * Send a chat message
     */
    fun sendMessage(message: String) {
        val bookingId = _currentBookingId.value
        if (bookingId == null) {
            Timber.w("No active chat session")
            return
        }
        
        if (message.isBlank()) {
            Timber.w("Cannot send empty message")
            return
        }
        
        try {
            // For now, we'll use a placeholder receiverId since we don't have driver info yet
            // In a real implementation, you'd get this from the booking data
            val receiverId = "driver_${bookingId}" // Placeholder - should be actual driver ID
            socketService.sendChatMessage(bookingId, receiverId, message)
            
            // Add message to local list immediately for better UX
            val newMessage = ChatMessage(
                id = generateMessageId(),
                bookingId = bookingId,
                senderId = getCurrentUserId(),
                senderName = getCurrentUserName(),
                message = message,
                timestamp = System.currentTimeMillis(),
                isFromDriver = false
            )
            
            addMessageToLocalList(newMessage)
            
            Timber.d("Sent message: $message")
            
        } catch (e: Exception) {
            Timber.e(e, "Error sending message")
        }
    }
    
    /**
     * Mark messages as read
     */
    fun markMessagesAsRead() {
        _unreadCount.value = 0
        Timber.d("Marked messages as read")
    }
    
    /**
     * Get messages for current booking
     */
    fun getMessagesForCurrentBooking(): List<ChatMessage> {
        return _chatMessages.value
    }
    
    /**
     * Get messages for specific booking
     */
    fun getMessagesForBooking(bookingId: Int): List<ChatMessage> {
        return _chatMessages.value.filter { it.bookingId == bookingId }
    }
    
    /**
     * Check if chat is active
     */
    fun isChatActive(): Boolean {
        return _currentBookingId.value != null
    }
    
    /**
     * Get current booking ID
     */
    fun getCurrentBookingId(): Int? {
        return _currentBookingId.value
    }
    
    /**
     * Get unread message count
     */
    fun getUnreadCount(): Int {
        return _unreadCount.value
    }
    
    /**
     * Observe socket connection status
     */
    private fun observeSocketConnection() {
        // TODO: Implement proper observation of socket connection
        // This would typically be done with Flow operators
        _isConnected.value = socketService.isConnected()
    }
    
    /**
     * Observe incoming chat messages
     */
    private fun observeIncomingMessages() {
        // TODO: Implement proper observation of incoming messages
        // This would typically be done with Flow operators
    }
    
    /**
     * Add message to local list
     */
    private fun addMessageToLocalList(message: ChatMessage) {
        val currentMessages = _chatMessages.value.toMutableList()
        currentMessages.add(message)
        _chatMessages.value = currentMessages
        
        // Update unread count if message is from driver
        if (message.isFromDriver) {
            _unreadCount.value = _unreadCount.value + 1
        }
    }
    
    /**
     * Generate unique message ID
     */
    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(0..999).random()}"
    }
    
    /**
     * Get current user ID
     */
    private fun getCurrentUserId(): String {
        // TODO: Get from user session or token
        return "user_${System.currentTimeMillis()}"
    }
    
    /**
     * Get current user name
     */
    private fun getCurrentUserName(): String {
        // TODO: Get from user profile
        return "You"
    }
    
    /**
     * Clear all messages
     */
    fun clearMessages() {
        _chatMessages.value = emptyList()
        _unreadCount.value = 0
        Timber.d("Cleared all messages")
    }
    
    /**
     * Get chat status
     */
    fun getChatStatus(): String {
        return buildString {
            append("Connected: ${_isConnected.value}")
            append(", Active: ${isChatActive()}")
            append(", Booking: ${_currentBookingId.value}")
            append(", Messages: ${_chatMessages.value.size}")
            append(", Unread: ${_unreadCount.value}")
        }
    }
}

/**
 * Chat message data class
 */
data class ChatMessage(
    val id: String,
    val bookingId: Int,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isFromDriver: Boolean
) {
    /**
     * Get formatted timestamp
     */
    val formattedTimestamp: String
        get() = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    
    /**
     * Check if message is recent (within last 5 minutes)
     */
    val isRecent: Boolean
        get() = System.currentTimeMillis() - timestamp < 5 * 60 * 1000
}
