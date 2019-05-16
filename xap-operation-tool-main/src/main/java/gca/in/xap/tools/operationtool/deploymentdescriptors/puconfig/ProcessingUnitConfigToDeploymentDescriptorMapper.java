package gca.in.xap.tools.operationtool.deploymentdescriptors.puconfig;

import gca.in.xap.tools.operationtool.deploymentdescriptors.DeploymentDescriptor;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProcessingUnitConfigToDeploymentDescriptorMapper {

	private static final String PARTITIONED_SCHEMA = "partitioned";

	public DeploymentDescriptor map(ProcessingUnitConfig processingUnitConfig) {

		// sla
		DeploymentDescriptor.ServiceLevelAgreement sla = null;
		if (processingUnitConfig.isRequiresIsolation() != null) {
			sla = lazyInit(sla);
			sla.setRequiresIsolation(processingUnitConfig.isRequiresIsolation());
		}
		if (processingUnitConfig.getMaxInstancesPerMachine() != null) {
			sla = lazyInit(sla);
			sla.setMaxInstancesPerMachine(processingUnitConfig.getMaxInstancesPerMachine());
		}
		if (processingUnitConfig.getMaxInstancesPerVM() != null) {
			sla = lazyInit(sla);
			sla.setMaxInstancesPerVM(processingUnitConfig.getMaxInstancesPerVM());
		}
		String maxInstancesPerZone = mapMapMaxInstancesPerZone(processingUnitConfig.getMaxInstancesPerZone());
		if (maxInstancesPerZone != null) {
			sla = lazyInit(sla);
			sla.setMaxInstancesPerZone(maxInstancesPerZone);
		}
		if (processingUnitConfig.getZones() != null) {
			sla = lazyInit(sla);
			sla.setZones(Arrays.asList(processingUnitConfig.getZones()));
		}

		// topology
		DeploymentDescriptor.Topology topology = null;
		if (PARTITIONED_SCHEMA.equals(processingUnitConfig.getClusterSchema())) {
			topology = lazyInit(topology);
			topology.setPartitions(processingUnitConfig.getNumberOfInstances());
		}
		if (processingUnitConfig.getClusterSchema() != null) {
			topology = lazyInit(topology);
			topology.setSchema(processingUnitConfig.getClusterSchema());
		}
		if (processingUnitConfig.getNumberOfInstances() != null) {
			topology = lazyInit(topology);
			topology.setInstances(processingUnitConfig.getNumberOfInstances());
		}
		if (processingUnitConfig.getNumberOfBackups() != null) {
			topology = lazyInit(topology);
			topology.setBackupsPerPartition(processingUnitConfig.getNumberOfBackups());
		}

		DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();

		// basics
		deploymentDescriptor.setName(processingUnitConfig.getName());
		deploymentDescriptor.setResource(processingUnitConfig.getProcessingUnit());


		Map<String, String> contextProperties = processingUnitConfig.getContextProperties();
		deploymentDescriptor.setContextProperties((Map) contextProperties);

		deploymentDescriptor.setSla(sla);
		deploymentDescriptor.setTopology(topology);
		return deploymentDescriptor;
	}

	private DeploymentDescriptor.Topology lazyInit(DeploymentDescriptor.Topology value) {
		if (value == null) {
			return new DeploymentDescriptor.Topology();
		}
		return value;
	}

	private DeploymentDescriptor.ServiceLevelAgreement lazyInit(DeploymentDescriptor.ServiceLevelAgreement value) {
		if (value == null) {
			return new DeploymentDescriptor.ServiceLevelAgreement();
		}
		return value;
	}


	public String mapMapMaxInstancesPerZone(Map<String, Integer> maxInstancesPerZoneMap) {
		if (maxInstancesPerZoneMap == null) {
			return null;
		}
		if (maxInstancesPerZoneMap.isEmpty()) {
			return null;
		}
		List<String> collect = maxInstancesPerZoneMap.entrySet().stream().map(entry -> entry.getValue() + "/" + entry.getKey()).collect(Collectors.toList());
		return String.join(",", collect);
	}

}
