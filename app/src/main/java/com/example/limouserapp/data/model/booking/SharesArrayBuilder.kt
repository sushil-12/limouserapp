package com.example.limouserapp.data.model.booking

import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags

/**
 * SharesArrayBuilder - matches iOS SharesArrayBuilder logic exactly
 * Calculates shares array from rate array for reservation requests
 */
object SharesArrayBuilder {
    
    /**
     * Build shares array from rate array
     * Matches iOS SharesArrayBuilder.buildSharesArray exactly
     */
    fun buildSharesArray(
        rateArray: BookingRateArray,
        serviceType: String,
        numberOfHours: String = "",
        accountType: String = "individual",
        returnGrandTotal: Double? = null,
        minRateInvolved: Boolean = false
    ): SharesArray {
        Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Building shares array for serviceType: $serviceType, numberOfHours: $numberOfHours")
        
        // Calculate total baserates (same as calculateTotalsFromRates)
        var totalBaserates = 0.0
        
        // Sum all_inclusive_rates baserates
        for ((key, item) in rateArray.allInclusiveRates) {
            val baserate = item.baserate
            
            // For Charter/Tour, multiply Base Rate by number of hours
            // If min_rate_involved is true, don't multiply by hours (use base rate as-is)
            // iOS checks for "Charter/Tour ?" but serviceType passed is "charter_tour" (mapped)
            // Check for both to handle the mapped value
            if ((serviceType == "Charter/Tour ?" || serviceType == "charter_tour") && key.contains("BASE_RATE") && !minRateInvolved) {
                val hours = numberOfHours.toIntOrNull() ?: 0
                val adjustedBaserate = baserate * hours.toDouble()
                totalBaserates += adjustedBaserate
                Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Adding Charter/Tour Base Rate: $key = $baserate × $hours hours = $adjustedBaserate")
            } else {
                totalBaserates += baserate
                Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Adding all_inclusive_rate: $key = $baserate")
            }
        }
        
        // Sum taxes - use amount instead of baserate (amount is the actual tax value)
        for ((key, item) in rateArray.taxes) {
            val taxAmount = item.amount
            totalBaserates += taxAmount
            Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Adding tax: $key = baserate=${item.baserate}, amount=$taxAmount, using amount=$taxAmount")
        }
        
        // Sum amenities baserates
        for ((key, item) in rateArray.amenities) {
            val baserate = item.baserate
            totalBaserates += baserate
            Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Adding amenity: $key = $baserate")
        }
        
        // Sum misc baserates
        for ((key, item) in rateArray.misc) {
            val baserate = item.baserate
            totalBaserates += baserate
            Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Adding misc: $key = $baserate")
        }
        
        Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Total baserates sum: $totalBaserates")
        
        // Calculate admin share baserates (same as calculateTotalsFromRates)
        var adminShareBaserates = 0.0
        for ((key, item) in rateArray.allInclusiveRates) {
            val baserate = item.baserate
            
            // For Charter/Tour, multiply Base Rate by number of hours for admin share calculation
            // If min_rate_involved is true, don't multiply by hours (use base rate as-is)
            // iOS checks for "Charter/Tour ?" but serviceType passed is "charter_tour" (mapped)
            // Check for both to handle the mapped value
            if ((serviceType == "Charter/Tour ?" || serviceType == "charter_tour") && key.contains("BASE_RATE") && !minRateInvolved) {
                val hours = numberOfHours.toIntOrNull() ?: 0
                val adjustedBaserate = baserate * hours.toDouble()
                adminShareBaserates += adjustedBaserate
                Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Adding Charter/Tour Base Rate to admin share: $key = $baserate × $hours hours = $adjustedBaserate")
            } else {
                adminShareBaserates += baserate
                Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Adding to admin share calculation: $key = $baserate")
            }
        }
        
        // Determine admin share percentage based on account_type
        val adminSharePercentage: Double
        val isTravelPlannerSpecialCase: Boolean
        val isFarmoutCase: Boolean
        
        // For individual account type, use standard 25%
        if (accountType == "individual") {
            adminSharePercentage = 0.25 // Default 25%
            isTravelPlannerSpecialCase = false
            isFarmoutCase = false
            Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Individual account type -> Admin share: 25%")
        } else {
            // For other account types, use standard 25% (can be extended later)
            adminSharePercentage = 0.25
            isTravelPlannerSpecialCase = false
            isFarmoutCase = false
            Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Other account type ($accountType) -> Admin share: 25%")
        }
        
        val adminShare = adminShareBaserates * adminSharePercentage
        Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Admin share (${(adminSharePercentage * 100).toInt()}% of all_inclusive_rates): $adminShare")
        
        // Calculate additional shares
        val shouldIncludeTravelAgentShare = isTravelPlannerSpecialCase
        val shouldIncludeFarmoutShare = isFarmoutCase
        
        // Calculate additional shares based on all_inclusive_rates only (just like admin share)
        val travelAgentShare = if (shouldIncludeTravelAgentShare) adminShareBaserates * 0.10 else 0.0 // 10% of all_inclusive_rates for travel planner case
        val farmoutShare = if (shouldIncludeFarmoutShare) adminShareBaserates * 0.10 else 0.0 // 10% of all_inclusive_rates for farmout case
        
        // Calculate subtotal (same as calculateTotalsFromRates)
        val subTotal = totalBaserates + adminShare + travelAgentShare + farmoutShare
        Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Subtotal (total baserates + admin share + additional shares): $subTotal")
        if (shouldIncludeTravelAgentShare) {
            Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Added travel agent share (10%): $travelAgentShare")
        }
        if (shouldIncludeFarmoutShare) {
            Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Added farmout share (10%): $farmoutShare")
        }
        
        // For individual bookings, assume 1 vehicle (matches iOS line 113)
        val numberOfVehicles = 1
        Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Number of vehicles: $numberOfVehicles")
        
        val calculatedGrandTotal = subTotal * numberOfVehicles.toDouble()
        Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Calculated grand total (subtotal × vehicles): $calculatedGrandTotal")
        
        val finalGrandTotal = calculatedGrandTotal
        
        val actualBaseRate = rateArray.allInclusiveRates["Base_Rate"]?.baserate ?: 0.0
        Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Actual Base_Rate from all_inclusive_rates: $actualBaseRate")
        
        val baseRate = actualBaseRate // Use the actual Base_Rate value
        
        val extraGratuityAmount = rateArray.misc["Extra_Gratuity"]?.amount ?: 0.0
        val extraGratuityShare = extraGratuityAmount * 0.25 // 25% of Extra_Gratuity
        Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Extra_Gratuity amount: $extraGratuityAmount, 25% share: $extraGratuityShare")
        
        val affiliateShare: Double = when {
            shouldIncludeFarmoutShare -> {
                finalGrandTotal - (adminShare + extraGratuityShare) - farmoutShare
            }
            shouldIncludeTravelAgentShare -> {
                finalGrandTotal - (adminShare + extraGratuityShare) - travelAgentShare
            }
            else -> {
                finalGrandTotal - (adminShare + extraGratuityShare)
            }
        }
        
        when {
            shouldIncludeFarmoutShare -> Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Farmout case: Affiliate share = Grand Total - Admin Share - Extra_Gratuity 25% - Farmout Share")
            shouldIncludeTravelAgentShare -> Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Travel Agent case: Affiliate share = Grand Total - Admin Share - Extra_Gratuity 25% - Travel Agent Share")
            else -> Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Standard case: Affiliate share = Grand Total - Admin Share - Extra_Gratuity 25%")
        }
        
        val stripeFee = finalGrandTotal * 0.05 + 0.30
        val deductedAdminShare = adminShare - stripeFee
        
        Log.d(DebugTags.BookingProcess, "🔄 SharesArrayBuilder - Final calculations:")
        Log.d(DebugTags.BookingProcess, "  Base rate: $baseRate")
        Log.d(DebugTags.BookingProcess, "  Admin share: $adminShare")
        Log.d(DebugTags.BookingProcess, "  Extra_Gratuity amount: $extraGratuityAmount")
        Log.d(DebugTags.BookingProcess, "  Extra_Gratuity 25% share: $extraGratuityShare")
        Log.d(DebugTags.BookingProcess, "  Affiliate share: $affiliateShare")
        if (shouldIncludeTravelAgentShare) {
            Log.d(DebugTags.BookingProcess, "  Travel agent share: $travelAgentShare")
        }
        if (shouldIncludeFarmoutShare) {
            Log.d(DebugTags.BookingProcess, "  Farmout share: $farmoutShare")
        }
        Log.d(DebugTags.BookingProcess, "  Stripe fee: $stripeFee")
        Log.d(DebugTags.BookingProcess, "  Deducted admin share: $deductedAdminShare")
        Log.d(DebugTags.BookingProcess, "  Final grand total: $finalGrandTotal")
        
        return SharesArray(
            baseRate = baseRate,
            grandTotal = finalGrandTotal,
            stripeFee = stripeFee,
            adminShare = adminShare,
            deductedAdminShare = deductedAdminShare,
            affiliateShare = affiliateShare,
            travelAgentShare = if (shouldIncludeTravelAgentShare) travelAgentShare else null,
            farmoutShare = if (shouldIncludeFarmoutShare) farmoutShare else null,
            returnGrandTotal = returnGrandTotal
        )
    }
}

