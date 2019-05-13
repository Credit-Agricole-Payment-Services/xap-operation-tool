package gca.in.xap.tools.operationtool.service.deployer;

import lombok.AllArgsConstructor;
import org.openspaces.admin.Admin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;

@AllArgsConstructor
public class DefaultProcessingUnitDeployer implements ProcessingUnitDeployer {

	private final Admin admin;

	@Override
	public ProcessingUnit deploy(ProcessingUnitDeployment processingUnitDeployment) {
		return admin.getGridServiceManagers().deploy(processingUnitDeployment);
	}

}
