package com.demo.zxzq

import android.app.Application
import com.demo.zxzq.data.TradeRepository

/** 应用入口：初始化交易仓库（恢复本地数据 / 首启播种）。 */
class ZxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TradeRepository.init(this)
    }
}
