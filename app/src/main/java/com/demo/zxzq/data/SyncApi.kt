package com.demo.zxzq.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 交易操作日志（uiOps）与服务器同步。整份覆盖式：push 整个列表，pull 整个列表。
 * 用 HttpURLConnection（风格同 QuoteRepository），零第三方依赖。任何失败均静默（离线可用）。
 */
object SyncApi {

    /** 拉取服务器 uiOps；成功返回列表（可能为空），失败/离线返回 null（上层回退本地）。 */
    suspend fun pull(): List<SeedEvent>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SyncConfig.BASE_URL}/trades?userId=${SyncConfig.USER_ID}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            try {
                if (conn.responseCode != 200) return@withContext null
                val text = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                val opsArr = JSONObject(text).getJSONArray("ops")
                SeedEventJson.decode(opsArr.toString())
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 覆盖上传整份 uiOps；成功 true，失败 false（不抛）。 */
    suspend fun push(ops: List<SeedEvent>): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("userId", SyncConfig.USER_ID)
                .put("ops", JSONArray(SeedEventJson.encode(ops)))
                .toString()
            val conn = (URL("${SyncConfig.BASE_URL}/trades").openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("x-passcode", SyncConfig.PASSCODE)
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                conn.responseCode == 200
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            false
        }
    }
}
