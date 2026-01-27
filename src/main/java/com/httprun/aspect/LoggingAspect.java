package com.httprun.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 日志切面
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Controller 层切点
     */
    @Pointcut("execution(* com.httprun.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * Service 层切点
     */
    @Pointcut("execution(* com.httprun.service.impl..*.*(..))")
    public void servicePointcut() {
    }

    /**
     * 记录 Controller 方法执行
     */
    @Around("controllerPointcut()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.info("==> {}.{}() - args: {}", className, methodName,
                Arrays.toString(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("<== {}.{}() - duration: {}ms", className, methodName, duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("<== {}.{}() - failed after {}ms: {}",
                    className, methodName, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * 记录 Service 方法执行（仅记录异常）
     */
    @Around("servicePointcut()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            log.error("Service error in {}.{}(): {}", className, methodName, e.getMessage());
            throw e;
        }
    }
}
