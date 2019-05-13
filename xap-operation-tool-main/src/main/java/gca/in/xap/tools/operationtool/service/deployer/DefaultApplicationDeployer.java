package gca.in.xap.tools.operationtool.service.deployer;

import lombok.AllArgsConstructor;
import org.openspaces.admin.Admin;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.config.ApplicationConfig;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class DefaultApplicationDeployer implements ApplicationDeployer {

	private final Admin admin;

	@Override
	public Application deploy(ApplicationConfig applicationConfig, long timeout, TimeUnit timeoutTimeUnit) {
		return admin.getGridServiceManagers().deploy(applicationConfig, timeout, timeoutTimeUnit);
	}

}
