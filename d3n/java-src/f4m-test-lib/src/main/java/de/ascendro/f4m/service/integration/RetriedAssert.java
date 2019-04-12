package de.ascendro.f4m.service.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RetriedAssert {
	private static final Logger LOGGER = LoggerFactory.getLogger(RetriedAssert.class);
	
	public static final int DEFAULT_TIMEOUT_MS = 2 * 1000;
	public static final int DEFAULT_INTERVAL_MS = 20;

	private RetriedAssert() {
	}

	public static void assertWithWait(Assertion assertion) {
		assertWithWait(assertion, DEFAULT_TIMEOUT_MS, DEFAULT_INTERVAL_MS);
	}
	
	public static void assertWithWait(Assertion assertion, int timeOutMs) {
		assertWithWait(assertion, timeOutMs, DEFAULT_INTERVAL_MS);
	}
	
	public static void assertWithWait(Assertion assertion, int timeOutMs, int intervalMs) {
		waitToGetAssertSuccessOrTimeout(assertion, timeOutMs, intervalMs);
		// All tries have failed so far. Try one last time,
		// now letting any failure pass out to the caller.
		assertion.executeAssert();
	}

	public static Assertion waitToGetAssertSuccessOrTimeout(Assertion assertion) {
		return waitToGetAssertSuccessOrTimeout(assertion, DEFAULT_TIMEOUT_MS, DEFAULT_INTERVAL_MS);
	}

	public static Assertion waitToGetAssertSuccessOrTimeout(Assertion assertion, int timeOutMs, int intervalMs) {
		long assertExecutionStartTime = System.currentTimeMillis();
		long stopAt = assertExecutionStartTime + timeOutMs;
		boolean assertFailing = true;
		while (assertFailing && System.currentTimeMillis() < stopAt) {
			try {
				assertion.executeAssert();
				assertFailing = false;
			} catch (AssertionError ignoreAndRetry) {
				//ignore and retry
			}
			try {
				Thread.sleep(intervalMs);
			} catch (InterruptedException ie) {
				//ignore interruptions
			}
		}
		long timeSpent = System.currentTimeMillis() - assertExecutionStartTime;
		if (timeSpent > timeOutMs / 2) {
			LOGGER.warn("Finished executing assert after more than a half of timeout ({} ms) with failure {}",
					timeSpent, assertFailing);
		}
		return assertion;
	}

	@FunctionalInterface
	public interface Assertion {
		void executeAssert();
	}
}