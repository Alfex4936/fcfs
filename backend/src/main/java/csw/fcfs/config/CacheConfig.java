package csw.fcfs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // 우리가 만든 최적화된 ObjectMapper를 사용하는 직렬화기
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 기본 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 기본 10분
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer)) // 우리의 ObjectMapper 사용
                .disableCachingNullValues()
                .prefixCacheNameWith("fcfs:");

        // 캐시별 개별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // User 캐시 - 짧은 TTL (자주 변경되는 데이터)
        cacheConfigurations.put("users", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));

        // Post 캐시 - 긴 TTL (상대적으로 안정적인 데이터)
        cacheConfigurations.put("posts", defaultConfig
                .entryTtl(Duration.ofHours(1)));

        // 인기 게시물 캐시 - 매우 짧은 TTL (실시간성 중요)
        cacheConfigurations.put("popular-posts", defaultConfig
                .entryTtl(Duration.ofMinutes(2)));

        // 통계 캐시 - 중간 TTL
        cacheConfigurations.put("statistics", defaultConfig
                .entryTtl(Duration.ofMinutes(15)));

        // 설정 캐시 - 매우 긴 TTL (거의 변경되지 않음)
        cacheConfigurations.put("settings", defaultConfig
                .entryTtl(Duration.ofHours(6)));

        // 페이지네이션 캐시 - 짧은 TTL (실시간성 중요)
        cacheConfigurations.put("posts-page", defaultConfig
                .entryTtl(Duration.ofMinutes(3)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
