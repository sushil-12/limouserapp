# 1800Limo iOS App - Authentication Flow Documentation

## Overview
This document provides comprehensive payload information for login and OTP screens, along with the complete authentication flow for AI implementation.

## 1. Login Screen Payload

### Request Payload
**Endpoint:** `POST /api/mobile/v1/auth/login-or-register`

```json
{
  "phone_isd": "+1",
  "phone_country": "us", 
  "phone": "1234567890",
  "user_type": "customer"
}
```

### Response Payload
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "message": "OTP sent to your phone number",
    "otp_type": "sms",
    "temp_user_id": "temp_1234567890",
    "expires_in": 300,
    "cooldown_remaining": 60
  },
  "timestamp": "2024-01-01T12:00:00Z",
  "code": 200
}
```

### Key Fields:
- `phone_isd`: Country code with + (e.g., "+1", "+44")
- `phone_country`: Country short code (e.g., "us", "uk")
- `phone`: Phone number without spaces or formatting
- `user_type`: Always "customer" for user app
- `temp_user_id`: Temporary ID for OTP verification
- `expires_in`: OTP expiration time in seconds
- `cooldown_remaining`: Resend cooldown in seconds

## 2. OTP Verification Screen Payload

### Request Payload
**Endpoint:** `POST /api/mobile/v1/auth/verify-otp`

```json
{
  "temp_user_id": "temp_1234567890",
  "otp": "123456"
}
```

### Response Payload
```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": {
    "user": {
      "id": 12345,
      "phone": "+11234567890",
      "role": 1,
      "is_profile_completed": false,
      "last_login_at": "2024-01-01T12:00:00Z",
      "created_from": "mobile_app",
      "customer_registration_state": {
        "current_step": "basic_details",
        "progress_percentage": 25,
        "is_completed": false,
        "next_step": "credit_card",
        "steps": {
          "phone_verified": true,
          "basic_details": false,
          "credit_card": false,
          "profile_complete": false
        },
        "completed_steps": ["phone_verified"],
        "total_steps": 4,
        "completed_count": 1
      }
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "action": "navigate_to_basic_info",
    "driver_registration_state": null
  },
  "timestamp": "2024-01-01T12:00:00Z",
  "code": 200
}
```

### Key Fields:
- `temp_user_id`: From login response
- `otp`: 6-digit verification code
- `user.id`: Unique user identifier
- `user.is_profile_completed`: Boolean indicating profile completion
- `token`: JWT authentication token
- `customer_registration_state`: Registration progress tracking
- `action`: Next navigation action

## 3. Complete Authentication Flow

### Flow Diagram
```
1. Login Screen
   ↓ (phone number input)
2. Send OTP API Call
   ↓ (success response with temp_user_id)
3. OTP Screen
   ↓ (6-digit OTP input)
4. Verify OTP API Call
   ↓ (success response with user data)
5. Navigation Decision Logic
   ↓
6. Next Screen (Basic Info / Dashboard)
```

### Navigation Logic After OTP Verification

#### Decision Tree:
```
IF driver_registration_state exists:
  IF driver_registration_state.is_completed == true:
    → Navigate to Dashboard
  ELSE:
    IF user.is_profile_completed == false:
      → Navigate to Basic Info Screen
    ELSE:
      → Navigate to Dashboard
ELSE:
  IF user.is_profile_completed == false:
    → Navigate to Basic Info Screen
  ELSE:
    → Navigate to Dashboard
```

### Storage Management

#### After Login (OTP Sent):
```swift
StorageManager.shared.saveTempAuthData(response)
// Stores: temp_user_id, otp_type, expires_in, cooldown_remaining
```

#### After OTP Verification:
```swift
StorageManager.shared.saveLoginData(response)
// Stores: user data, auth token, driver registration state
// Clears: temp data
```

## 4. Error Handling

### Common Error Responses:
```json
{
  "success": false,
  "message": "Invalid phone number format",
  "code": 400,
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Error Codes:
- `400`: Bad Request (invalid input)
- `429`: Too Many Requests (rate limiting)
- `500`: Internal Server Error

## 5. Resend OTP Flow

### Request Payload
**Endpoint:** `POST /api/mobile/v1/auth/resend-otp`

```json
{
  "temp_user_id": "temp_1234567890"
}
```

### Response Payload
```json
{
  "success": true,
  "message": "OTP resent successfully",
  "data": {
    "message": "New OTP sent to your phone number",
    "otp_type": "sms",
    "temp_user_id": "temp_1234567890",
    "expires_in": 300,
    "cooldown_remaining": 60
  },
  "timestamp": "2024-01-01T12:00:00Z",
  "code": 200
}
```

## 6. Implementation Guidelines for AI

### Required Models:
1. `LoginRegisterRequest` - Login request payload
2. `AuthResponse` - Login response payload
3. `VerifyOTPRequest` - OTP verification request
4. `VerifyOTPResponse` - OTP verification response
5. `User` - User data model
6. `CustomerRegistrationState` - Registration progress tracking

### Required Services:
1. `NetworkService` - API communication
2. `StorageManager` - Data persistence
3. `AuthViewModel` - Business logic

### Key Implementation Points:
1. **Phone Validation**: Ensure proper country code and phone number formatting
2. **OTP Validation**: 6-digit numeric validation
3. **State Management**: Proper handling of loading states and errors
4. **Navigation Logic**: Implement the decision tree for post-authentication navigation
5. **Data Persistence**: Store temporary and permanent authentication data
6. **Error Handling**: Comprehensive error handling with user-friendly messages
7. **Resend Logic**: Implement cooldown mechanism for OTP resending

### Security Considerations:
1. Store sensitive data (tokens) in Keychain
2. Clear temporary data after successful authentication
3. Implement proper session management
4. Handle token expiration gracefully

## 7. Next Steps After Authentication

### If Profile Not Completed:
1. Navigate to Basic Info Screen
2. Collect: name, email
3. Submit via `/api/mobile/v1/user/registration/basic-details`
4. Navigate to Credit Card Screen
5. Collect: card details
6. Submit via `/api/mobile/v1/user/registration/credit-card`
7. Navigate to Dashboard

### If Profile Completed:
1. Navigate directly to Dashboard
2. Load user's active rides
3. Show booking history
4. Enable ride booking functionality

This documentation provides all necessary information for implementing the authentication flow in any AI development environment.
