package com.demo.zxzq.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 实时行情拉取——使用新浪财经公开行情接口（无需鉴权）。
 *
 * 请求：GET https://hq.sinajs.cn/list=sh601658,sh601988,...
 * 注意：新浪要求带 Referer 头，否则返回 403；返回体为 GBK 编码。
 *
 * 返回格式（每行）：
 *   var hq_str_sh601658="邮储银行,今开,昨收,现价,最高,最低,...";
 *   下标：0=名称 1=今开 2=昨收 3=现价 ...
 */
object QuoteRepository {

    private const val REFERER = "https://finance.sina.com.cn"

    /** 拉取一组代码的实时行情；失败或缺失的代码不会出现在结果中。 */
    suspend fun fetch(codes: List<String>): Map<String, Quote> = withContext(Dispatchers.IO) {
        if (codes.isEmpty()) return@withContext emptyMap()
        val url = URL("https://hq.sinajs.cn/list=" + codes.joinToString(","))
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Referer", REFERER)
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }
        try {
            if (conn.responseCode != 200) return@withContext emptyMap()
            // 新浪返回 GBK 编码
            val text = BufferedReader(InputStreamReader(conn.inputStream, "GBK")).use { it.readText() }
            parse(text)
        } catch (e: Exception) {
            emptyMap()
        } finally {
            conn.disconnect()
        }
    }

    /** 用行情把静态持仓合成展示视图（现价缺失时用成本价兜底）。 */
    fun toViews(holdings: List<Holding>, quotes: Map<String, Quote>): List<HoldingView> =
        holdings.map { h ->
            val q = quotes[h.code]
            HoldingView(
                holding = h,
                price = q?.price ?: h.cost,
                prevClose = q?.prevClose ?: 0.0,
                hasQuote = q != null,
            )
        }

    private val LINE = Regex("""var hq_str_(\w+)="([^"]*)";""")

    private fun parse(text: String): Map<String, Quote> {
        val result = mutableMapOf<String, Quote>()
        LINE.findAll(text).forEach { m ->
            val code = m.groupValues[1]
            val fields = m.groupValues[2].split(",")
            if (fields.size > 3) {
                val price = fields[3].toDoubleOrNull() ?: return@forEach
                val prevClose = fields[2].toDoubleOrNull() ?: price
                // 停牌等情况现价可能为 0，回退到昨收
                val real = if (price > 0.0) price else prevClose
                if (real > 0.0) result[code] = Quote(code, real, prevClose)
            }
        }
        return result
    }
}
