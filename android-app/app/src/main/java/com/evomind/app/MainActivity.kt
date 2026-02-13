package com.evomind.app

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<FrameLayout>(R.id.container)
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)

        fun renderPage(title: String, subtitle: String) {
            container.removeAllViews()
            val card = MaterialCardView(this).apply {
                radius = 28f
                setCardBackgroundColor(0xCC1B2A40.toInt())
                useCompatPadding = true
                val layout = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(48, 64, 48, 64)
                }
                val tv1 = TextView(context).apply {
                    text = title
                    textSize = 30f
                    setTextColor(0xFFEAF2FF.toInt())
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                val tv2 = TextView(context).apply {
                    text = subtitle
                    textSize = 15f
                    setTextColor(0xFFB7C8E6.toInt())
                    setPadding(0, 20, 0, 0)
                }
                val tv3 = TextView(context).apply {
                    text = "AI生成，仅供参考"
                    textSize = 13f
                    setTextColor(0xFFAFC3E8.toInt())
                    setPadding(0, 24, 0, 0)
                }
                layout.addView(tv1)
                layout.addView(tv2)
                layout.addView(tv3)
                addView(layout)
            }
            container.addView(card)
        }

        renderPage("EvoMind 进化意志", "输入-认知-内化-行动-反馈，构建你的成长闭环")

        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> renderPage("首页", "今日认知卡片、每日一问、挑战任务")
                R.id.nav_sources -> renderPage("信息源", "支持截图识别导入与手动链接导入")
                R.id.nav_library -> renderPage("语料库", "本地优先存储，支持搜索与导出")
                R.id.nav_group -> renderPage("小组", "匿名互助讨论，展示优质成果")
                R.id.nav_profile -> renderPage("我的", "订阅、资产、设置与帮助反馈")
            }
            true
        }
    }
}
