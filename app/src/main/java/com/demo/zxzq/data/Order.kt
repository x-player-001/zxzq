package com.demo.zxzq.data

/** 买卖方向。 */
enum class OrderSide { BUY, SELL }

/** 委托状态：已报 / 已成 / 已撤。 */
enum class OrderStatus { REPORTED, FILLED, CANCELED }

/**
 * 委托单。下单生成一条「已报」委托，可在撤单页手动成交（→已成）或撤单（→已撤）。
 * 成交时才真实变更持仓与资金（挂单模型）。
 */
data class Order(
    val id: String,          // 唯一委托号
    val code: String,        // 行情代码，如 sh601658
    val name: String,        // 证券名称
    val side: OrderSide,
    val price: Double,       // 委托价
    val qty: Long,           // 委托数量（100 的整数倍）
    val status: OrderStatus,
    val time: Long,          // 委托时间戳（毫秒）
)

/** 下单 / 成交 / 撤单结果：成功，或带原因的失败。 */
sealed interface OrderResult {
    data object Success : OrderResult
    data class Failure(val reason: String) : OrderResult
}

/**
 * 一只股一轮持仓（建仓→全部清仓）的已实现盈亏记录，用于「已清仓证券」页。
 * 同一只股同一轮的多次卖出合并到同一条（累加）；全部清仓后 partial=false 封口。
 */
data class ClosedPosition(
    val code: String,
    val name: String,
    val soldShares: Long,       // 累计卖出股数（多次卖出累加）
    val soldCost: Double,       // 累计卖出成本 = Σ(每次股数×当时加权成本，含费)
    val soldTurnover: Double,   // 累计卖出成交额（不扣费），用于卖出均价
    val proceeds: Double,       // 累计卖出净所得（已扣佣金/印花/过户）
    val dividend: Double,       // 全部清仓才结算：该股累计分红
    val dividendTax: Double,    // 全部清仓才结算：补缴的红利税
    val firstBuyDate: String,   // 建仓日
    val closeDate: String,      // 最近一次卖出日
    val partial: Boolean,       // true=部分清仓（未封口，可继续合并），false=全部清仓
) {
    /** 已实现盈亏 = 累计卖出净所得 − 累计成本 + 分红 − 红利税。 */
    val realizedPnl: Double get() = proceeds - soldCost + dividend - dividendTax
    val pnlPct: Double get() = if (soldCost == 0.0) 0.0 else realizedPnl / soldCost * 100
    /** 买入均价：已卖股的含费加权成本均价。 */
    val buyAvg: Double get() = if (soldShares == 0L) 0.0 else soldCost / soldShares
    /** 卖出均价：纯成交均价（不扣费）。 */
    val sellAvg: Double get() = if (soldShares == 0L) 0.0 else soldTurnover / soldShares
}

/**
 * 交易模块的聚合状态。TradeRepository 以 StateFlow 暴露，交易页/我的页/交易主页共用，
 * 下单成交后一次重组带动三页联动。默认值必须安全（异步恢复前的初帧）。
 */
data class TradeState(
    val holdings: List<Holding> = emptyList(),
    val cash: Double = 0.0,
    val orders: List<Order> = emptyList(),
    val statements: List<StatementItem> = emptyList(), // 资金流水（买卖成交 / 分红入账），倒序展示
    val closed: List<ClosedPosition> = emptyList(),    // 已清仓证券（历史盈亏）
    val uiOpCount: Int = 0,                            // 可撤销的 UI 操作数（>0 时最新一笔可删）
    val todaySells: List<TodaySell> = emptyList(),     // 今日卖出明细（算当日盈亏今卖部分，跨日重放自动空）
)

/** 今日卖出一笔：算当日盈亏"今卖 = (卖价−昨收)×股数"用。 */
data class TodaySell(
    val code: String,
    val price: Double,      // 卖出价
    val prevClose: Double,  // 卖出当天昨收
    val qty: Long,          // 卖出股数
)
