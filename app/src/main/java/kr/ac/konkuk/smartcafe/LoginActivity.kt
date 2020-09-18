package kr.ac.konkuk.smartcafe

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import kotlin.system.exitProcess
import com.google.firebase.iid.*
import java.io.IOException


class LoginActivity : AppCompatActivity(), View.OnClickListener {

    //firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth
    //google client
    private lateinit var googleSignInClient: GoogleSignInClient
    companion object {
        // Registeration token for FCM Client App Instance
        var token: String? = null
    }

    //private const val TAG = "GoogleActivity"
    private val RC_SIGN_IN = 99

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //Google 로그인 옵션 구성. requestIdToken 및 Email 요청
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //firebase auth 객체
        firebaseAuth = FirebaseAuth.getInstance()

        //FCM 토큰 획득
        myToken()

        // Fire
    }

    public override fun onStart() {
        super.onStart()
//        val account = GoogleSignIn.getLastSignedInAccount(this)
//        if(account!==null){ // 이미 로그인 되어있을시 바로 메인 액티비티로 이동
//            toMainActivity(firebaseAuth.currentUser)
//        }

        //btn_googleSignIn.setOnClickListener (this) // 구글 로그인 버튼
        btn_googleSignIn.setOnClickListener {
            val account = GoogleSignIn.getLastSignedInAccount(this)
            if(account!==null){ // 이미 로그인 되어있을시 바로 메인 액티비티로 이동
                toMainActivity(firebaseAuth.currentUser)
            }
            else {signIn()}
        }

        // Sign out 버튼
        btn_googleSignOut.setOnClickListener {
            val account = GoogleSignIn.getLastSignedInAccount(this)
            if(account!==null) {signOut()}  // 로그인 상태인 경우에만 로그아웃을 실행할 수 있다.
            else {Snackbar.make(findViewById(R.id.activity_login), "로그인 상태가 아닙니다.", Snackbar.LENGTH_SHORT).show()}
        }

        btn_exit.setOnClickListener {
            val dlg: AlertDialog.Builder = AlertDialog.Builder(this@LoginActivity,  android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth)
            dlg.setTitle("종료.") //제목
            dlg.setMessage("어플리케이션을 종료하시겠습니까?.") // 메시지
            dlg.setPositiveButton("예", DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
                val account = GoogleSignIn.getLastSignedInAccount(this)
                if(account!==null) {signOut()}
                ActivityCompat.finishAffinity(this)
                exitProcess(0)
            })
            dlg.setNegativeButton("아니오") { dialog, _ -> dialog.dismiss() }
            dlg.show()
        }
    } //onStart End

    private fun myToken() {
        Thread(Runnable {
            try {
                FirebaseInstanceId.getInstance().instanceId
                    .addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.i("token fail", "getInstanceId failed", task.exception)
                            return@OnCompleteListener
                        }
                        token = task.result?.token
                        Log.d("token", token)
                        //Log.d(TAG, token)
                    })
            } catch (e: IOException){
                e.printStackTrace()
            }
        }).start()
    }

    // onActivityResult
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                Snackbar.make(findViewById(R.id.activity_login), "로그인 되었습니다.", Snackbar.LENGTH_SHORT).show()
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)

            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w("LoginActivity", "Google sign in failed", e)
            }
        }
    } // onActivityResult End


    // firebaseAuthWithGoogle
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d("LoginActivity", "firebaseAuthWithGoogle:" + acct.id!!)

        //Google SignInAccount 객체에서 ID 토큰을 가져와서 Firebase Auth로 교환하고 Firebase에 인증
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.w("LoginActivity", "firebaseAuthWithGoogle 성공", task.exception)
                    toMainActivity(firebaseAuth?.currentUser)
                } else {
                    Log.w("LoginActivity", "firebaseAuthWithGoogle 실패", task.exception)
                    Snackbar.make(findViewById(R.id.activity_login), "로그인에 실패하였습니다.", Snackbar.LENGTH_SHORT).show()
                }
            }
    }  // firebaseAuthWithGoogle END


    // toMainActivity
    private fun toMainActivity(user: FirebaseUser?) {
        if(user !=null) {
            val userEmail = user?.email  // 사용자(자신)의 이메일
            val intent = Intent(this@LoginActivity, SetOptions::class.java)
            intent.putExtra("email", userEmail)  // 인텐트로 값 전송
            startActivity(intent)
            finish()
        }
    } // toMainActivity End

    // signIn
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    // signIn End

    override fun onClick(p0: View?) {
    }


    private fun signOut() { // 로그아웃
        if(firebaseAuth?.currentUser != null){
            // Firebase sign out
            firebaseAuth.signOut()

            // Google sign out
            googleSignInClient.signOut().addOnCompleteListener(this) {
                Snackbar.make(findViewById(R.id.activity_login), "로그아웃이 완료되었습니다.", Snackbar.LENGTH_SHORT).show()
            }
        }

        else { Snackbar.make(findViewById(R.id.activity_login), "로그인 상태가 아닙니다.", Snackbar.LENGTH_SHORT).show() }
    }

    private fun revokeAccess() { //회원탈퇴
        // Firebase sign out
        firebaseAuth.signOut()
        googleSignInClient.revokeAccess().addOnCompleteListener(this) {

        }
    }


}