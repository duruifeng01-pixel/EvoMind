# 02 本地/服务端数据库模型设计

## 1. 本地数据库（Room + SQLCipher）

> 原则：核心学习数据本地优先，离线可读写。

### 1.1 主要表

- `local_user_session`：登录态、token缓存（加密）
- `local_source`：信息源（博主昵称、平台、主页链接、分组、置顶）
- `local_card`：认知卡片（标题、导读、来源、收藏状态）
- `local_mindmap_node`：脑图节点（树结构）
- `local_discussion`：每日一问会话（问题、回答、追问、总结）
- `local_asset`：作品、录音、导出文件路径
- `local_task`：挑战任务（阶段、难度、状态、截止时间）
- `local_settings`：推送、隐私、显示、动效开关
- `local_keyword_cloud`：关键词云快照

### 1.2 加密策略

- SQLCipher全库加密
- AES-GCM字段级加密（如手机号掩码前原文、token）
- 密钥由 Android Keystore 托管

## 2. 服务端数据库（MySQL/PostgreSQL）

### 2.1 用户与认证

- `user_account`：手机号、微信openid、昵称、头像、状态
- `user_auth_credential`：密码哈希、验证码登录记录、设备信息
- `user_privacy_log`：授权、导出申请、注销申请与执行记录

### 2.2 订阅与支付

- `plan_catalog`：基础/进阶/定制套餐定义
- `subscription_order`：订单主表（渠道、金额、状态）
- `payment_transaction`：微信/支付宝流水
- `user_subscription`：订阅权益生效与到期
- `refund_ticket`：退款申请与审核

### 2.3 算力成本账本

- `compute_meter_rule`：计费指标单价（元/单位）
- `compute_usage_fact`：用户维度实际用量（按日聚合）
- `compute_cost_snapshot`：10分钟刷新成本快照

### 2.4 内容与AIGC

- `source_link_index`：信息源链接索引（不存全文）
- `ai_call_log`：模型调用日志（模型、token、时延、成本）
- `content_audit_log`：审核结果日志
- `group_highlight`：匿名小组精华（脱敏）

## 3. 数据生命周期

- 热数据：30天高频访问缓存（Redis）
- 冷数据：>48小时未读源内容标记冷数据
- 删除策略：注销后 T+7 完成全量清除（符合法规保留项除外）
