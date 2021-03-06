package com.example.letschat.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.letschat.R
import com.example.letschat.databinding.ActivityOTPBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_o_t_p.*
import java.util.concurrent.TimeUnit

class OTPActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOTPBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dialog: ProgressDialog
    lateinit var verifyId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOTPBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        dialog = ProgressDialog(this)
        dialog.setTitle("Sending OTP...")
        dialog.setCancelable(false)
        dialog.show()
        auth = FirebaseAuth.getInstance()
        val phoneNumber = intent.getStringExtra("PhoneNumber")
        binding.textViewEnterOtp.text = "Enter the OTP to verify \n$phoneNumber"
        val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber!!)
                .setTimeout(60L,TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object: PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) {

                    }

                    override fun onVerificationFailed(p0: FirebaseException) {

                    }

                    override fun onCodeSent(verificationId: String, p1: PhoneAuthProvider.ForceResendingToken) {
                        super.onCodeSent(verificationId, p1)
                        dialog.dismiss()
                        verifyId = verificationId
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0)
                        binding.otpView.requestFocus()
                    }
                }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        binding.otpView.setOtpCompletionListener { s ->
            saveButton.setBackgroundColor(getColor(R.color.gray))
            val credential = PhoneAuthProvider.getCredential(verifyId,s)
            auth.signInWithCredential(credential).addOnCompleteListener {
                if(it.isSuccessful){
                    Toast.makeText(this,"Verified Successfully",Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SetupProfileActivity::class.java))
                    finishAffinity()
                }else{
                    Toast.makeText(this,"Error Occurred: ${it.exception?.localizedMessage}",Toast.LENGTH_SHORT).show()
                }
            }
            saveButton.setBackgroundColor(getColor(R.color.green))
        }
    }
}