package org.github.caps.xap.tools.applicationdeployer.configuration;

import lombok.Data;

import java.util.List;

@Data
public class XapClient {

	List<String> locators;

	List<String> groups;

	long timeoutInMilliseconds;

}
