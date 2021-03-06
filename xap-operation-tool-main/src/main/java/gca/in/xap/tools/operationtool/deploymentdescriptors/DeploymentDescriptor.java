package gca.in.xap.tools.operationtool.deploymentdescriptors;

import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
//@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(exclude = {"contextProperties"})
public class DeploymentDescriptor {

	@Data
	//@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Topology {
		private String schema;
		private Integer instances;
		private Integer partitions;
		private Integer backupsPerPartition;
	}

	@Data
	//@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ServiceLevelAgreement {
		private Boolean requiresIsolation;
		private List<String> zones;
		private Integer maxInstancesPerVM;
		private Integer maxInstancesPerMachine;
		private String maxInstancesPerZone;
	}

	private String name;
	private String resource;
	private Topology topology;
	private ServiceLevelAgreement sla;

	/**
	 * Values can either be String, Integer, Boolean, ...
	 */
	private Map<String, Object> contextProperties;

}
