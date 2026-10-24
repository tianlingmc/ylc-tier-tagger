#!/usr/bin/env bash
# 一键出两个包：
#   dist/1.20.5-1.21.11/YLCTierTagger-<版本>-fabric-mc1.21.11.jar   Java 21
#   dist/1.20.1-1.20.4/YLCTierTagger-<版本>-fabric-mc1.20.4.jar     Java 17
# 两个包版本号一致（第二遍用 -PnoVersionBump 固定住），
# 文件名里的 -mc 后缀由构建时按目标 MC 版本自动加上。
#
# 用法：
#   bash tools/build-all.sh            正常构建，版本号第三位自动 +1
#   bash tools/build-all.sh --no-bump  版本号不动（发正式版时用）

set -e
cd "$(dirname "$0")/.."

# 正式版：两遍都不动版本号
NO_BUMP=""
if [ "${1:-}" = "--no-bump" ]; then
  NO_BUMP="-PnoVersionBump"
  echo "==> 版本号保持不变"
fi

echo "==> 编译 1.20.5 ~ 1.21.11（Java 21）"
./gradlew build --no-daemon $NO_BUMP

VERSION=$(grep '^mod_version=' gradle.properties | cut -d'=' -f2)
echo "==> 版本号 ${VERSION}"

mkdir -p "dist/1.20.5-1.21.11"
rm -f "dist/1.20.5-1.21.11/"*.jar
cp "build/libs/YLCTierTagger-${VERSION}-fabric-mc1.21.11.jar" "dist/1.20.5-1.21.11/"

echo "==> 编译 1.20.1 ~ 1.20.4（Java 17）"
./gradlew build --no-daemon \
  -PnoVersionBump \
  -PmcVersion=1.20.4 \
  -PyarnVersion=1.20.4+build.3 \
  -PjavaRelease=17 \
  -PfabricApiVersion=0.97.3+1.20.4 \
  -PmodmenuVersion=9.2.0

mkdir -p "dist/1.20.1-1.20.4"
rm -f "dist/1.20.1-1.20.4/"*.jar
cp "build/libs/YLCTierTagger-${VERSION}-fabric-mc1.20.4.jar" "dist/1.20.1-1.20.4/"

echo
echo "==> 完成"
ls -l dist/*/
