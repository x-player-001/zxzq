package com.demo.zxzq.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 自绘线性图标，形状对齐中信证券参考图（比 Material 内置更接近原版）。
 * 统一 24x24 viewport，线宽 ~2，圆角端点。
 */
object ZxIcons {

    private inline fun stroke(
        name: String,
        block: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply(block).build()

    private fun androidx.compose.ui.graphics.vector.ImageVector.Builder.line(
        pathData: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ) = path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathData
    )

    /** 六边形齿轮（设置）——参考图右上角。 */
    val Settings: ImageVector = stroke("Settings") {
        line {
            // 外六边形
            moveTo(12f, 3f)
            lineTo(19f, 7f)
            lineTo(19f, 15f)
            lineTo(12f, 19f)
            lineTo(5f, 15f)
            lineTo(5f, 7f)
            close()
        }
        line {
            // 内圆（用八边近似）
            moveTo(12f, 9f)
            curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
            curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
            curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
            curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
            close()
        }
    }

    /** 耳机客服。 */
    val Headset: ImageVector = stroke("Headset") {
        line {
            // 头梁
            moveTo(5f, 13f)
            verticalLineTo(11f)
            curveTo(5f, 7.13f, 8.13f, 4f, 12f, 4f)
            curveTo(15.87f, 4f, 19f, 7.13f, 19f, 11f)
            verticalLineTo(13f)
        }
        line {
            // 左耳罩
            moveTo(5f, 12f)
            lineTo(7f, 12f)
            lineTo(7f, 17f)
            lineTo(5f, 17f)
            curveTo(4.45f, 17f, 4f, 16.55f, 4f, 16f)
            verticalLineTo(13f)
            curveTo(4f, 12.45f, 4.45f, 12f, 5f, 12f)
            close()
        }
        line {
            // 右耳罩
            moveTo(19f, 12f)
            lineTo(17f, 12f)
            lineTo(17f, 17f)
            lineTo(19f, 17f)
            curveTo(19.55f, 17f, 20f, 16.55f, 20f, 16f)
            verticalLineTo(13f)
            curveTo(20f, 12.45f, 19.55f, 12f, 19f, 12f)
            close()
        }
    }

    /** 信封（消息）。 */
    val Mail: ImageVector = stroke("Mail") {
        line {
            moveTo(4f, 6f)
            lineTo(20f, 6f)
            curveTo(20.55f, 6f, 21f, 6.45f, 21f, 7f)
            verticalLineTo(17f)
            curveTo(21f, 17.55f, 20.55f, 18f, 20f, 18f)
            lineTo(4f, 18f)
            curveTo(3.45f, 18f, 3f, 17.55f, 3f, 17f)
            verticalLineTo(7f)
            curveTo(3f, 6.45f, 3.45f, 6f, 4f, 6f)
            close()
        }
        line {
            moveTo(4f, 7f)
            lineTo(12f, 13f)
            lineTo(20f, 7f)
        }
    }

    /** 搜索放大镜。 */
    val Search: ImageVector = stroke("Search") {
        line {
            moveTo(11f, 4f)
            curveTo(14.31f, 4f, 17f, 6.69f, 17f, 10f)
            curveTo(17f, 13.31f, 14.31f, 16f, 11f, 16f)
            curveTo(7.69f, 16f, 5f, 13.31f, 5f, 10f)
            curveTo(5f, 6.69f, 7.69f, 4f, 11f, 4f)
            close()
        }
        line {
            moveTo(15.5f, 14.5f)
            lineTo(20f, 19f)
        }
    }

    /** 行情：折线走势。 */
    val Chart: ImageVector = stroke("Chart") {
        line {
            moveTo(4f, 15f)
            lineTo(9f, 10f)
            lineTo(13f, 13f)
            lineTo(20f, 6f)
        }
    }

    /** 资讯：对话气泡。 */
    val Chat: ImageVector = stroke("Chat") {
        line {
            moveTo(6f, 5f)
            lineTo(18f, 5f)
            curveTo(19.1f, 5f, 20f, 5.9f, 20f, 7f)
            verticalLineTo(14f)
            curveTo(20f, 15.1f, 19.1f, 16f, 18f, 16f)
            lineTo(11f, 16f)
            lineTo(7f, 20f)
            verticalLineTo(16f)
            lineTo(6f, 16f)
            curveTo(4.9f, 16f, 4f, 15.1f, 4f, 14f)
            verticalLineTo(7f)
            curveTo(4f, 5.9f, 4.9f, 5f, 6f, 5f)
            close()
        }
    }

    /** 刷新（环形箭头）。 */
    val Refresh: ImageVector = stroke("Refresh") {
        line {
            moveTo(19f, 8f)
            curveTo(17.7f, 5.6f, 15.05f, 4f, 12f, 4f)
            curveTo(7.58f, 4f, 4f, 7.58f, 4f, 12f)
            curveTo(4f, 16.42f, 7.58f, 20f, 12f, 20f)
            curveTo(15.64f, 20f, 18.71f, 17.57f, 19.65f, 14.25f)
        }
        line {
            moveTo(19f, 4f)
            verticalLineTo(8f)
            horizontalLineTo(15f)
        }
    }

    /** 眼睛（隐藏/显示金额）。 */
    val Eye: ImageVector = stroke("Eye") {
        line {
            moveTo(3f, 12f)
            curveTo(5f, 8f, 8.5f, 6f, 12f, 6f)
            curveTo(15.5f, 6f, 19f, 8f, 21f, 12f)
            curveTo(19f, 16f, 15.5f, 18f, 12f, 18f)
            curveTo(8.5f, 18f, 5f, 16f, 3f, 12f)
            close()
        }
        line {
            moveTo(12f, 9f)
            curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
            curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
            curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
            curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
            close()
        }
    }

    /* ---------- 宫格图标（业务办理 / 我的服务 / 功能专区） ---------- */

    /** 持仓：房形轮廓 + 饼图。 */
    val Holding: ImageVector = stroke("Holding") {
        line {
            moveTo(5f, 9f)
            lineTo(12f, 4f)
            lineTo(19f, 9f)
            verticalLineTo(19f)
            curveTo(19f, 19.55f, 18.55f, 20f, 18f, 20f)
            horizontalLineTo(6f)
            curveTo(5.45f, 20f, 5f, 19.55f, 5f, 19f)
            close()
        }
        line {
            moveTo(12f, 11f)
            verticalLineTo(15f)
            horizontalLineTo(15.5f)
        }
        line {
            moveTo(15.5f, 15f)
            curveTo(15.5f, 13.07f, 13.93f, 11.5f, 12f, 11.5f)
        }
    }

    /** 交易记录：上下相反的两个箭头。 */
    val TradeRecord: ImageVector = stroke("TradeRecord") {
        line {
            moveTo(7f, 5f)
            verticalLineTo(17f)
            moveTo(4f, 8f)
            lineTo(7f, 5f)
            lineTo(10f, 8f)
        }
        line {
            moveTo(17f, 19f)
            verticalLineTo(7f)
            moveTo(14f, 16f)
            lineTo(17f, 19f)
            lineTo(20f, 16f)
        }
    }

    /** 手机开户：手机 + 减号/横线。 */
    val OpenAccount: ImageVector = stroke("OpenAccount") {
        line {
            moveTo(8f, 3f)
            horizontalLineTo(16f)
            curveTo(16.55f, 3f, 17f, 3.45f, 17f, 4f)
            verticalLineTo(20f)
            curveTo(17f, 20.55f, 16.55f, 21f, 16f, 21f)
            horizontalLineTo(8f)
            curveTo(7.45f, 21f, 7f, 20.55f, 7f, 20f)
            verticalLineTo(4f)
            curveTo(7f, 3.45f, 7.45f, 3f, 8f, 3f)
            close()
        }
        line {
            moveTo(10f, 7f)
            horizontalLineTo(13f)
        }
    }

    /** 账户信息：人像 + 列表条。 */
    val AccountInfo: ImageVector = stroke("AccountInfo") {
        line {
            moveTo(8f, 8f)
            curveTo(9.1f, 8f, 10f, 8.9f, 10f, 10f)
            curveTo(10f, 11.1f, 9.1f, 12f, 8f, 12f)
            curveTo(6.9f, 12f, 6f, 11.1f, 6f, 10f)
            curveTo(6f, 8.9f, 6.9f, 8f, 8f, 8f)
            close()
        }
        line {
            moveTo(4.5f, 17f)
            curveTo(4.5f, 14.5f, 6f, 13.5f, 8f, 13.5f)
            curveTo(10f, 13.5f, 11.5f, 14.5f, 11.5f, 17f)
        }
        line {
            moveTo(14f, 9f)
            horizontalLineTo(19f)
            moveTo(14f, 13f)
            horizontalLineTo(19f)
        }
    }

    /** 问卷回访：文档 + 对勾。 */
    val Survey: ImageVector = stroke("Survey") {
        line {
            moveTo(6f, 4f)
            horizontalLineTo(18f)
            curveTo(18.55f, 4f, 19f, 4.45f, 19f, 5f)
            verticalLineTo(19f)
            curveTo(19f, 19.55f, 18.55f, 20f, 18f, 20f)
            horizontalLineTo(6f)
            curveTo(5.45f, 20f, 5f, 19.55f, 5f, 19f)
            verticalLineTo(5f)
            curveTo(5f, 4.45f, 5.45f, 4f, 6f, 4f)
            close()
        }
        line {
            moveTo(8.5f, 12f)
            lineTo(11f, 14.5f)
            lineTo(15.5f, 9.5f)
        }
    }

    /** 我的权限：盾 + 钥匙孔。 */
    val Shield: ImageVector = stroke("Shield") {
        line {
            moveTo(12f, 3f)
            lineTo(19f, 6f)
            verticalLineTo(11f)
            curveTo(19f, 15.5f, 16f, 19f, 12f, 21f)
            curveTo(8f, 19f, 5f, 15.5f, 5f, 11f)
            verticalLineTo(6f)
            close()
        }
        line {
            moveTo(12f, 10f)
            curveTo(12.83f, 10f, 13.5f, 10.67f, 13.5f, 11.5f)
            curveTo(13.5f, 12.1f, 13.15f, 12.6f, 12.65f, 12.85f)
            lineTo(13f, 15f)
            horizontalLineTo(11f)
            lineTo(11.35f, 12.85f)
            curveTo(10.85f, 12.6f, 10.5f, 12.1f, 10.5f, 11.5f)
            curveTo(10.5f, 10.67f, 11.17f, 10f, 12f, 10f)
            close()
        }
    }

    /** 我的订阅：书签。 */
    val Bookmark: ImageVector = stroke("Bookmark") {
        line {
            moveTo(7f, 4f)
            horizontalLineTo(17f)
            verticalLineTo(20f)
            lineTo(12f, 16f)
            lineTo(7f, 20f)
            close()
        }
        line {
            moveTo(9.5f, 9f)
            horizontalLineTo(14.5f)
        }
    }

    /** 已购工具：盒子/下载。 */
    val Tools: ImageVector = stroke("Tools") {
        line {
            moveTo(5f, 8f)
            horizontalLineTo(19f)
            verticalLineTo(18f)
            curveTo(19f, 18.55f, 18.55f, 19f, 18f, 19f)
            horizontalLineTo(6f)
            curveTo(5.45f, 19f, 5f, 18.55f, 5f, 18f)
            close()
        }
        line {
            moveTo(12f, 6f)
            verticalLineTo(13f)
            moveTo(9.5f, 10.5f)
            lineTo(12f, 13f)
            lineTo(14.5f, 10.5f)
        }
    }

    /** 浏览历史：时钟 + 回退箭头。 */
    val History: ImageVector = stroke("History") {
        line {
            moveTo(12f, 5f)
            curveTo(15.87f, 5f, 19f, 8.13f, 19f, 12f)
            curveTo(19f, 15.87f, 15.87f, 19f, 12f, 19f)
            curveTo(8.13f, 19f, 5f, 15.87f, 5f, 12f)
        }
        line {
            moveTo(5f, 8f)
            verticalLineTo(12f)
            horizontalLineTo(9f)
        }
        line {
            moveTo(12f, 9f)
            verticalLineTo(12.5f)
            lineTo(14.5f, 14f)
        }
    }

    /** 权益专区：钻石 + 内折线。 */
    val Diamond: ImageVector = stroke("Diamond") {
        line {
            moveTo(6f, 9f)
            horizontalLineTo(18f)
            lineTo(12f, 19f)
            close()
        }
        line {
            moveTo(6f, 9f)
            lineTo(8f, 6f)
            horizontalLineTo(16f)
            lineTo(18f, 9f)
        }
        line {
            moveTo(9f, 9f)
            lineTo(12f, 13f)
            lineTo(15f, 9f)
        }
    }

    /** 增值专区：上升趋势箭头。 */
    val Trending: ImageVector = stroke("Trending") {
        line {
            moveTo(4f, 16f)
            lineTo(9f, 11f)
            lineTo(13f, 15f)
            lineTo(20f, 8f)
        }
        line {
            moveTo(20f, 12f)
            verticalLineTo(8f)
            horizontalLineTo(16f)
        }
    }

    /** 模拟交易：六边形 + 内叉。 */
    val Simulate: ImageVector = stroke("Simulate") {
        line {
            moveTo(12f, 3f)
            lineTo(19f, 7f)
            verticalLineTo(15f)
            lineTo(12f, 19f)
            lineTo(5f, 15f)
            verticalLineTo(7f)
            close()
        }
        line {
            moveTo(9.5f, 9.5f)
            lineTo(14.5f, 14.5f)
            moveTo(14.5f, 9.5f)
            lineTo(9.5f, 14.5f)
        }
    }

    /** 智能盯盘：铃铛 + 闪电。 */
    val Alarm: ImageVector = stroke("Alarm") {
        line {
            moveTo(6f, 16f)
            curveTo(6f, 16f, 7f, 15f, 7f, 12f)
            verticalLineTo(10f)
            curveTo(7f, 7.24f, 9.24f, 5f, 12f, 5f)
            curveTo(14.76f, 5f, 17f, 7.24f, 17f, 10f)
            verticalLineTo(12f)
            curveTo(17f, 15f, 18f, 16f, 18f, 16f)
            close()
        }
        line {
            moveTo(10f, 19f)
            curveTo(10.5f, 19.6f, 11.2f, 20f, 12f, 20f)
            curveTo(12.8f, 20f, 13.5f, 19.6f, 14f, 19f)
        }
    }

    /** 综合账户：人像 + 底座。 */
    val AccountPerson: ImageVector = stroke("AccountPerson") {
        line {
            moveTo(12f, 6f)
            curveTo(13.38f, 6f, 14.5f, 7.12f, 14.5f, 8.5f)
            curveTo(14.5f, 9.88f, 13.38f, 11f, 12f, 11f)
            curveTo(10.62f, 11f, 9.5f, 9.88f, 9.5f, 8.5f)
            curveTo(9.5f, 7.12f, 10.62f, 6f, 12f, 6f)
            close()
        }
        line {
            moveTo(6.5f, 18f)
            curveTo(6.5f, 14.5f, 9f, 13f, 12f, 13f)
            curveTo(15f, 13f, 17.5f, 14.5f, 17.5f, 18f)
        }
    }
}
