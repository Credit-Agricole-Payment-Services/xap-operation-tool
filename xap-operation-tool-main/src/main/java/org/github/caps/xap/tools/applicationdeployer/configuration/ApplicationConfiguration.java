package org.github.caps.xap.tools.applicationdeployer.configuration;

import lombok.Data;

@Data
public class ApplicationConfiguration {

	private XapClient xapClient;

	private Credentials credentials;

}
