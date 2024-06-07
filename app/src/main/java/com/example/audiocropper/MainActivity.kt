package com.example.audiocropper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.google.android.material.slider.RangeSlider


class MainActivity : AppCompatActivity() {
    var PICK_FILE_REQ_CODE = 1

    lateinit var urlDisplay: TextView
    lateinit var mediaPlayer: MediaPlayer
    lateinit var timeSlider: RangeSlider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mediaPlayer = MediaPlayer()

        findViewById<Button>(R.id.selectFileButton).setOnClickListener {
            checkPermissionAndSelectFile()
        }

        findViewById<Button>(R.id.playButton).setOnClickListener {
            playPreview()
        }

        timeSlider = findViewById<RangeSlider>(R.id.timeSlider)

        timeSlider.addOnChangeListener { rangeSlider, value, fromUser ->
            run {
                println(value.toString())
            }
        }

        timeSlider.setLabelFormatter {value ->
            val duration = value.toInt()
            val minutes = (duration / 60).toInt()
            val seconds = duration % 60
            String.format("%02d", minutes) + ":" + String.format("%02d", seconds)
        }
    }

    private fun audioFilePicker() {
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.setType("audio/mpeg")
        val  getFile = Intent.createChooser(chooseFile, "Select audio file")
        startActivityForResult(getFile, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val selectedUri: Uri? = data?.data
        selectedUri?.let {
            mediaPlayer.setDataSource(applicationContext, selectedUri)
            mediaPlayer.prepare()
            timeSlider.valueTo = (mediaPlayer.duration / 1000).toFloat()
        }
    }

    private fun playPreview() {
        val startSec = timeSlider.values[0].toInt() * 1000

        mediaPlayer.seekTo(startSec)
        mediaPlayer.start()

        println((timeSlider.values[1].toInt() * 1000) - startSec)

        Handler(Looper.getMainLooper()).postDelayed({
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.reset()
            }
        }, (((timeSlider.values[1].toInt() * 1000) - startSec).toLong()))
    }

    private fun checkPermissionAndSelectFile() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            audioFilePicker()
        }
    }
}