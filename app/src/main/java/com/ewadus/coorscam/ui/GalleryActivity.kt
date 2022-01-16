package com.ewadus.coorscam.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.ewadus.coorscam.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val directory = File(externalMediaDirs[0].absolutePath)
        val file = directory.listFiles() as Array<File>

        val adapter = GalleryAdapter(file.reversedArray())
        binding.recyclerview.apply {
            layoutManager =
                LinearLayoutManager(this@GalleryActivity, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }
    }
}