package hu.ait.photoshare.adapter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.photoshare.CommentsActivity
import hu.ait.photoshare.CreatePostActivity
import hu.ait.photoshare.data.Post
import hu.ait.photoshare.databinding.PostRowBinding
import hu.ait.photoshare.dialog.CommentDialog

class PostAdapter : RecyclerView.Adapter<PostAdapter.ViewHolder> {

    lateinit var context: Context
    var  postsList = mutableListOf<Post>()
    var  postKeys = mutableListOf<String>()

    lateinit var currentUid: String

    constructor(context: Context, uid: String) : super() {
        this.context = context
        this.currentUid = uid
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return postsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var post = postsList.get(holder.adapterPosition)

        holder.tvUser.text = post.user
        holder.tvLocation.text = post.location
        holder.tvCaption.text = post.caption



        if (currentUid == post.uid) {
            holder.btnDelete.visibility = View.VISIBLE
        } else {
            holder.btnDelete.visibility = View.GONE
        }

        holder.btnDelete.setOnClickListener {
            removePost(holder.adapterPosition)
        }

        holder.btnComment.setOnClickListener {
            val dialog = CommentDialog()

            val bundle = Bundle()
            bundle.putSerializable("POST_KEY", postKeys[holder.adapterPosition])
            dialog.arguments = bundle

            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            dialog.show(fragmentManager, "COMMENT_DIALOG")
        }

        holder.btnViewComments.setOnClickListener {
            val intentDetails = Intent(context, CommentsActivity::class.java)
            intentDetails.putExtra("POST_KEY", postKeys[holder.adapterPosition])
            context.startActivity(intentDetails)
        }

        if (post.imgUrl.isNotEmpty()) {
            holder.ivPhoto.visibility = View.VISIBLE
            Glide.with(context).load(post.imgUrl).into(holder.ivPhoto)
        } else {
            holder.ivPhoto.visibility = View.GONE
        }

    }

    fun addPost(post: Post, key: String) {
        postsList.add(post)
        postKeys.add(key)
        //notifyDataSetChanged()
        notifyItemInserted(postsList.lastIndex)
    }

    // when I remove the post object
    private fun removePost(index: Int) {
        FirebaseFirestore.getInstance().collection(
            CreatePostActivity.POSTS_COLLECTION).document(
            postKeys[index]
        ).delete()

        postsList.removeAt(index)
        postKeys.removeAt(index)
        notifyItemRemoved(index)
    }

    // when somebody else removes an object
    fun removePostByKey(key: String) {
        val index = postKeys.indexOf(key)
        if (index != -1) {
            postsList.removeAt(index)
            postKeys.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(val binding: PostRowBinding) : RecyclerView.ViewHolder(binding.root){
        var tvUser = binding.tvUser
        var tvLocation = binding.tvLocation
        var tvCaption = binding.tvCaption
        var btnDelete = binding.btnDelete
        var btnComment = binding.btnComment
        var btnViewComments = binding.btnViewComments
        var ivPhoto = binding.ivPhoto

    }
}