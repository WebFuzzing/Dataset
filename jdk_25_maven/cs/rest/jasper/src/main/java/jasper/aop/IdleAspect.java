package jasper.aop;

import jasper.config.Props;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Aspect that resets the idle timer when a REST controller method annotated
 * with @ClearIdle is invoked.
 */
@Aspect
@Component
public class IdleAspect {

	@Autowired
	Props props;

	private volatile Instant lastActivity = Instant.now();

	/**
	 * Record that the server received a REST API request.
	 */
	public void clearIdle() {
		lastActivity = Instant.now();
	}

	/**
	 * Check if the server has been idle for the configured amount of time.
	 */
	public boolean isIdle() {
		if (props.getBackfillIdleSec() <= 0) return true;
		return Instant.now().isAfter(lastActivity.plusSeconds(props.getBackfillIdleSec()));
	}

	/**
	 * Pointcut matching methods in classes annotated with @ClearIdle.
	 */
	@Pointcut("within(@jasper.aop.ClearIdle *)")
	public void clearIdleClass() {}

	/**
	 * Pointcut matching methods annotated with @ClearIdle.
	 */
	@Pointcut("@annotation(jasper.aop.ClearIdle)")
	public void clearIdleMethod() {}

	@Before("clearIdleClass() || clearIdleMethod()")
	public void resetIdleTimer() {
		clearIdle();
	}
}
