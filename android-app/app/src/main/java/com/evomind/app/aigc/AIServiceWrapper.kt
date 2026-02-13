package com.evomind.app.aigc

import android.content.Context
import com.evomind.app.BuildConfig
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * AI服务封装类
 * 封装对Deepseek API的调用，处理鉴权和错误处理
 */
class AIServiceWrapper(
    private val context: Context
) {
    private val TAG = "AIServiceWrapper"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val API_ENDPOINT = "https://api.deepseek.com/v1/chat/completions"
    private val API_KEY = BuildConfig.DEEPSEEK_API_KEY

    companion object {
        private var instance: AIServiceWrapper? = null

        @JvmStatic
        fun getInstance(context: Context): AIServiceWrapper {
            return instance ?: synchronized(this) {
                AIServiceWrapper(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    data class AIResponse(
        val success: Boolean,
        val content: String,
        val tokenUsage: Int,
        val errorMessage: String?
    )

    data class AIMessage(
        val content: String,
        val role: Role
    )

    enum class Role {
        USER, ASSISTANT, SYSTEM
    }

    fun chat(
        messages: List<AIMessage>,
        temperature: Double = 0.7,
        onSuccess: (AIResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val jsonBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("temperature", temperature)
                put("max_tokens", 4096)

                val messagesArray = JSONArray()
                messages.forEach { message ->
                    val messageObj = JSONObject().apply {
                        put("role", message.role.name.lowercase())
                        put("content", message.content)
                    }
                    messagesArray.put(messageObj)
                }
                put("messages", messagesArray)
            }

            val requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                jsonBody.toString()
            )

            val request = Request.Builder()
                .url(API_ENDPOINT)
                .header("Authorization", "Bearer $API_KEY")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onError("网络请求失败: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val responseBody = response.body()?.string()
                            if (responseBody.isNullOrEmpty()) {
                                onError("响应为空")
                                return
                            }

                            val jsonResponse = JSONObject(responseBody)
                            val choices = jsonResponse.getJSONArray("choices")
                            if (choices.length() > 0) {
                                val firstChoice = choices.getJSONObject(0)
                                val message = firstChoice.getJSONObject("message")
                                val content = message.getString("content")

                                val usage = jsonResponse.optJSONObject("usage")
                                val tokenUsage = usage?.optInt("total_tokens") ?: 0

                                onSuccess(
                                    AIResponse(
                                        success = true,
                                        content = content,
                                        tokenUsage = tokenUsage,
                                        errorMessage = null
                                    )
                                )
                            } else {
                                onError("没有返回有效的回复")
                            }
                        } catch (e: Exception) {
                            onError("解析响应失败: ${e.message}")
                        }
                    } else {
                        val errorBody = response.body()?.string()
                        val errorMessage = try {
                            val errorJson = JSONObject(errorBody ?: "")
                            errorJson.getString("error")
                        } catch (e: Exception) {
                            "请求失败: ${response.code()}"
                        }
                        onError(errorMessage)
                    }
                }
            })
        } catch (e: Exception) {
            onError("创建请求失败: ${e.message}")
        }
    }
}
