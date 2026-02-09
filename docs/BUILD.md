# 构建说明

## Maven 自动化构建流程

本项目已配置Maven自动化构建，一次性完成前后端的完整打包。

### 构建流程

执行 `mvn clean package` 时，会按以下顺序自动执行：

1. **安装 Node.js 和 npm**（如果系统中不存在）
   - Node.js: v20.11.0
   - npm: 10.8.0

2. **安装前端依赖**
   ```bash
   cd webapp && npm install
   ```

3. **构建前端项目**
   ```bash
   cd webapp && npm run build
   ```
   - 构建产物输出到 `webapp/dist/` 目录

4. **复制前端资源**
   - 将 `webapp/dist/` 的内容复制到 `target/classes/static/`

5. **编译Java代码并打包jar**
   - 最终jar包包含后端代码和前端静态资源

### 构建命令

#### 完整构建（推荐）
```bash
mvn clean package
```

#### 跳过测试
```bash
mvn clean package -DskipTests
```

#### 仅构建后端（跳过前端）
```bash
mvn clean package -DskipTests -Dfrontend-maven-plugin.skip=true
```

### 构建产物

构建完成后生成：
- `target/httprun-java-1.0.0.jar` - 包含前后端的完整应用

### 运行应用

```bash
java -jar target/httprun-java-1.0.0.jar
```

应用会自动：
- 启动后端服务（端口：8081）
- 提供前端静态页面服务

访问：http://localhost:8081/

## 静态资源处理

### WebConfig 智能资源加载

`WebConfig.java` 配置了双重资源位置，按优先级链式查找，不以静态资源目录是否存在来区分环境：

1. **本地文件系统** `file:./webapp/dist/` → 优先查找（支持开发热更新）
2. **classpath:/static/** → 本地未找到时回退到 jar 包内资源
3. **SPA 路由兜底**：`index.html` 同样先找本地、再找 classpath

#### 开发环境
- **行为**：优先从 `webapp/dist/` 读取，如果本地没有则从 jar 包内读取
- **优势**：支持前端热更新，无需重启后端

#### 生产环境（jar 包部署）
- **行为**：本地通常不存在 `webapp/dist/`，自动从 `classpath:/static/` 读取
- **优势**：单 jar 包部署，无需外部文件

#### 测试环境
- **行为**：即使 jar 包内已编译前端资源，本地 `webapp/dist/` 存在时仍优先使用
- **优势**：与开发一致的资源加载逻辑，方便调试

### SPA 路由支持

WebConfig 配置了路由回退机制：
- 静态资源（JS、CSS、图片等）：直接返回
- API 请求（`/api/*`）：交由Spring处理
- 其他路径：返回 `index.html`（支持前端路由）

## 开发模式

### 前端独立开发

如果需要前端热更新和快速迭代：

```bash
# 启动后端
mvn spring-boot:run

# 另一个终端启动前端
cd webapp
npm start
```

前端服务会运行在 http://localhost:8000，并自动代理API请求到后端（8081端口）。

### 联调模式

如果希望通过后端访问前端（模拟生产环境）：

```bash
# 构建前端
cd webapp
npm run build

# 启动后端
cd ..
mvn spring-boot:run
```

访问 http://localhost:8081/ 即可看到完整应用。

## 常见问题

### Q: 构建时前端报错？
A: 检查 Node.js 版本，确保系统网络可以访问npm registry。

### Q: 如何加速构建？
A: 
- 可以跳过前端构建：`mvn clean package -Dfrontend-maven-plugin.skip=true`
- 使用国内npm镜像：在 `webapp/.npmrc` 中配置

### Q: jar包中没有前端资源？
A: 检查 `target/classes/static/` 目录，确保前端构建成功且资源被正确复制。

### Q: 打包后访问页面404？
A: 
1. 确认 `target/classes/static/index.html` 存在
2. 检查 WebConfig.java 的配置
3. 查看应用日志，确认静态资源加载方式

## 构建优化建议

1. **CI/CD 环境**：使用 Docker 构建，预装 Node.js，避免每次下载
2. **开发环境**：配置 `-Dfrontend-maven-plugin.skip=true` 跳过前端构建，提高构建速度
3. **生产环境**：使用默认配置，确保前端资源完整打包

## 相关配置文件

- `pom.xml` - Maven构建配置
- `src/main/java/com/httprun/config/WebConfig.java` - 静态资源配置
- `src/main/resources/application.yml` - Spring Boot配置
- `webapp/package.json` - 前端构建配置
