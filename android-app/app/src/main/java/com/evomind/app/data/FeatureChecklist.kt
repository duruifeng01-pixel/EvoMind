package com.evomind.app.data

data class FeatureChecklistItem(
    val title: String,
    val status: String,
    val note: String
)

object FeatureChecklist {
    fun items(): List<FeatureChecklistItem> = listOf(
        FeatureChecklistItem("手机号/微信登录", "已接入演示接口", "后续接短信网关与微信SDK"),
        FeatureChecklistItem("信息源OCR导入", "已接入演示接口", "后续替换国内OCR SDK"),
        FeatureChecklistItem("认知卡片与脑图", "已接入演示接口", "后续接入真实模型"),
        FeatureChecklistItem("支付与订阅", "已接入演示接口", "后续接微信支付与支付宝SDK"),
        FeatureChecklistItem("隐私导出与注销", "已接入演示接口", "后续接真实数据清理流程")
    )
}
