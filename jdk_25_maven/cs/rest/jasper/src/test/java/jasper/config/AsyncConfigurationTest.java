package jasper.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.autoconfigure.task.TaskSchedulingProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link AsyncConfiguration} class.
 */
class AsyncConfigurationTest {

    AsyncConfiguration asyncConfiguration;
    TaskExecutionProperties taskExecutionProperties;
    TaskSchedulingProperties taskSchedulingProperties;

    @BeforeEach
    void setup() {
        asyncConfiguration = new AsyncConfiguration();
        taskExecutionProperties = new TaskExecutionProperties();
        taskExecutionProperties.setThreadNamePrefix("async-");
        taskExecutionProperties.getPool().setCoreSize(8);
        taskExecutionProperties.getPool().setMaxSize(32);
        taskExecutionProperties.getPool().setQueueCapacity(100);
        asyncConfiguration.taskExecutionProperties = taskExecutionProperties;
        taskSchedulingProperties = new TaskSchedulingProperties();
        taskSchedulingProperties.setThreadNamePrefix("scheduler-");
        taskSchedulingProperties.getPool().setSize(2);
        asyncConfiguration.taskSchedulingProperties = taskSchedulingProperties;
    }

    @Test
    void shouldCreateAsyncExecutor() {
        ExecutorService executor = asyncConfiguration.getAsyncExecutor();

        assertThat(executor).isNotNull();
        verifyVirtualThreadExecutor(executor);
    }

    @Test
    void shouldCreateGetSchedulerExecutor() {
        ThreadPoolTaskScheduler scheduler = asyncConfiguration.getSchedulerExecutor();

        assertThat(scheduler).isNotNull();
        assertThat(scheduler.getThreadNamePrefix()).isEqualTo("scheduler-");
    }

    /**
     * Verify that the executor uses virtual threads by submitting a task and checking thread properties.
     */
    private void verifyVirtualThreadExecutor(ExecutorService executor) {
        AtomicBoolean isVirtual = new AtomicBoolean(false);
        try {
            executor.submit(() -> {
                isVirtual.set(Thread.currentThread().isVirtual());
            }).get();
            assertThat(isVirtual.get()).isTrue();
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify virtual thread executor", e);
        }
    }
}
