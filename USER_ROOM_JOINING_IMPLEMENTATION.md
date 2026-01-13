# User Room Joining Implementation - Android Matching iOS

## Overview
Implemented user room joining functionality in Android to match iOS `SimpleSocketIOService.swift` behavior.

## iOS Implementation Reference

### iOS Behavior:
```swift
// In setupEventHandlers()
socket.on(clientEvent: .connect) { [weak self] data, ack in
    // ...
    // Join room with current user ID
    self.joinUserRoom()
    // ...
}

socket.on(clientEvent: .reconnect) { [weak self] data, ack in
    // ...
    // Rejoin room with current user ID after reconnection
    self.joinUserRoom()
    // ...
}

private func joinUserRoom() {
    guard let socket = socket, isConnected else { return }
    
    let userId = StorageManager.shared.getUserIdString() ?? "unknown"
    if userId == "unknown" { return }
    
    let roomData: [String: Any] = ["room": userId]
    socket.emit("join-room", roomData as SocketData)
}
```

## Android Implementation

### Changes Made:

1. **Added `joinUserRoom()` method** (matching iOS):
   - Extracts userId from JWT token
   - Emits "join-room" event with `{"room": userId}`
   - Includes proper error handling and logging

2. **Called on Socket Connect**:
   - Added to `EVENT_CONNECT` handler
   - Automatically joins user room when socket connects

3. **Called on Connection Confirmation**:
   - Added to "connected" event handler
   - Ensures room is joined after server confirms connection

### Code Location:
- **File**: `app/src/main/java/com/example/limouserapp/data/socket/SocketService.kt`
- **Method**: `joinUserRoom()` (private)
- **Called from**: `EVENT_CONNECT` and `"connected"` event handlers

## Implementation Details

### User ID Extraction:
- Extracts userId from JWT token using `extractUserIdFromToken()`
- Same method used in `connect()` for consistency

### Room Joining:
- Emits "join-room" event with JSON payload: `{"room": userId}`
- Matches iOS format exactly

### Error Handling:
- Checks for token availability
- Validates userId is not "unknown"
- Verifies socket is connected before emitting
- Includes logging for debugging

### Reconnection Handling:
- When socket reconnects, `connect()` is called again
- This triggers `EVENT_CONNECT` which calls `joinUserRoom()`
- Matches iOS behavior where reconnect also calls `joinUserRoom()`

## Benefits

1. **User-Specific Notifications**: User receives notifications for their user ID
2. **Consistent with iOS**: Same behavior across platforms
3. **Automatic**: No manual calls needed - happens on connect/reconnect
4. **Error Resilient**: Handles missing token, invalid userId, disconnected socket

## Testing Checklist

- [x] User room is joined when socket connects
- [x] User room is joined on connection confirmation
- [x] User room is joined on reconnect (via EVENT_CONNECT)
- [x] Error handling for missing token
- [x] Error handling for invalid userId
- [x] Error handling for disconnected socket
- [x] Logging for debugging

## Notes

- The method is `private` (matching iOS)
- Room joining happens automatically on connect/reconnect
- No manual calls needed from views
- User ID is extracted from JWT token each time (consistent with iOS pattern)
