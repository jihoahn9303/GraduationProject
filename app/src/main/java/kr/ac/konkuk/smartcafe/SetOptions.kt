package kr.ac.konkuk.smartcafe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_set_options.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class SetOptions : AppCompatActivity(), CoroutineScope {

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private lateinit var ioScope : CoroutineScope

    companion object {
        var category : String? = "basic"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_options)
        job = Job()
        ioScope = CoroutineScope(coroutineContext)

        set_options_category.text = "SmartCafe Options"
        set_options_category.textSize = 40.toFloat()

        logout.setOnClickListener {
            val intent = Intent(this@SetOptions, LoginActivity::class.java)
            startActivity(intent)
        }

        set_options_camera.setOnClickListener {
            val intent = Intent(this@SetOptions, Camera::class.java)
            startActivity(intent)
            finish()
        }

        set_options_device.setOnClickListener {
            val intent = Intent(this@SetOptions, Connect::class.java)
            startActivity(intent)
        }

        set_options_diffuser.setOnClickListener {
            val intent = Intent(this@SetOptions, DiffuserSetting::class.java)
            startActivity(intent)
        }

        set_options_lightning.setOnClickListener {
            val intent = Intent(this@SetOptions, Lightning::class.java)
            startActivity(intent)
        }

        set_options_music.setOnClickListener {
            launch {
                val jobReadCategory = launch(Dispatchers.IO, CoroutineStart.LAZY) {readCategory()}
                jobReadCategory.join()
                val intent = Intent(this@SetOptions, MusicPlayer::class.java)
                startActivity(intent)
            }
        }
    }

    private fun readCategory() {
        Log.d("start", "start reading category!")
        val subpath = LoginActivity.userEmail?.split("@")!![0]
        val ref = Firebase.database.reference
        ref.child("userInfo").child(subpath)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var map = snapshot.value as Map<String, Any>
                    category = map["category"].toString()
                    Log.d("read_category", "$category")
                }

                override fun onCancelled(error: DatabaseError) {}

            })
    }
}
