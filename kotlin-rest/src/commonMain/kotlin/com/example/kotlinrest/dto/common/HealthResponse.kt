package com.example.kotlinrest.dto.common

import kotlinx.serialization.Serializable
import onl.ycode.stormify.coroutines.PoolStats

/**
 * `/api/health` payload. Beyond a liveness flag it reports the connection pool's counters,
 * which is the one operational detail this native server can expose that a blocking build
 * cannot — the pool only exists on the suspend path.
 */
@Serializable
data class HealthResponse(
    val status: String,
    val pool: PoolStatsResponse,
)

@Serializable
data class PoolStatsResponse(
    val total: Int,
    val inUse: Int,
    val idle: Int,
    val acquireCount: Long,
    val waitedAcquireCount: Long,
    val evictedCount: Long,
    val retiredCount: Long,
)

fun PoolStats.toResponse() = PoolStatsResponse(
    total = total,
    inUse = inUse,
    idle = idle,
    acquireCount = acquireCount,
    waitedAcquireCount = waitedAcquireCount,
    evictedCount = evictedCount,
    retiredCount = retiredCount,
)
