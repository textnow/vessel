package com.example.vesselsample.ui.friends

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.vesselsample.databinding.FriendsFragmentBinding
import com.example.vesselsample.utils.Response
import org.koin.android.ext.android.inject

class FriendsFragment : Fragment() {

    private lateinit var binding: FriendsFragmentBinding
    private val friendsViewModel: FriendsViewModel by inject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FriendsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFriendsView()
    }

    private fun initializeFriendsView() {
        friendsViewModel.friends.observe(viewLifecycleOwner) {
            TransitionManager.beginDelayedTransition(binding.root)
            if (it is Response.Success) {
                binding.shimmerViewFriends.stopShimmer()
                binding.shimmerViewFriends.visibility = View.GONE
                binding.friendsList.adapter = FriendsListAdapter(it.value) { friend ->
                    val action =
                        FriendsFragmentDirections.actionFriendsFragmentToFriendDetailFragment(friend.id)
                    findNavController().navigate(action)
                }
            }
            if (it is Response.Failure) {
                binding.shimmerViewFriends.stopShimmer()
                binding.shimmerViewFriends.visibility = View.VISIBLE
                binding.friendsList.visibility = View.GONE
            }
        }
    }

}