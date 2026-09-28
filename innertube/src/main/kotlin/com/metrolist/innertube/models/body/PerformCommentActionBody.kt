package com.metrolist.innertube.models.body

import com.metrolist.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PerformCommentActionBody(
    val context: Context,
    val actions: List<String>,
)
