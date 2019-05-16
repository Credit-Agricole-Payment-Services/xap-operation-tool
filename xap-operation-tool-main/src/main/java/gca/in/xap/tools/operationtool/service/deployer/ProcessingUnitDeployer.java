package gca.in.xap.tools.operationtool.service.deployer;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;

public interface ProcessingUnitDeployer {

	ProcessingUnit deploy(String puName, ProcessingUnitConfig processingUnitConfig);

}
