package kr.co.gooroomeelite.views.login

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kr.co.gooroomeelite.R
import kr.co.gooroomeelite.databinding.ActivityLoginnicknameBinding
import kr.co.gooroomeelite.model.ContentDTO
import kr.co.gooroomeelite.utils.LoginUtils.Companion.getUid
import kr.co.gooroomeelite.views.common.MainActivity

class LoginNicknameActivity : AppCompatActivity() {
    private lateinit var binding : ActivityLoginnicknameBinding
    var bundle : Bundle? = null
    var email : String? = null
    var password : String? = null
    var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null
    var storage: FirebaseStorage? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginnicknameBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        bundle = intent.getBundleExtra("bundle")
        email = bundle?.getString("email")
        password= bundle?.getString("password")

        setContentView(binding.root)


        //백버튼 활성화
        binding.icBack.setOnClickListener {
            onBackPressed()
        }

        binding.editTextNickname.setOnFocusChangeListener { v, hasFocus ->
            when (hasFocus) {
                true -> changecolor()
                false -> changecolor()
            }
        }

        changecolor()

        binding.editTextNickname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.editTextNickname.setBackgroundResource(R.drawable.btn_skyblue)
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.btnLoginNext.setOnClickListener {
            if (binding.editTextNickname.text.toString().length>6) {
                binding.tvError.text = "닉네임은 최대 6글자입니다."
                binding.editTextNickname.setBackgroundResource(R.drawable.btn_red)
            } else {
                binding.tvError.text = ""
                binding.editTextNickname.setBackgroundResource(R.drawable.btn_white)
                signinAndSignup()
            }
        }
    }
    fun signinAndSignup() {
        auth?.createUserWithEmailAndPassword(
            email.toString(),
            password.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                //Creating a user account
                contentUpload()
                moveMainPage(task.result?.user)
            }

            else if (task.exception?.message.isNullOrEmpty()) {
            } else {
            }
        }
    }
    private fun contentUpload() {
        val contentDTO = ContentDTO()
        contentDTO.userId = auth?.currentUser?.email
        contentDTO.nickname = binding.editTextNickname.text.toString()
        contentDTO.studyTime = 0
        firestore?.collection("users")?.document(getUid()!!)?.set(contentDTO)
        setResult(Activity.RESULT_OK)
    }

    fun moveMainPage(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    private fun changecolor() {
        binding.editTextNickname.setOnFocusChangeListener { v, hasFocus ->
            when (hasFocus) {
                true -> v.setBackgroundResource(R.drawable.btn_skyblue)
                false -> v.setBackgroundResource(R.drawable.btn_white)
            }
        }
    }
}