# 复习系统模块文档

## 模块7: 复习系统

**状态**: ✅ 100% 已完成

### 已实现功能

#### 1. 间隔重复算法 (核心功能)
- **SM2算法优化版**: 基于SuperMemo 2算法，专为认知卡片设计
- **自适应调整**: 根据用户的复习质量自动调整下次复习时间
- **指数增长**: 连续答对会导致间隔时间指数级增长（1天→6天→16天→...）
- **记忆容易度因子**: 动态调整每个卡片的难度系数

**算法逻辑**:
- 质量评分0-2：降低难度因子，重置间隔为1天
- 质量评分3：保持当前难度，小幅增加间隔
- 质量评分4-5：提高难度因子，大幅增加间隔

#### 2. 复习历史记录
- **ReviewSessionEntity**: 记录每次复习的详细信息
  - 复习类型（快速/深度/测试/联想）
  - 复习质量（0-5分）
  - 间隔天数
  - 复习时长
  - 用户笔记
- **完整审计**: 支持查看每张卡片的完整复习历史

#### 3. 复习计划生成
- **自动计算**: 基于算法自动计算下次复习时间
- **智能提醒**: 到期的卡片自动出现在复习列表
- **优先级排序**: 按到期时间排序，先到期先复习

#### 4. 统计与数据分析
- **今日统计**: 今日复习次数、复习卡片数
- **本周统计**: 本周总复习次数、平均质量分
- **待复习统计**: 当前待复习卡片数量
- **复习历史**: 支持查看任意时间段的复习记录

#### 5. 多种复习模式
- **快速复习**: 简单浏览，适合碎片时间
- **深度复习**: 仔细阅读，加深理解
- **测试复习**: 自测模式，检验记忆效果
- **联想复习**: 关联卡片，建立知识网络

### 文件结构

```
review/
├── SpacedRepetitionAlgorithm.kt     # 间隔重复算法核心
├── ReviewSessionService.kt          # 复习会话管理服务
├── ReviewViewModel.kt               # 复习ViewModel
├── ReviewFragment.kt                # 复习界面Fragment
└── ReviewSessionEntity.kt           # 复习历史实体
```

database/
├── ReviewSessionDao.kt              # 复习历史数据访问
└── CardEntity.kt                    # 卡片实体（已包含复习字段）

layout/
└── fragment_review.xml              # 复习界面布局
```

### 数据库字段

#### CardEntity 复习相关字段
```kotlin
@ColumnInfo(name = "review_count")
val reviewCount: Int = 0,        // 复习次数

@ColumnInfo(name = "last_reviewed_at")
val lastReviewedAt: Long? = null, // 上次复习时间

@ColumnInfo(name = "next_review_at")
val nextReviewAt: Long           // 下次复习时间
```

#### ReviewSessionEntity 字段
- `card_id`: 关联卡片ID
- `session_type`: 复习类型
- `ease_factor`: 记忆容易度因子
- `quality`: 质量评分0-5
- `interval_days`: 间隔天数
- `reviewed_at`: 复习时间
- `review_duration`: 复习时长
- `notes`: 用户笔记

### 使用方法

```kotlin
// 1. 初始化复习服务
val reviewService = ReviewSessionService(context)

// 2. 开始复习会话
val sessionId = reviewService.startReviewSession(cardId, ReviewSessionType.QUICK)

// 3. 完成复习并评分
val updatedCard = reviewService.completeReviewSession(
    sessionId = sessionId,
    quality = 4,  // 4 = "认识"
    notes = "记忆清晰"
)

// 4. 获取待复习卡片
val dueCards = database.cardDao().getCardsDueForReview()

// 5. 获取复习统计
val stats = reviewService.getReviewStats()
```

### 质量评分标准

| 分数 | 描述 | 算法影响 |
|------|------|---------|
| 5 | 完美回答，轻松回忆 | 大幅增加间隔，提高难度因子 |
| 4 | 正确回答，有些犹豫 | 正常增加间隔 |
| 3 | 正确回答，费了很大劲 | 小幅增加间隔 |
| 2 | 错误回答，看到答案后觉得熟悉 | 降低难度因子，重置间隔 |
| 1 | 错误回答，看到答案后记得 | 降低难度因子，重置间隔 |
| 0 | 完全忘记 | 大幅降低难度因子，重置为1天 |

### 核心特性

✅ **科学化**: 基于50年认知科学研究的间隔重复原理
✅ **自适应**: 根据每个人的记忆曲线调整
✅ **防遗忘**: 在最佳时间提醒复习，减少遗忘
✅ **高效率**: 优先复习最需要的卡片，节省时间
✅ **可追踪**: 完整的复习历史记录和分析

### 性能优化

- **数据库索引**: 为reviewed_at、card_id添加索引
- **分页加载**: 大列表使用分页加载，避免内存溢出
- **异步处理**: 所有数据库操作都在IO线程执行
- **内存管理**: 及时释放不再使用的资源

### 与其他模块集成

- **认知卡片**: 复习系统建立在卡片基础之上
- **数据分析**: 复习数据可用于学习效果分析
- **提醒通知**: 可接入本地通知提醒用户复习

### 下一步扩展

1. **复习提醒**: 使用WorkManager实现定时通知
2. **进阶模式**: 增加填空、选择等交互式复习
3. **学习报告**: 生成周/月学习报告
4. **目标设定**: 设置每日复习目标
5. **成就系统**: 复习里程碑和成就徽章

---

**创建时间**: 2026-02-14
**开发者**: AI Assistant
**审核状态**: 已通过单元测试
