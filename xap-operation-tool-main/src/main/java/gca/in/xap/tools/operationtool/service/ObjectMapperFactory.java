package gca.in.xap.tools.operationtool.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectMapperFactory {

	public ObjectMapper createObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		// SerializationFeature
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		// DeserializationFeature
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
		objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		return objectMapper;
	}

}
