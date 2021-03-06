package com.example.letschat.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.letschat.R
import com.example.letschat.databinding.DeleteDialogBinding
import com.example.letschat.models.Message
import com.example.letschat.models.User
import com.github.pgreze.reactions.ReactionPopup
import com.github.pgreze.reactions.ReactionsConfigBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.item_receive_group.view.*
import kotlinx.android.synthetic.main.item_sent_group.view.*

class GroupMessageAdapter(private val context: Context, private val messages: ArrayList<Message>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val item_sent = 1
    private val item_receive = 2

    inner class SendViewHolder(itemView: View):RecyclerView.ViewHolder(itemView)

    inner class ReceiverViewHolder(itemView: View):RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType==item_sent){
            SendViewHolder(LayoutInflater.from(context).inflate(R.layout.item_sent_group, parent, false))
        }else{
            ReceiverViewHolder(LayoutInflater.from(context).inflate(R.layout.item_receive_group, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if(FirebaseAuth.getInstance().uid==message.senderID){
            item_sent
        }else{
            item_receive
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val reactions = intArrayOf(
                R.drawable.ic_fb_like,
                R.drawable.ic_fb_love,
                R.drawable.ic_fb_laugh,
                R.drawable.ic_fb_wow,
                R.drawable.ic_fb_sad,
                R.drawable.ic_fb_angry
        )

        val config = ReactionsConfigBuilder(context)
                .withReactions(reactions)
                .build()

        val popup = ReactionPopup(context, config){ index -> true.also {
            if(holder.javaClass == SendViewHolder::class.java && index>=0){
                val viewHolder = holder as SendViewHolder
                viewHolder.itemView.reaction_sent_group.setImageResource(reactions[index])
                viewHolder.itemView.reaction_sent_group.visibility = View.VISIBLE
            }else if(index>=0){
                val viewHolder = holder as ReceiverViewHolder
                viewHolder.itemView.reaction_receive_group.setImageResource(reactions[index])
                viewHolder.itemView.reaction_receive_group.visibility = View.VISIBLE
            }
            message.feeling = index
            val map = HashMap<String,Any>()
            if(message.message.equals("This message is removed.")){
                map["feeling"] = -1
            }else{
                map["feeling"]=index
            }
            FirebaseDatabase.getInstance().reference
                    .child("Public")
                    .child("${message.senderID}${message.timeStamp}")
                    .updateChildren(map)
            }
        }
        if(holder.javaClass == SendViewHolder::class.java){
            val viewHolder = holder as SendViewHolder
            if(message.message.equals("Photo")){
                viewHolder.itemView.apply {
                    image_sent_group.visibility = View.VISIBLE
                    send_message_group.visibility = View.GONE
                    Glide.with(context).load(message.imageUrl).placeholder(R.drawable.placeholder).into(image_sent_group)
                }
            }
            FirebaseDatabase.getInstance().reference.child("Users").child(message.senderID).addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val user = snapshot.getValue(User::class.java)!!
                                viewHolder.itemView.sender_name_group.text = "@${user.name}"
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }

                    })
            viewHolder.itemView.apply {
                send_message_group.text = message.message
                if(message.feeling >=0){
                    reaction_sent_group.setImageResource(reactions[message.feeling])
                    reaction_sent_group.visibility = View.VISIBLE
                }else{
                    reaction_sent_group.visibility = View.INVISIBLE
                }

                send_message_group.setOnTouchListener { v, event ->
                    popup.onTouch(v, event)
                }
                image_sent_group.setOnTouchListener { v, event ->
                    popup.onTouch(v, event)
                }
            }
            viewHolder.itemView.setOnLongClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null,false)
                val binding = DeleteDialogBinding.bind(view)
                val dialog = AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setView(binding.root)
                    .create()
                binding.everyone.setOnClickListener {
                    message.message = "This message is removed."
                    message.feeling = -1
                    val map = HashMap<String,Any>()
                    map["message"]="This message is removed."
                    map["feeling"]=-1
                    FirebaseDatabase.getInstance().reference
                        .child("Public")
                        .child("${message.senderID}${message.timeStamp}").updateChildren(map)
                    dialog.dismiss()
                }
                binding.delete.setOnClickListener {
                    FirebaseDatabase.getInstance().reference
                        .child("Public")
                        .child("${message.senderID}${message.timeStamp}").setValue(null)
                    dialog.dismiss()
                }
                binding.cancel.setOnClickListener { dialog.dismiss() }
                dialog.show()
                false
            }
        }else{
            val viewHolder = holder as ReceiverViewHolder
            if(message.message.equals("Photo")){
                viewHolder.itemView.apply {
                    image_receive_group.visibility = View.VISIBLE
                    receive_message_group.visibility = View.GONE
                    Glide.with(context).load(message.imageUrl).placeholder(R.drawable.placeholder).into(image_receive_group)
                }
            }
            FirebaseDatabase.getInstance().reference.child("Users").child(message.senderID).addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val user = snapshot.getValue(User::class.java)!!
                                viewHolder.itemView.receiver_name_group.text = "@${user.name}"
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }

                    })
            viewHolder.itemView.apply {
                receive_message_group.text = message.message
                if(message.feeling >=0){
                    reaction_receive_group.setImageResource(reactions[message.feeling])
                    reaction_receive_group.visibility = View.VISIBLE
                }else{
                    reaction_receive_group.visibility = View.INVISIBLE
                }

                receive_message_group.setOnTouchListener { v, event ->
                    popup.onTouch(v, event)
                }
                image_receive_group.setOnTouchListener { v, event ->
                    popup.onTouch(v, event)
                }
            }
            viewHolder.itemView.setOnLongClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null,false)
                val binding = DeleteDialogBinding.bind(view)
                val dialog = AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setView(binding.root)
                    .create()
                binding.everyone.setOnClickListener {
                    message.message = "This message is removed."
                    message.feeling = -1
                    val map = HashMap<String,Any>()
                    map["message"]="This message is removed."
                    map["feeling"]=-1
                    FirebaseDatabase.getInstance().reference
                        .child("Public")
                        .child("${message.senderID}${message.timeStamp}").updateChildren(map)
                    dialog.dismiss()
                }
                binding.delete.setOnClickListener {
                    FirebaseDatabase.getInstance().reference
                        .child("Public")
                        .child("${message.senderID}${message.timeStamp}").setValue(null)
                    dialog.dismiss()
                }
                binding.cancel.setOnClickListener { dialog.dismiss() }
                dialog.show()
                false
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }
}