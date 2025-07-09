package csw.fcfs.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@Slf4j
public class RedisConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.cluster.enabled", havingValue = "true")
    public RedisConnectionFactory redisClusterConnectionFactory(RedisProperties redisProperties) {
        log.info("Configuring Redis in CLUSTER mode");

        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();

        // Add cluster nodes from properties
        if (redisProperties.getCluster() != null && redisProperties.getCluster().getNodes() != null) {
            redisProperties.getCluster().getNodes().forEach(node -> {
                String[] parts = node.split(":");
                String host = parts[0];
                int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6379;
                clusterConfig.clusterNode(host, port);
                log.info("Added cluster node: {}:{}", host, port);
            });
        }

        // Set max redirects for cluster
        if (redisProperties.getCluster().getMaxRedirects() != null) {
            clusterConfig.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());
        }

        // Set cluster password if present
        if (redisProperties.getPassword() != null) {
            clusterConfig.setPassword(redisProperties.getPassword());
            log.info("Redis cluster password configured");
        }

        // Set cluster username if present
        if (redisProperties.getUsername() != null) {
            clusterConfig.setUsername(redisProperties.getUsername());
            log.info("Redis cluster username configured: {}", redisProperties.getUsername());
        }

        return new LettuceConnectionFactory(clusterConfig);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.cluster.enabled", havingValue = "false", matchIfMissing = true)
    @Primary
    public RedisConnectionFactory redisStandaloneConnectionFactory(RedisProperties redisProperties) {
        log.info("Configuring Redis in STANDALONE mode");

        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName(redisProperties.getHost() != null ? redisProperties.getHost() : "localhost");
        standaloneConfig.setPort(redisProperties.getPort() != 0 ? redisProperties.getPort() : 6379);

        // Set password if present
        if (redisProperties.getPassword() != null) {
            standaloneConfig.setPassword(redisProperties.getPassword());
            log.info("Redis standalone password configured");
        }

        // Set username if present
        if (redisProperties.getUsername() != null) {
            standaloneConfig.setUsername(redisProperties.getUsername());
            log.info("Redis standalone username configured: {}", redisProperties.getUsername());
        }

        // Set database if specified
        if (redisProperties.getDatabase() != 0) {
            standaloneConfig.setDatabase(redisProperties.getDatabase());
        }

        log.info("Redis standalone configured: {}:{}, database: {}, auth: {}",
                standaloneConfig.getHostName(),
                standaloneConfig.getPort(),
                standaloneConfig.getDatabase(),
                redisProperties.getPassword() != null || redisProperties.getUsername() != null ? "enabled" : "disabled");
        return new LettuceConnectionFactory(standaloneConfig);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
