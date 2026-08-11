package com.demo.zxzq.data

/**
 * A 股交易费率配置（演示口径，集中一处，改费率只改这里）。
 *
 * - 佣金：双边收，按成交额比例，最低 5 元
 * - 印花税：仅卖出单边
 * - 过户费：双边（沪深统一按此简化）
 * - 红利税：卖出时按持股时长补扣（≤1月 20% / ≤1年 10% / >1年 0%）
 */
object FeeConfig {
    const val COMMISSION_RATE = 0.0002854   // 佣金 0.028540%
    const val COMMISSION_MIN = 5.0          // 佣金最低 5 元
    const val STAMP_RATE = 0.0005           // 印花税 0.05%（卖出）
    const val TRANSFER_RATE = 0.00001       // 过户费 0.001%（双边）

    /** 佣金 = max(成交额×费率, 最低 5 元)。 */
    fun commission(turnover: Double): Double = maxOf(turnover * COMMISSION_RATE, COMMISSION_MIN)

    /** 印花税（卖出）。 */
    fun stampTax(turnover: Double): Double = turnover * STAMP_RATE

    /** 过户费。 */
    fun transferFee(turnover: Double): Double = turnover * TRANSFER_RATE

    /** 红利税率：按持股天数。≤30 天 20%，≤365 天 10%，超过 1 年免税。 */
    fun dividendTaxRate(days: Long): Double = when {
        days <= 30 -> 0.20
        days <= 365 -> 0.10
        else -> 0.0
    }
}
