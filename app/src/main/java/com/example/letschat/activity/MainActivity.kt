package com.example.letschat.activity

import android.app.ProgressDialog
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.letschat.R
import com.example.letschat.adapter.StatusAdapter
import com.example.letschat.adapter.UsersAdapter
import com.example.letschat.databinding.ActivityMainBinding
import com.example.letschat.models.Status
import com.example.letschat.models.User
import com.example.letschat.models.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var users:ArrayList<User>
    private lateinit var usersAdapter: UsersAdapter
    private lateinit var statusAdapter: StatusAdapter
    private lateinit var usersStatuses: ArrayList<UserStatus>
    private lateinit var dialog:ProgressDialog
    private lateinit var user:User
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialog = ProgressDialog(this)
        dialog.setTitle("Uploading Status....")
        dialog.setCancelable(false)

        database = FirebaseDatabase.getInstance()
        users = arrayListOf()
        usersStatuses = arrayListOf()

        FirebaseAuth.getInstance().uid?.let {
            database.reference.child("Users")
                .child(it)
                .addValueEventListener(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        user = snapshot.getValue(User::class.java)!!
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
        }
        usersAdapter = UsersAdapter(this,users)
        statusAdapter = StatusAdapter(this,usersStatuses)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.HORIZONTAL
        binding.statusRecyclerView.layoutManager = layoutManager
        binding.recentChatRecyclerView.adapter = usersAdapter
        binding.statusRecyclerView.adapter = statusAdapter
        binding.recentChatRecyclerView.showShimmerAdapter()
        binding.statusRecyclerView.showShimmerAdapter()
        database.reference.child("Users").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for(snapshot1 in snapshot.children){
                    val user = snapshot1.getValue(User::class.java)
                    if (user?.uid != null && (user.uid) != FirebaseAuth.getInstance().uid) {
                        users.add(user)
                    }
                }
                binding.recentChatRecyclerView.hideShimmerAdapter()
                binding.statusRecyclerView.hideShimmerAdapter()
                usersAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
        database.reference.child("stories").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    usersStatuses.clear()
                    for(snapshot2 in snapshot.children){
                        var status:UserStatus? = null
                        if(snapshot2.child("lastUpdated").value != null) {
                            status = UserStatus(snapshot2.child("name").value as String, snapshot2.child("profileImage").value as String, snapshot2.child("lastUpdated").value as Long)
                        }
                        val statuses = ArrayList<Status>()
                        for(snapshot1 in snapshot2.child("statuses").children){
                            val sampleStory = snapshot1.getValue(Status::class.java)
                            if (sampleStory != null) {
                                statuses.add(sampleStory)
                            }
                        }
                        status?.statuses=statuses
                        if (status != null) {
                            usersStatuses.add(status)
                        }
                    }
                    statusAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
        binding.bottomNavView.setOnNavigationItemSelectedListener {
            if (it.itemId == R.id.status_menu) {
                val intent = Intent()
                intent.setType("image/*").action = ACTION_GET_CONTENT
                startActivityForResult(intent, 75)
            }
            if(it.itemId == R.id.call_menu){
                Toast.makeText(this,"Call is clicked",Toast.LENGTH_SHORT).show()
            }
            return@setOnNavigationItemSelectedListener false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data!=null){
            if(data.data!=null){
                dialog.show()
                val storage = FirebaseStorage.getInstance()
                val date = Date()
                val reference = storage.reference.child("status").child(date.time.toString())
                reference.putFile(data.data!!)
                        .addOnCompleteListener { task ->
                            if(task.isSuccessful){
                                reference.downloadUrl.addOnCompleteListener {
                                    if(it.isSuccessful){
                                        val userStatus = user.profileImg?.let { it1 -> user.name?.let { it2 -> UserStatus(it2, it1,date.time) } }
                                        val obj = HashMap<String,Any>()
                                        var status: Status?=null
                                        if (userStatus != null) {
                                            obj["name"] = userStatus.name
                                            obj["profileImage"] = userStatus.profileImage
                                            obj["lastUpdate"] = userStatus.lastUpdated
                                            status = Status(it.toString(), userStatus.lastUpdated)
                                        }
                                        database.reference.child("stories")
                                                .child(FirebaseAuth.getInstance().uid.toString())
                                                .updateChildren(obj)
                                        database.reference.child("stories")
                                                .child(FirebaseAuth.getInstance().uid.toString())
                                                .child("statuses")
                                                .push()
                                                .setValue(status)
                                        dialog.dismiss()
                                    }else{
                                        Toast.makeText(this,"Error occurred: ${it.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }else{
                                Toast.makeText(this,"Error occurred: ${task.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                            }
                        }
            }else{
                Toast.makeText(this,"Please Select A Image",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        database.reference.child("Presence").child(FirebaseAuth.getInstance().uid.toString()).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        val calender = Calendar.getInstance()
        database.reference.child("Presence").child(FirebaseAuth.getInstance().uid.toString()).setValue("${calender.timeInMillis}")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.groups_menu -> startActivity(Intent(this,GroupChatActivity::class.java))
            R.id.search_menu -> Toast.makeText(this,"Search Clicked",Toast.LENGTH_SHORT).show()
            R.id.settings_menu -> Toast.makeText(this,"Settings Clicked",Toast.LENGTH_SHORT).show()
            R.id.invite_menu -> Toast.makeText(this,"Invite Clicked",Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }
}