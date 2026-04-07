package com.example.forkit.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Kotlin coroutine bridge so Java code can read Health Connect data.
 */
object HealthConnectBridge {

    data class Metrics(
        val stepsToday: Int,
        val activeCaloriesToday: Int,
        val exerciseMinutesToday: Int
    )

    interface Callback {
        fun onSuccess(m: Metrics)
        fun onError(message: String)
    }

    @JvmStatic
    fun permissions(): Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    @JvmStatic
    fun readTodayMetrics(ctx: Context, cb: Callback) {
        val client = HealthConnectClient.getOrCreate(ctx)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = Instant.now()
                val start = now.truncatedTo(ChronoUnit.DAYS)

                val steps = client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, now)
                    )
                ).records.sumOf { it.count.toLong() }.toInt()

                val activeCalories = client.readRecords(
                    ReadRecordsRequest(
                        recordType = ActiveCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, now)
                    )
                ).records.sumOf { it.energy.inKilocalories }.toInt()

                val exerciseMinutes = client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, now)
                    )
                ).records.sumOf {
                    java.time.Duration.between(it.startTime, it.endTime).toMinutes()
                }.toInt()

                val m = Metrics(
                    stepsToday = steps.coerceAtLeast(0),
                    activeCaloriesToday = activeCalories.coerceAtLeast(0),
                    exerciseMinutesToday = exerciseMinutes.coerceAtLeast(0)
                )
                withContext(Dispatchers.Main) { cb.onSuccess(m) }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) { cb.onError(t.message ?: "Health Connect read failed") }
            }
        }
    }
}

