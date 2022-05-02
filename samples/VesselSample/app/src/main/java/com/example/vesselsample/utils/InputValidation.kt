package com.example.vesselsample.utils

object InputValidation {
    fun validateID(idString: String): Boolean {
        val id = try {
            idString.toInt()
        } catch (e: NumberFormatException) {
            return false
        }

        return id in 1..10
    }
}