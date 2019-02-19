package gca.in.xap.tools.operationtool.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentDescriptor {

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Topology {
		private String schema;
		private Integer instances;
		private Integer partitions;
		private Integer backupsPerPartition;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ServiceLevelAgreement {
		private Boolean requiresIsolation;
		private Integer maxInstancesPerVM;
		private Integer maxInstancesPerMachine;
		private String maxInstancesPerZone;
		private List<String> zones;
	}

	private String name;
	private String resource;
	private Topology topology;
	private ServiceLevelAgreement sla;

	/**
	 * Values can either be String, Integer, Boolean, ...
	 */
	private Map<String, String> contextProperties;

}
