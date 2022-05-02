package com.example.vesselsample.model

data class Stats (
    val running: Float = 1f,
    val sleeping: Float = 1f,
    val eating: Float = 1f,
    val biking: Float = 1f
) {
    fun getCumulativeScore() : Float {
        return (-eating * 5f + running * 5f + sleeping * 1f + biking * 10f).coerceIn(0f..100f)
    }
}