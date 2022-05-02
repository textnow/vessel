package com.example.vesselsample.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.vesselsample.MainActivity
import com.example.vesselsample.R
import com.example.vesselsample.UserViewModel
import com.example.vesselsample.databinding.LoginActivityBinding
import com.example.vesselsample.utils.Response
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginActivity : AppCompatActivity() {

    private val authViewModel: LoginViewModel by viewModel()
    private val userViewModel: UserViewModel by viewModel()
    private lateinit var binding: LoginActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginActivityBinding.inflate(layoutInflater)

        setContentView(binding.root)

        authViewModel.loginFormState.observe(this) {
            val loginState = it ?: return@observe

            // disable login button unless both username / password is valid
            binding.loginButton.isEnabled = loginState.isDataValid

            if (loginState.idError != null) {
                binding.inputUserId.error = getString(loginState.idError)
            }
        }

        binding.inputUserId.afterTextChanged {
            authViewModel.loginDataChanged(
                binding.inputUserId.text.toString()
            )
        }

        binding.loginButton.setOnClickListener {
            TransitionManager.beginDelayedTransition(binding.loginForm)
            binding.progress.visibility = View.VISIBLE
            binding.loginButton.visibility = View.GONE
            userViewModel.login(binding.inputUserId.text.toString()).observe(this, {
                if (it is Response.Loading) return@observe
                if (it is Response.Failure) {
                    binding.progress.visibility = View.GONE
                    binding.loginButton.visibility = View.GONE
                    showLoginFailed(R.string.login_unknown_error_msg)
                    return@observe
                }
                navigateToMainActivity()
            })
        }
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        setResult(Activity.RESULT_OK)
        finish()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}