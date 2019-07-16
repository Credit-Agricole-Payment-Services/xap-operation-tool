package gca.in.xap.tools.operationtool.service.rebalance;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.util.concurrent.AtomicLongMap;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class AtomicLongMapSerializer extends StdSerializer<AtomicLongMap> {

	private final boolean hideZeros;

	public AtomicLongMapSerializer(boolean hideZeros) {
		this(null, hideZeros);
	}

	public AtomicLongMapSerializer(Class<AtomicLongMap> t, boolean hideZeros) {
		super(t);
		this.hideZeros = hideZeros;
	}

	@Override
	public void serialize(AtomicLongMap atomicLongMap, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
		doSerialize(atomicLongMap, jsonGenerator, serializerProvider, hideZeros);
	}

	public static <T> void doSerialize(AtomicLongMap<T> atomicLongMap, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, boolean hideZeros) throws IOException {
		jsonGenerator.writeStartObject();
		SortedMap<T, Long> sortedMap = new TreeMap<>(atomicLongMap.asMap());
		for (Map.Entry<T, Long> entry : sortedMap.entrySet()) {

			String key = String.valueOf(entry.getKey());
			long value = entry.getValue();
			if (value == 0 && hideZeros) {
				// nothing to do, skip this entry
			} else {
				jsonGenerator.writeNumberField(key, value);
			}
		}
		jsonGenerator.writeEndObject();
	}

}
