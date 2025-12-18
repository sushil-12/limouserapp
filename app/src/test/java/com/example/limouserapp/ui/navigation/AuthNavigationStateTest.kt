package com.example.limouserapp.ui.navigation

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AuthNavigationState
 * Tests the navigation logic to ensure it matches iOS behavior
 */
class AuthNavigationStateTest {

    @Test
    fun `test new user navigation flow`() {
        // Test case: New user (not authenticated)
        val userState = UserAuthState(isAuthenticated = false)
        val nextDestination = userState.getNextDestination()
        
        assertEquals("New user should go to onboarding", 
            AuthNavigationState.Onboarding, nextDestination)
    }

    @Test
    fun `test authenticated user with incomplete profile`() {
        // Test case: Authenticated user but profile not completed
        val userState = UserAuthState(
            isAuthenticated = true,
            isProfileCompleted = false,
            hasDriverRegistration = false
        )
        val nextDestination = userState.getNextDestination()
        
        assertEquals("Authenticated user with incomplete profile should go to basic details", 
            AuthNavigationState.BasicDetails, nextDestination)
    }

    @Test
    fun `test authenticated user with complete profile`() {
        // Test case: Authenticated user with complete profile
        val userState = UserAuthState(
            isAuthenticated = true,
            isProfileCompleted = true,
            hasDriverRegistration = false
        )
        val nextDestination = userState.getNextDestination()
        
        assertEquals("Authenticated user with complete profile should go to dashboard", 
            AuthNavigationState.Dashboard, nextDestination)
    }

    @Test
    fun `test user with driver registration incomplete`() {
        // Test case: User has driver registration but it's not completed
        val userState = UserAuthState(
            isAuthenticated = true,
            isProfileCompleted = false,
            hasDriverRegistration = true,
            isDriverRegistrationCompleted = false
        )
        val nextDestination = userState.getNextDestination()
        
        assertEquals("User with incomplete driver registration should go to basic details", 
            AuthNavigationState.BasicDetails, nextDestination)
    }

    @Test
    fun `test user with completed driver registration`() {
        // Test case: User has completed driver registration
        val userState = UserAuthState(
            isAuthenticated = true,
            isProfileCompleted = true,
            hasDriverRegistration = true,
            isDriverRegistrationCompleted = true
        )
        val nextDestination = userState.getNextDestination()
        
        assertEquals("User with completed driver registration should go to dashboard", 
            AuthNavigationState.Dashboard, nextDestination)
    }

    @Test
    fun `test user with driver registration but incomplete profile`() {
        // Test case: User has driver registration but profile is incomplete
        val userState = UserAuthState(
            isAuthenticated = true,
            isProfileCompleted = false,
            hasDriverRegistration = true,
            isDriverRegistrationCompleted = false
        )
        val nextDestination = userState.getNextDestination()
        
        assertEquals("User with driver registration but incomplete profile should go to basic details", 
            AuthNavigationState.BasicDetails, nextDestination)
    }
}
