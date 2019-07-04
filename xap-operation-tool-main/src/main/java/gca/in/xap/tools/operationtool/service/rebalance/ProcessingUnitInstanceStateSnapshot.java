package gca.in.xap.tools.operationtool.service.rebalance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.util.concurrent.AtomicLongMap;
import gca.in.xap.tools.operationtool.service.ObjectMapperFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.SortedMap;

@ToString
@Builder
public class ProcessingUnitInstanceStateSnapshot {

	private static final ObjectMapper objectMapperWithZeros = createObjectMapper(false);

	private static final ObjectMapper objectMapperWithoutZeros = createObjectMapper(true);

	private static ObjectMapper createObjectMapper(boolean hideZeros) {
		ObjectMapper result = new ObjectMapperFactory().createObjectMapper();
		result.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		result.enable(SerializationFeature.INDENT_OUTPUT);
		SimpleModule module = new SimpleModule();
		module.addSerializer(AtomicLongMap.class, new AtomicLongMapSerializer(hideZeros));
		result.registerModule(module);
		return result;
	}

	@JsonIgnore
	@Getter
	final ProcessingUnitInstance[] processingUnitInstances;

	@Getter
	final ProcessingUnitInstanceBreakdownSnapshot potentialCounts;

	@Getter
	final ProcessingUnitInstanceRepartitionSnapshot processingUnitInstanceRepartitionSnapshot;

	/**
	 * key : partition ID
	 * value : snapshot for that partition
	 */
	@Getter
	final SortedMap<Integer, ProcessingUnitInstanceRepartitionSnapshot> processingUnitInstanceRepartitionSnapshotPerPartition;

	public void removeAllZeros() {
		potentialCounts.removeAllZeros();
		processingUnitInstanceRepartitionSnapshot.removeAllZeros();
		processingUnitInstanceRepartitionSnapshotPerPartition.values().forEach(ProcessingUnitInstanceRepartitionSnapshot::removeAllZeros);
	}

	public String toJson() {
		try {
			return objectMapperWithZeros.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public String toJsonWithoutZeros() {
		try {
			return objectMapperWithoutZeros.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
