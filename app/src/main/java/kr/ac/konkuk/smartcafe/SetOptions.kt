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


class SetOptions : AppCompatActivity() {

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
            val intent = Intent(this@SetOptions, MusicPlayer::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
