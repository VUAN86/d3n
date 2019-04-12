package de.ascendro.f4m.service.game.engine.dao.pool;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

public class QuestionPoolSizeMeta {
	private final String[] poolIds;
	private final long[] poolSizes;
	private long poolsSizeTotal;
	
	public QuestionPoolSizeMeta(String[] poolIds) {
		this.poolIds = poolIds;
		this.poolSizes = new long[poolIds.length];
		this.poolsSizeTotal = 0;
	}
	
	public void addPoolSize(String poolId, long size){
		poolSizes[ArrayUtils.indexOf(poolIds, poolId)] = size;
		poolsSizeTotal += size;
	}
	
	public long getPoolsSizeTotal() {
		return poolsSizeTotal;
	}
	
	public long[] getPoolSizes() {
		return poolSizes;
	}
	
	public String[] getPoolIds() {
		return poolIds;
	}
	
	public boolean hasAnyQuestions() {
		return poolsSizeTotal > 0;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("QuestionPoolSizeMeta [poolIds=");
		builder.append(Arrays.toString(poolIds));
		builder.append(", poolSizes=");
		builder.append(Arrays.toString(poolSizes));
		builder.append(", poolsSizeTotal=");
		builder.append(poolsSizeTotal);
		builder.append("]");
		return builder.toString();
	}	
}
