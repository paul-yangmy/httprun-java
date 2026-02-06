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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置
 * 支持开发环境（从文件系统读取）和生产环境（从classpath读取）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${httprun.webapp-build-dir:./webapp/dist}")
    private String webappBuildDir;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 检查开发环境目录是否存在
        Path devPath = Paths.get(webappBuildDir).toAbsolutePath();
        boolean isDevMode = Files.exists(devPath);

        if (isDevMode) {
            // 开发环境：从文件系统读取
            String resourceLocation = "file:" + devPath.toString().replace("\\", "/") + "/";
            registry.addResourceHandler("/**")
                    .addResourceLocations(resourceLocation)
                    .resourceChain(true)
                    .addResolver(createFileSystemResourceResolver(devPath));
        } else {
            // 生产环境：从classpath读取
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/")
                    .resourceChain(true)
                    .addResolver(createClasspathResourceResolver());
        }
    }

    /**
     * 创建文件系统资源解析器（开发环境）
     */
    private PathResourceResolver createFileSystemResourceResolver(Path basePath) {
        return new PathResourceResolver() {
            @Override
            protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location)
                    throws IOException {
                Resource requestedResource = location.createRelative(resourcePath);

                // 如果请求的资源存在，返回它
                if (requestedResource.exists() && requestedResource.isReadable()) {
                    return requestedResource;
                }

                // 对于 API 请求，不做处理
                if (resourcePath.startsWith("api/")) {
                    return null;
                }

                // 对于 SPA 路由，返回 index.html
                Path indexPath = basePath.resolve("index.html");
                Resource indexResource = new FileSystemResource(indexPath);
                if (indexResource.exists()) {
                    return indexResource;
                }

                return null;
            }
        };
    }

    /**
     * 创建classpath资源解析器（生产环境）
     */
    private PathResourceResolver createClasspathResourceResolver() {
        return new PathResourceResolver() {
            @Override
            protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location)
                    throws IOException {
                Resource requestedResource = location.createRelative(resourcePath);

                // 如果请求的资源存在，返回它
                if (requestedResource.exists() && requestedResource.isReadable()) {
                    return requestedResource;
                }

                // 对于 API 请求，不做处理
                if (resourcePath.startsWith("api/")) {
                    return null;
                }

                // 对于 SPA 路由，返回 index.html
                Resource indexResource = new ClassPathResource("static/index.html");
                if (indexResource.exists()) {
                    return indexResource;
                }

                return null;
            }
        };
    }
}
