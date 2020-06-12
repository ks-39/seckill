package com.ks39.seckill.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;


@Aspect     //aop切面
@Component      //spring组件
public class controllerAop {

    private final Logger LOGGER = LoggerFactory.getLogger(controllerAop.class) ;

    //指定aspect()方法为所有Controller的切面
    @Pointcut("execution(public * com.ks39.seckill.controller..*(..))")
    public void aspect(){}

    //在aspect切面之前执行
    @Before("aspect()")
    public void before(JoinPoint joinPoint){

        //接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();


        //3. 打印日志记录
        System.out.println('\n'+"开始记录:");
        LOGGER.info("URL : " + request.getRequestURI().toString());
        LOGGER.info("HTTP_METHOD : " + request.getMethod());
        LOGGER.info("IP : " + request.getRemoteAddr());
        LOGGER.info("CLASS_METHOD : " + joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        LOGGER.info(":" + joinPoint.getSignature() );
        LOGGER.info("ARGS : " + Arrays.toString(joinPoint.getArgs()));
    }

    //在aspect切面后执行
    @AfterReturning(returning = "result", pointcut = "aspect()")
    public void afterReturning(JoinPoint joinPoint,Object result){

        //接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        LOGGER.info("RESPONSE : " + result);
    }

    //在aspect切面后执行
    @AfterThrowing(throwing = "exception", pointcut = "aspect()")
    public void afterThrowing(JoinPoint joinPoint,Throwable exception){

        //接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        LOGGER.info("EXCEPTION : " + exception);
    }
}
