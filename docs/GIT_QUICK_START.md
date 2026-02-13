# Git 快速上手指南

## 一、首次设置（只需一次）

### 1.1 初始化Git仓库
```bash
# 在项目根目录执行
git init
```

### 1.2 配置提交信息模板
```bash
# 使用项目提供的模板
git config commit.template .gitmessage
```

### 1.3 配置用户信息（如果还没配置）
```bash
git config --global user.name "你的名字"
git config --global user.email "你的邮箱"
```

### 1.4 添加远程仓库
```bash
# 在GitHub创建仓库后，添加远程地址
git remote add origin https://github.com/你的用户名/EvoMind.git

# 或者使用SSH（推荐）
git remote add origin git@github.com:你的用户名/EvoMind.git
```

---

## 二、日常开发流程（每天使用）

### 2.1 开始新功能
```bash
# 1. 确保在develop分支，并更新到最新
git checkout develop
git pull origin develop

# 2. 创建功能分支
git checkout -b feature/功能名称

# 例如：
git checkout -b feature/user-login
```

### 2.2 开发并提交
```bash
# 1. 查看修改状态
git status

# 2. 查看具体修改内容
git diff

# 3. 添加要提交的文件
git add .                    # 添加所有修改
# 或
git add 文件路径              # 添加特定文件

# 4. 提交（使用规范格式）
git commit

# 或者直接写提交信息
git commit -m "feat(ui): 添加用户登录页面

- 实现手机号验证码登录UI
- 集成验证码发送接口
- 添加登录状态管理

Closes #10"
```

### 2.3 推送到远程
```bash
# 首次推送
git push -u origin feature/功能名称

# 后续推送
git push
```

### 2.4 创建Pull Request
1. 在GitHub上打开仓库
2. 点击 "New Pull Request"
3. 选择你的功能分支
4. 填写PR描述（使用模板）
5. 提交PR，等待审查

---

## 三、提交信息模板使用

### 3.1 使用模板提交
```bash
# 配置模板后，直接执行
git commit

# 会自动打开编辑器，显示模板
# 按照模板填写即可
```

### 3.2 提交信息格式示例

#### 新功能
```
feat(ui): 添加用户登录页面

- 实现手机号验证码登录UI
- 集成验证码发送接口
- 添加登录状态管理

Closes #10
```

#### Bug修复
```
fix(data): 修复数据库加密配置问题

- 修复数据库初始化时的加密密钥错误
- 添加密钥验证逻辑
- 更新数据库迁移策略

Fixes #25
```

#### 重构
```
refactor(domain): 重构UseCase层结构

- 提取通用UseCase基类
- 统一错误处理逻辑
- 优化依赖注入方式
```

#### 文档更新
```
docs: 更新开发计划文档

- 添加阶段3详细任务清单
- 更新技术选型对比
- 补充风险控制策略
```

---

## 四、常用场景

### 4.1 查看提交历史
```bash
# 简洁格式
git log --oneline

# 图形化显示
git log --oneline --graph

# 查看最近5次提交
git log --oneline -5
```

### 4.2 撤销修改
```bash
# 撤销工作区修改（未add）
git checkout -- 文件路径

# 撤销暂存区（已add，未commit）
git reset HEAD 文件路径

# 修改最后一次提交
git commit --amend
```

### 4.3 合并代码
```bash
# 从develop更新到当前分支
git checkout feature/你的分支
git pull origin develop

# 如果有冲突，解决后
git add .
git commit -m "merge: 合并develop分支"
```

### 4.4 查看差异
```bash
# 查看工作区与暂存区的差异
git diff

# 查看暂存区与最后一次提交的差异
git diff --staged

# 查看与远程分支的差异
git diff origin/develop
```

---

## 五、提交信息详细说明模板

### 5.1 功能开发提交
```bash
git commit -m "feat(模块): 简短描述

详细说明：
- 具体修改点1
- 具体修改点2
- 具体修改点3

影响范围：
- 影响的模块或功能

测试情况：
- 已测试的功能点

相关Issue：
Closes #123"
```

### 5.2 阶段完成提交示例

#### 阶段0完成
```bash
git commit -m "chore: 完成阶段0 - 基础设施搭建

本次更新内容：
- 配置Hilt依赖注入框架
- 配置Room数据库和加密
- 配置Retrofit网络层
- 创建基础UI组件库
- 实现CryptoUtils加密工具
- 实现CostCalculator成本计算工具
- 完善Theme系统（深色模式支持）

技术选型确认：
- AIGC模型：Moonshot API
- OCR服务：百度OCR

下一步计划：
- 开始阶段1：用户体系开发

Refs #5"
```

#### 阶段1完成
```bash
git commit -m "feat: 完成阶段1 - 用户体系

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
- 实现设置页面
  - 推送设置
  - 隐私设置
  - 显示设置

数据库设计：
- 用户表（UserEntity）
- 语料库表（CorpusEntity）
- 认知卡片表（CognitiveCardEntity）

下一步计划：
- 开始阶段2：信息源导入功能

Closes #10, #11, #12"
```

---

## 六、版本发布流程

### 6.1 创建版本标签
```bash
# 1. 确保代码已合并到main分支
git checkout main
git pull origin main

# 2. 创建标签
git tag -a v1.0.0 -m "版本1.0.0: MVP发布

主要功能：
- 用户体系（注册/登录/个人中心）
- 信息源导入（OCR识别/手动导入）
- 认知卡片（AI生成导读/脑图）
- 每日一问（AI讨论）
- 语料库管理
- 基础支付功能

技术栈：
- Kotlin + Jetpack Compose
- Clean Architecture + MVVM
- Hilt + Room + Retrofit
- Moonshot AI API

Closes #50"

# 3. 推送标签
git push origin v1.0.0
```

### 6.2 版本号规范
- `v0.1.0` - 阶段0完成（基础设施）
- `v0.2.0` - 阶段1完成（用户体系）
- `v0.3.0` - 阶段2完成（信息源导入）
- `v0.4.0` - 阶段3完成（认知模块）
- `v0.5.0` - 阶段4完成（内化模块）
- `v0.6.0` - 阶段5完成（行动+支付）
- `v1.0.0` - MVP版本发布

---

## 七、问题排查

### 7.1 提交被拒绝
```bash
# 原因：远程有新的提交
# 解决：先拉取再推送
git pull origin develop
# 解决冲突后
git push
```

### 7.2 忘记提交信息
```bash
# 修改最后一次提交信息
git commit --amend -m "新的提交信息"
```

### 7.3 提交了错误文件
```bash
# 撤销最后一次提交（保留修改）
git reset --soft HEAD~1

# 重新添加正确的文件
git add 正确的文件
git commit -m "正确的提交信息"
```

---

## 八、GitHub操作指南

### 8.1 创建仓库
1. 登录GitHub
2. 点击右上角 "+" → "New repository"
3. 填写仓库名称：`EvoMind`
4. 选择可见性（公开/私有）
5. **不要**初始化README（项目已有）
6. 点击 "Create repository"

### 8.2 首次推送
```bash
# 在项目根目录执行
git add .
git commit -m "chore: 初始化项目

- 创建Clean Architecture目录结构
- 配置Gradle构建文件
- 添加基础依赖
- 创建开发文档"

git branch -M main
git remote add origin https://github.com/你的用户名/EvoMind.git
git push -u origin main
```

### 8.3 创建Pull Request
1. 推送功能分支后，GitHub会提示创建PR
2. 点击 "Compare & pull request"
3. 填写PR描述（使用模板）
4. 选择审查者（如有）
5. 点击 "Create pull request"

### 8.4 合并PR
1. 审查通过后，点击 "Merge pull request"
2. 选择合并方式（推荐 "Squash and merge"）
3. 确认合并
4. 删除功能分支（可选）

---

## 九、提交信息检查清单

提交前确认：
- [ ] 提交信息格式正确（type + scope + subject）
- [ ] 详细说明了修改内容（body）
- [ ] 关联了相关Issue（如有）
- [ ] 代码可以编译通过
- [ ] 无明显的语法错误
- [ ] 无敏感信息（API密钥等）

---

## 十、快速参考

### 最常用的5个命令
```bash
git status          # 查看状态
git add .           # 添加所有修改
git commit -m "..."  # 提交
git push            # 推送
git pull            # 拉取
```

### 提交信息类型速查
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档
- `refactor`: 重构
- `chore`: 配置/工具

### 模块范围速查
- `ui`: UI相关
- `data`: 数据层
- `domain`: 领域层
- `di`: 依赖注入
- `utils`: 工具类

---

**提示**: 详细的工作流程请参考 `docs/GIT_WORKFLOW.md`
