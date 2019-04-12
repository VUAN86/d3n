package de.ascendro.f4m.server;

import com.aerospike.client.policy.WritePolicy;

/**
 * Update logic between read and write operations.
 * Update should be re-runnable.
 * @param <T>
 */
@FunctionalInterface
public interface UpdateCall<T> {
	
	T update(T readResult, WritePolicy writePolicy);

}
