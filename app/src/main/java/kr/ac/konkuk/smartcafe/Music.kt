package kr.ac.konkuk.smartcafe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_music.*

class Music : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        var denominator : Int = 5
        var numerator : Int = 1
        var fraction : String = numerator.toString() + " / " + denominator.toString()

        music_fraction.setText(fraction)

        var title : String = "Sunday Morning"
        var singer : String = "Maroon 5"

        music_title.setText(title)
        music_singer.setText(singer)

        music_prev.setOnClickListener {
            val intent = Intent(this@Music, SetOptions::class.java)
            startActivity(intent)
        }

        music_play.setOnClickListener{

        }

        music_stop.setOnClickListener {

        }

        music_skip_prev.setOnClickListener {

        }

        music_skip_forward.setOnClickListener {

        }

        music_play_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

    }
}
