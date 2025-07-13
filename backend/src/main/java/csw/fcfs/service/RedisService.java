package csw.fcfs.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.ClusterCommandExecutionFailureException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import io.lettuce.core.RedisException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    private String claimScriptSha1;
    private String declaimScriptSha1;

    @PostConstruct
    public void loadScriptSha1s() {
        claimScriptSha1   = loadAndBroadcast("claim.lua");
        declaimScriptSha1 = loadAndBroadcast("declaim.lua");
    }

    private String loadAndBroadcast(String file) {
        byte[] script = loadScript(file).getBytes(StandardCharsets.UTF_8);

        return redisTemplate.execute((RedisCallback<String>) conn ->
            conn.scriptingCommands().scriptLoad(script)    // ← 모든 master에 전파
        );
    }

    public String getClaimScriptSha1() {
        return claimScriptSha1;
    }

    public String getDeclaimScriptSha1() {
        return declaimScriptSha1;
    }

    public String loadScript(String filename) {
        try (var is = getClass().getClassLoader().getResourceAsStream("scripts/" + filename)) {
            if (is == null) {
                throw new IllegalStateException("Script not found: " + filename);
            }
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Could not load script: " + filename, e);
        }
    }

    /**
     * Execute a Lua script with proper error handling for both single Redis and cluster
     */
    public String executeScript(String scriptContent, List<String> keys, String... args) {
        try {
            DefaultRedisScript<String> script = new DefaultRedisScript<>();
            script.setScriptText(scriptContent);
            script.setResultType(String.class);

            log.debug("Executing Redis script with keys: {} and args: {}", keys, List.of(args));
            String result = redisTemplate.execute(script, keys, (Object[]) args);
            log.debug("Script execution successful, result: {}", result);
            return result;

        } catch (ClusterCommandExecutionFailureException e) {
            log.error("Redis cluster command execution failed for keys: {} - {}", keys, e.getMessage(), e);
            throw new RedisOperationException("Redis cluster operation failed", e);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed for keys: {} - {}", keys, e.getMessage(), e);
            throw new RedisOperationException("Redis connection failed", e);
        } catch (RedisException e) {
            log.error("Redis operation failed for keys: {} - {}", keys, e.getMessage(), e);
            log.error("Redis operation failed for keys: {} - {}", keys, e.getMessage(), e);
            throw new RedisOperationException("Redis operation failed", e);
        } catch (Exception e) {
            log.error("Unexpected error executing Redis script with keys: {} and args: {}", keys, List.of(args), e);
            throw new RedisOperationException("Failed to execute Redis script", e);
        }
    }

    /**
     * Execute a Lua script by SHA1 (EVALSHA), fallback to full script if needed
     */
    public String executeScriptBySha1(String sha1, List<String> keys, String... args) {
        return redisTemplate.execute((RedisCallback<String>) conn -> {
            Object raw = conn.scriptingCommands().evalSha(
                sha1.getBytes(StandardCharsets.UTF_8),
                ReturnType.VALUE,
                keys.size(),
                concatKeysAndArgs(keys, args)
            );

            if (raw == null) return null;

            // bulk string → String
            if (raw instanceof byte[] bytes) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            return raw.toString();
        });
    }

    /**
     * Helper to concatenate keys and args as required by scriptingCommands().evalSha
     */
    private byte[][] concatKeysAndArgs(List<String> keys, String... args) {
        byte[][] result = new byte[keys.size() + args.length][];
        int i = 0;
        for (String key : keys) {
            result[i++] = key.getBytes();
        }
        for (String arg : args) {
            result[i++] = arg.getBytes();
        }
        return result;
    }

    /**
     * Get set size with proper error handling
     */
    public Long getSetSize(String key) {
        try {
            log.debug("Getting set size for key: {}", key);
            return redisTemplate.opsForSet().size(key);
        } catch (ClusterCommandExecutionFailureException e) {
            log.error("Redis cluster error getting set size for key: {} - {}", key, e.getMessage(), e);
            throw new RedisOperationException("Failed to get set size from cluster", e);
        } catch (Exception e) {
            log.error("Error getting set size for key: {}", key, e);
            throw new RedisOperationException("Failed to get set size", e);
        }
    }

    /**
     * Check if member exists in set
     */
    public Boolean isMember(String key, String member) {
        try {
            return redisTemplate.opsForSet().isMember(key, member);
        } catch (Exception e) {
            log.error("Error checking membership for key: {} and member: {}", key, member, e);
            throw new RedisOperationException("Failed to check set membership", e);
        }
    }

    /**
     * Get all members of a set
     */
    public java.util.Set<String> getSetMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Error getting set members for key: {}", key, e);
            throw new RedisOperationException("Failed to get set members", e);
        }
    }

    /**
     * Delete keys
     */
    public void deleteKeys(String... keys) {
        try {
            redisTemplate.delete(List.of(keys));
        } catch (Exception e) {
            log.error("Error deleting keys: {}", List.of(keys), e);
            throw new RedisOperationException("Failed to delete keys", e);
        }
    }

    /**
     * Get a string value from Redis
     */
    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error getting value for key: {}", key, e);
            throw new RedisOperationException("Failed to get value", e);
        }
    }

    public static class RedisOperationException extends RuntimeException {
        public RedisOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
