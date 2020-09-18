package kr.ac.konkuk.smartcafe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_set_options.*


class SetOptions : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_options)

        set_options_category.setText("TRENDY")
        set_options_category.setTextSize(50.toFloat())

        logout.setOnClickListener {
            val intent = Intent(this@SetOptions, LoginActivity::class.java)
            startActivity(intent)
        }

        set_options_camera.setOnClickListener {
            val userEmail = intent.getStringExtra("email")
            val intent = Intent(this@SetOptions, Camera::class.java)
            intent.putExtra("email", userEmail)
            startActivity(intent)
//            finish()
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
            val intent = Intent(this@SetOptions, Music::class.java)
            startActivity(intent)
        }
    }

}
