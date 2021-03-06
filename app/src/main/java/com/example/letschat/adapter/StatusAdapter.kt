package com.example.letschat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.letschat.R
import com.example.letschat.activity.MainActivity
import com.example.letschat.models.UserStatus
import kotlinx.android.synthetic.main.item_status.view.*
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory


class StatusAdapter(private val context: Context, private val userStatus: ArrayList<UserStatus>) : RecyclerView.Adapter<StatusAdapter.StatusViewHolder>() {

    inner class StatusViewHolder(itemView: View):RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        return StatusViewHolder(LayoutInflater.from(context).inflate(R.layout.item_status, parent, false))
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        val userStatus = userStatus[position]
        val lastStatus = userStatus.statuses?.last()
        if (lastStatus != null) {
            Glide.with(context).load(lastStatus.imageUrl).into(holder.itemView.status_view_image)
        }
        userStatus.statuses?.let { holder.itemView.circular_status_view.setPortionsCount(it.size) }
        holder.itemView.circular_status_view.setOnClickListener {
            val myStories = ArrayList<MyStory>()
            for(status in userStatus.statuses!!){
                myStories.add(MyStory(status.imageUrl))
            }
            StoryView.Builder((context as MainActivity).supportFragmentManager)
                    .setStoriesList(myStories)
                    .setStoryDuration(5000)
                    .setTitleText(userStatus.name)
                    .setTitleLogoUrl(userStatus.profileImage)
                    .setStoryClickListeners(object : StoryClickListeners {
                        override fun onDescriptionClickListener(position: Int) {

                        }

                        override fun onTitleIconClickListener(position: Int) {

                        }
                    })
                    .build()
                    .show()
        }
    }

    override fun getItemCount(): Int {
        return userStatus.size
    }
}