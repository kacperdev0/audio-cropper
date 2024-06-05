package com.example.audiocropper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {
    var PICK_FILE_REQ_CODE = 1
    lateinit var urlDisplay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        urlDisplay = findViewById(R.id.filePath)

        findViewById<Button>(R.id.selectFileButton).setOnClickListener {
            checkPermissionAndSelectFile()
        }
    }

    private fun audioFilePicker() {
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.setType("audio/mpeg")
        val  getFile = Intent.createChooser(chooseFile, "Select audio file")
        startActivityForResult(getFile, 1)
    }

    private fun checkPermissionAndSelectFile() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            audioFilePicker()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        urlDisplay.text = data?.data.toString()
    }
}