package com.example.vesselsample

import android.content.Intent
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.vesselsample.bases.BaseActivity
import com.example.vesselsample.databinding.MainActivityBinding
import com.example.vesselsample.ui.auth.LoginActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : BaseActivity() {
    private val userViewModel: UserViewModel by viewModel()
    private lateinit var binding: MainActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)

        userViewModel.isAuthenticated.observe(this, {
            if (it) {
                setContentView(binding.root)
                val navController = findNavController(R.id.nav_host_fragment)
                val appConfiguration = AppBarConfiguration(navController.graph)

                binding.toolbar.setupWithNavController(navController, appConfiguration)
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        })
    }
}