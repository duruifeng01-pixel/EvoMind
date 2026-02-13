# Git 仓库初始化脚本 (Windows PowerShell)

Write-Host "=== EvoMind Git 仓库初始化 ===" -ForegroundColor Green

# 检查是否已初始化
if (Test-Path .git) {
    Write-Host "警告: Git仓库已初始化" -ForegroundColor Yellow
    $continue = Read-Host "是否继续? (y/n)"
    if ($continue -ne "y") {
        exit
    }
}

# 1. 初始化Git仓库
Write-Host "`n1. 初始化Git仓库..." -ForegroundColor Cyan
git init
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: Git初始化失败" -ForegroundColor Red
    exit 1
}

# 2. 配置提交模板
Write-Host "`n2. 配置提交信息模板..." -ForegroundColor Cyan
git config commit.template .gitmessage
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ 提交模板配置成功" -ForegroundColor Green
} else {
    Write-Host "警告: 提交模板配置失败" -ForegroundColor Yellow
}

# 3. 检查用户配置
Write-Host "`n3. 检查Git用户配置..." -ForegroundColor Cyan
$userName = git config user.name
$userEmail = git config user.email

if (-not $userName -or -not $userEmail) {
    Write-Host "需要配置Git用户信息:" -ForegroundColor Yellow
    $name = Read-Host "请输入你的名字"
    $email = Read-Host "请输入你的邮箱"
    
    git config --global user.name $name
    git config --global user.email $email
    Write-Host "✓ 用户信息配置成功" -ForegroundColor Green
} else {
    Write-Host "✓ 用户信息已配置: $userName <$userEmail>" -ForegroundColor Green
}

# 4. 创建develop分支
Write-Host "`n4. 创建develop分支..." -ForegroundColor Cyan
git checkout -b develop
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ develop分支创建成功" -ForegroundColor Green
}

# 5. 添加远程仓库（可选）
Write-Host "`n5. 配置远程仓库..." -ForegroundColor Cyan
$addRemote = Read-Host "是否现在添加远程仓库? (y/n)"
if ($addRemote -eq "y") {
    $remoteUrl = Read-Host "请输入远程仓库URL (如: https://github.com/用户名/EvoMind.git)"
    if ($remoteUrl) {
        git remote add origin $remoteUrl
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ 远程仓库配置成功" -ForegroundColor Green
        } else {
            Write-Host "警告: 远程仓库配置失败" -ForegroundColor Yellow
        }
    }
}

# 6. 首次提交
Write-Host "`n6. 准备首次提交..." -ForegroundColor Cyan
$doCommit = Read-Host "是否现在进行首次提交? (y/n)"
if ($doCommit -eq "y") {
    git add .
    git commit -m "chore: 初始化项目

- 创建Clean Architecture目录结构
- 配置Gradle构建文件
- 添加基础依赖（Compose, Material3）
- 创建开发文档（PRD, 开发计划, Git工作流程）"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 首次提交成功" -ForegroundColor Green
    } else {
        Write-Host "警告: 首次提交失败" -ForegroundColor Yellow
    }
}

Write-Host "`n=== 初始化完成 ===" -ForegroundColor Green
Write-Host "`n下一步操作:" -ForegroundColor Cyan
Write-Host "1. 如果已配置远程仓库，执行: git push -u origin develop" -ForegroundColor White
Write-Host "2. 查看Git工作流程文档: docs/GIT_WORKFLOW.md" -ForegroundColor White
Write-Host "3. 查看快速上手指南: docs/GIT_QUICK_START.md" -ForegroundColor White
