package gca.in.xap.tools.operationtool.model;

import lombok.Data;

import java.time.Duration;
import java.util.Map;

@Data
public class VirtualMachineDescription {

	private ComponentType componentType;

	private String uid;

	private Long pid;

	private String hostName;

	private Long heapSizeInMBInit;

	private Long heapSizeInMBMax;

	private Duration uptime;

	private String jvmDescription;

	private Map<String, String> systemProperties;

	private Map<String, String> environmentVariables;

}
