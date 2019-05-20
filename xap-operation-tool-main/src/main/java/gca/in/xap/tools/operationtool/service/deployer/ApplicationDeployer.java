package gca.in.xap.tools.operationtool.service.deployer;

import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.config.ApplicationConfig;

import java.util.concurrent.TimeUnit;

public interface ApplicationDeployer {

	Application deploy(ApplicationConfig applicationConfig, long timeout, TimeUnit timeoutTimeUnit);

}
