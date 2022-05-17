package hu.ait.photoshare

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import hu.ait.photoshare.adapter.PostAdapter
import hu.ait.photoshare.data.Post
import hu.ait.photoshare.databinding.ActivityPostsBinding

class PostsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostsBinding
    private lateinit var postsAdapter: PostAdapter
    private var listenerReg: ListenerRegistration? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = title

        binding.fab.setOnClickListener { view ->
            startActivity(Intent(this, CreatePostActivity::class.java))
        }

        postsAdapter = PostAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid)
        binding.recyclerPosts.adapter = postsAdapter

        initFirebaseQuery()
    }

    private fun initFirebaseQuery() {
        val queryRef =
            FirebaseFirestore.getInstance().collection(
                CreatePostActivity.POSTS_COLLECTION)

        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?,
                                 e: FirebaseFirestoreException?) {
                if (e != null) {
                    Toast.makeText(
                        this@PostsActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                for (docChange in querySnapshot?.getDocumentChanges()!!) {
                    if (docChange.type == DocumentChange.Type.ADDED) {
                        val post = docChange.document.toObject(Post::class.java)
                        postsAdapter.addPost(post, docChange.document.id)
                    } else if (docChange.type == DocumentChange.Type.REMOVED) {
                        postsAdapter.removePostByKey(docChange.document.id)
                    } else if (docChange.type == DocumentChange.Type.MODIFIED) {
                    }
                }
            }
        }

        listenerReg = queryRef.addSnapshotListener(eventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerReg?.remove()
    }

}
