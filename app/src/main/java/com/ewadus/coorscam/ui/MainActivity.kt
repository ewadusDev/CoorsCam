package com.ewadus.coorscam.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.ewadus.coorscam.util.Permissions
import com.ewadus.coorscam.R
import com.ewadus.coorscam.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Bitmap

import android.content.ContextWrapper





class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File
    private lateinit var saveUri: Uri
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA



        if (Permissions.hasPermissionCamera(this)) {
            startCamera()

        } else {
            Permissions.requestCameraPermission(this)
            finish()

        }

        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                animateFlash()
            }
        }

        binding.btnSwitchCam.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        binding.btnGallery.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()


    }

    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply {
                mkdir()
            }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else {
            filesDir
        }

    }

    private fun takePhoto() {

        imageCapture?.let {
            val filename = "JPEG_${System.currentTimeMillis()}.jpg"
            val file = File(externalMediaDirs[0], filename)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            it.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        // executed save here
                        saveUri = Uri.fromFile(file)
                        Log.i("GetURI", saveUri.toString())
                        timestampImageAndSave(saveUri)

                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            this@MainActivity,
                            "${exception.message}",
                            Toast.LENGTH_SHORT
                        )
                            .show()

                    }

                }
            )
        }

    }



    private fun timestampImageAndSave(uri: Uri?): Bitmap {
        val bitmapFromUri = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        val newImage =
            Bitmap.createBitmap(bitmapFromUri.width, bitmapFromUri.height, Bitmap.Config.ARGB_8888)

        val paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.textSize = 20f
        val canvas = Canvas(newImage)
        canvas.drawText("Hello", 0f, 0f, paint)
        canvas.save()
        Log.i("canvas", "${canvas.toString()}")

        return newImage


    }


    private fun drawTextToBitmap(bitmap: Bitmap) {
        val sizeBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val currentTime = System.currentTimeMillis().toString()
        val canvas = Canvas(sizeBitmap)
        val paint = Paint()
        paint.textSize = 50f
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        val height: Float = paint.measureText("yY")
        canvas.drawText(currentTime, 20f, height + 15f, paint)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        try {
            val filename = "JPEG_${System.currentTimeMillis()}.jpg"
            val file = File(externalMediaDirs[0], filename)
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                FileOutputStream(file)

            )
            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
            Log.i("Bitmap", outputOptions.toString())

        } catch (e: Exception) {
            Toast.makeText(
                this@MainActivity,
                "${e.message}",
                Toast.LENGTH_SHORT
            )
                .show()

        }


    }


    private fun startCamera() {

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewfinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .build()

            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (e: Exception) {
                Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()

            }
        }, ContextCompat.getMainExecutor(this))


    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}