# 多阶段构建 Dockerfile
#
# 构建命令（在本机联网环境执行，构建 linux/amd64 镜像并导出）:
#   docker build --platform linux/amd64 -t httprun-java:1.0.0 .
#   docker save -o httprun-java-1.0.0.tar httprun-java:1.0.0
#   docker save -o postgres-16-alpine.tar postgres:16-alpine
#
# 目标服务器（内网，无需联网）:
#   docker load -i httprun-java-1.0.0.tar
#   docker load -i postgres-16-alpine.tar
#   docker compose up -d

# 阶段1: 构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 并下载依赖(利用缓存)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制前端源码（供 frontend-maven-plugin 构建）
COPY webapp ./webapp

# 复制后端源码并构建（含前端打包）
COPY src ./src
RUN mvn clean package -DskipTests -B

# 阶段2: 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 安装必要工具
RUN apk add --no-cache curl bash

# 创建非 root 用户
RUN addgroup -S httprun && adduser -S httprun -G httprun

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /var/log/httprun && chown -R httprun:httprun /var/log/httprun

# 设置权限
RUN chown -R httprun:httprun /app

# 切换到非 root 用户
USER httprun

# 暴露端口（与 application.yml server.port 一致）
EXPOSE 8081

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/api/health || exit 1

# JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
# Spring Profile（可通过 docker run -e SPRING_PROFILES_ACTIVE=xxx 覆盖）
ENV SPRING_PROFILES_ACTIVE="prod"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
