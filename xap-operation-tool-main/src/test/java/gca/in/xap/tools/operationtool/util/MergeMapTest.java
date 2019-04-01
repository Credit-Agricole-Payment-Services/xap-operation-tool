package gca.in.xap.tools.operationtool.util;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class MergeMapTest {

	@Test
	public void should_to_string_match_hashmap_to_string_when_empty() {
		MergeMap<String, String> mergeMap = new MergeMap<>(new HashMap<>());
		assertEquals("{" + new HashMap<>().toString() + "}", mergeMap.toString());
	}

	@Test
	public void should_merge_empty_maps() {
		MergeMap<String, String> mergeMap = new MergeMap<>(new HashMap<>(), new HashMap<>());
		assertEquals("{{},{}}", mergeMap.toString());
	}

	@Test
	public void should_merge_non_overlapping_maps() {
		HashMap<String, String> map1 = new HashMap<>();
		HashMap<String, String> map2 = new HashMap<>();
		//
		map1.put("property1", "value1");
		map2.put("property2", "value2");
		//
		MergeMap<String, String> mergeMap = new MergeMap<>(map1, map2);
		assertEquals("{{property1=value1},{property2=value2}}", mergeMap.toString());
	}

	@Test
	public void should_merge_overlapping_maps() {
		HashMap<String, String> map1 = new HashMap<>();
		HashMap<String, String> map2 = new HashMap<>();
		//
		map1.put("property1", "value1");
		map2.put("property1", "value2");
		//
		MergeMap<String, String> mergeMap = new MergeMap<>(map1, map2);
		assertEquals("{{property1=value1},{property1=value2}}", mergeMap.toString());
	}

	@Test
	public void should_merge_normal_map_and_secretmap() {
		HashMap<String, String> map1 = new HashMap<>();
		SecretsMap<String, String> map2 = new SecretsMap<>();
		//
		map1.put("property1", "value1");
		map2.put("property2", "value2");
		//
		MergeMap<String, String> mergeMap = new MergeMap<>(map1, map2);
		assertEquals("{{property1=value1},{property2=" + SecretsMap.PLACEHOLDER + "}}", mergeMap.toString());
	}

}
