#!/bin/bash

# Git Pre-commit Hook for Spring Boot Multi-Module Project
# 确保代码质量检查在提交前执行

echo "🔍 正在执行代码质量检查..."

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查Java文件是否存在
if [ ! -d "src/main/java" ]; then
    echo -e "${RED}❌ 未找到Java源代码目录${NC}"
    exit 1
fi

# 检查Maven是否可用
if ! command -v mvn &> /dev/null; then
    echo -e "${YELLOW}⚠️  未找到Maven，跳过Maven检查${NC}"
else
    echo "📦 检查Maven项目结构..."
    if [ ! -f "pom.xml" ]; then
        echo -e "${RED}❌ 未找到pom.xml文件${NC}"
        exit 1
    fi

    # 编译检查
    echo "🔨 编译项目..."
    if ! mvn compile -q; then
        echo -e "${RED}❌ 项目编译失败${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ 编译成功${NC}"

    # 运行测试
    echo "🧪 运行单元测试..."
    if ! mvn test -q; then
        echo -e "${RED}❌ 单元测试失败${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ 单元测试通过${NC}"
fi

# 检查代码格式（如果安装了Checkstyle）
if [ -f "checkstyle.xml" ] && command -v java &> /dev/null; then
    echo "📋 检查代码格式..."
    # 这里可以添加Checkstyle检查逻辑
fi

# 检查敏感信息
echo "🔒 检查敏感信息..."
if grep -r "password\|secret\|key\|token" src/main/resources/*.yml src/main/resources/*.properties 2>/dev/null | grep -v "#" | grep -v "application.yml"; then
    echo -e "${RED}❌ 发现可能的敏感信息，请检查配置文件${NC}"
    exit 1
fi

# 检查TODO和FIXME注释
TODO_COUNT=$(grep -r "TODO\|FIXME" src/main/java --include="*.java" | wc -l)
if [ $TODO_COUNT -gt 0 ]; then
    echo -e "${YELLOW}⚠️  发现 $TODO_COUNT 个TODO/FIXME注释，建议处理后再提交${NC}"
    grep -r "TODO\|FIXME" src/main/java --include="*.java"
fi

# 检查文件大小（超过500KB的文件）
echo "📁 检查大文件..."
find src/main/java -name "*.java" -size +500k | while read file; do
    echo -e "${YELLOW}⚠️  文件过大: $file${NC}"
done

# 检查注释覆盖率（简单检查）
echo "📝 检查注释覆盖率..."
JAVA_FILES=$(find src/main/java -name "*.java" | wc -l)
COMMENTED_LINES=$(grep -r "/\*\*\|//\| \*" src/main/java --include="*.java" | wc -l)
TOTAL_LINES=$(find src/main/java -name "*.java" -exec wc -l {} + | tail -1 | awk '{print $1}')

if [ $TOTAL_LINES -gt 0 ]; then
    COVERAGE=$((COMMENTED_LINES * 100 / TOTAL_LINES))
    if [ $COVERAGE -lt 20 ]; then
        echo -e "${YELLOW}⚠️  注释覆盖率较低: ${COVERAGE}%，建议增加注释${NC}"
    else
        echo -e "${GREEN}✅ 注释覆盖率: ${COVERAGE}%${NC}"
    fi
fi

# 检查中文注释比例
echo "🇨🇳 检查中文注释..."
CHINESE_COMMENTS=$(grep -r "[一-龯]" src/main/java --include="*.java" | wc -l)
if [ $CHINESE_COMMENTS -gt 0 ]; then
    echo -e "${GREEN}✅ 发现 $CHINESE_COMMENTS 行中文注释${NC}"
else
    echo -e "${YELLOW}⚠️  建议添加中文注释以提高可读性${NC}"
fi

# 检查项目结构
echo "📂 检查项目结构..."
EXPECTED_DIRS=("controller" "service" "mapper" "entity" "dto" "config" "common" "utils" "constant" "enums")
MISSING_DIRS=()

for dir in "${EXPECTED_DIRS[@]}"; do
    if [ ! -d "src/main/java/com/ziwen/moudle/$dir" ]; then
        MISSING_DIRS+=("$dir")
    fi
done

if [ ${#MISSING_DIRS[@]} -gt 0 ]; then
    echo -e "${YELLOW}⚠️  缺少目录: ${MISSING_DIRS[*]}${NC}"
else
    echo -e "${GREEN}✅ 项目结构完整${NC}"
fi

# 检查命名规范
echo "📏 检查命名规范..."
# 检查Controller命名
find src/main/java -name "*Controller.java" | while read file; do
    if [[ ! "$file" =~ Controller$ ]]; then
        echo -e "${RED}❌ Controller命名不规范: $file${NC}"
    fi
done

# 检查Service命名
find src/main/java -name "*Service.java" | while read file; do
    if [[ ! "$file" =~ Service$ ]]; then
        echo -e "${RED}❌ Service命名不规范: $file${NC}"
    fi
done

# 检查DTO命名
find src/main/java -name "*DTO.java" | while read file; do
    if [[ ! "$file" =~ DTO$ ]]; then
        echo -e "${RED}❌ DTO命名不规范: $file${NC}"
    fi
done

echo -e "${GREEN}🎉 代码质量检查完成！${NC}"
echo "📊 检查摘要："
echo "  - Java文件数量: $JAVA_FILES"
echo "  - TODO/FIXME数量: $TODO_COUNT"
echo "  - 中文注释行数: $CHINESE_COMMENTS"

exit 0