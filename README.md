# EvoMind (进化意志)

个人成长认知外骨骼 Android 应用

## 项目简介

EvoMind 是一款面向国内用户的 Android 原生应用，通过「输入-认知-内化-行动-反馈」的完整闭环，将国内主流平台的碎片化优质信息转化为可落地、可沉淀的个人能力资产。

## 核心特性

- 🧠 **本地优先**: 核心学习数据私有化存储，支持离线使用
- 💰 **成本透明**: 算力成本实时计算，订阅费透明展示
- 🇨🇳 **国内适配**: 无外网依赖，使用国内AIGC模型和支付渠道
- 🎯 **认知外骨骼**: AI驱动的个人成长助手

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose (Material Design 3)
- **架构**: Clean Architecture + MVVM
- **依赖注入**: Hilt
- **本地数据库**: Room (SQLite, 加密)
- **网络**: Retrofit + OkHttp
- **异步**: Coroutines + Flow
- **图片加载**: Coil

## 项目结构

```
app/src/main/java/com/evomind/app/
├── data/              # 数据层
│   ├── local/        # 本地数据（Room, Entity, DAO）
│   ├── remote/       # 远程数据（Retrofit, DTO）
│   └── repository/   # Repository实现
├── domain/           # 领域层
│   ├── model/       # 领域模型
│   ├── repository/  # 仓库接口
│   └── usecase/     # 业务用例
├── di/              # 依赖注入
├── ui/              # UI层
│   ├── theme/       # 主题（Color, Type, Theme）
│   ├── components/  # 通用组件
│   └── screens/     # 业务页面
└── utils/           # 工具类
```

## 快速开始

### 环境要求

- Android Studio (最新稳定版)
- JDK 11+
- Android SDK (API 24+)

### 克隆项目

```bash
git clone https://github.com/你的用户名/EvoMind.git
cd EvoMind
```

### 初始化Git仓库（首次）

**Windows:**
```powershell
.\scripts\init-git.ps1
```

**Linux/Mac:**
```bash
chmod +x scripts/init-git.sh
./scripts/init-git.sh
```

### 构建项目

```bash
./gradlew build
```

### 运行项目

在 Android Studio 中打开项目，连接设备或启动模拟器，点击运行。

## 开发文档

- [产品需求文档 (PRD)](docs/PRD.md)
- [开发计划](docs/DEVELOPMENT_PLAN.md)
- [Git工作流程](docs/GIT_WORKFLOW.md)
- [Git快速上手](docs/GIT_QUICK_START.md)

## 开发阶段

- [x] 阶段0: 基础设施搭建
- [ ] 阶段1: 用户体系 + 本地数据库
- [ ] 阶段2: 信息源导入
- [ ] 阶段3: 认知模块
- [ ] 阶段4: 内化模块
- [ ] 阶段5: 行动模块 + 支付体系
- [ ] 阶段6: 反馈模块 + 优化

## 贡献指南

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: 添加AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

详细提交规范请参考 [Git工作流程](docs/GIT_WORKFLOW.md)

## 许可证

[待定]

## 联系方式

[待定]

---

**注意**: 本项目正在积极开发中，API和功能可能会有变化。
