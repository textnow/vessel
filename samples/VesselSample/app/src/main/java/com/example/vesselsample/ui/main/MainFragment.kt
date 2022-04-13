package com.example.vesselsample.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.vesselsample.R
import com.example.vesselsample.UserViewModel
import com.example.vesselsample.bases.BaseFragment
import com.example.vesselsample.databinding.MainFragmentBinding
import com.example.vesselsample.model.Stats
import com.example.vesselsample.utils.Response
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.math.roundToInt


class MainFragment : BaseFragment() {
    // viewModel() is Koin's built-in way to inject ViewModels
    private val mainViewModel: MainViewModel by viewModel()
    private val userViewModel: UserViewModel by sharedViewModel()

    private var _binding: MainFragmentBinding? = null

    // Although !! isn't usually recommended, in this case
    // we want the app to crash if the fragment is attempting to access views
    // after killing the fragment
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeObservers()
        initializePieChart()
        binding.logoutButton.setOnClickListener {
            userViewModel.logout()
        }

        binding.friendsButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_friendsFragment)
        }

        binding.foodIcon.setOnClickListener {
            mainViewModel.updateStats {
                it.copy(eating = it.eating + 1)
            }
        }

        binding.restIcon.setOnClickListener {
            mainViewModel.updateStats {
                it.copy(sleeping = it.sleeping + 1)
            }
        }

        binding.runIcon.setOnClickListener {
            mainViewModel.updateStats {
                it.copy(running = it.running + 1)
            }
        }

        binding.bikeIcon.setOnClickListener {
            mainViewModel.updateStats {
                it.copy(biking = it.biking + 1)
            }
        }



    }

    private fun initializeObservers() {
        userViewModel.userInfo.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Response.Loading -> {
                    binding.shimmerViewContainer.visibility = View.VISIBLE
                    binding.shimmerViewContainer.startShimmer()
                }
                is Response.Success -> {
                    finishedLoading()
                    binding.message.text = getString(R.string.user_message_greeting, response.value.name)
                }
                is Response.Failure -> {
                    finishedLoading()
                    Toast.makeText(
                        context,
                        response.message ?: getString(R.string.get_user_failure_msg),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        mainViewModel.stats.observe(viewLifecycleOwner) {
            when (it) {
                is Response.Success -> updatePieChart(it.value)
                is Response.Failure -> Toast.makeText(context, "Could not find user data", Toast.LENGTH_LONG).show()
                else -> Timber.d("State: $it")
            }
        }
    }

    private fun initializePieChart() {
        binding.userPieChart.apply {
            setNoDataText(getString(R.string.loading))

            description.isEnabled = false
            holeRadius = 75f
            transparentCircleRadius = 75f
            setTransparentCircleColor(Color.TRANSPARENT);
            setHoleColor(Color.TRANSPARENT)
            setCenterTextSize(45f)
            setCenterTextColor(context.getColor(R.color.primary_text_color))
            setUsePercentValues(true)
            setDrawRoundedSlices(true)

            legend.isEnabled = false
        }
    }


    /**
     * Update the pie chart to reflect the latest [Stats]
     */
    private fun updatePieChart(stats: Stats) {
        val pieChartData = PieDataSet(listOf(
            PieEntry(stats.eating),
            PieEntry(stats.sleeping),
            PieEntry(stats.running),
            PieEntry(stats.biking),
        ), "Exercise")

        val colorsList = coloursIDs.mapNotNull { context?.getColor(it) }
        pieChartData.apply {
            colors = colorsList
            valueFormatter = PercentFormatter()
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }

        binding.userPieChart.apply {
            data = PieData(pieChartData)
            notifyDataSetChanged()
            centerText = "${stats.getCumulativeScore().roundToInt()}%"

            // only animate the chart if this is the first load
            if (mainViewModel.isFirstLoad) {
                animateY(500)
                mainViewModel.isFirstLoad = false
            } else {
                invalidate()
            }
        }
    }

    private fun finishedLoading() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val coloursIDs = listOf(R.color.md_red_300, R.color.md_deep_purple_300, R.color.md_blue_300, R.color.md_green_300)
    }

}