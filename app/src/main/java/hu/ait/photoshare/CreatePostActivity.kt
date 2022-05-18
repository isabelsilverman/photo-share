package hu.ait.photoshare

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import hu.ait.mapdemo.MyLocationManager
import hu.ait.photoshare.data.Post
import hu.ait.photoshare.databinding.ActivityCreatePostBinding
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.*
import kotlin.concurrent.thread

class CreatePostActivity : AppCompatActivity(), MyLocationManager.OnNewLocationAvailable {

    companion object {
        const val POSTS_COLLECTION = "posts"
        const val REQUEST_CAMERA_PERMISSION = 1001
    }

    lateinit var binding: ActivityCreatePostBinding
    lateinit var currentLocation : String
    private lateinit var myLocationManager: MyLocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        myLocationManager = MyLocationManager(this, this)
        requestNeededPermission()

        binding.btnAttach.setOnClickListener {
            val intentPhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            resultLauncher.launch(intentPhoto)
        }

        requestNeededPermission()
    }
    override fun onStop() {
        super.onStop()
        myLocationManager.stopLocationMonitoring()
    }


    var uploadBitmap: Bitmap? = null
    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK){
            val data: Intent? = result.data
            uploadBitmap = data!!.extras!!.get("data") as Bitmap
            binding.imgAttach.setImageBitmap(uploadBitmap)
            binding.imgAttach.visibility = View.VISIBLE
        }
    }

    private fun requestNeededPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                Toast.makeText(this,
                    "I need it for camera", Toast.LENGTH_SHORT).show()
            }

            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
        } else {
            // we already have permission
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        } else {
            // we have the permission
            myLocationManager.startLocationMonitoring()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "CAMERA perm granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "CAMERA perm NOT granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun sendClick(v: View) {
        if (uploadBitmap == null) {
            Toast.makeText(this, "IMAGE UPLOAD required", Toast.LENGTH_LONG).show()
        } else {
            try {
                uploadPostWithImage()
            } catch (e: java.lang.Exception){
                e.printStackTrace()
            }
        }
    }


    fun uploadPost(imgUrl: String = "") {
        val newPost = Post(
            FirebaseAuth.getInstance().currentUser!!.uid,
            FirebaseAuth.getInstance().currentUser!!.email!!,
            binding.etCaption.text.toString(),
            imgUrl,currentLocation
        )

        // "connect" to posts collection (table)
        val postsCollection =
            FirebaseFirestore.getInstance().collection(POSTS_COLLECTION)
        postsCollection.add(newPost)
            .addOnSuccessListener {
                Toast.makeText(this@CreatePostActivity,
                    "Post SAVED", Toast.LENGTH_LONG).show()

                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this@CreatePostActivity,
                    "Error ${it.message}", Toast.LENGTH_LONG).show()
            }
    }



    @Throws(Exception::class)
    private fun uploadPostWithImage() {
        val baos = ByteArrayOutputStream()
        uploadBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageInBytes = baos.toByteArray()

        val storageRef = FirebaseStorage.getInstance().getReference()
        val newImage = URLEncoder.encode(UUID.randomUUID().toString(), "UTF-8") + ".jpg"
        val newImagesRef = storageRef.child("images/$newImage")

        newImagesRef.putBytes(imageInBytes)
            .addOnFailureListener { exception ->
                Toast.makeText(this@CreatePostActivity, exception.message, Toast.LENGTH_SHORT).show()
                exception.printStackTrace()
            }.addOnSuccessListener { taskSnapshot ->
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.

                newImagesRef.downloadUrl.addOnCompleteListener(object: OnCompleteListener<Uri> {
                    override fun onComplete(task: Task<Uri>) {
                        // the public URL of the image is: task.result.toString()

                        uploadPost(task.result.toString())
                    }
                })
            }
    }

    var lastLocation: Location? = null

    override fun onNewLocation(location: Location) {
        lastLocation = location
        if (lastLocation != null) {
            geocodeLocation(lastLocation!!.latitude,
                lastLocation!!.longitude)
        }

    }
    private fun geocodeLocation(latitude : Double, longitude : Double) {
        thread {
            try {
                val gc = Geocoder(this, Locale.getDefault())
                val addrs: List<Address> =
                    gc.getFromLocation(latitude, longitude, 3)
                val addr = addrs[0].getAddressLine(0)

                runOnUiThread {
                    currentLocation = addr
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@CreatePostActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


}
