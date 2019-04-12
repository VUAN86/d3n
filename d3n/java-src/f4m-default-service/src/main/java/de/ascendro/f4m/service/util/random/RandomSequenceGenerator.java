package de.ascendro.f4m.service.util.random;

import java.util.ArrayList;
import java.util.List;

public class RandomSequenceGenerator {
	
	protected final List<Item> usedNumbers = new ArrayList<>(7);
	
	private final long range;
	private final RandomUtil randomUtil;
	
	public RandomSequenceGenerator(long range, RandomUtil randomUtil) {
		this.range = range;
		this.randomUtil = randomUtil;
	}
	
	public RandomSequenceGenerator(int range, RandomUtil randomUtil) {
		this((long) range, randomUtil);
	}
	
	public int nextInt() throws OutOfUniqueRandomNumbersException, ArithmeticException {
		final long nextLong = nextLong();
		return Math.toIntExact(nextLong);
	}
	
	public long nextLong() throws OutOfUniqueRandomNumbersException {
		if(range > usedNumbers.size()){
			final long nextLong = randomUtil.nextLong(range - usedNumbers.size());
			return toUniqueNextLong(nextLong);
		}else{
			throw new OutOfUniqueRandomNumbersException();
		}
	}
	
	public int nextIntWithAutoReset(){
		int nextInt;
		try {
			if(!hasNumbersLeft()){
				reset();
			}
			nextInt = nextInt();
		} catch (OutOfUniqueRandomNumbersException rEx) {
			throw new RuntimeException("Out of unqiue numbers when requesting next int with auto reset", rEx);
		}
		return nextInt;
	}
	
	public void reset(){
		usedNumbers.clear();
	}
	
	public boolean hasNumbersLeft(){
		return usedNumbers.size() < range;
	}
	
	private long toUniqueNextLong(long nextLong) {
		long realNextLong;
		if(!usedNumbers.isEmpty()){
			realNextLong = usedNumbers.get(0).getLeftDistance();
			boolean exceedLimit = false;
			for(Item item : usedNumbers){
				final Long itemRightDistance = item.getRightDistnace() != null ? item.getRightDistnace() : 0;
				final long newRealNextLongValue = realNextLong + itemRightDistance;
				
				if(newRealNextLongValue > nextLong){//iterate until exceed limit
					final long shift = realNextLong >= nextLong + 1 ? 0 :1; //shift if nextInt is already available
					
					realNextLong = (item.getNumber() + itemRightDistance) - (newRealNextLongValue - nextLong) + shift;
					
					exceedLimit = true;
					break;
				}else{
					realNextLong = newRealNextLongValue;
				}
			}
			if(!exceedLimit){//calculate even if not exeeded limit
				final Item lastItem = usedNumbers.get(usedNumbers.size() - 1);
				realNextLong = lastItem.getNumber() + nextLong - realNextLong + 1;
			}
		}else{
			realNextLong = nextLong;
		}
		markAsUsed(realNextLong);
		return realNextLong;
	}

	private void markAsUsed(long number){
		if(!usedNumbers.contains(number)){
			addItem(number);
		}
	}
	
	protected void addItem(long number){
		int j = usedNumbers.size();
		for(int i = 0; i < usedNumbers.size(); i++){
			if(usedNumbers.get(i).getNumber() > number){
				j = i;
				break;
			}
		}
		
		long leftDistnace = getLeftDistance(j, number);
		Long rightDistance = getRightDistance(j, number);
		usedNumbers.add(j, new Item(leftDistnace, number, rightDistance));
		
		//Update neighbours distances
		if(j > 0){//Left neighbour right distance
			usedNumbers.get(j - 1).setRightDistnace(leftDistnace);
		}
		
		if(j + 1 < usedNumbers.size()){//Right neighbour left distance
			usedNumbers.get(j + 1).setLeftDistance(rightDistance);
		}
		
	}
	
	private long getLeftDistance(int index, long number){
		final long left;
		
		if(usedNumbers.isEmpty()){
			left = number;
		}else{
			final int leftIndex = index == 0 ? 0 : index - 1; 
			final long leftNumber = usedNumbers.get(leftIndex).getNumber();
			if(index == 0){
				left = number;
			}else{
				left = Math.abs(number - leftNumber - 1);
			}
		}
		return left;
	}
	
	private Long getRightDistance(int index, long number){
		final Long right;
		if(!usedNumbers.isEmpty() && index < usedNumbers.size()){
			final long rightNumber = usedNumbers.get(index).getNumber();
			right = rightNumber - number - 1;
		}else{
			right = null;
		}
		return right;
	}
	
	static class Item {
		private final long number;
		private long leftDistance;
		private Long rightDistance;
		
		public Item(long leftDistance, long number, Long rightDistance){
			this.number = number;
			this.leftDistance = leftDistance;
			this.rightDistance = rightDistance;		
		}
		
		public Item(long number){
			this(0, number, null);
		}
		
		public Item(long left, long number){
			this(left, number, null);
		}
		
		public long getNumber() {
			return number;
		}
		
		public long getLeftDistance() {
			return leftDistance;
		}
		
		public void setLeftDistance(long left) {
			this.leftDistance = left;
		}
		
		public Long getRightDistnace() {
			return rightDistance;
		}
		
		public void setRightDistnace(Long right) {
			this.rightDistance = right;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (leftDistance ^ (leftDistance >>> 32));
			result = prime * result + (int) (number ^ (number >>> 32));
			result = prime * result + ((rightDistance == null) ? 0 : rightDistance.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
            boolean equals = false;
            
            if(obj != null && obj != this){
                Number otherNumber = null;
                if(obj instanceof Item){
                    otherNumber = ((Item) obj).getNumber();
                }else if(obj instanceof Number){
                    otherNumber = (Number) obj;
                }
                equals = Long.valueOf(getNumber()).equals(otherNumber);
            }else {
                equals = obj != null && obj != this;
            }
            return equals;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(" [left=").append(leftDistance);
			builder.append(", number=").append(number);
			builder.append(", right=").append(rightDistance);
			builder.append("]");

			return builder.toString();
		}
		
	}

}
