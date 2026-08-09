package com.lladlam.melox.ui.library

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class MeloXDetailPalette(
    val background: Color,
    val prefersDarkAppearance: Boolean,
) {
    companion object {
        val LightFallback = MeloXDetailPalette(Color(0xFFE0E0E0), false)
        val DarkFallback = MeloXDetailPalette(Color(0xFF292929), true)
    }
}

/**
 * Pixel-for-pixel port of the palette branch used by
 * ArtworkAccentColorProvider.makeDetailPalette in MeloX iOS.
 *
 * The original downsamples artwork to <= 160 px, area-averages the image,
 * uses a 0.52 luminance split, then mixes the source average toward 0.055
 * for dark artwork or 0.94 for light artwork.
 */
internal object MeloXDetailPaletteProvider {
    private const val TARGET_SIZE = 160
    private val http = OkHttpClient()
    private val cache = ConcurrentHashMap<String, MeloXDetailPalette>()

    suspend fun paletteFor(url: String?): MeloXDetailPalette {
        val source = url?.takeIf(String::isNotBlank) ?: return MeloXDetailPalette.LightFallback
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
                    val maximum = maxOf(decoded.width, decoded.height)
                    val scale = if (maximum > TARGET_SIZE) TARGET_SIZE.toFloat() / maximum else 1f
                    val scaled = if (scale < 1f) {
                        Bitmap.createScaledBitmap(
                            decoded,
                            (decoded.width * scale).toInt().coerceAtLeast(1),
                            (decoded.height * scale).toInt().coerceAtLeast(1),
                            true,
                        )
                    } else decoded
                    try {
                        makePalette(scaled)
                    } finally {
                        if (scaled !== decoded) scaled.recycle()
                        decoded.recycle()
                    }
                }
            }.getOrElse { MeloXDetailPalette.LightFallback }
            cache[source] = palette
            palette
        }
    }

    private fun makePalette(bitmap: Bitmap): MeloXDetailPalette {
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var count = 0L
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                r += ((pixel shr 16) and 0xFF) / 255.0
                g += ((pixel shr 8) and 0xFF) / 255.0
                b += (pixel and 0xFF) / 255.0
                count += 1
            }
        }
        if (count == 0L) return MeloXDetailPalette.LightFallback
        r /= count
        g /= count
        b /= count

        val luminance = r * 0.2126 + g * 0.7152 + b * 0.0722
        val dark = luminance < 0.52
        val mix = if (dark) 0.055 else 0.94
        val sourceWeight = if (dark) 0.38 else 0.30
        val neutralWeight = if (dark) 0.62 else 0.70
        val background = Color(
            red = (r * sourceWeight + mix * neutralWeight).toFloat().coerceIn(0f, 1f),
            green = (g * sourceWeight + mix * neutralWeight).toFloat().coerceIn(0f, 1f),
            blue = (b * sourceWeight + mix * neutralWeight).toFloat().coerceIn(0f, 1f),
            alpha = 1f,
        )
        return MeloXDetailPalette(background, dark)
    }

    private fun optimizedArtworkUrl(source: String): String {
        if (!source.contains(".music.126.net")) return source
        val withoutParam = source
            .replace(Regex("([?&])param=[^&]*&?", RegexOption.IGNORE_CASE)) { match ->
                if (match.value.startsWith("?")) "?" else "&"
            }
            .trimEnd('?', '&')
        val separator = if (withoutParam.contains('?')) '&' else '?'
        return "$withoutParam${separator}param=160y160"
    }
}
