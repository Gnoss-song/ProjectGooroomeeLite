package kr.co.gooroomeelite.views.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kr.co.gooroomeelite.R
import kr.co.gooroomeelite.databinding.ActivityLoginBinding
import kr.co.gooroomeelite.views.common.MainActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    var auth: FirebaseAuth? = null
    var googleSignInClient: GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 100
    var firestore: FirebaseFirestore? = null
    var storage: FirebaseStorage? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Firebase Database
        firestore = FirebaseFirestore.getInstance()
        // Firebase Auth
        auth = FirebaseAuth.getInstance()
        // Firebase Storage
        storage = FirebaseStorage.getInstance()



        binding.tvPreview.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.startEmail.setOnClickListener {
            //이메일로 시작
            startActivity(Intent(this, LoginEmailActivity::class.java))
            Toast.makeText(this, "이메일로 시작", Toast.LENGTH_SHORT).show()
        }
        binding.startGoogle.setOnClickListener {
            //구글로 시작
            googleLogin()
            Toast.makeText(this, "구글로 시작", Toast.LENGTH_SHORT).show()
        }
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    fun googleLogin() {
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_LOGIN_CODE) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result!!.isSuccess) {
                var account = result.signInAccount
                //Second step
                firebaseAuthWithGoogle(account)
            }
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                //Login

                val email = account?.email
                val check = firestore!!.collection("users")

                check.whereEqualTo("userId", email).get().addOnSuccessListener {
                    //신규유저
                    if (it.isEmpty) {
                        val intent = Intent(this, LoginNicknameActivity::class.java)
                        val bundle = Bundle()
                        bundle.putString("email",account?.email)
                        intent.putExtra("bundle",bundle)
                        startActivity(intent)
                        finish()
                    }
                    //이미 있는 이메일일경우
                    else {
                        val intent1 = Intent(this, MainActivity::class.java)
                        startActivity(intent1)
                    }
                }
            } else {
                //Show the error message
                Toast.makeText(this, "TEST#", Toast.LENGTH_LONG).show()
            }
        }
    }
}

