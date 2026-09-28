package com.metrolist.innertube.models.body

import com.metrolist.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentReplyBody(
    val context: Context,
    val createReplyParams: String,
    val commentText: String,
)
