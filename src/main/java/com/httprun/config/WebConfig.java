package com.httprun.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置
 * 静态资源查找策略：优先从本地文件系统（webapp/dist）读取，找不到时回退到 classpath:/static/（jar 包内资源）
 * 不以静态资源目录是否存在来区分环境
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);
    private static final String CLASSPATH_STATIC_ROOT = "static/";

    @Value("${httprun.webapp-build-dir:./webapp/dist}")
    private String webappBuildDir;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        Path devPath = Paths.get(webappBuildDir).toAbsolutePath();
        String fileLocation = "file:" + devPath.toString().replace("\\", "/") + "/";

        logStaticResourceInfo(devPath, fileLocation);

        // 同时注册两个资源位置：本地文件系统优先，classpath 兜底
        registry.addResourceHandler("/**")
                .addResourceLocations(fileLocation, "classpath:/static/")
                .resourceChain(true)
                .addResolver(createFallbackResourceResolver(devPath));
    }

    /**
     * 创建支持双重回退的资源解析器
     * 查找顺序：本地文件系统 → classpath:/static/ → SPA index.html 回退
     */
    private PathResourceResolver createFallbackResourceResolver(Path localBasePath) {
        return new PathResourceResolver() {
            @Override
            protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location)
                    throws IOException {
                Resource requestedResource = resolveStaticResource(resourcePath, localBasePath);
                if (requestedResource != null) {
                    return requestedResource;
                }

                // 对于 API 请求，不做处理
                if (resourcePath.startsWith("api/")) {
                    return null;
                }

                // 静态资源路径（包含后缀）不做 SPA 回退
                if (isAssetRequest(resourcePath)) {
                    return null;
                }

                // 2. SPA 路由回退：优先本地 index.html，其次 classpath 中的 index.html
                Path localIndex = localBasePath.resolve("index.html");
                Resource localIndexResource = new FileSystemResource(localIndex);
                if (localIndexResource.exists() && localIndexResource.isReadable()) {
                    return localIndexResource;
                }

                Resource classpathIndex = new ClassPathResource("static/index.html");
                if (classpathIndex.exists() && classpathIndex.isReadable()) {
                    return classpathIndex;
                }

                return null;
            }
        };
    }

    private Resource resolveStaticResource(String resourcePath, Path localBasePath) {
        Path localPath = localBasePath.resolve(resourcePath);
        // 只返回常规文件，不返回目录
        if (Files.exists(localPath) && Files.isReadable(localPath) && Files.isRegularFile(localPath)) {
            return new FileSystemResource(localPath);
        }

        Resource classpathResource = new ClassPathResource(CLASSPATH_STATIC_ROOT + resourcePath);
        if (classpathResource.exists() && classpathResource.isReadable()) {
            return classpathResource;
        }

        return null;
    }

    private boolean isAssetRequest(String resourcePath) {
        int lastSlash = resourcePath.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? resourcePath.substring(lastSlash + 1) : resourcePath;
        return fileName.contains(".");
    }

    private void logStaticResourceInfo(Path localBasePath, String fileLocation) {
        Path localIndex = localBasePath.resolve("index.html");
        boolean localDirExists = Files.isDirectory(localBasePath);
        boolean localIndexExists = Files.exists(localIndex) && Files.isReadable(localIndex);
        boolean classpathIndexExists = new ClassPathResource(CLASSPATH_STATIC_ROOT + "index.html").exists();

        log.info(
                "Static resources: localDir={} (exists={}, indexExists={}), classpathIndexExists={}, locations=[{}, classpath:/static/]",
                localBasePath,
                localDirExists,
                localIndexExists,
                classpathIndexExists,
                fileLocation);
    }
}
