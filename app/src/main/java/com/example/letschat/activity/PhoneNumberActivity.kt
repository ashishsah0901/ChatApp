package com.example.letschat.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.letschat.R
import com.example.letschat.databinding.ActivityPhoneNumberBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_phone_number.*

class PhoneNumberActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhoneNumberBinding
    private lateinit var auth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        auth = FirebaseAuth.getInstance()
        if(auth.currentUser != null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
        binding.phoneNumberEditText.requestFocus()
        continueButton.setOnClickListener {
            continueButton.setBackgroundColor(getColor(R.color.gray))
            val intent = Intent(this, OTPActivity::class.java)
            val number = phoneNumberEditText.text
            intent.putExtra("PhoneNumber",number.toString())
            startActivity(intent)
            continueButton.setBackgroundColor(getColor(R.color.green))
        }
    }
}