package com.httprun.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${httprun.webapp-build-dir:./webapp/dist}")
    private String webappBuildDir;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 获取绝对路径
        Path absolutePath = Paths.get(webappBuildDir).toAbsolutePath();
        String resourceLocation = "file:" + absolutePath.toString().replace("\\", "/") + "/";

        // 静态资源映射（前端构建产物）
        // UmiJS 构建产物的各种资源
        registry.addResourceHandler("/**")
                .addResourceLocations(resourceLocation)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
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
                        Path indexPath = absolutePath.resolve("index.html");
                        Resource indexResource = new FileSystemResource(indexPath);
                        if (indexResource.exists()) {
                            return indexResource;
                        }

                        return null;
                    }
                });
    }
}
