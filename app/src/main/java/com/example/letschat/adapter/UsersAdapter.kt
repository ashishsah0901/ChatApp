package com.example.letschat.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.letschat.activity.ChatActivity
import com.example.letschat.R
import com.example.letschat.models.User
import com.example.letschat.utils.UtilClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.row_conversation.view.*

class UsersAdapter(private val context: Context, private val users: ArrayList<User>):RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView:View):RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(LayoutInflater.from(context).inflate(R.layout.row_conversation,parent,false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val senderId = FirebaseAuth.getInstance().uid
        val senderRoom = senderId + user.uid
        FirebaseDatabase.getInstance().reference
                .child("Chats")
                .child(senderRoom)
                .addValueEventListener(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()){
                            val lastMessage = snapshot.child("lastMessage").getValue(String::class.java)
                            val time = snapshot.child("lastMessageTime").value as Long
                            holder.itemView.user_recent_chat.text = lastMessage
                            holder.itemView.user_recent_time.text = UtilClass.getFormattedDate(time)
                        }else{
                            holder.itemView.user_recent_time.text = "Today"
                            holder.itemView.user_recent_chat.text = "Tap to chat"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
        holder.itemView.apply {
            user_name.text = user.name
            Glide.with(context).load(user.profileImg).placeholder(R.drawable.person).into(user_profile_image)
            setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("name",user.name)
                intent.putExtra("uid",user.uid)
                intent.putExtra("profileImage",user.profileImg)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }
}