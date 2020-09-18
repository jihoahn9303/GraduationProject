package kr.ac.konkuk.smartcafe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_diffuser_setting.*
import kotlinx.android.synthetic.main.activity_lightning.*

class Lightning : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lightning)

        lightening_prev.setOnClickListener {
            val intent = Intent(this@Lightning, SetOptions::class.java)
            startActivity(intent)
        }

        lightening_switch.setOnCheckedChangeListener { buttonView, isChecked ->
             if (isChecked) {

             } else {

             }
         }

        lightening_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }
}
