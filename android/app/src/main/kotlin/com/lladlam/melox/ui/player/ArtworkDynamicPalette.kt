package com.lladlam.melox.ui.player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class ArtworkDynamicPalette(
    val cells: List<Color>,
    val average: Color,
) {
    companion object {
        val Fallback = ArtworkDynamicPalette(
            cells = List(9) { Color(0xFF5B4B45) },
            average = Color(0xFF5B4B45),
        )
    }
}

/**
 * Android counterpart of MeloX iOS ArtworkAccentColorProvider.
 *
 * The iOS implementation down-samples artwork to roughly 160 px, samples a
 * 3x3 grid and uses those nine average colors to drive the flowing-light
 * player background. Keep the same data model here instead of reducing an
 * artwork to one dominant swatch.
 */
internal object ArtworkDynamicPaletteProvider {
    private const val GRID = 3
    private const val TARGET_SIZE = 160
    private val cache = ConcurrentHashMap<String, ArtworkDynamicPalette>()
    private val http = OkHttpClient()

    suspend fun paletteFor(url: String?): ArtworkDynamicPalette {
        val source = url?.takeIf(String::isNotBlank) ?: return ArtworkDynamicPalette.Fallback
        cache[source]?.let { return it }

        return withContext(Dispatchers.IO) {
            cache[source]?.let { return@withContext it }
            val palette = runCatching {
                val request = Request.Builder()
                    .url(optimizedArtworkUrl(source))
                    .header("User-Agent", "MeloX-Android/0.1")
                    .build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Artwork HTTP ${response.code}")
                    val bytes = response.body.bytes()
                    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: error("Unable to decode artwork")
                    val scaled = if (decoded.width == TARGET_SIZE && decoded.height == TARGET_SIZE) {
                        decoded
                    } else {
                        Bitmap.createScaledBitmap(decoded, TARGET_SIZE, TARGET_SIZE, true)
                    }
                    try {
                        makePalette(scaled)
                    } finally {
                        if (scaled !== decoded) scaled.recycle()
                        decoded.recycle()
                    }
                }
            }.getOrElse { ArtworkDynamicPalette.Fallback }

            cache[source] = palette
            palette
        }
    }

    private fun makePalette(bitmap: Bitmap): ArtworkDynamicPalette {
        val width = bitmap.width
        val height = bitmap.height
        val cellWidth = width / GRID
        val cellHeight = height / GRID
        val cells = buildList(GRID * GRID) {
            for (row in 0 until GRID) {
                for (column in 0 until GRID) {
                    val left = column * cellWidth
                    val top = row * cellHeight
                    val right = if (column == GRID - 1) width else (column + 1) * cellWidth
                    val bottom = if (row == GRID - 1) height else (row + 1) * cellHeight
                    add(averageColor(bitmap, left, top, right, bottom))
                }
            }
        }
        return ArtworkDynamicPalette(
            cells = cells,
            average = averageColor(bitmap, 0, 0, width, height),
        )
    }

    private fun averageColor(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ): Color {
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L

        // Sample every second pixel. A 160x160 input is already tiny and this
        // keeps palette extraction cheap enough to perform on every song swap.
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val pixel = bitmap.getPixel(x, y)
                red += (pixel shr 16) and 0xFF
                green += (pixel shr 8) and 0xFF
                blue += pixel and 0xFF
                count += 1
                x += 2
            }
            y += 2
        }

        if (count == 0L) return Color(0xFF5B4B45)
        return Color(
            red = (red.toFloat() / count / 255f).coerceIn(0f, 1f),
            green = (green.toFloat() / count / 255f).coerceIn(0f, 1f),
            blue = (blue.toFloat() / count / 255f).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }

    private fun optimizedArtworkUrl(source: String): String {
        val uri = runCatching { URI(source) }.getOrNull() ?: return source
        if (uri.host?.endsWith(".music.126.net") != true) return source
        val separator = if (source.contains('?')) '&' else '?'
        return if (source.contains("param=")) source else "$source${separator}param=160y160"
    }
}
