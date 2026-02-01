package com.example.limouserapp.data.service

import com.google.android.gms.maps.model.LatLng
import com.example.limouserapp.data.api.GoogleRoadsApi
import com.example.limouserapp.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for snapping GPS points to roads using Google Roads API
 * Batches up to 100 points per request for efficiency
 */
@Singleton
class RoadsSnappingService @Inject constructor(
    private val roadsApi: GoogleRoadsApi
) {
    private val maxBatchSize = 100
    private var snapSuccessCount = 0
    private var snapFailureCount = 0
    private var snapLatencySum = 0L
    private var snapCallCount = 0

    /**
     * Snap a sequence of GPS points to roads
     * Returns snapped points in same order, or original points if snapping fails
     */
    suspend fun snapToRoads(points: List<LatLng>): List<LatLng> = withContext(Dispatchers.IO) {
        if (points.isEmpty()) return@withContext emptyList()
        if (points.size > maxBatchSize) {
            // Batch large sequences
            return@withContext points.chunked(maxBatchSize).flatMap { batch ->
                snapBatch(batch)
            }
        }
        return@withContext snapBatch(points)
    }

    private suspend fun snapBatch(points: List<LatLng>): List<LatLng> {
        val startTime = System.currentTimeMillis()
        return try {
            val path = points.joinToString("|") { "${it.latitude},${it.longitude}" }
            val response = roadsApi.snapToRoads(
                path = path,
                interpolate = true,
                apiKey = NetworkConfig.GOOGLE_PLACES_API_KEY
            )

            val latency = System.currentTimeMillis() - startTime
            snapCallCount++
            snapLatencySum += latency

            val snappedPoints = response.snappedPoints?.map { it.location.toLatLng() }
            if (snappedPoints != null && snappedPoints.isNotEmpty()) {
                snapSuccessCount++
                snappedPoints
            } else {
                snapFailureCount++
                points // Fallback to original points
            }
        } catch (e: Exception) {
            snapFailureCount++
            Timber.tag("RoadsSnappingService").e(e, "Failed to snap points to roads")
            points // Fallback to original points
        }
    }

    /**
     * Get instrumentation metrics
     */
    fun getMetrics(): Map<String, Any> {
        val avgLatency = if (snapCallCount > 0) {
            snapLatencySum / snapCallCount
        } else {
            0L
        }
        val successRate = if (snapCallCount > 0) {
            (snapSuccessCount.toDouble() / snapCallCount) * 100.0
        } else {
            0.0
        }
        return mapOf(
            "snapSuccessCount" to snapSuccessCount,
            "snapFailureCount" to snapFailureCount,
            "snapCallCount" to snapCallCount,
            "snapAvgLatencyMs" to avgLatency,
            "snapSuccessRate" to successRate
        )
    }
}
