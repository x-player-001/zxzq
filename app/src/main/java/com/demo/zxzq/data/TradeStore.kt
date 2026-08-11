package com.demo.zxzq.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "trade_store")

/**
 * 本地持久化 UI 操作日志（List<SeedEvent>），作服务器同步的离线兜底。
 * 完整状态由 TradeRepository 用 seedEvents + uiOps 重放重建。编解码复用 SeedEventJson。
 */
class TradeStore(context: Context) {

    private val appContext = context.applicationContext
    private val key = stringPreferencesKey("ui_ops_json")

    /** 读取 UI 操作日志；首次/旧格式/解析失败均返回 null，由上层回退。 */
    suspend fun load(): List<SeedEvent>? {
        val json = appContext.dataStore.data.first()[key] ?: return null
        return runCatching { SeedEventJson.decode(json) }.getOrNull()
    }

    suspend fun save(ops: List<SeedEvent>) {
        appContext.dataStore.edit { it[key] = SeedEventJson.encode(ops) }
    }
}
