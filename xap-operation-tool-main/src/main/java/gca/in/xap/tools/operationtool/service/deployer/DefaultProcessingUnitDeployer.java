package gca.in.xap.tools.operationtool.service.deployer;

import gca.in.xap.tools.operationtool.service.CustomProcessingUnitDeployment;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.Admin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;

@AllArgsConstructor
@Slf4j
public class DefaultProcessingUnitDeployer implements ProcessingUnitDeployer {

	private final Admin admin;

	@Override
	public ProcessingUnit deploy(String puName, ProcessingUnitConfig processingUnitConfig) {
		ProcessingUnitDeployment processingUnitDeployment = new CustomProcessingUnitDeployment(puName, processingUnitConfig);
		log.debug("processingUnitDeployment = {}", processingUnitDeployment);
		return admin.getGridServiceManagers().deploy(processingUnitDeployment);
	}

}
