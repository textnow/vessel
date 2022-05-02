package com.example.vesselsample.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.vesselsample.R
import com.example.vesselsample.utils.InputValidation

class LoginViewModel : ViewModel() {

    // use a backing property() to hide the mutable property
    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState>
        get() = _loginForm

    /**
     * Called when the login input has changed. Checks whether
     * the data is valid and updates the login state accordingly
     */
    fun loginDataChanged(id: String) {
        if (!InputValidation.validateID(id)) {
            _loginForm.value = LoginFormState(idError = R.string.invalid_id)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }
}