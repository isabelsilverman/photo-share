package hu.ait.photoshare.data

data class Post (
    var uid: String = "",
    var user: String = "",
    var caption: String = "",
    var imgUrl: String = "",
    var location: String = "",
    var comments: Array<Comment> = emptyArray()
)