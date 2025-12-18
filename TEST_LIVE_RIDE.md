# Quick Test Guide - Live Ride Feature

## ✅ What's Been Added

### 1. **Test Button** (📍 Top-right corner of Dashboard)
- Red floating action button with 🧪 emoji
- **TEMPORARY** - Will be removed after testing
- Location: Top-end corner, below menu button

### 2. **SocketService Test Method**
```kotlin
fun setTestActiveRide(testRide: ActiveRide)
```
- Directly sets active ride data for testing
- Bypasses socket connection requirement

## 🧪 How to Test

### Step 1: Run the App
1. Build and run the app
2. Navigate to Dashboard screen

### Step 2: Trigger Test
1. Look for red **🧪** button in top-right corner
2. Click it
3. You'll be navigated to Live Ride screen

### Step 3: What You'll See
- Map showing driver location (San Francisco coordinates)
- Pickup and dropoff markers
- Bottom card with ride status
- ETA and distance information

## 📍 Test Data Coordinates

```
Driver Location: 37.7749, -122.4194
Pickup: 37.7849, -122.4094  
Dropoff: 37.7649, -122.4294
```

## 🔧 Code Locations

### Files to Remove After Testing:
1. **DashboardScreen.kt** - Test button code (lines ~249-277)
2. **MainActivity.kt** - Test parameters (line ~388, ~398)
3. **SocketService.kt** - `setTestActiveRide()` method (lines ~467-470)
4. **LiveRideTestHelper.kt** - Entire file (can delete)

## 🗑️ Quick Removal Guide

### Step 1: Remove Test Button from DashboardScreen
Remove lines 248-277 in `DashboardScreen.kt`

### Step 2: Remove from MainActivity
Remove:
- Line 388: `val socketService...`
- Line 398: `socketService = socketService,`

### Step 3: Remove from SocketService
Remove:
- Lines 463-470: `setTestActiveRide()` method

### Step 4: Delete Test Helper
- Delete: `LiveRideTestHelper.kt`

### Step 5: Update Dashboard Signature
Remove parameters from `DashboardScreen` function:
```kotlin
// REMOVE these parameters:
socketService: SocketService? = null,
onNavigateToLiveRide: () -> Unit = {}
```

## ✅ Production Flow (After Removal)

Once removed, the live ride screen will only trigger when:
1. **Real socket event** arrives with `type: "live_ride"`
2. **user.notifications** event received
3. SocketService automatically processes and updates `_activeRide`
4. ViewModel observes and navigates to Live Ride screen

## 🎯 Current Status

✅ Test button works
✅ Navigation works
✅ Live ride screen displays
✅ Map loads correctly
✅ Mock data displays

**Test it now, then remove the test code!**
