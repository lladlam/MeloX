package com.lladlam.melox.ui.player

internal data class MeloXPlayerContainerKey(
    val mediaId: String,
)

internal fun sharedPlayerContainerKey(mediaId: String?): MeloXPlayerContainerKey =
    MeloXPlayerContainerKey(mediaId.orEmpty())
