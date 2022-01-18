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


        initText()
        initButton()
        startCamera()
        requestLocation()

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()


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

        // only open local app gallery
        binding.imgLast.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, null)
            intent.type = "image/jpg"
            startActivity(Intent.createChooser(intent, "Open in..."))

        }

        // hidden button for request location again
        binding.btnRefreshLocation.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                requestLocation()
            }
        }
        )
    }

    private fun requestLocation() {
        // check permission then use obj fun location manager
        if (Permissions.hasPermissionLocation(this)) {
            LocationManager.Builder
                // repeat every 4 sec
                .setInterval(4000)
                // repeat every 2 sec if is possible
                .setFastestInterval(2000)
                // get high acc type gps location
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
        } else {
            Permissions.requestLocationPermission(this)
        }

    }

    // make photo folder from file
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

    private fun startCamera() {
        //check camera permission
        if (Permissions.hasPermissionCamera(this)) {

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

        } else {
            Permissions.requestCameraPermission(this)

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
                        // add location / timestamp to URI File
                        timestampImageAndSave(saveUri)
                        // rotate imageview to portrait
                        binding.imgLast.rotation = 90f
                        binding.imgLast.visibility = View.VISIBLE
                        // bind bitmap from func stamp to imageview
                        binding.imgLast.setImageBitmap(timestampImageAndSave(saveUri))
                        // Save bitmap to gallery at album "CoorsCam"
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

    // flash when touch take photo's button
    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    private fun saveBitmapToStorage(context: Context, bitmap: Bitmap, albumName: String) {
        // rotates bitmap to 90 degrees
        val rotedBitmap = bitmapRotated(bitmap)
        // set up name of file photo
        val filename = "${System.currentTimeMillis()}.jpg"
        // set compress and set format jpg
        val write: (OutputStream) -> Boolean = {
            rotedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        // check android Q then can create album
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
            // checked lower then android Q create album and insert file photo
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

    // func rotate bitmap
    private fun bitmapRotated(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    }

    // draw content on bitmap
    private fun timestampImageAndSave(uri: Uri?): Bitmap {
        // convert drawable image to bitmap
        val icon =
            BitmapFactory.decodeResource(this.applicationContext.resources, R.drawable.ic_app_round)
        // resize icon image bitmap to small
        val iconScaled = Bitmap.createScaledBitmap(icon, 300, 300, true)
        // convert uri image(take photo) to bitmap
        val bitmapFromUri = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        // get size from actual image(take photo)
        val bitmapSize =
            Bitmap.createBitmap(bitmapFromUri.width, bitmapFromUri.height, bitmapFromUri.config)
        // create canvas and get bound from actual image(take photo)
        val canvas = Canvas(bitmapSize)
        // take photo image to center background
        canvas.drawBitmap(bitmapFromUri, 0f, 0f, null)
        // set up Paint for design textview to canvas
        val paint = Paint()
        paint.color = Color.WHITE
        paint.textSize = 130f
        paint.textAlign = Paint.Align.LEFT
        //  determine location of paint textview set1
        val x = -canvas.height.toFloat()
        val y = canvas.width.toFloat()
        canvas.rotate(-90f)
        canvas.drawText("${tvAccuracy.text}", x, y - 1120f, paint)
        canvas.drawText("${tvTimeStamp.text}", x, y - 970f, paint)
        canvas.drawText("EPSG:4326 WGS84", x, y - 270f, paint)
        // save state on canvas
        canvas.save()
        // set2 of textview
        paint.textSize = 200f
        canvas.drawText("Lat: ${tvLat.text}", x, y - 750f, paint)
        canvas.drawText("Long: ${tvLong.text}", x, y - 480f, paint)
        // add icon image to top right
        canvas.drawBitmap(iconScaled, x + 2500f, y - 3800f, paint)
        canvas.save()

        return bitmapSize

    }

    override fun onDestroy() {
        super.onDestroy()
        // turn of camera system
        cameraExecutor.shutdown()
    }
}