package com.evomind.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application类
 */
@HiltAndroidApp
class EvoMindApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化全局配置
        initializeApp()
    }

    private fun initializeApp() {
        // 日志初始化
        android.util.Log.d("EvoMindApp", "应用启动")
    }
}
