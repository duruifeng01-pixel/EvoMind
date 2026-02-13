#!/bin/bash
# Git 仓库初始化脚本 (Linux/Mac)

echo "=== EvoMind Git 仓库初始化 ==="

# 检查是否已初始化
if [ -d .git ]; then
    echo "警告: Git仓库已初始化"
    read -p "是否继续? (y/n) " continue
    if [ "$continue" != "y" ]; then
        exit
    fi
fi

# 1. 初始化Git仓库
echo ""
echo "1. 初始化Git仓库..."
git init
if [ $? -ne 0 ]; then
    echo "错误: Git初始化失败"
    exit 1
fi

# 2. 配置提交模板
echo ""
echo "2. 配置提交信息模板..."
git config commit.template .gitmessage
if [ $? -eq 0 ]; then
    echo "✓ 提交模板配置成功"
else
    echo "警告: 提交模板配置失败"
fi

# 3. 检查用户配置
echo ""
echo "3. 检查Git用户配置..."
userName=$(git config user.name)
userEmail=$(git config user.email)

if [ -z "$userName" ] || [ -z "$userEmail" ]; then
    echo "需要配置Git用户信息:"
    read -p "请输入你的名字: " name
    read -p "请输入你的邮箱: " email
    
    git config --global user.name "$name"
    git config --global user.email "$email"
    echo "✓ 用户信息配置成功"
else
    echo "✓ 用户信息已配置: $userName <$userEmail>"
fi

# 4. 创建develop分支
echo ""
echo "4. 创建develop分支..."
git checkout -b develop
if [ $? -eq 0 ]; then
    echo "✓ develop分支创建成功"
fi

# 5. 添加远程仓库（可选）
echo ""
echo "5. 配置远程仓库..."
read -p "是否现在添加远程仓库? (y/n) " addRemote
if [ "$addRemote" = "y" ]; then
    read -p "请输入远程仓库URL (如: https://github.com/用户名/EvoMind.git): " remoteUrl
    if [ -n "$remoteUrl" ]; then
        git remote add origin "$remoteUrl"
        if [ $? -eq 0 ]; then
            echo "✓ 远程仓库配置成功"
        else
            echo "警告: 远程仓库配置失败"
        fi
    fi
fi

# 6. 首次提交
echo ""
echo "6. 准备首次提交..."
read -p "是否现在进行首次提交? (y/n) " doCommit
if [ "$doCommit" = "y" ]; then
    git add .
    git commit -m "chore: 初始化项目

- 创建Clean Architecture目录结构
- 配置Gradle构建文件
- 添加基础依赖（Compose, Material3）
- 创建开发文档（PRD, 开发计划, Git工作流程）"
    
    if [ $? -eq 0 ]; then
        echo "✓ 首次提交成功"
    else
        echo "警告: 首次提交失败"
    fi
fi

echo ""
echo "=== 初始化完成 ==="
echo ""
echo "下一步操作:"
echo "1. 如果已配置远程仓库，执行: git push -u origin develop"
echo "2. 查看Git工作流程文档: docs/GIT_WORKFLOW.md"
echo "3. 查看快速上手指南: docs/GIT_QUICK_START.md"
