package com.example.letschat.activity

import android.app.ProgressDialog
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.letschat.R
import com.example.letschat.models.User
import com.example.letschat.databinding.ActivitySetupProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_setup_profile.*

class SetupProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupProfileBinding
    private lateinit var auth:FirebaseAuth
    private lateinit var database:FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var selectedImage: Uri?=null
    private lateinit var dialog:ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dialog = ProgressDialog(this)
        dialog.setTitle("Please wait while we save your data...")
        dialog.setCancelable(false)
        supportActionBar?.hide()
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        binding.profileImage.setOnClickListener {
            val intent = Intent()
            intent.action = ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent,45)
        }
        binding.saveButton.setOnClickListener {
            saveButton.setBackgroundColor(getColor(R.color.gray))
            dialog.show()
            val name = binding.nameEditText.text.toString()
            if(name.isEmpty()){
                dialog.dismiss()
                binding.nameEditText.error = "Please Type a name!!"
                saveButton.setBackgroundColor(getColor(R.color.green))
                return@setOnClickListener
            }
            if(selectedImage != null){
                val reference = storage.reference.child("Profiles").child(auth.uid!!)
                reference.putFile(selectedImage!!)
                    .addOnCompleteListener { task ->
                        if(task.isSuccessful){
                            reference.downloadUrl.addOnCompleteListener {task2 ->
                                if(task2.isSuccessful){
                                    val imageUrl = it.toString()
                                    val user = User(auth.uid,binding.nameEditText.text.toString(),auth.currentUser!!.phoneNumber,imageUrl)
                                    database.reference.child("Users").child(auth.uid!!).setValue(user)
                                        .addOnCompleteListener {
                                            dialog.dismiss()
                                            if(it.isSuccessful){
                                                startActivity(Intent(this, MainActivity::class.java))
                                                saveButton.setBackgroundColor(getColor(R.color.green))
                                                finish()
                                            }else{
                                                saveButton.setBackgroundColor(getColor(R.color.green))
                                                Toast.makeText(this,"Error Occurred: ${it.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }else{
                                    dialog.dismiss()
                                    saveButton.setBackgroundColor(getColor(R.color.green))
                                    Toast.makeText(this,"Error Occurred: ${task.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                                }
                            }
                        }else{
                            dialog.dismiss()
                            saveButton.setBackgroundColor(getColor(R.color.green))
                            Toast.makeText(this,"Error Occurred: ${task.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                        }
                    }
            }else{
                val user = User(auth.uid,binding.nameEditText.text.toString(),auth.currentUser!!.phoneNumber,"No Image")
                database.reference.child("Users").child(auth.uid!!).setValue(user)
                    .addOnCompleteListener {
                        dialog.dismiss()
                        if(it.isSuccessful){
                            startActivity(Intent(this, MainActivity::class.java))
                            saveButton.setBackgroundColor(getColor(R.color.green))
                            finish()
                        }else{
                            saveButton.setBackgroundColor(getColor(R.color.green))
                            Toast.makeText(this,"Error Occurred: ${it.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data!=null){
            if(data.data!=null){
                binding.profileImage.setImageURI(data.data)
                selectedImage = data.data
            }
        }
    }
}