package gca.in.xap.tools.operationtool.util;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SecretsMapTest {

	@Test
	public void should_print_same_thing_when_empty() {
		assertEquals(new HashMap<>().toString(), new SecretsMap<>().toString());
	}

	@Test
	public void should_not_print_same_thing_when_empty() {
		SecretsMap<String, String> secretsMap = new SecretsMap<>();
		secretsMap.put("password", "1234");

		HashMap normalMap = new HashMap<>();
		normalMap.put("password", "1234");

		assertNotEquals(normalMap.toString(), secretsMap.toString());
	}

	@Test
	public void should_print_same_thing_when_value_is_the_placeholder() {
		SecretsMap<String, String> secretsMap = new SecretsMap<>();
		secretsMap.put("password", "1234");

		HashMap normalMap = new HashMap<>();
		normalMap.put("password", SecretsMap.PLACEHOLDER);

		assertEquals(normalMap.toString(), secretsMap.toString());
	}

	@Test
	public void should_hide_values_one_element() {
		SecretsMap<String, String> secretsMap = new SecretsMap<>();
		secretsMap.put("password", "1234");

		HashMap normalMap = new HashMap<>();
		normalMap.put("password", "1234");

		assertNotEquals(normalMap.toString(), secretsMap.toString());
		assertEquals("{password=****secret****}", secretsMap.toString());
	}

	@Test
	public void should_hide_values_two_elements() {
		SecretsMap<String, String> secretsMap = new SecretsMap<>();
		secretsMap.put("database.password", "1234");
		secretsMap.put("mq.password", "abcd");

		assertEquals("{database.password=****secret****,mq.password=****secret****}", secretsMap.toString());
	}

}
