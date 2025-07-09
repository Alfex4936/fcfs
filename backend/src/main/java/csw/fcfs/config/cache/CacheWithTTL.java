package csw.fcfs.config.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheWithTTL {
    String value() default "";
    String key() default "";
    long ttl() default 10;
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    // 조건부 캐싱
    String condition() default "";
    String unless() default "";
}
