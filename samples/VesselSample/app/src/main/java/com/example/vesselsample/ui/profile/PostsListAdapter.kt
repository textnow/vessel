package com.example.vesselsample.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.vesselsample.databinding.PostRowBinding
import com.example.vesselsample.model.Post

class PostsListAdapter (private val posts: List<Post>) : RecyclerView.Adapter<PostsListAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: PostRowBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostRowBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]

        holder.binding.apply {
            title.text = post.title
            description.text = post.body
        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }
}