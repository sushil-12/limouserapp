package com.example.limouserapp.ui.liveride

/**
 * Metrics tracking for Live Ride screen
 * Production-ready instrumentation for monitoring route calculation performance
 */
object LiveRideMetrics {
    private var routeFailureCount = 0
    private var routeSuccessCount = 0
    private var totalRouteCalculationTime = 0L
    private var routeCalculationCount = 0
    private var fallbackOccurrences = 0
    
    /**
     * Record successful route calculation
     */
    fun recordRouteSuccess(durationMs: Long) {
        routeSuccessCount++
        totalRouteCalculationTime += durationMs
        routeCalculationCount++
    }
    
    /**
     * Record failed route calculation
     */
    fun recordRouteFailure() {
        routeFailureCount++
    }
    
    /**
     * Record fallback occurrence (should not happen in production)
     */
    fun recordFallback() {
        fallbackOccurrences++
    }
    
    /**
     * Get average route calculation latency
     */
    fun getAverageRouteLatency(): Long {
        return if (routeCalculationCount > 0) {
            totalRouteCalculationTime / routeCalculationCount
        } else {
            0L
        }
    }
    
    /**
     * Get route success rate
     */
    fun getRouteSuccessRate(): Double {
        val total = routeSuccessCount + routeFailureCount
        return if (total > 0) {
            routeSuccessCount.toDouble() / total
        } else {
            0.0
        }
    }
    
    /**
     * Get metrics summary
     */
    fun getMetricsSummary(): String {
        return "RouteMetrics: success=$routeSuccessCount, failures=$routeFailureCount, " +
                "avgLatency=${getAverageRouteLatency()}ms, fallbacks=$fallbackOccurrences"
    }
    
    /**
     * Reset metrics (for testing)
     */
    fun reset() {
        routeFailureCount = 0
        routeSuccessCount = 0
        totalRouteCalculationTime = 0L
        routeCalculationCount = 0
        fallbackOccurrences = 0
    }
}
