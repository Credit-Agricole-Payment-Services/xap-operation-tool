package gca.in.xap.tools.operationtool.service.deployer;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;

public interface ProcessingUnitDeployer {

	ProcessingUnit deploy(ProcessingUnitDeployment processingUnitDeployment);

}
