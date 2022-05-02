package com.example.vesselsample.ui.profile

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.vesselsample.R
import com.example.vesselsample.databinding.ProfileFragmentBinding
import com.example.vesselsample.ui.friends.FriendsListAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.recyclerview.widget.DividerItemDecoration




class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModel()

    private lateinit var binding: ProfileFragmentBinding

    private val args: ProfileFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        profileViewModel.setUserID(args.userId)
        binding = ProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileViewModel.userInfo.observe(viewLifecycleOwner) { user ->
            binding.email.text = user.email
            binding.name.text = user.name
            binding.phone.text = user.phone
            val selectedColour = FriendsListAdapter.colours[user.id.hashCode() % FriendsListAdapter.colours.size]

            // load image from mock API
            Glide.with(binding.root)
                .load("https://avatars.dicebear.com/api/personas/${user.id}.png?b=${selectedColour}")
                .placeholder(R.color.md_grey_300)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.circleImageView)
        }

        profileViewModel.userPosts.observe(viewLifecycleOwner) {
            TransitionManager.beginDelayedTransition(binding.root)
            binding.shimmerViewPosts.stopShimmer()
            binding.shimmerViewPosts.visibility = View.GONE

            val dividerItemDecoration = DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
            binding.posts.apply {
                adapter = PostsListAdapter(it)
                addItemDecoration(dividerItemDecoration)
            }
        }


    }

}