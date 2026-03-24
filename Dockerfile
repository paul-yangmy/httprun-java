# Dockerfile.local - 内网环境使用本地预编译 JAR 构建镜像
# 仅包含运行时阶段，跳过 Maven/Node 在线构建
#
# !! 内网注意事项 !!
# 推荐流程（Windows 上一次性完成构建 + 导出）:
#   1. mvn clean package -DskipTests               # 生成 target/*.jar
#   2. $env:DOCKER_DEFAULT_PLATFORM="linux/amd64"
#      docker build --platform linux/amd64 -f Dockerfile.local -t httprun-java:1.0.0 .
#   3. docker save -o httprun-java-1.0.0.tar httprun-java:1.0.0
#      docker save -o postgres-16-alpine.tar postgres:16-alpine
#
# 服务器部署（无需 docker build）:
#   docker load -i httprun-java-1.0.0.tar
#   docker load -i postgres-16-alpine.tar
#   docker compose -f docker-compose.deploy.yml up -d

FROM eclipse-temurin:17-jre-alpine

# 安装必要工具
RUN apk add --no-cache curl bash

# 创建非 root 用户
RUN addgroup -S httprun && adduser -S httprun -G httprun

# 设置工作目录
WORKDIR /app

# 从本地 target/ 目录复制已编译的 JAR 文件
COPY target/*.jar app.jar

# 创建日志目录并设置权限
RUN mkdir -p /var/log/httprun \
    && chown -R httprun:httprun /var/log/httprun \
    && chown -R httprun:httprun /app

# 切换到非 root 用户
USER httprun

# 暴露端口（与 application.yml server.port 一致）
EXPOSE 8081

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/api/health || exit 1

# JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

# Spring Profile（可通过 docker run -e SPRING_PROFILES_ACTIVE=xxx 覆盖）
ENV SPRING_PROFILES_ACTIVE="prod"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]