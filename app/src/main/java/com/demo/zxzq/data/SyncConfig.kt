package com.demo.zxzq.data

/**
 * 服务器同步配置。单用户固定账号，不做登录。
 * 演示阶段常量内联；生产应移出源码。
 */
object SyncConfig {
    const val BASE_URL = "https://api.129club.cloud/zxzq/api"
    const val PASSCODE = "zxzq-sync-8f3a2c"   // 与服务器 .env 的 PASSCODE 一致
    const val USER_ID = "demo-user"
}
