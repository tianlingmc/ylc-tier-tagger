#!/usr/bin/env bash
# 一键出两个包：
#   dist/1.20.5-1.21.11/YLCTierTagger-<版本>-fabric.jar   Java 21
#   dist/1.20.1-1.20.4/YLCTierTagger-<版本>-fabric.jar   Java 17
# 两个包版本号一致（第二遍用 -PnoVersionBump 固定住）。

set -e
cd "$(dirname "$0")/.."

echo "==> 编译 1.20.5 ~ 1.21.11（Java 21）"
./gradlew build --no-daemon

VERSION=$(grep '^mod_version=' gradle.properties | cut -d'=' -f2)
echo "==> 版本号 ${VERSION}"

mkdir -p "dist/1.20.5-1.21.11"
rm -f "dist/1.20.5-1.21.11/YLCTierTagger-"*-fabric.jar
cp "build/libs/YLCTierTagger-${VERSION}-fabric.jar" "dist/1.20.5-1.21.11/"

echo "==> 编译 1.20.1 ~ 1.20.4（Java 17）"
./gradlew build --no-daemon \
  -PnoVersionBump \
  -PmcVersion=1.20.4 \
  -PyarnVersion=1.20.4+build.3 \
  -PjavaRelease=17 \
  -PfabricApiVersion=0.97.3+1.20.4 \
  -PmodmenuVersion=9.2.0

mkdir -p "dist/1.20.1-1.20.4"
rm -f "dist/1.20.1-1.20.4/YLCTierTagger-"*-fabric.jar
cp "build/libs/YLCTierTagger-${VERSION}-fabric.jar" "dist/1.20.1-1.20.4/"

echo
echo "==> 完成"
ls -l dist/*/
