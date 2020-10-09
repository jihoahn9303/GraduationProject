package kr.ac.konkuk.smartcafe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_connect.*

class Connect : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        device_prev.setOnClickListener {
            val intent = Intent(this@Connect, SetOptions::class.java)
            startActivity(intent)
        }

        connect_speaker.setOnClickListener {

        }

        connect_diffuser.setOnClickListener {
            val intent = Intent(this@Connect, DeviceScanActivity::class.java)
            startActivity(intent)
        }
    }
}
