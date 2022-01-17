package com.ewadus.coorscam.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.ewadus.coorscam.util.Permissions
import com.ewadus.coorscam.R
import com.ewadus.coorscam.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.os.*
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import com.ewadus.coorscam.util.Constant.Companion.FILENAME_FORMAT
import com.ewadus.coorscam.util.LocationManager
import com.google.android.gms.location.LocationRequest
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File
    private lateinit var saveUri: Uri
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private lateinit var tvAccuracy: TextView
    private lateinit var tvTimeStamp: TextView
    private lateinit var tvLat: TextView
    private lateinit var tvLong: TextView


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

        if (Permissions.hasPermissionLocation(this)) {
            requestLocation()

        } else {
            Permissions.requestLocationPermission(this)
        }

        initText()
        initButton()


        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()


    }


    private fun requestLocation() {
        LocationManager.Builder
            .setInterval(4000)
            .setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .create(this)
            .request { latitude, longitude, accuracy ->
                tvLat.text = "Lat: $latitude"
                tvLong.text = "Long: $longitude"
                tvAccuracy.text = "Acc: $accuracy m."
                val getTime = System.currentTimeMillis()
                val converterTime = SimpleDateFormat("d/MMM/yyyy HH:mm")
                val formattedTime = converterTime.format(getTime)
                tvTimeStamp.text = "Time:$formattedTime"


            }

    }

    private fun initText() {
        tvTimeStamp = binding.tvTimeStamp
        tvLong = binding.tvLon
        tvLat = binding.tvLat
        tvAccuracy = binding.tvAccuracy

    }


    private fun initButton() {
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

        binding.imgLast.setOnClickListener {
            Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()

        }
        binding.btnRefreshLocation.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                requestLocation()
            }

        }

        )
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

            // Create time-stamped output file to hold the image
            val photoFile = File(
                outputDirectory,
                SimpleDateFormat(
                    FILENAME_FORMAT,
                    Locale.US
                ).format(System.currentTimeMillis()) + ".jpg"
            )
            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()


            // Set up image capture listener,
            // which is triggered after photo has
            // been taken
            it.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        // executed save here
                        saveUri = Uri.fromFile(photoFile)


                        timestampImageAndSave(saveUri)
                        binding.imgLast.rotation = 90f
                        binding.imgLast.visibility = View.VISIBLE
                        binding.imgLast.setImageBitmap(timestampImageAndSave(saveUri))

                        saveBitmapToStorage(
                            this@MainActivity,
                            timestampImageAndSave(saveUri),
                            "CoorsCam"
                        )


                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.i("takephoto", exception.message.toString())

                    }

                }
            )
        }

    }

    private fun saveBitmapToStorage(context: Context, bitmap: Bitmap, albumName: String) {
        val rotedBitmap = bitmapRoted(bitmap)
        val filename = "${System.currentTimeMillis()}.jpg"
        val write: (OutputStream) -> Boolean = {
            rotedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DCIM}/$albumName"
                )
            }

            context.contentResolver.let {
                it.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                    it.openOutputStream(uri)?.let(write)
                }
            }
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .toString() + File.separator + albumName
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val image = File(imagesDir, filename)
            write(FileOutputStream(image))

        }

    }

    private fun bitmapRoted(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    }


    private fun timestampImageAndSave(uri: Uri?): Bitmap {


        val bitmapFromUri = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        val bitmapSize =
            Bitmap.createBitmap(bitmapFromUri.width, bitmapFromUri.height, bitmapFromUri.config)

        val canvas = Canvas(bitmapSize)
        canvas.drawBitmap(bitmapFromUri, 0f, 0f, null)
        val paint = Paint()
        paint.color = Color.WHITE
        paint.textSize = 130f
        paint.textAlign = Paint.Align.LEFT
        val x = -canvas.height.toFloat()
        val y = canvas.width.toFloat()


        canvas.rotate(-90f)
        canvas.drawText("${tvAccuracy.text}", x, y - 1120f, paint)
        canvas.drawText("${tvTimeStamp.text}", x, y - 970f, paint)
        canvas.drawText("EPSG:4326 WGS84", x, y - 270f, paint)
        canvas.save()

        paint.textSize = 200f
        canvas.drawText("Lat: ${tvLat.text}", x, y - 730f, paint)
        canvas.drawText("Long: ${tvLong.text}", x, y - 480f, paint)
        canvas.save()



        return bitmapSize

    }


    private fun startCamera() {
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build().also {
                    it.setSurfaceProvider(binding.viewfinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder().build()
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (e: Exception) {
                Log.e("startCamera", e.message.toString())

            }
        }, ContextCompat.getMainExecutor(this))


    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}