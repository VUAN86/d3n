package de.ascendro.f4m.service.util.random;

import java.security.SecureRandom;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.Validate;

import de.ascendro.f4m.service.util.random.bbs.BlumBlumShubProvider;
import de.ascendro.f4m.service.util.random.bbs.BlumBlumShubSpi;

public class RandomUtilImpl implements RandomUtil {

	private final SecureRandom bbsRandom = new BBSRandom();

	@Override
	public int nextInt(int n) {
		return RandomUtils.nextInt(0, n);
	}

	@Override
	public int nextBlumBlumShubInt(int startInclusive, int endExclusive) {
        Validate.isTrue(endExclusive >= startInclusive, "Start value must be smaller or equal to end value.");
        Validate.isTrue(startInclusive >= 0, "Both range values must be non-negative.");
        if (startInclusive == endExclusive) {
            return startInclusive;
        }
        return startInclusive + bbsRandom.nextInt(endExclusive - startInclusive);
	}
	
	@Override
	public long nextLong(long n) {
		return RandomUtils.nextLong(0, n);
	}

	private static class BBSRandom extends SecureRandom {

		private static final long serialVersionUID = 80397705902371559L;

		private BBSRandom() {
			super(new BlumBlumShubSpi(), new BlumBlumShubProvider());
		}
	}
	
}
