package de.ascendro.f4m.service.util.random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.ascendro.f4m.service.util.random.RandomSequenceGenerator.Item;

public class RandomSequenceGeneratorTest {
	private static final int RANGE = 10;//0..9 
	
	@Mock
	private RandomUtil randomUtill;
	
	private RandomSequenceGenerator randomSequenceGenerator;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		randomSequenceGenerator = new RandomSequenceGenerator(RANGE, randomUtill);
	}
	
	@Test
	public void testNextInt() throws OutOfUniqueRandomNumbersException{
		final long[] randoms = new long[]{ 4L, 0L, 3L, 6L, 5L, 2L, 1L, 2L, 1L, 0L};
		
		when(randomUtill.nextLong(any(Long.class))).then(new Answer<Long>() {
			int i = 0;
			@Override
			public Long answer(InvocationOnMock invocation) throws Throwable {
				return randoms[i++];
			}
		});
	
		assertEquals(4, randomSequenceGenerator.nextInt());//r(10): 4->4
		assertEquals(0, randomSequenceGenerator.nextInt());//r(9):  0->0
		assertEquals(5, randomSequenceGenerator.nextInt());//r(8):  3->5
		assertEquals(9, randomSequenceGenerator.nextInt());//r(7):  6->9
		
		assertEquals(8, randomSequenceGenerator.nextInt());//r(6):  5->8
		assertEquals(3, randomSequenceGenerator.nextInt());//r(5):  2->3
		assertEquals(2, randomSequenceGenerator.nextInt());//r(4):  1->2
		assertEquals(7, randomSequenceGenerator.nextInt());//r(3):  2->7
		assertEquals(6, randomSequenceGenerator.nextInt());//r(2):  1->6
		assertEquals(1, randomSequenceGenerator.nextInt());//r(1):  0->1
	}
	
	@Test(expected = OutOfUniqueRandomNumbersException.class)
	public void testOutOfUniqueRandomNumbers() throws OutOfUniqueRandomNumbersException{
		final long[] randoms = new long[]{ 5L, 6L, 3L, 0L, 0L, 2L, 3L, 0L, 0L, 0L};
		
		when(randomUtill.nextLong(any(Long.class))).then(new Answer<Long>() {
			int i = 0;
			@Override
			public Long answer(InvocationOnMock invocation) throws Throwable {
				return randoms[i++];
			}
		});
		
		assertEquals(5, randomSequenceGenerator.nextInt());//r(10): 5->5
		assertEquals(7, randomSequenceGenerator.nextInt());//r(9):  6->7
		assertEquals(3, randomSequenceGenerator.nextInt());//r(8):  3->3
		assertEquals(0, randomSequenceGenerator.nextInt());//r(7):  0->0
		assertEquals(1, randomSequenceGenerator.nextInt());//r(6):  0->1
		assertEquals(6, randomSequenceGenerator.nextInt());//r(5):  2->6
		assertEquals(9, randomSequenceGenerator.nextInt());//r(4):  3->9
		assertEquals(2, randomSequenceGenerator.nextInt());//r(3):  0->2
		assertEquals(4, randomSequenceGenerator.nextInt());//r(2):  0->4
		assertEquals(8, randomSequenceGenerator.nextInt());//r(1):  0->8
	
		randomSequenceGenerator.nextInt(); //throws OutOfUniqueRandomNumbers
	}

	@Test
	public void testNextIntWithRealRandom() throws Exception {
		randomSequenceGenerator = new RandomSequenceGenerator(RANGE, new RandomUtilImpl());
		
		final Set<Integer> numbers = new HashSet<>();
		for(int i = 0; i < RANGE; i++){
			final int nextInt = randomSequenceGenerator.nextInt();
			assertTrue("next item (" + nextInt + ") is not in range [0, " + RANGE + "]", nextInt >= 0 && nextInt < RANGE);
			assertTrue(i + ". random number already picked before", numbers.add(nextInt));
			System.out.println(" -> actual next int:" + nextInt);
		}
		
		assertEquals(RANGE, numbers.size());
	}
	
	@Test
	public void testAddItem(){
		//Item 1: 5 -> [5-"5"-null] 
		randomSequenceGenerator.addItem(5);
		assertEquals(1, randomSequenceGenerator.usedNumbers.size());
		int firstItemIndex = randomSequenceGenerator.usedNumbers.indexOf(new Item(5));
		assertEquals(0, firstItemIndex);
		final Item firstItem = randomSequenceGenerator.usedNumbers.get(firstItemIndex);
		assertNotNull(firstItem);
		assertEquals(5, firstItem.getLeftDistance());
		assertNull(firstItem.getRightDistnace());
		
		//Item 2: 7 -> [5-"5"-1, 1-"7"-null] 
		randomSequenceGenerator.addItem(7);
		assertEquals(2, randomSequenceGenerator.usedNumbers.size());
		int secondItemIndex = randomSequenceGenerator.usedNumbers.indexOf(new Item(7));
		assertEquals(1, secondItemIndex);
		//new
		final Item secondItem = randomSequenceGenerator.usedNumbers.get(secondItemIndex);
		assertNotNull(secondItem);
		assertEquals(1, secondItem.getLeftDistance());
		assertNull(secondItem.getRightDistnace());
		//right neighbor
		assertEquals(Long.valueOf(1), firstItem.getRightDistnace());
		
		//Item 3: 3 -> [3-"3"-1, 1-"5"-1, 1-"7"-null] 
		randomSequenceGenerator.addItem(3);
		assertEquals(3, randomSequenceGenerator.usedNumbers.size());
		int thirdItemIndex = randomSequenceGenerator.usedNumbers.indexOf(new Item(3));
		assertEquals(0, thirdItemIndex);
		//new
		final Item thirdItem = randomSequenceGenerator.usedNumbers.get(thirdItemIndex);
		assertNotNull(thirdItem);
		assertEquals(3, thirdItem.getLeftDistance());
		assertEquals(Long.valueOf(1), thirdItem.getRightDistnace());
		//right neighbor
		assertEquals(1, firstItem.getLeftDistance());
		
		//Item 4: 0 -> [0-"0"-2, 2-"3"-1, 1-"5"-1, 1-"7"-null] 
		randomSequenceGenerator.addItem(0);
		assertEquals(4, randomSequenceGenerator.usedNumbers.size());
		int fourthItemIndex = randomSequenceGenerator.usedNumbers.indexOf(new Item(0));
		assertEquals(0, fourthItemIndex);
		//new
		final Item fourthItem = randomSequenceGenerator.usedNumbers.get(fourthItemIndex);
		assertNotNull(fourthItem);
		assertEquals(0, fourthItem.getLeftDistance());
		assertEquals(Long.valueOf(2), fourthItem.getRightDistnace());
		//right neighbor
		assertEquals(2, thirdItem.getLeftDistance());
		
		//Item 5: 1 -> [0-"0"-0, 0-"1"-1, 1-"3"-1, 1-"5"-1, 1-"7"-null] 
		randomSequenceGenerator.addItem(1);
		assertEquals(5, randomSequenceGenerator.usedNumbers.size());
		int fifthItemIndex = randomSequenceGenerator.usedNumbers.indexOf(new Item(1));
		assertEquals(1, fifthItemIndex);
		fourthItemIndex = randomSequenceGenerator.usedNumbers.indexOf(new Item(0));
		assertEquals(0, fourthItemIndex);
		//new
		final Item fifthItem = randomSequenceGenerator.usedNumbers.get(fifthItemIndex);
		assertNotNull(fifthItem);
		assertEquals(0, fifthItem.getLeftDistance());
		assertEquals(Long.valueOf(1), fifthItem.getRightDistnace());
	}
	
	@Test
	public void testItemEquals() {
		assertTrue(new Item(1L, 2L, 3L).equals(new Item(1L, 2L, 3L)));
		assertTrue(new Item(1L, 2L, 3L).equals(2L));
		
		assertFalse(new Item(1L, 8L, 3L).equals(new Item(1L, 2L, 3L)));
		assertFalse(new Item(1L, 2L, 3L).equals(null));
	}

}
