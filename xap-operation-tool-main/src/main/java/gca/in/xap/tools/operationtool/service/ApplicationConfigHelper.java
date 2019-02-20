package gca.in.xap.tools.operationtool.service;

import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.topology.ProcessingUnitConfigHolder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationConfigHelper {

	public static List<String> getPuNamesInOrderOfDeployment(ApplicationConfig applicationConfig) {
		return Arrays.stream(applicationConfig.getProcessingUnits())
				.map(ProcessingUnitConfigHolder::getName).collect(Collectors.toList());
	}

}
