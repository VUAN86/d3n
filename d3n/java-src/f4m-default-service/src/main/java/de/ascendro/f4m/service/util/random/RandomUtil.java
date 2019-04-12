package de.ascendro.f4m.service.util.random;


public interface RandomUtil {
	
	/**
	 * Pick random number from range 0..n-1
	 * @param n Max boundary exclusive
	 * @return Random number from range 0..n-1
	 */
	int nextInt(int n);
	
	/**
	 * Pick random number from range, using Blum Blum Shub algorithm.
	 * @return Random number from range startInclusive..endExclusive - 1
	 */
	int nextBlumBlumShubInt(int startInclusive, int endExclusive);
	
	/**
	 * Pick random number from range 0..n-1
	 * @param n Max boundary exclusive
	 * @return Random number from range 0..n-1
	 */
	long nextLong(long n);
	
}
