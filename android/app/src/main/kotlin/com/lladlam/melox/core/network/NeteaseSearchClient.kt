package com.lladlam.melox.core.network

import com.lladlam.melox.core.model.SearchSong
import java.io.IOException
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class NeteaseSearchClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    suspend fun searchSongs(
        keywords: String,
        limit: Int = 30,
    ): List<SearchSong> = withContext(Dispatchers.IO) {
        val query = keywords.trim()
        if (query.isEmpty()) return@withContext emptyList()

        val payload = JSONObject()
            .put("s", query)
            .put("type", 1)
            .put("limit", limit.coerceIn(1, 50))
            .put("offset", 0)

        val response = eapi(
            uri = "/api/search/get",
            data = payload,
        )

        val result = response.optJSONObject("result") ?: return@withContext emptyList()
        val songs = result.optJSONArray("songs") ?: JSONArray()
        buildList {
            for (index in 0 until songs.length()) {
                val song = songs.optJSONObject(index) ?: continue
                val id = song.optLong("id", -1L)
                if (id <= 0L) continue

                val artistsArray = song.optJSONArray("ar")
                    ?: song.optJSONArray("artists")
                    ?: JSONArray()
                val artists = buildList {
                    for (artistIndex in 0 until artistsArray.length()) {
                        artistsArray.optJSONObject(artistIndex)
                            ?.optString("name")
                            ?.takeIf(String::isNotBlank)
                            ?.let(::add)
                    }
                }.joinToString(" / ")

                val albumObject = song.optJSONObject("al")
                    ?: song.optJSONObject("album")
                val album = albumObject?.optString("name").orEmpty()
                val artwork = albumObject
                    ?.optString("picUrl")
                    ?.takeIf(String::isNotBlank)
                    ?: albumObject
                        ?.optString("blurPicUrl")
                        ?.takeIf(String::isNotBlank)

                add(
                    SearchSong(
                        id = id,
                        name = song.optString("name", "未知歌曲"),
                        artists = artists.ifBlank { "未知歌手" },
                        album = album,
                        artworkUrl = artwork,
                    ),
                )
            }
        }
    }

    private fun eapi(
        uri: String,
        data: JSONObject,
    ): JSONObject {
        val timestampMillis = System.currentTimeMillis()
        val header = JSONObject()
            .put("os", "ios")
            .put("appver", "9.0.90")
            .put("osver", "18.0")
            .put("buildver", (timestampMillis / 1_000L).toString())
            .put("channel", "distribution")
            .put("requestId", "${timestampMillis}_0000")
            .put("__csrf", "")

        val requestData = JSONObject(data.toString())
            .put("header", header)
            .put("e_r", false)
        val json = requestData.toString()
        val digest = md5Hex("nobody${uri}use${json}md5forencrypt")
        val encryptedPayload = "$uri-36cd479b6b5-$json-36cd479b6b5-$digest"
        val params = aesEcbEncrypt(
            encryptedPayload.toByteArray(Charsets.UTF_8),
            "e82ckenh8dichen8".toByteArray(Charsets.UTF_8),
        ).toHexUppercase()

        val path = uri.replace("/api/", "/eapi/")
        val request = Request.Builder()
            .url("https://interface.music.163.com$path")
            .header(
                "User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) " +
                    "AppleWebKit/605.1.15 Mobile/15E148",
            )
            .header("Accept", "*/*")
            .post(
                FormBody.Builder()
                    .add("params", params)
                    .build(),
            )
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                throw IOException("网易云请求失败：HTTP ${response.code}")
            }
            if (body.isBlank()) {
                throw IOException("网易云返回了空响应")
            }

            val jsonObject = JSONObject(body)
            val code = jsonObject.optInt("code", response.code)
            if (code !in 200..299) {
                val message = jsonObject.optString("message")
                    .ifBlank { jsonObject.optString("msg") }
                    .ifBlank { "请求失败" }
                throw IOException("网易云请求失败（$code）：$message")
            }
            return jsonObject
        }
    }

    private fun md5Hex(value: String): String =
        MessageDigest.getInstance("MD5")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun aesEcbEncrypt(
        data: ByteArray,
        key: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    private fun ByteArray.toHexUppercase(): String =
        joinToString("") { byte -> "%02X".format(byte) }
}
