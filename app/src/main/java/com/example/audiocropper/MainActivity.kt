package com.example.audiocropper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.google.android.material.slider.RangeSlider
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    var PICK_FILE_REQ_CODE = 1
    var SAVE_FILE_REQ_CODE = 2

    lateinit var url: Uri
    lateinit var saveUrl: Uri

    val handler = Handler(Looper.getMainLooper())
    lateinit var mediaPlayer: MediaPlayer

    lateinit var timeSlider: RangeSlider
    lateinit var previewTimeTextView: TextView

    var currentPreviewTime = -1

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

        timeSlider = findViewById(R.id.timeSlider)
        previewTimeTextView = findViewById(R.id.textView)

        findViewById<Button>(R.id.selectFileButton).setOnClickListener {
            if (checkPermissions()) {
                audioFilePicker()
            }
        }

        findViewById<Button>(R.id.playButton).setOnClickListener {
            playPreview()
        }

        findViewById<Button>(R.id.pauseButton).setOnClickListener {
            handlePause()
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            if (checkPermissions()) {
                cutAudio()
            }
        }


        timeSlider.addOnChangeListener { _, value, _ ->
            run {
                if (mediaPlayer.isPlaying) {
                    playPreview()
                }
            }
        }

        timeSlider.setLabelFormatter { value ->
            val duration = value.toInt()
            formatTime(duration)
        }
    }

    private fun handlePause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        } else {
            mediaPlayer.start()
        }
    }

    private fun formatTime(duration: Int): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return String.format("%02d", minutes) + ":" + String.format("%02d", seconds)
    }

    private fun cutAudio() {
        val inputFilePath = getFileAbsolutePath(url)
        println(inputFilePath)
        val outputFilePath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "output.mp3").absolutePath
        val startSeconds = timeSlider.values[0].toInt()
        val durationSeconds = timeSlider.values[1].toInt() - startSeconds

        val command = " -y -i '$inputFilePath' -ss $startSeconds -t $durationSeconds -c copy '$outputFilePath'"

        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode)) {
                // SUCCESS
                println("Command succeeded with return code ${returnCode}.")
            } else if (ReturnCode.isCancel(returnCode)) {
                // CANCEL
                println("Command cancelled with return code ${returnCode}.")
            } else {
                // FAILURE
                println("Command failed with return code ${returnCode}.")
            }
        }
    }


    private fun audioFilePicker() {
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/mpeg"
        }
        val getFile = Intent.createChooser(chooseFile, "Select audio file")
        startActivityForResult(getFile, PICK_FILE_REQ_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == PICK_FILE_REQ_CODE && resultCode == RESULT_OK) {
                data?.data?.let {
                    url = it
                    mediaPlayer.setDataSource(applicationContext, it)
                    mediaPlayer.prepare()
                    timeSlider.valueTo = mediaPlayer.duration / 1000F
                    timeSlider.setValues(0.0F, (mediaPlayer.duration / 1000F))
                }
            }
            if (requestCode == SAVE_FILE_REQ_CODE && resultCode == RESULT_OK) {
                data?.data?.let {
                    saveUrl = it
                    cutAudio()
                }
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            // Handle the exception or log it appropriately
        }
    }

    private fun showPointer() {
        handler.removeCallbacksAndMessages(null) // Clear all previous callbacks

        currentPreviewTime = (timeSlider.values[0]).toInt()
        val endSec = (timeSlider.values[1]).toInt()

        val updateTime = object : Runnable {
            override fun run() {
                previewTimeTextView.text = formatTime(currentPreviewTime)
                currentPreviewTime += 1

                if (currentPreviewTime <= endSec) {
                    handler.postDelayed(this, 1000) // Schedule the next update after 1 second
                }
            }
        }

        handler.post(updateTime) // Start the initial update
    }

    private fun playPreview() {
        val startSec = (timeSlider.values[0] * 1000).toInt()
        val endSec = (timeSlider.values[1] * 1000).toInt()
        mediaPlayer.seekTo(startSec)
        mediaPlayer.start()

        Handler(Looper.getMainLooper()).postDelayed({
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                mediaPlayer.seekTo(startSec)
            }
        }, (endSec - startSec).toLong())

        showPointer()
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), 1)
            false
        } else {
            true
        }
    }
    private fun getFileAbsolutePath(uri: Uri): String? {
        var filePath: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val fileName = it.getString(columnIndex)
                val file = File(applicationContext.cacheDir, fileName)
                filePath = file.absolutePath
                val inputStream = contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
            }
        }
        return filePath
    }
}
