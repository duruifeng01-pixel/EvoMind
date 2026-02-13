# 01 安卓端专属系统架构方案

## 1. 总体架构（Android Only）

- 客户端：Kotlin + Jetpack（Compose 或 XML 均可，建议 Compose）
- 架构：MVVM + Repository + UseCase
- 本地存储：Room（SQLCipher）+ DataStore（偏好设置）+ 文件沙箱
- 网络层：Retrofit + OkHttp + Kotlinx Serialization
- 后台任务：WorkManager（定时同步、冷数据归档提醒、续费提醒）
- 推送：厂商通道 + FCM 替代方案（以国内厂商为主）
- 崩溃监控：友盟/bugly（国内合规）

## 2. 模块化设计

- `feature-auth`：手机号验证码登录、微信登录、密码登录、忘记密码
- `feature-onboarding`：5步蒙层引导 + 7天基础体验权益发放
- `feature-ingestion`：截图OCR识别导入、平台绑定导入、手动链接导入
- `feature-cognition`：认知卡片、脑图下钻、7:3混合流
- `feature-internalization`：每日一问、讨论记录、语料库管理
- `feature-action`：挑战任务、作品上传、能力解锁
- `feature-subscription`：套餐、成本透明展示、支付、续费
- `feature-profile`：个人中心、设置、导出、注销
- `feature-group`：匿名小组、讨论精华、成果区
- `core-ai`：模型路由、提示词模板、审核链路
- `core-security`：AES密钥管理、权限编排、隐私审计

## 3. 关键流程闭环

1. 输入：导入信息源（OCR/绑定/手动）
2. 认知：AI生成导读+脑图，下钻联动原文段落（不落库）
3. 内化：每日一问+苏格拉底追问+总结入库
4. 行动：挑战任务+作品上传+能力解锁
5. 反馈：生成分享图+匿名小组曝光
6. 商业化：透明计费页展示算力成本、订阅费、权益

## 4. 性能指标（SLA）

- 冷启动 ≤ 3s
- OCR识别 ≤ 3s/张
- 轻量AIGC响应 ≤ 3s
- 重度AIGC响应 ≤ 5s（Agent训练除外）
- App 内存占用常驻 ≤ 500MB

## 5. 合规控制点

- AI内容统一标注“AI生成，仅供参考”
- 内容审核链：用户输入审核 + AI输出审核 + 分享前审核
- 权限最小化申请：相册/麦克风/通知/存储按需触发
- 数据处理说明：本地优先、可导出、可注销、可删除
