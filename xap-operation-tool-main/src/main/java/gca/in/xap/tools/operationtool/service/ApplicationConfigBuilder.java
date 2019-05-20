package gca.in.xap.tools.operationtool.service;

import org.openspaces.admin.application.config.ApplicationConfig;

import java.util.function.Predicate;

public interface ApplicationConfigBuilder {
	ApplicationConfig loadApplicationConfig(Predicate<String> procesingUnitNamesPredicates);
}
