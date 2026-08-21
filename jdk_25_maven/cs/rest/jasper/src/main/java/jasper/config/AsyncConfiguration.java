package jasper.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.autoconfigure.task.TaskSchedulingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration implements AsyncConfigurer, SchedulingConfigurer {
	private final Logger logger = LoggerFactory.getLogger(AsyncConfiguration.class);

	@Autowired
	TaskExecutionProperties taskExecutionProperties;

	@Autowired
	TaskSchedulingProperties taskSchedulingProperties;

	@Bean("taskExecutor")
	@Override
	public ExecutorService getAsyncExecutor() {
		return newVirtualThreadPerTaskExecutor();
	}

	@Bean("taskScheduler")
	public ThreadPoolTaskScheduler getSchedulerExecutor() {
		var scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(taskSchedulingProperties.getPool().getSize());
		scheduler.setThreadNamePrefix(taskSchedulingProperties.getThreadNamePrefix());
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setAwaitTerminationSeconds(60);
		scheduler.initialize();
		return scheduler;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setTaskScheduler(getSchedulerExecutor());
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return new SimpleAsyncUncaughtExceptionHandler();
	}
}
