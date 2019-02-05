package gca.in.xap.tools.operationtool.configuration;

import lombok.Data;

import java.util.List;

@Data
public class XapClient {

	List<String> locators;

	List<String> groups;

	long timeoutInMilliseconds;

}
