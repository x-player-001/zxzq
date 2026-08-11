package com.demo.zxzq.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * SeedEvent 列表 ↔ JSON 编解码（org.json）。
 * TradeStore（本地 DataStore）与 SyncApi（服务器同步）共用同一格式，避免两套格式漂移。
 * type 字段区分 buy/sell/dividend/deposit。
 */
object SeedEventJson {

    fun encode(ops: List<SeedEvent>): String {
        val arr = JSONArray()
        ops.forEach { e ->
            val o = JSONObject().put("date", e.date)
            when (e) {
                is SeedEvent.Buy -> o.put("type", "buy")
                    .put("code", e.code).put("name", e.name).put("price", e.price)
                    .put("qty", e.qty ?: JSONObject.NULL)
                    .put("amount", e.amount ?: JSONObject.NULL)
                is SeedEvent.Sell -> o.put("type", "sell")
                    .put("code", e.code).put("price", e.price)
                    .put("qty", e.qty ?: JSONObject.NULL)
                    .put("prevClose", e.prevClose ?: JSONObject.NULL)
                is SeedEvent.Dividend -> o.put("type", "dividend")
                    .put("code", e.code).put("perShare", e.perShare)
                is SeedEvent.Deposit -> o.put("type", "deposit")
                    .put("amount", e.amount)
            }
            arr.put(o)
        }
        return arr.toString()
    }

    fun decode(json: String): List<SeedEvent> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val date = o.getString("date")
            when (o.getString("type")) {
                "buy" -> SeedEvent.Buy(
                    date, o.getString("code"), o.getString("name"), o.getDouble("price"),
                    qty = if (o.isNull("qty")) null else o.getLong("qty"),
                    amount = if (o.isNull("amount")) null else o.getDouble("amount"),
                )
                "sell" -> SeedEvent.Sell(
                    date, o.getString("code"), o.getDouble("price"),
                    qty = if (o.isNull("qty")) null else o.getLong("qty"),
                    prevClose = if (o.isNull("prevClose")) null else o.getDouble("prevClose"),
                )
                "dividend" -> SeedEvent.Dividend(date, o.getString("code"), o.getDouble("perShare"))
                "deposit" -> SeedEvent.Deposit(date, o.getDouble("amount"))
                else -> error("unknown event type")
            }
        }
    }
}
