# EvoMind（进化意志）安卓端交付包（架构版）

本仓库提供面向中国市场的 **EvoMind 安卓端专属完整技术方案与可落地工程骨架**，覆盖：

- 安卓端系统架构（Kotlin + Jetpack + MVVM + Room + WorkManager）
- 服务端架构（Spring Boot + MySQL/PostgreSQL + Redis + MQ）
- 本地/服务端数据库模型（含加密与合规字段）
- 核心接口设计（认证、信息源、认知卡片、任务、支付、订阅、导出、注销）
- 国内 AIGC/OCR/语音/支付 SDK 集成策略
- 透明算力成本导向订阅方案（订阅费 = 向上取整算力成本 × 2）
- 联调与测试、兼容性与上架清单

> 说明：当前提交为“可直接进入研发执行”的工程化文档与脚本基线，用于指导安卓端与后端团队并行开发、联调、测试与上架。

## 目录

- `docs/01-android-system-architecture.md` 安卓端专属系统架构
- `docs/02-database-models.md` 本地/服务端数据库模型设计
- `docs/03-api-contract.md` 后端接口设计（REST）
- `docs/04-android-uiux-and-modules.md` 安卓 UI/UX 与功能模块开发方案
- `docs/05-backend-aigc-integration.md` 后端与国内 AIGC 能力集成方案
- `docs/06-payment-and-subscription.md` 支付体系与透明订阅计费设计
- `docs/07-testing-release-checklist.md` 联调、测试、兼容与上架清单
- `sql/server_schema.sql` 服务端数据库初始化脚本（MySQL 8）
- `deploy/docker-compose.yml` 基础部署编排（MySQL + Redis + API）
- `deploy/backend/Dockerfile` 后端服务镜像模板

- `docs/08-全量技术蓝图-客户端服务端.md` 全量客户端/服务端技术架构
- `docs/09-测试方案与用例清单.md` 全量测试方案与用例
- `docs/10-中文文案与交互规范.md` 全中文文案与交互规范
- `android/ARCHITECTURE.md` 安卓工程包结构与开发约定
- `docs/11-可部署版本说明.md` 当前可运行交付范围与部署说明
- `docs/12-上线发行差距清单.md` 当前版本到正式上线版的差距与待办
- `backend/` Spring Boot 可运行后端源码
- `backend/.mvn/settings.xml` Maven 国内镜像配置（方案A）
- `backend/run-tests-with-mirror.sh` 使用国内镜像执行测试
- `android-app/` Android Studio 可构建客户端工程骨架

## 下一步研发建议

1. 先落地账号、引导、信息源导入、认知卡片主流程（MVP）。
2. 再接入支付、订阅、算力成本账本和权益系统。
3. 最后接入 Agent 训练、能力报告和小组互助。
