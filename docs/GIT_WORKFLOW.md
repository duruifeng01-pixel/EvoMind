# Git 工作流程规范

## 一、仓库初始化

### 1.1 本地初始化
```bash
# 初始化Git仓库
git init

# 添加远程仓库（首次）
git remote add origin https://github.com/你的用户名/EvoMind.git

# 或者使用SSH（推荐）
git remote add origin git@github.com:你的用户名/EvoMind.git
```

### 1.2 首次提交
```bash
# 添加所有文件
git add .

# 提交（使用规范格式）
git commit -m "chore: 初始化项目结构

- 创建Clean Architecture目录结构
- 配置Gradle构建文件
- 添加基础依赖（Compose, Material3）
- 创建开发文档（PRD, 开发计划）"

# 推送到远程（首次）
git push -u origin main
```

---

## 二、分支策略

### 2.1 分支命名规范
- `main`: 主分支，生产环境代码，必须稳定
- `develop`: 开发分支，集成所有功能
- `feature/功能名`: 功能分支（如 `feature/user-login`）
- `fix/问题描述`: 修复分支（如 `fix/login-crash`）
- `hotfix/紧急问题`: 热修复分支
- `docs/文档更新`: 文档更新分支

### 2.2 分支工作流程
```
main (生产)
  ↑
develop (开发)
  ↑
feature/user-login (功能开发)
```

### 2.3 创建分支示例
```bash
# 从develop创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/user-login

# 或者使用新语法
git checkout -b feature/user-login develop
```

---

## 三、提交信息规范（Conventional Commits）

### 3.1 提交格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

### 3.2 Type 类型（必填）
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具/依赖更新
- `ci`: CI/CD配置

### 3.3 Scope 范围（可选）
- `ui`: UI相关
- `data`: 数据层
- `domain`: 领域层
- `di`: 依赖注入
- `utils`: 工具类
- `config`: 配置

### 3.4 Subject 主题（必填）
- 简短描述，不超过50字符
- 使用中文
- 不使用句号结尾
- 使用祈使语气（"添加"而非"添加了"）

### 3.5 Body 正文（可选）
- 详细说明修改内容
- 说明为什么修改
- 说明与之前版本的差异
- 每行不超过72字符

### 3.6 Footer 脚注（可选）
- 关闭Issue: `Closes #123`
- 关联Issue: `Refs #456`
- 破坏性变更: `BREAKING CHANGE: 描述`

---

## 四、提交示例

### 4.1 功能开发
```bash
git commit -m "feat(ui): 添加用户登录页面

- 实现手机号验证码登录UI
- 集成验证码发送接口
- 添加登录状态管理
- 实现自动登录功能

Closes #10"
```

### 4.2 Bug修复
```bash
git commit -m "fix(data): 修复Room数据库加密配置问题

- 修复数据库初始化时的加密密钥错误
- 添加密钥验证逻辑
- 更新数据库迁移策略

Fixes #25"
```

### 4.3 重构
```bash
git commit -m "refactor(domain): 重构UseCase层结构

- 提取通用UseCase基类
- 统一错误处理逻辑
- 优化依赖注入方式"
```

### 4.4 文档更新
```bash
git commit -m "docs: 更新开发计划文档

- 添加阶段3详细任务清单
- 更新技术选型对比
- 补充风险控制策略"
```

### 4.5 配置更新
```bash
git commit -m "chore(config): 添加Hilt依赖配置

- 添加Hilt核心依赖
- 配置Hilt插件
- 创建AppModule基础结构"
```

---

## 五、日常开发流程

### 5.1 开始新功能
```bash
# 1. 更新develop分支
git checkout develop
git pull origin develop

# 2. 创建功能分支
git checkout -b feature/信息源导入

# 3. 开发完成后提交
git add .
git commit -m "feat(data): 实现信息源导入功能

- 添加信息源Entity和DAO
- 实现OCR截图识别逻辑
- 创建信息源管理Repository
- 添加信息源列表UI

Closes #15"

# 4. 推送到远程
git push origin feature/信息源导入

# 5. 在GitHub创建Pull Request
```

### 5.2 修复Bug
```bash
# 1. 从develop创建修复分支
git checkout develop
git pull origin develop
git checkout -b fix/登录页面崩溃

# 2. 修复并提交
git add .
git commit -m "fix(ui): 修复登录页面空指针异常

- 添加ViewModel空值检查
- 修复状态初始化时机问题
- 添加错误日志

Fixes #28"

# 3. 推送并创建PR
git push origin fix/登录页面崩溃
```

### 5.3 代码审查流程
1. **创建Pull Request**
   - 标题：使用提交信息格式
   - 描述：详细说明修改内容、测试情况、影响范围
   - 关联Issue：使用 `Closes #123`

2. **审查检查清单**
   - [ ] 代码符合规范
   - [ ] 提交信息清晰
   - [ ] 功能测试通过
   - [ ] 无编译错误
   - [ ] 无明显的性能问题

3. **合并到develop**
   - 审查通过后合并
   - 删除功能分支（可选）

---

## 六、提交信息模板

### 6.1 创建提交模板
在项目根目录创建 `.gitmessage` 文件：

```
# <type>(<scope>): <subject>
# 
# <body>
# 
# <footer>
#
# Type: feat, fix, docs, style, refactor, perf, test, chore, ci
# Scope: ui, data, domain, di, utils, config
# Subject: 简短描述（不超过50字符）
# Body: 详细说明（可选）
# Footer: 关闭Issue等（可选）
```

### 6.2 配置Git使用模板
```bash
# 全局配置（推荐）
git config --global commit.template ~/.gitmessage

# 或者项目级配置
git config commit.template .gitmessage
```

---

## 七、标签管理（版本发布）

### 7.1 创建标签
```bash
# 创建带注释的标签（推荐）
git tag -a v1.0.0 -m "版本1.0.0: MVP发布

- 完成用户体系
- 实现信息源导入
- 完成认知卡片功能
- 实现基础支付功能"

# 推送标签
git push origin v1.0.0

# 推送所有标签
git push origin --tags
```

### 7.2 版本号规范（Semantic Versioning）
- `主版本号.次版本号.修订号` (如 `1.2.3`)
- **主版本号**: 不兼容的API修改
- **次版本号**: 向下兼容的功能新增
- **修订号**: 向下兼容的问题修复

### 7.3 版本标签示例
```bash
# 阶段0完成
git tag -a v0.1.0 -m "基础设施搭建完成"

# 阶段1完成
git tag -a v0.2.0 -m "用户体系完成"

# MVP版本
git tag -a v1.0.0 -m "MVP版本发布"
```

---

## 八、常用命令速查

### 8.1 日常操作
```bash
# 查看状态
git status

# 查看差异
git diff

# 查看提交历史
git log --oneline --graph

# 查看某个文件的修改历史
git log --follow -p -- 文件路径

# 撤销工作区修改
git checkout -- 文件路径

# 撤销暂存区
git reset HEAD 文件路径

# 修改最后一次提交
git commit --amend
```

### 8.2 分支操作
```bash
# 查看所有分支
git branch -a

# 删除本地分支
git branch -d 分支名

# 删除远程分支
git push origin --delete 分支名

# 查看分支差异
git diff develop..feature/xxx
```

### 8.3 远程操作
```bash
# 查看远程仓库
git remote -v

# 更新远程分支信息
git fetch origin

# 拉取并合并
git pull origin develop

# 推送（首次）
git push -u origin 分支名

# 推送（后续）
git push
```

---

## 九、GitHub仓库设置建议

### 9.1 仓库设置
- **描述**: EvoMind - 个人成长认知外骨骼Android应用
- **Topics**: `android`, `kotlin`, `jetpack-compose`, `clean-architecture`, `ai`, `education`
- **可见性**: 根据需求设置（公开/私有）

### 9.2 分支保护规则（main分支）
- [ ] 要求Pull Request审查
- [ ] 要求状态检查通过
- [ ] 要求分支最新
- [ ] 不允许强制推送

### 9.3 Issue模板
创建 `.github/ISSUE_TEMPLATE/feature.md`:
```markdown
## 功能描述
<!-- 描述要添加的功能 -->

## 实现方案
<!-- 描述实现思路 -->

## 验收标准
- [ ] 标准1
- [ ] 标准2
```

### 9.4 Pull Request模板
创建 `.github/pull_request_template.md`:
```markdown
## 变更类型
- [ ] 新功能
- [ ] Bug修复
- [ ] 重构
- [ ] 文档更新
- [ ] 其他

## 变更描述
<!-- 详细描述本次变更 -->

## 相关Issue
Closes #

## 测试情况
- [ ] 已测试
- [ ] 测试通过

## 截图（如适用）
<!-- 添加截图 -->
```

---

## 十、提交信息详细示例（按阶段）

### 阶段0：基础设施
```bash
git commit -m "chore(config): 配置Hilt依赖注入

- 添加Hilt核心依赖和插件
- 创建AppModule基础结构
- 配置Application类

Refs #5"

git commit -m "chore(config): 配置Room数据库

- 添加Room依赖
- 配置数据库加密
- 创建Database基类

Refs #5"

git commit -m "feat(ui): 创建基础UI组件库

- 创建Button、Card、Loading组件
- 实现ErrorState组件
- 添加组件预览

Closes #8"
```

### 阶段1：用户体系
```bash
git commit -m "feat(data): 实现用户数据层

- 创建UserEntity和UserDao
- 实现UserRepository
- 添加数据加密逻辑

Closes #10"

git commit -m "feat(ui): 实现用户登录页面

- 创建登录UI组件
- 实现手机号验证码登录
- 添加登录状态管理
- 集成验证码发送接口

Closes #11"

git commit -m "feat(domain): 实现登录UseCase

- 创建LoginUseCase
- 实现登录业务逻辑
- 添加错误处理

Refs #11"
```

### 阶段2：信息源导入
```bash
git commit -m "feat(data): 实现信息源数据模型

- 创建SourceEntity和SourceDao
- 实现信息源Repository
- 添加信息源分组功能

Closes #15"

git commit -m "feat(utils): 集成OCR识别功能

- 集成百度OCR SDK
- 实现截图识别逻辑
- 添加识别结果解析

Closes #16"

git commit -m "feat(ui): 实现信息源管理页面

- 创建信息源列表UI
- 实现添加/删除功能
- 添加分组管理

Closes #17"
```

---

## 十一、最佳实践

### 11.1 提交频率
- **小步提交**: 每完成一个小功能就提交
- **原子性**: 每次提交只做一件事
- **可回滚**: 确保每次提交后项目可编译运行

### 11.2 提交前检查
```bash
# 1. 检查状态
git status

# 2. 查看差异
git diff

# 3. 编译检查
./gradlew build

# 4. 运行测试（如果有）
./gradlew test

# 5. 确认无误后提交
git add .
git commit -m "..."
```

### 11.3 提交信息质量
- ✅ **好的提交信息**:
  ```
  feat(ui): 添加用户登录页面
  
  - 实现手机号验证码登录UI
  - 集成验证码发送接口
  - 添加登录状态管理
  ```

- ❌ **不好的提交信息**:
  ```
  更新代码
  修复bug
  添加功能
  ```

### 11.4 避免的提交
- ❌ 提交临时调试代码
- ❌ 提交大量无关修改
- ❌ 提交编译错误
- ❌ 提交敏感信息（API密钥等）

---

## 十二、故障处理

### 12.1 撤销提交
```bash
# 撤销最后一次提交（保留修改）
git reset --soft HEAD~1

# 撤销最后一次提交（丢弃修改）
git reset --hard HEAD~1

# 撤销到指定提交
git reset --hard 提交hash
```

### 12.2 合并冲突
```bash
# 1. 拉取最新代码
git pull origin develop

# 2. 解决冲突
# 编辑冲突文件，保留需要的代码

# 3. 标记已解决
git add 冲突文件

# 4. 完成合并
git commit
```

### 12.3 误删文件恢复
```bash
# 恢复工作区文件
git checkout -- 文件路径

# 恢复已删除的文件
git checkout HEAD -- 文件路径
```

---

## 十三、自动化建议

### 13.1 Git Hooks（可选）
创建 `.git/hooks/pre-commit`:
```bash
#!/bin/sh
# 提交前检查
./gradlew check
if [ $? -ne 0 ]; then
    echo "检查失败，请修复后重试"
    exit 1
fi
```

### 13.2 GitHub Actions（可选）
创建 `.github/workflows/ci.yml`:
```yaml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build
        run: ./gradlew build
```

---

**文档版本**: v1.0  
**创建日期**: 2026-02-13  
**最后更新**: 2026-02-13
