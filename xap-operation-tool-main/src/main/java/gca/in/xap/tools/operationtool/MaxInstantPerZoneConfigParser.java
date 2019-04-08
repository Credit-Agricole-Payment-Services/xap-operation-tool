package gca.in.xap.tools.operationtool;

import java.util.HashMap;
import java.util.Map;

public class MaxInstantPerZoneConfigParser {

	public MaxInstantPerZoneConfigParser() {
		super();
	}

	/**
	 * example of value = "DAL/1,DID/1"
	 */
	public Map<String, Integer> parseMaxInstancePerZone(String value) {
		if (value == null) {
			return null;
		}
		if (value.equals("")) {
			return new HashMap<>();
		}
		final String[] items = value.split(",");
		final Map<String, Integer> result = new HashMap<>();
		for (String item : items) {
			proceedItem(item, result);
		}
		return result;
	}

	private void proceedItem(String item, Map<String, Integer> result) {
		String[] split = item.split("/");
		final int length = split.length;
		switch (length) {
			case 1:
				return;
			case 2:
				String zoneId = split[0].trim();
				Integer maxInstancesValue = Integer.parseInt(split[1].trim());
				result.put(zoneId, maxInstancesValue);
				return;
			default:
				throw new IllegalArgumentException("Invalid Max Instance Per Zone Configuration : " + item);
		}
	}

}
