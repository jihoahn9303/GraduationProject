package kr.ac.konkuk.smartcafe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.SparseArray
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_diffuser_setting.*

class DiffuserSetting : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diffuser_setting)

        diffuser_setting_prev.setOnClickListener {
            val intent = Intent(this@DiffuserSetting, SetOptions::class.java)
            startActivity(intent)
        }

        diffuser_setting_repeat_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                diffuser_setting_repeat_seekbar_text.text = "Number of times : ${progress + 1}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        diffuser_setting_period_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                diffuser_setting_period_seekbar_text.text = "Period : $progress min"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

    }
}
