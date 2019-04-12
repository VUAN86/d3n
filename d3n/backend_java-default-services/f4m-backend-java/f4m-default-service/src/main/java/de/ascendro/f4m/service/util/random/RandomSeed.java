package de.ascendro.f4m.service.util.random;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Properties;

import de.ascendro.f4m.service.util.random.bbs.BlumBlumShubException;

public class RandomSeed {
	private byte[] entropyPool;
	private int entropyRIdx;
	private int entropyWIdx;
	private volatile int entropyCount;

	public RandomSeed() {
		init();
		// Generate 64 bits of entropy as default
		for (int i = 0; i < 16; i++) {
			addEntropyBits((byte) spin(8), 4);
		}
	}

	private void init() {
		entropyPool = new byte[1200];
		entropyCount = 16;
		entropyRIdx = 0;
		entropyWIdx = 0;
		byte[] sysState = getSystemStateHash();
		System.arraycopy(sysState, 0, entropyPool, 0, sysState.length);
	}

	/**
	 * Add entropy to the pool of randomness.
	 * @param bits byte containing the random data to add
	 * @param count estimate how many bits of entropy the byte holds
	 */
	private synchronized void addEntropyBits(byte bits, int count) {
		entropyPool[entropyWIdx++] ^= bits;
		if (entropyWIdx == entropyPool.length) {
			entropyWIdx = 0;
		}
		entropyCount += count;
	}

	/**
     * Gets random bytes in a non-blocking way. Note that this
     * function may generate random numbers of lower quality if there
     * is not enough entropy in the pool.
     * @param numBytes number of bytes to extract
     * @return an array containing the requested number of random bytes.
     */
    public synchronized byte[] getBytes(int numBytes) {
		MessageDigest sha1 = null;
		try {
			sha1 = MessageDigest.getInstance("SHA1");
		} catch (Exception e) {
			throw new BlumBlumShubException("Error in RandomSeed, no sha1 hash", e);
		}

		int curLen = 0;
		byte[] bytes = new byte[numBytes];
		int offset = entropyRIdx;
		while (curLen < numBytes) {
			sha1.update((byte) System.currentTimeMillis());
			sha1.update(entropyPool, offset, 40); // estimate 4 bits/byte
			byte[] material = sha1.digest();
			System.arraycopy(material, 0, bytes, curLen,
					(numBytes - curLen > material.length) ? material.length : (numBytes - curLen));
			curLen += material.length;
			offset += 40;
			offset %= entropyPool.length;
		}

		entropyRIdx = offset;
		entropyCount -= (numBytes * 8);
		if (entropyCount < 0) {
			entropyCount = 0;
		}

		return bytes;
	}

	/**
	 * Get a hash-value which reflects the current system state. The value is the SHA1 hash over a bunch of data while
	 * includes the current time, memory information, all system properties, IP address etc. This hash can be used as a
	 * random seed, it is not cryptographically strong but is a lot better than nothing.
	 * @return a byte array containg data which reflects the current system state.
	 */
	private static synchronized byte[] getSystemStateHash() {
		MessageDigest sha1;
		try {
			sha1 = MessageDigest.getInstance("SHA1");
		} catch (Exception e) {
			throw new BlumBlumShubException("Error in RandomSeed, no sha1 hash", e);
		}

		sha1.update(getBytes(System.nanoTime()));
		sha1.update(getBytes(Runtime.getRuntime().totalMemory()));
		sha1.update(getBytes(Runtime.getRuntime().freeMemory()));
		sha1.update(stackDump(new Throwable()));

		try {
			Properties props = System.getProperties();
			Enumeration<?> names = props.propertyNames();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				sha1.update(name.getBytes());
				sha1.update(props.getProperty(name).getBytes());
			}
		} catch (Exception t) {
			sha1.update(stackDump(t));
		}
		sha1.update(getBytes(System.nanoTime()));

		try {
			sha1.update(InetAddress.getLocalHost().toString().getBytes());
		} catch (Exception t) {
			sha1.update(stackDump(t));
		}
		sha1.update(getBytes(System.nanoTime()));

		Runtime.getRuntime().gc();
		sha1.update(getBytes(Runtime.getRuntime().freeMemory()));
		sha1.update(getBytes(System.nanoTime()));

		return sha1.digest();
	}

	private static byte[] getBytes(long value) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.putLong(System.nanoTime());
		return buffer.array();
	}

	/**
	 * Print a stack-dump into a byte array.
	 * @param t throwable to generate stack dump from
	 * @return a byte-array containing the stack dump
	 */
	private static byte[] stackDump(Throwable t) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		t.printStackTrace(pw);
		return baos.toByteArray();
	}

	/**
	 * Creates a random number by checking how many times we can run a loop and yield during a set time.
	 * @param t how many milliseconds the loop should run for
	 */
	private static int spin(long t) {
		int counter = 0;
		Sleeper s = new Sleeper(t);
		do {
			counter++;
			Thread.yield();
		} while (s.isAlive());
		return counter;
	}

	/**
	 * This class is used in the spin function to help generate a random number. It basically just starts a thread which
	 * lives for a specified number of milliseconds.
	 */
	private static class Sleeper extends Thread {
		long sleepTime;

		/**
		 * Create an instance which just lives for the specified time and then exits.
		 * @param sleepTime how many milliseconds the thread should live
		 */
		private Sleeper(long sleepTime) {
			super("RandomSeed.Sleeper");
			this.sleepTime = sleepTime;
			this.start();
		}

		@Override
		public void run() {
			Thread.yield();
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
