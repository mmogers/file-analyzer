package lv.marmog.test.fileAnalyzer.log;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlbeans.InterfaceExtension;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class InjectLogAspect {

    @Around("@annotation (InjectLog)")
    public Object injectLogs(ProceedingJoinPoint joinPoint) throws Throwable {
        final long initTime = System.currentTimeMillis();
        log.info("BEGIN {}, {} with input params: {}", joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName(), joinPoint.getArgs());

        Object result = joinPoint.proceed();
        boolean printResult = ((MethodSignature) joinPoint.getSignature()).getMethod().getAnnotation(InjectLog.class).printResult();

        log.info("END {}, {} in {} ms with data : {}", joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName(), System.currentTimeMillis() - initTime, (printResult ? result : "HIDDEN"));
        return result;

    }
}
