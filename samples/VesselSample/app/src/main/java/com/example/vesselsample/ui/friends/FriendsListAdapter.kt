package com.example.vesselsample.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.vesselsample.R
import com.example.vesselsample.databinding.ContactRowBinding
import com.example.vesselsample.model.User

class FriendsListAdapter(private val friends: List<User>, val onClickFriend: (User) -> Unit) :
    RecyclerView.Adapter<FriendsListAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ContactRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ContactRowBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = friends[position]

        holder.binding.apply {
            name.text = friend.name
            email.text = friend.email
            phone.text = friend.phone
        }

        holder.binding.root.setOnClickListener {
            onClickFriend(friend)
        }

        val selectedColour = colours[friend.id.hashCode() % colours.size]
        Glide.with(holder.binding.root)
            .load("https://avatars.dicebear.com/api/personas/${friend.id}.png?b=${selectedColour}")
            .placeholder(R.color.md_grey_300)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(holder.binding.circleImageView)
    }

    override fun getItemCount(): Int {
        return friends.size
    }

    companion object {
        val colours =
            listOf("blue", "yellow", "purple", "turquoise", "grey", "green", "pink")
    }
}