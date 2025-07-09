package csw.fcfs.config.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class CacheWithTTLAspect {

    private final StringRedisTemplate redisTemplate;

    public CacheWithTTLAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(cacheWithTTL)")
    public Object cache(ProceedingJoinPoint joinPoint, CacheWithTTL cacheWithTTL) throws Throwable {
        // 캐시 키 생성
        String cacheKey = generateCacheKey(joinPoint, cacheWithTTL);

        // 캐시에서 값 조회
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            // JSON 역직렬화 필요시 ObjectMapper 사용
            return deserializeValue(cached, joinPoint.getSignature().getDeclaringType());
        }

        // 캐시 미스 시 메서드 실행
        Object result = joinPoint.proceed();

        // 결과를 캐시에 저장 (TTL 포함)
        String serializedResult = serializeValue(result);
        redisTemplate.opsForValue().set(
            cacheKey,
            serializedResult,
            cacheWithTTL.ttl(),
            cacheWithTTL.timeUnit()
        );

        return result;
    }

    private String generateCacheKey(ProceedingJoinPoint joinPoint, CacheWithTTL annotation) {
        String prefix = annotation.value().isEmpty() ?
            joinPoint.getSignature().getDeclaringTypeName() : annotation.value();
        String methodName = joinPoint.getSignature().getName();
        String args = String.valueOf(joinPoint.getArgs()[0]); // 첫 번째 파라미터를 키로 사용

        return String.format("fcfs:%s:%s:%s", prefix, methodName, args);
    }

    private String serializeValue(Object value) {
        // 간단한 toString() 또는 JSON 직렬화 사용
        return value.toString();
    }

    private Object deserializeValue(String value, Class<?> returnType) {
        // 간단한 구현 - 실제로는 JSON 파싱 필요
        return value;
    }
}
