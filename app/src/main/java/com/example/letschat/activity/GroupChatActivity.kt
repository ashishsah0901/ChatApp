package com.example.letschat.activity

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letschat.adapter.GroupMessageAdapter
import com.example.letschat.databinding.ActivityGroupChatBinding
import com.example.letschat.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_group_chat.*
import java.util.*

class GroupChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupChatBinding
    private lateinit var adapter: GroupMessageAdapter
    private lateinit var messages: ArrayList<Message>
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var dialog: ProgressDialog
    private var senderUid:String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Group Chat"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        senderUid = FirebaseAuth.getInstance().uid
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        dialog = ProgressDialog(this)
        dialog.setTitle("Please Wait...")
        dialog.setCancelable(false)

        messages = arrayListOf()

        adapter = GroupMessageAdapter(this,messages)
        binding.recyclerViewChatGroup.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChatGroup.adapter = adapter

        database.reference.child("Public")
            .addValueEventListener(object: ValueEventListener {
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

        binding.sendBtnGroup.setOnClickListener {
            val messageTxt = binding.messageBoxGroup.text.toString()
            val date = Date()
            val message = Message(messageTxt, senderUid!! ,date.time)
            binding.messageBoxGroup.setText("")
            database.reference.child("Public").child("${message.senderID}${message.timeStamp}").setValue(message)
        }

        binding.attachmentGroup.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent,20)
        }
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
                            reference.downloadUrl.addOnCompleteListener {
                                if(it.isSuccessful){
                                    val filePath = it.toString()
                                    val date = Date()
                                    val message = Message("Photo",senderUid!!,date.time,filePath)
                                    binding.messageBoxGroup.setText("")
                                    database.reference.child("Public")
                                            .child("${message.senderID}${message.timeStamp}")
                                            .setValue(message)
                                }else{
                                    Toast.makeText(this,"Error Occurred: ${it.exception?.localizedMessage}",
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                        }else{
                            Toast.makeText(this,"Error Occurred: ${task.exception?.localizedMessage}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }else{
                    Toast.makeText(this,"Please Select a Image!!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}