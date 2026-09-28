package com.metrolist.innertube.models

data class YouTubeComment(
    val id: String,
    val text: String,
    val authorName: String,
    val authorChannelId: String?,
    val authorThumbnailUrl: String?,
    val publishedTime: String,
    val likeCount: String?,
    val replyCount: String?,
    val replyContinuation: String?,
    val replyParams: String?,
    val likeAction: String?,
    val unlikeAction: String?,
    val likeCountAfterLike: String?,
    val likeCountAfterUnlike: String?,
    val isLiked: Boolean,
    val isVerified: Boolean,
    val isCreator: Boolean,
    val isPinned: Boolean,
)

data class YouTubeCommentRepliesPage(
    val replies: List<YouTubeComment>,
    val continuation: String?,
)

data class YouTubeCommentsPage(
    val comments: List<YouTubeComment>,
    val continuation: String?,
    val createCommentParams: String?,
    val totalCount: String?,
    val viewerThumbnailUrl: String?,
)
