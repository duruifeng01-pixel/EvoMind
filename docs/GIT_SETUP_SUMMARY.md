# Git 配置总结

## 已创建的文件

### 1. Git工作流程文档
- **文件**: `docs/GIT_WORKFLOW.md`
- **内容**: 完整的Git工作流程规范，包括：
  - 分支策略
  - 提交信息规范（Conventional Commits）
  - 日常开发流程
  - 版本发布流程
  - 故障处理

### 2. Git快速上手指南
- **文件**: `docs/GIT_QUICK_START.md`
- **内容**: 简化的操作指南，包括：
  - 首次设置步骤
  - 日常开发流程
  - 常用场景示例
  - 提交信息模板使用

### 3. 提交信息模板
- **文件**: `.gitmessage`
- **用途**: Git提交时的模板文件
- **配置**: 执行 `git config commit.template .gitmessage`

### 4. GitHub模板文件
- **文件**: `.github/pull_request_template.md`
  - Pull Request模板
- **文件**: `.github/ISSUE_TEMPLATE/feature.md`
  - 功能需求Issue模板
- **文件**: `.github/ISSUE_TEMPLATE/bug.md`
  - Bug报告Issue模板

### 5. Git初始化脚本
- **Windows**: `scripts/init-git.ps1`
- **Linux/Mac**: `scripts/init-git.sh`
- **功能**: 自动化Git仓库初始化流程

### 6. 优化的.gitignore
- **文件**: `.gitignore`
- **更新**: 添加了Android项目常见的忽略项，包括：
  - 构建文件
  - IDE配置
  - 敏感信息（API密钥等）
  - 日志文件

---

## 提交信息规范

### 格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type类型
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `refactor`: 重构
- `chore`: 配置/工具更新

### 示例
```bash
git commit -m "feat(ui): 添加用户登录页面

- 实现手机号验证码登录UI
- 集成验证码发送接口
- 添加登录状态管理

Closes #10"
```

---

## 快速开始步骤

### 1. 初始化Git仓库
```bash
# Windows
.\scripts\init-git.ps1

# Linux/Mac
chmod +x scripts/init-git.sh
./scripts/init-git.sh
```

### 2. 配置提交模板
```bash
git config commit.template .gitmessage
```

### 3. 首次提交
```bash
git add .
git commit -m "chore: 初始化项目

- 创建Clean Architecture目录结构
- 配置Gradle构建文件
- 添加基础依赖
- 创建开发文档"
```

### 4. 添加远程仓库
```bash
git remote add origin https://github.com/你的用户名/EvoMind.git
```

### 5. 推送到GitHub
```bash
git branch -M main
git push -u origin main
```

---

## 日常开发流程

### 开始新功能
```bash
# 1. 更新develop分支
git checkout develop
git pull origin develop

# 2. 创建功能分支
git checkout -b feature/功能名称

# 3. 开发并提交
git add .
git commit -m "feat(模块): 功能描述

- 具体修改点1
- 具体修改点2

Closes #Issue编号"

# 4. 推送
git push origin feature/功能名称

# 5. 在GitHub创建Pull Request
```

---

## 提交信息详细说明要求

### 必须包含的内容
1. **Type + Scope + Subject**（必填）
   - 清晰说明修改类型和模块
   - 简短描述修改内容

2. **Body**（推荐）
   - 详细说明修改点
   - 使用列表格式，每行一个修改点
   - 说明影响范围

3. **Footer**（如有）
   - 关联Issue: `Closes #123`
   - 修复Bug: `Fixes #456`

### 好的提交信息示例

#### 功能开发
```
feat(ui): 添加用户登录页面

- 实现手机号验证码登录UI
- 集成验证码发送接口
- 添加登录状态管理
- 实现自动登录功能

影响范围：
- UI层：新增LoginScreen组件
- Data层：新增AuthRepository
- Domain层：新增LoginUseCase

测试情况：
- 已测试验证码发送
- 已测试登录流程
- 已测试错误处理

Closes #10
```

#### 阶段完成
```
feat: 完成阶段1 - 用户体系

本次更新内容：
- 实现用户注册/登录功能
  - 手机号验证码注册
  - 手机号/密码登录
  - 微信快捷登录
- 创建用户数据层
  - UserEntity、UserDao
  - UserRepository实现
  - 数据加密存储
- 实现个人中心页面
  - 用户信息展示
  - 学习数据概览
  - 功能入口

数据库设计：
- 用户表（UserEntity）
- 语料库表（CorpusEntity）

下一步计划：
- 开始阶段2：信息源导入功能

Closes #10, #11, #12
```

---

## 版本标签规范

### 创建标签
```bash
git tag -a v1.0.0 -m "版本1.0.0: MVP发布

主要功能：
- 用户体系
- 信息源导入
- 认知卡片
- 每日一问

技术栈：
- Kotlin + Jetpack Compose
- Clean Architecture + MVVM

Closes #50"
```

### 版本号规范
- `v0.1.0` - 阶段0完成
- `v0.2.0` - 阶段1完成
- `v1.0.0` - MVP版本发布

---

## 文档索引

- **详细工作流程**: `docs/GIT_WORKFLOW.md`
- **快速上手指南**: `docs/GIT_QUICK_START.md`
- **开发计划**: `docs/DEVELOPMENT_PLAN.md`

---

**提示**: 
- 每次提交前，确保提交信息清晰详细
- 使用提交模板可以确保格式一致
- 阶段完成时，提交信息应该更详细，说明所有更新内容
