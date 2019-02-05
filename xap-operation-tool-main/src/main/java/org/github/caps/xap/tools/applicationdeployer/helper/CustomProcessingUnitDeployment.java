package org.github.caps.xap.tools.applicationdeployer.helper;

import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;

import java.lang.reflect.Field;

public class CustomProcessingUnitDeployment extends ProcessingUnitDeployment {

	public CustomProcessingUnitDeployment(String processingUnitName, ProcessingUnitConfig processingUnitConfig) {
		super(processingUnitName);
		setConfig(processingUnitConfig);
	}

	private void setConfig(ProcessingUnitConfig processingUnitConfig) {
		// the config field is private in the super class, so we use reflexion to set the field value
		try {
			Field configField = ProcessingUnitDeployment.class.getDeclaredField("config");
			configField.setAccessible(true);
			configField.set(this, processingUnitConfig);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
