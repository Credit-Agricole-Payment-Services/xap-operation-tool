package gca.in.xap.tools.operationtool.service.rebalance;

import com.google.common.util.concurrent.AtomicLongMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Slf4j
public class MinAndMaxTest {

	@Test
	public void should_find_min_and_max_when_contains_different_values() {
		AtomicLongMap<String> atomicLongMap = AtomicLongMap.create();
		atomicLongMap.addAndGet("foo", 3);
		atomicLongMap.addAndGet("bar", 7);
		MinAndMax<String> minAndMax = MinAndMax.findMinAndMax(atomicLongMap);
		log.info("minAndMax = {}", minAndMax);
		assertEquals("foo", minAndMax.getBestKeyOfMin());
		assertEquals("bar", minAndMax.getBestKeyOfMax());
	}

	@Test
	public void should_find_min_and_max_when_contains_mulitpledifferent_values() {
		AtomicLongMap<String> atomicLongMap = AtomicLongMap.create();
		atomicLongMap.addAndGet("min1", 3);
		atomicLongMap.addAndGet("max1", 7);
		atomicLongMap.addAndGet("max2", 7);
		atomicLongMap.addAndGet("max3", 7);
		atomicLongMap.addAndGet("max4", 7);
		atomicLongMap.addAndGet("min2", 3);
		MinAndMax<String> minAndMax = MinAndMax.findMinAndMax(atomicLongMap);
		log.info("minAndMax = {}", minAndMax);
		assertEquals("min1", minAndMax.getBestKeyOfMin());
		assertEquals("max1", minAndMax.getBestKeyOfMax());
	}

	@Test
	public void should_find_same_key_when_contains_same_values() {
		AtomicLongMap<String> atomicLongMap = AtomicLongMap.create();
		atomicLongMap.addAndGet("foo", 3);
		atomicLongMap.addAndGet("bar", 3);
		MinAndMax<String> minAndMax = MinAndMax.findMinAndMax(atomicLongMap);
		log.info("minAndMax = {}", minAndMax);
		assertEquals("bar", minAndMax.getBestKeyOfMin());
		assertEquals("bar", minAndMax.getBestKeyOfMax());
	}

	@Test
	public void should_find_same_key_when_contains_only_one_entry() {
		AtomicLongMap<String> atomicLongMap = AtomicLongMap.create();
		atomicLongMap.addAndGet("foo", 3);
		MinAndMax<String> minAndMax = MinAndMax.findMinAndMax(atomicLongMap);
		log.info("minAndMax = {}", minAndMax);
		assertEquals("foo", minAndMax.getBestKeyOfMin());
		assertEquals("foo", minAndMax.getBestKeyOfMax());
	}

}
