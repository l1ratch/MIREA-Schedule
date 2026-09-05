package com.jetbrains.kmpapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubContributor(
    val login: String,
    val id: Long? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
    val contributions: Int = 0,
    val role: String? = null
)
