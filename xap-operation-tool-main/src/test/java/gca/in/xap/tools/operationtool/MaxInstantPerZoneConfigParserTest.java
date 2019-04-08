package gca.in.xap.tools.operationtool;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MaxInstantPerZoneConfigParserTest {

	@Test
	public void should_support_null_value() {
		String value = null;
		MaxInstantPerZoneConfigParser parser = new MaxInstantPerZoneConfigParser();
		Map<String, Integer> actualResult = parser.parseMaxInstancePerZone(value);
		Map<String, Integer> expectedResult = null;
		assertEquals(expectedResult, actualResult);
	}

	@Test
	public void should_support_empty_string() {
		String value = "";
		MaxInstantPerZoneConfigParser parser = new MaxInstantPerZoneConfigParser();
		Map<String, Integer> actualResult = parser.parseMaxInstancePerZone(value);
		Map<String, Integer> expectedResult = new HashMap<>();
		assertEquals(expectedResult, actualResult);
	}

	@Test
	public void should_support_blank_string() {
		String value = "   ";
		MaxInstantPerZoneConfigParser parser = new MaxInstantPerZoneConfigParser();
		Map<String, Integer> actualResult = parser.parseMaxInstancePerZone(value);
		Map<String, Integer> expectedResult = new HashMap<>();
		assertEquals(expectedResult, actualResult);
	}

	@Test
	public void should_parse_normal_string() {
		String value = "DAL/1,DID/1";
		MaxInstantPerZoneConfigParser parser = new MaxInstantPerZoneConfigParser();
		Map<String, Integer> actualResult = parser.parseMaxInstancePerZone(value);
		Map<String, Integer> expectedResult = new HashMap<>();
		expectedResult.put("DAL", 1);
		expectedResult.put("DID", 1);
		assertEquals(expectedResult, actualResult);
	}

	@Test
	public void should_parse_normal_string_with_space_before_zoneid() {
		String value = "DAL/1, DID/1";
		MaxInstantPerZoneConfigParser parser = new MaxInstantPerZoneConfigParser();
		Map<String, Integer> actualResult = parser.parseMaxInstancePerZone(value);
		Map<String, Integer> expectedResult = new HashMap<>();
		expectedResult.put("DAL", 1);
		expectedResult.put("DID", 1);
		assertEquals(expectedResult, actualResult);
	}

	@Test
	public void should_parse_normal_string_with_space_after_count() {
		String value = "DAL/1 ,DID/1";
		MaxInstantPerZoneConfigParser parser = new MaxInstantPerZoneConfigParser();
		Map<String, Integer> actualResult = parser.parseMaxInstancePerZone(value);
		Map<String, Integer> expectedResult = new HashMap<>();
		expectedResult.put("DAL", 1);
		expectedResult.put("DID", 1);
		assertEquals(expectedResult, actualResult);
	}

	@Test
	public void should_parse_string_with_zoneid_but_without_count() {
		String value = "ZONE_A,DAL_8G/1,ZONE_B,DID_8G/1";
		MaxInstantPerZoneConfigParser parser = new MaxInstantPerZoneConfigParser();
		Map<String, Integer> actualResult = parser.parseMaxInstancePerZone(value);
		Map<String, Integer> expectedResult = new HashMap<>();
		expectedResult.put("DAL_8G", 1);
		expectedResult.put("DID_8G", 1);
		assertEquals(expectedResult, actualResult);
	}

}
