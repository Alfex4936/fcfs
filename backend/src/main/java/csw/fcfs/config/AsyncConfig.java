package csw.fcfs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        // Virtual Threads 사용 (Java 21+)
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "claimExecutor")
    public Executor claimExecutor() {
        // Virtual Threads 사용 (Java 21+)
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // Platform Threads가 필요한 경우를 위한 백업 executor
    @Bean(name = "platformExecutor")
    public Executor platformExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("fcfs-platform-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
