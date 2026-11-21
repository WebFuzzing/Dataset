package reciter.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Data;

/**
 * @author szd2013
 * 
 * Introduce delay for pubmed api calls
 *
 */
@Data
public class ThreadDelay {
	private static final Logger log = LoggerFactory.getLogger(ThreadDelay.class);
	
	/**
	 * @param timeout Timeout for the delay
	 */
	public static void pubmedApiDelay(long timeout) {
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {
			log.error("InterruptedException", e);
		}
	}

}
