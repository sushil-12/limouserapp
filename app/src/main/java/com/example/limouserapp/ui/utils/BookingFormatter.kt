package com.example.limouserapp.ui.utils

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility object for formatting booking-related data
 * Centralizes formatting functions to avoid code duplication
 */
object BookingFormatter {
    
    /**
     * Format date string to "EEE, MMM dd" format
     * Example: 2025-10-27 -> Mon, Oct 27
     */
    fun formatDate(dateString: String): String {
        return try {
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
            val date = inputFormatter.parse(dateString) ?: return dateString
            outputFormatter.format(date)
        } catch (e: Exception) {
            dateString
        }
    }
    
    /**
     * Format time string from "HH:mm:ss" to "h:mm a" format
     * Example: 14:30:00 -> 2:30 PM
     */
    fun formatTime(timeString: String): String {
        return try {
            val inputFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val outputFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = inputFormatter.parse(timeString) ?: return timeString
            outputFormatter.format(date)
        } catch (e: Exception) {
            timeString
        }
    }
    
    /**
     * Format service type to display text
     */
    fun formatServiceType(serviceType: String?): String {
        return when (serviceType?.lowercase()) {
            "oneway" -> "One Way"
            "roundtrip" -> "Round Trip"
            "chartertour", "charter_tour" -> "Charter Tour"
            "hourly" -> "Hourly"
            else -> serviceType?.capitalize() ?: ""
        }
    }
    
    /**
     * Format transfer type to display text
     */
    fun formatTransferType(transferType: String): String {
        return transferType.replace("_", " ").capitalize()
    }
    
    /**
     * Format currency amount
     * Example: 1234.56 -> $1,234.56
     */
    fun formatCurrency(amount: Double): String {
        return String.format(Locale.getDefault(), "$%.2f", amount)
    }
    
    /**
     * Format currency with custom symbol
     */
    fun formatCurrency(amount: Double, symbol: String): String {
        return String.format(Locale.getDefault(), "$symbol%.2f", amount)
    }
    
    /**
     * Get booking status color
     */
    fun getBookingStatusColor(status: String): Color {
        return when (status.lowercase()) {
            "confirmed" -> Color(0xFF4CAF50) // Green
            "pending" -> Color(0xFFFF9800) // Orange
            "completed" -> Color(0xFF2196F3) // Blue
            "cancelled" -> Color(0xFFF44336) // Red
            else -> Color.Gray
        }
    }
    
    /**
     * Format date and time together
     */
    fun formatBookingTime(date: String, time: String): String {
        return "${formatDate(date)}, ${formatTime(time)}"
    }
    
    /**
     * Parse date from UI format back to API format
     */
    fun parseDateToAPI(dateString: String): String {
        return try {
            val inputFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
            val outputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormatter.parse(dateString) ?: return dateString
            outputFormatter.format(date)
        } catch (e: Exception) {
            dateString
        }
    }
    
    /**
     * Parse time from UI format back to API format
     */
    fun parseTimeToAPI(timeString: String): String {
        return try {
            val inputFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
            val outputFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val date = inputFormatter.parse(timeString) ?: return timeString
            outputFormatter.format(date)
        } catch (e: Exception) {
            timeString
        }
    }
}

