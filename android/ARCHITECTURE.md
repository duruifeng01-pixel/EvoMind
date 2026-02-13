# 安卓工程落地结构（建议）

## 1. 包结构

- `com.evomind.app`
- `com.evomind.core.ui`
- `com.evomind.core.network`
- `com.evomind.core.db`
- `com.evomind.core.ai`
- `com.evomind.feature.auth`
- `com.evomind.feature.onboarding`
- `com.evomind.feature.ingestion`
- `com.evomind.feature.cognition`
- `com.evomind.feature.internalization`
- `com.evomind.feature.action`
- `com.evomind.feature.subscription`
- `com.evomind.feature.profile`
- `com.evomind.feature.group`

## 2. 组件约定

- ViewModel 命名：`xxxViewModel`
- 页面状态：`UiState` + `UiEvent`
- 数据访问：`Repository` 接口 + `RepositoryImpl`
- 错误封装：统一 `AppError`
- 导航：`Navigation Graph` 分业务子图

## 3. 测试约定

- 单元测试目录：`src/test`
- 仪器测试目录：`src/androidTest`
- 命名：`Given_When_Then`
