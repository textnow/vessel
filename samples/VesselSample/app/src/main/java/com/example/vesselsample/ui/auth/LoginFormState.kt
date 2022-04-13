package com.example.vesselsample.ui.auth

/**
 * Data validation state of the login form.
 */
data class LoginFormState(
    val idError: Int? = null,
    val isDataValid: Boolean = false
)