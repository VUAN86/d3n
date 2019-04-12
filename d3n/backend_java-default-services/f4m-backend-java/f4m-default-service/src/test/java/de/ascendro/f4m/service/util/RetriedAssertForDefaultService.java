package de.ascendro.f4m.service.util;

public final class RetriedAssertForDefaultService {
	public static final int DEFAULT_TIMEOUT_MS = 2 * 1000;
	public static final int DEFAULT_INTERVAL_MS = 20;

	private RetriedAssertForDefaultService() {
	}

	public static void assertWithWait(Assertion assertion) {
		assertWithWait(assertion, DEFAULT_TIMEOUT_MS, DEFAULT_INTERVAL_MS);
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
		long stopAt = System.currentTimeMillis() + timeOutMs;
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
		return assertion;
	}

	public interface Assertion {
		void executeAssert();
	}
}