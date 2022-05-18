package hu.ait.photoshare.dialog

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import hu.ait.photoshare.CreatePostActivity
import hu.ait.photoshare.data.Comment
import hu.ait.photoshare.data.Post
import hu.ait.photoshare.databinding.CommentDialogBinding
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.*

class CommentDialog : DialogFragment() {


    lateinit var binding: CommentDialogBinding


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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = AlertDialog.Builder(requireContext())



        binding = CommentDialogBinding.inflate(requireActivity().layoutInflater)
        dialogBuilder.setView(binding.root)

        binding.btnAttach.setOnClickListener {
            val intentPhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            resultLauncher.launch(intentPhoto)
        }

        requestNeededPermission()


        dialogBuilder.setPositiveButton("Ok") { dialog, which ->
            //
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, which ->
        }
        return dialogBuilder.create()
    }

    override fun onResume() {
        super.onResume()

        val dialog = dialog as AlertDialog
        val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)

        positiveButton.setOnClickListener { view

            if (uploadBitmap == null) {
                uploadComment()
            } else {
                try {
                    uploadCommentWithImage()
                } catch (e: java.lang.Exception){
                    e.printStackTrace()
                }
            }

            dialog.dismiss()

        }
    }



    private fun requestNeededPermission() {
        if (ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this.requireActivity(),
                    Manifest.permission.CAMERA)) {
                Toast.makeText( this.requireContext(), "I need it for camera", Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.requestPermissions(this.requireActivity(),
                arrayOf(android.Manifest.permission.CAMERA),
                CreatePostActivity.REQUEST_CAMERA_PERMISSION
            )
        } else {
            // we already have permission
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CreatePostActivity.REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText( this.requireContext(), "CAMERA perm granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText( this.requireContext(), "CAMERA perm NOT granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    fun uploadComment(imgUrl: String = "") {
        //get comments from post , add new comment, rewrite comments

        val postKey = requireArguments().getSerializable("POST_KEY").toString()

        val postDocument = FirebaseFirestore.getInstance().collection(CreatePostActivity.POSTS_COLLECTION)
            .document(postKey)

        lateinit var newComments: Array<Comment?>

        val newComment = Comment(
            FirebaseAuth.getInstance().currentUser!!.email!!,
            binding.etCommentText.text.toString(),
            imgUrl
        )

        postDocument.get().addOnSuccessListener { documentSnapshot ->
            val post = documentSnapshot.toObject<Post>()
            var comments = post!!.comments
            newComments = comments.copyOf(comments.size)
            newComments[comments.size] = newComment

            postDocument.update("comments", newComments)
                .addOnSuccessListener {
                    Toast.makeText(
                        this.requireContext(),
                        "Post SAVED", Toast.LENGTH_LONG
                    ).show()

                    //finish() why doesn't this work?
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this.requireContext(),
                        "Error ${it.message}", Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this.requireContext(),
                    "Error ${it.message}", Toast.LENGTH_LONG
                ).show()
            }


    }


    @Throws(Exception::class)
    fun uploadCommentWithImage() {
        val baos = ByteArrayOutputStream()
        uploadBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageInBytes = baos.toByteArray()

        val storageRef = FirebaseStorage.getInstance().getReference()
        val newImage = URLEncoder.encode(UUID.randomUUID().toString(), "UTF-8") + ".jpg"
        val newImagesRef = storageRef.child("images/$newImage")

        newImagesRef.putBytes(imageInBytes)
            .addOnFailureListener { exception ->
                Toast.makeText( this.requireContext(), exception.message, Toast.LENGTH_SHORT)
                    .show()
                exception.printStackTrace()
            }.addOnSuccessListener { taskSnapshot ->
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.

                newImagesRef.downloadUrl.addOnCompleteListener(object : OnCompleteListener<Uri> {
                    override fun onComplete(task: Task<Uri>) {
                        // the public URL of the image is: task.result.toString()

                        uploadComment(task.result.toString())
                    }
                })
            }
    }
}


