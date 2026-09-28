/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeComment
import com.metrolist.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun YouTubeCommentsSheet(
    videoId: String,
    modifier: Modifier = Modifier,
) {
    var comments by remember(videoId) { mutableStateOf<List<YouTubeComment>>(emptyList()) }
    var continuation by remember(videoId) { mutableStateOf<String?>(null) }
    var consumedContinuations by remember(videoId) { mutableStateOf<Set<String>>(emptySet()) }
    var createCommentParams by remember(videoId) { mutableStateOf<String?>(null) }
    var totalCount by remember(videoId) { mutableStateOf<String?>(null) }
    var viewerThumbnailUrl by remember(videoId) { mutableStateOf<String?>(null) }
    var isInitialLoading by remember(videoId) { mutableStateOf(true) }
    var isLoadingMore by remember(videoId) { mutableStateOf(false) }
    var errorMessage by remember(videoId) { mutableStateOf<String?>(null) }
    var commentText by remember(videoId) { mutableStateOf("") }
    var replyTarget by remember(videoId) { mutableStateOf<YouTubeComment?>(null) }
    var isPosting by remember(videoId) { mutableStateOf(false) }
    var postError by remember(videoId) { mutableStateOf(false) }
    var expandedReplyIds by remember(videoId) { mutableStateOf<Set<String>>(emptySet()) }
    var repliesByComment by remember(videoId) {
        mutableStateOf<Map<String, List<YouTubeComment>>>(emptyMap())
    }
    var replyContinuations by remember(videoId) { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var loadingReplyIds by remember(videoId) { mutableStateOf<Set<String>>(emptySet()) }
    var likedOverrides by remember(videoId) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var likingIds by remember(videoId) { mutableStateOf<Set<String>>(emptySet()) }
    val isLoggedIn = remember(YouTube.cookie) { YouTube.cookie != null }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sheetColor = MaterialTheme.colorScheme.surface
    val cardColor = MaterialTheme.colorScheme.surfaceContainer

    suspend fun loadInitial() {
        isInitialLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) { YouTube.comments(videoId) }
            .onSuccess { page ->
                comments = page.comments
                continuation = page.continuation
                consumedContinuations = emptySet()
                createCommentParams = page.createCommentParams
                totalCount = page.totalCount
                viewerThumbnailUrl = page.viewerThumbnailUrl
            }
            .onFailure { errorMessage = it.message }
        isInitialLoading = false
    }

    fun loadReplies(comment: YouTubeComment, append: Boolean) {
        if (comment.id in loadingReplyIds) return
        val token = if (append) replyContinuations[comment.id] else comment.replyContinuation
        if (token == null) return
        loadingReplyIds = loadingReplyIds + comment.id
        scope.launch {
            withContext(Dispatchers.IO) { YouTube.commentReplies(token) }
                .onSuccess { page ->
                    val existing = if (append) repliesByComment[comment.id].orEmpty() else emptyList()
                    repliesByComment = repliesByComment + (
                        comment.id to (existing + page.replies).distinctBy(YouTubeComment::id)
                    )
                    replyContinuations = replyContinuations + (comment.id to page.continuation)
                    expandedReplyIds = expandedReplyIds + comment.id
                }
            loadingReplyIds = loadingReplyIds - comment.id
        }
    }

    LaunchedEffect(videoId) { loadInitial() }

    LaunchedEffect(listState, videoId) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (
                comments.isNotEmpty() &&
                lastVisible >= comments.lastIndex - 3 &&
                !isLoadingMore
            ) {
                continuation?.takeUnless(consumedContinuations::contains)
            } else {
                null
            }
        }
            .distinctUntilChanged()
            .filterNotNull()
            .collect { next ->
                isLoadingMore = true
                consumedContinuations = consumedContinuations + next
                try {
                    withContext(Dispatchers.IO) { YouTube.comments(videoId, next) }
                        .onSuccess { page ->
                            comments = (comments + page.comments).distinctBy(YouTubeComment::id)
                            continuation = page.continuation
                                ?.takeUnless { it == next || it in consumedContinuations }
                        }
                        .onFailure { continuation = null }
                } finally {
                    isLoadingMore = false
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(sheetColor)
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.comments),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                totalCount?.let {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isInitialLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    errorMessage != null -> ErrorContent { scope.launch { loadInitial() } }
                    comments.isEmpty() -> Text(
                        text = stringResource(R.string.no_comments),
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 12.dp, bottom = if (isLoggedIn) 112.dp else 28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(comments, key = YouTubeComment::id) { comment ->
                            val isExpanded = comment.id in expandedReplyIds
                            val likedOverride = likedOverrides[comment.id]
                            val isLiked = likedOverride ?: comment.isLiked
                            val displayedLikeCount = when (likedOverride) {
                                true -> comment.likeCountAfterLike ?: comment.likeCount
                                false -> comment.likeCountAfterUnlike ?: comment.likeCount
                                null -> comment.likeCount
                            }
                            CommentCard(
                                comment = comment,
                                replies = repliesByComment[comment.id].orEmpty(),
                                isExpanded = isExpanded,
                                isLoadingReplies = comment.id in loadingReplyIds,
                                hasMoreReplies = replyContinuations[comment.id] != null,
                                isLiked = isLiked,
                                likeCount = displayedLikeCount,
                                isLiking = comment.id in likingIds,
                                isLoggedIn = isLoggedIn,
                                containerColor = cardColor,
                                onToggleReplies = {
                                    if (isExpanded) {
                                        expandedReplyIds = expandedReplyIds - comment.id
                                    } else if (repliesByComment.containsKey(comment.id)) {
                                        expandedReplyIds = expandedReplyIds + comment.id
                                    } else {
                                        loadReplies(comment, append = false)
                                    }
                                },
                                onLoadMoreReplies = { loadReplies(comment, append = true) },
                                onReply = {
                                    replyTarget = comment
                                    commentText = ""
                                    postError = false
                                },
                                onToggleLike = {
                                    val currentlyLiked = likedOverrides[comment.id] ?: comment.isLiked
                                    val action = if (currentlyLiked) comment.unlikeAction else comment.likeAction
                                    if (action != null && comment.id !in likingIds) {
                                        likedOverrides = likedOverrides + (comment.id to !currentlyLiked)
                                        likingIds = likingIds + comment.id
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                YouTube.performCommentAction(action)
                                            }
                                            if (result.isFailure) {
                                                likedOverrides = likedOverrides + (comment.id to currentlyLiked)
                                            }
                                            likingIds = likingIds - comment.id
                                        }
                                    }
                                },
                            )
                        }
                        if (isLoadingMore) {
                            item(key = "comments-loading-more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }

                if (comments.isNotEmpty()) {
                    EdgeFade(
                        color = sheetColor,
                        modifier = Modifier.align(Alignment.TopCenter),
                        reversed = false,
                    )
                    EdgeFade(
                        color = sheetColor,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        reversed = true,
                    )
                }
            }
        }

        if (isLoggedIn && (createCommentParams != null || replyTarget?.replyParams != null)) {
            FloatingCommentComposer(
                avatarUrl = viewerThumbnailUrl,
                value = commentText,
                replyingTo = replyTarget?.authorName,
                isPosting = isPosting,
                hasError = postError,
                onValueChange = {
                    if (it.length <= 10_000) commentText = it
                    postError = false
                },
                onCancelReply = {
                    replyTarget = null
                    commentText = ""
                    postError = false
                },
                onSend = {
                    val target = replyTarget
                    val params = target?.replyParams ?: createCommentParams
                        ?: return@FloatingCommentComposer
                    val submittedText = commentText
                    scope.launch {
                        isPosting = true
                        postError = false
                        withContext(Dispatchers.IO) {
                            if (target != null) {
                                YouTube.createCommentReply(params, submittedText)
                            } else {
                                YouTube.createComment(params, submittedText)
                            }
                        }
                            .onSuccess {
                                commentText = ""
                                replyTarget = null
                                if (target != null) {
                                    repliesByComment = repliesByComment - target.id
                                    replyContinuations = replyContinuations - target.id
                                    expandedReplyIds = expandedReplyIds - target.id
                                }
                                loadInitial()
                            }
                            .onFailure { postError = true }
                        isPosting = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.error),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.comments_unavailable),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(50)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun EdgeFade(
    color: Color,
    reversed: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (reversed) 76.dp else 18.dp)
            .background(
                Brush.verticalGradient(
                    if (reversed) listOf(Color.Transparent, color) else listOf(color, Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun FloatingCommentComposer(
    avatarUrl: String?,
    value: String,
    replyingTo: String?,
    isPosting: Boolean,
    hasError: Boolean,
    onValueChange: (String) -> Unit,
    onCancelReply: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (hasError) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(50),
                shadowElevation = 3.dp,
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.comment_post_failed),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        replyingTo?.let { authorName ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(50),
                shadowElevation = 3.dp,
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 14.dp, end = 3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.replying_to, authorName),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(34.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.cancel_reply),
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(30.dp),
            shadowElevation = 10.dp,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(30.dp)),
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                )
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 11.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    minLines = 1,
                    maxLines = 5,
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.add_comment),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                FilledIconButton(
                    enabled = value.isNotBlank() && !isPosting,
                    onClick = onSend,
                    modifier = Modifier.size(44.dp),
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.send),
                            contentDescription = stringResource(R.string.send),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: YouTubeComment,
    replies: List<YouTubeComment>,
    isExpanded: Boolean,
    isLoadingReplies: Boolean,
    hasMoreReplies: Boolean,
    isLiked: Boolean,
    likeCount: String?,
    isLiking: Boolean,
    isLoggedIn: Boolean,
    containerColor: Color,
    onToggleReplies: () -> Unit,
    onLoadMoreReplies: () -> Unit,
    onReply: () -> Unit,
    onToggleLike: () -> Unit,
) {
    Surface(
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            CommentIdentityRow(comment)
            Spacer(Modifier.height(9.dp))
            Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (comment.isPinned) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = stringResource(R.string.pinned),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                IconButton(
                    onClick = onToggleLike,
                    enabled = isLoggedIn && !isLiking &&
                        (if (isLiked) comment.unlikeAction else comment.likeAction) != null,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.thumb_up),
                        contentDescription = stringResource(R.string.like_comment),
                        modifier = Modifier.size(18.dp),
                        tint = if (isLiked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                likeCount?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isLoggedIn && comment.replyParams != null) {
                    TextButton(onClick = onReply, shape = RoundedCornerShape(50)) {
                        Text(stringResource(R.string.reply))
                    }
                }
                Spacer(Modifier.weight(1f))
                if (comment.replyContinuation != null) {
                    TextButton(onClick = onToggleReplies, shape = RoundedCornerShape(50)) {
                        if (isLoadingReplies) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (isExpanded) R.drawable.expand_less else R.drawable.expand_more,
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isExpanded) {
                                stringResource(R.string.hide_replies)
                            } else {
                                stringResource(R.string.comment_replies, comment.replyCount.orEmpty())
                            },
                        )
                    }
                }
            }

            if (isExpanded) {
                replies.forEach { reply ->
                    Spacer(Modifier.height(8.dp))
                    ReplyRow(reply)
                }
                if (hasMoreReplies) {
                    TextButton(
                        onClick = onLoadMoreReplies,
                        enabled = !isLoadingReplies,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.show_more_replies))
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentIdentityRow(comment: YouTubeComment) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = comment.authorThumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.authorName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (comment.isCreator) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(comment.publishedTime)
                    if (comment.isVerified) append("  ✓")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReplyRow(reply: YouTubeComment) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp),
    ) {
        Row(modifier = Modifier.padding(11.dp)) {
            AsyncImage(
                model = reply.authorThumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reply.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = reply.publishedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(text = reply.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
