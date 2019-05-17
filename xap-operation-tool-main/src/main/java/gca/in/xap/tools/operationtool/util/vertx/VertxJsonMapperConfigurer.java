package gca.in.xap.tools.operationtool.util.vertx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.Json;

public class VertxJsonMapperConfigurer {

	//public static AtomicBoolean alreadyConfigured = new AtomicBoolean(false);

	private VertxJsonMapperConfigurer() {
		throw new IllegalStateException();
	}

	public static void configure() {
		//if (!alreadyConfigured.getAndSet(true)) {
		Json.mapper.registerModule(new JavaTimeModule());
		Json.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		Json.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		//}
	}

}
