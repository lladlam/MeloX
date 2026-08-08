package com.lladlam.melox.ui.player

internal data class MeloXSharedArtworkKey(
    val mediaId: String,
)

internal fun sharedArtworkKey(mediaId: String?): MeloXSharedArtworkKey =
    MeloXSharedArtworkKey(mediaId.orEmpty())
