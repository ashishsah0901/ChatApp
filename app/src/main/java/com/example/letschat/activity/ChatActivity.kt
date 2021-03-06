package com.example.letschat.activity

import android.app.ProgressDialog
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.letschat.R
import com.example.letschat.adapter.MessageAdapter
import com.example.letschat.databinding.ActivityChatBinding
import com.example.letschat.models.Message
import com.example.letschat.utils.UtilClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_chat.*
import java.util.*
import kotlin.collections.HashMap

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var messages: ArrayList<Message>
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var senderRoom: String
    private lateinit var receiverRoom: String
    private lateinit var dialog:ProgressDialog
    private var senderUid:String?=null
    private var receiverUid:String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(toolbar)

        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        dialog = ProgressDialog(this)
        dialog.setTitle("Please Wait...")
        dialog.setCancelable(false)

        messages = arrayListOf()

        val name = intent.getStringExtra("name")
        val profileImage = intent.getStringExtra("profileImage")

        name_user.text = name
        Glide.with(this).load(profileImage).placeholder(R.drawable.person).into(profile)

        binding.backButton.setOnClickListener {
            finish()
        }

        receiverUid = intent.getStringExtra("uid")
        senderUid = FirebaseAuth.getInstance().uid

        database.reference.child("Presence").child(receiverUid!!).addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val status = snapshot.getValue(String::class.java)
                    if(status!=null && status.isNotEmpty()){
                        status_availability.visibility = View.VISIBLE
                        if(status == "Online" || status == "Typing..."){
                            status_availability.text =status
                        }else{
                            status_availability.text = "Last Seen At ${UtilClass.getFormattedDate(status.toLongOrNull()!!)}"
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        senderRoom = senderUid + receiverUid
        receiverRoom = receiverUid + senderUid

        adapter = MessageAdapter(this,messages,senderRoom,receiverRoom)
        recyclerView_chat.adapter = adapter
        recyclerView_chat.layoutManager = LinearLayoutManager(this)

        database.reference.child("Chats")
            .child(senderRoom)
            .child("messages")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages.clear()
                    for(dataSnapshot in snapshot.children){
                        val message = dataSnapshot.getValue(Message::class.java)
                        if (message != null) {
                            message.messageID = snapshot.key.toString()
                            messages.add(message)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        binding.sendBtn.setOnClickListener {
            val messageTxt = binding.messageBox.text.toString()
            val date = Date()
            val message = Message(messageTxt, senderUid!! ,date.time)
            binding.messageBox.setText("")
            val randomKey = "${message.senderID}${message.timeStamp}${message.message.hashCode()}"
            val lastMessageObject = HashMap<String,Any>()
            lastMessageObject["lastMessage"] = message.message!!
            lastMessageObject["lastMessageTime"] = date.time
            database.reference.child("Chats")
                .child(senderRoom)
                .updateChildren(lastMessageObject)
            database.reference.child("Chats")
                .child(receiverRoom)
                .updateChildren(lastMessageObject)
            database.reference.child("Chats")
                .child(senderRoom)
                .child("messages")
                .child(randomKey)
                .setValue(message)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        database.reference.child("Chats")
                            .child(receiverRoom)
                            .child("messages")
                            .child(randomKey)
                            .setValue(message)
                            .addOnCompleteListener {
                                if(it.isSuccessful){

                                }else{
                                    Toast.makeText(this,"Error Occurred: ${it.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                                }
                            }
                    }else{
                        Toast.makeText(this,"Error Occurred: ${task.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                    }
                }
        }
        binding.attachment.setOnClickListener {
            val intent = Intent()
            intent.action = ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent,20)
        }
        val handler = Handler()
        binding.messageBox.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                database.reference.child("Presence").child(senderUid!!).setValue("Typing...")
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping,1000)
            }

            val userStoppedTyping = Runnable {
                database.reference.child("Presence").child(senderUid!!).setValue("Online")
            }

        })

        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==20){
            if(data!=null){
                if(data.data!=null){
                    val uri = data.data
                    val time = Calendar.getInstance()
                    val reference = storage.reference.child("Chats").child(time.timeInMillis.toString())
                    dialog.show()
                    reference.putFile(uri!!).addOnCompleteListener { task ->
                        dialog.dismiss()
                        if(task.isSuccessful){
                            reference.downloadUrl.addOnCompleteListener { task1 ->
                                if(task1.isSuccessful){
                                    val filePath = task1.toString()
                                    val date = Date()
                                    val message = Message("Photo",senderUid!!,date.time,filePath)
                                    binding.messageBox.setText("")
                                    val randomKey = "${message.senderID}${message.timeStamp}${message.message.hashCode()}"
                                    val lastMessageObject = HashMap<String,Any>()
                                    lastMessageObject["lastMessage"] = message.message!!
                                    lastMessageObject["lastMessageTime"] = date.time
                                    database.reference.child("Chats")
                                            .child(senderRoom)
                                            .updateChildren(lastMessageObject)
                                    database.reference.child("Chats")
                                            .child(receiverRoom)
                                            .updateChildren(lastMessageObject)
                                    database.reference.child("Chats")
                                            .child(senderRoom)
                                            .child("messages")
                                            .child(randomKey)
                                            .setValue(message)
                                            .addOnCompleteListener { task ->
                                                if(task.isSuccessful){
                                                    database.reference.child("Chats")
                                                            .child(receiverRoom)
                                                            .child("messages")
                                                            .child(randomKey)
                                                            .setValue(message)
                                                            .addOnCompleteListener {
                                                                if(it.isSuccessful){
                                                                    messageBox.setText("")
                                                                }else{
                                                                    Toast.makeText(this,"Error Occurred: ${it.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                }else{
                                                    Toast.makeText(this,"Error Occurred: ${task.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                }else{
                                    Toast.makeText(this,"Error Occurred: ${task1.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                                }
                            }
                        }else{
                            dialog.dismiss()
                            Toast.makeText(this,"Error Occurred: ${task.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                        }
                    }
                }else{
                    Toast.makeText(this,"Please Select a Image!!",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val calender = Calendar.getInstance()
        database.reference.child("Presence").child(FirebaseAuth.getInstance().uid.toString()).setValue("${calender.timeInMillis}")
    }

    override fun onResume() {
        super.onResume()
        database.reference.child("Presence").child(FirebaseAuth.getInstance().uid.toString()).setValue("Online")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}