package gca.in.xap.tools.operationtool;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.*;

@Slf4j
public class ApplicationArguments {

	private static final String PROP_CREDENTIAL_USERNAME = "credential.username";
	private static final String PROP_CREDENTIAL_SECRET = "credential.password";

	private static final String PROP_LOOKUP_GROUPS = "lookup.groups";
	private static final String PROP_LOOKUP_GROUPS_ENV = "XAP_LOOKUP_GROUPS";

	private static final String PROP_LOOKUP_LOCATORS = "lookup.locators";
	private static final String PROP_LOOKUP_LOCATORS_ENV = "XAP_LOOKUP_LOCATORS";

	private static final String PROP_TIMEOUT = "timeout";
	private static final String PROP_TIMEOUT_DEFAULT = "PT1M";

	private static final String USAGE = "args: <zipFile> (<propsFile>)"
			+ "\nAvailable system properties:"
			+ "\n -D" + PROP_LOOKUP_GROUPS + " (comma separated multi-values. Default value (cf. env:" + PROP_LOOKUP_GROUPS_ENV + ") : " + findLookupGroups() + ")"
			+ "\n -D" + PROP_LOOKUP_LOCATORS + " (comma separated multi-values. Default (cf. env:" + PROP_LOOKUP_LOCATORS_ENV + ") : " + findLookupGroups() + ")"
			+ "\n -D" + PROP_CREDENTIAL_USERNAME + " (URL Encoded value)"
			+ "\n -D" + PROP_CREDENTIAL_SECRET + " (URL Encoded value)"
			+ "\n -D" + PROP_TIMEOUT + " (ISO-8601 Duration. Default value: " + PROP_TIMEOUT_DEFAULT + ")";

	final String username = System.getProperty(PROP_CREDENTIAL_USERNAME);

	final String password = System.getProperty(PROP_CREDENTIAL_SECRET, "");

	final List<String> locators = findLookupLocators();

	final List<String> groups = findLookupGroups();

	final Duration timeoutDuration = Duration.parse(System.getProperty(PROP_TIMEOUT, PROP_TIMEOUT_DEFAULT));

	final List<String> commandLineArgs;

	public ApplicationArguments(List<String> commandLineArgs) {
		this.commandLineArgs = commandLineArgs;
	}

	public void printInfo() {
		log.info("commandLineArgs = {}", commandLineArgs);

		log.info("locators = {}", locators);
		log.info("groups = {}", groups);
		log.info("timeoutDuration = {}", timeoutDuration);
		log.info("username = {}", username);
		log.info("password = **** (hidden)");

		printNetworkInfo();
	}

	public void printNetworkInfo() {
		//
		Set<String> addresses = new TreeSet<>();
		try {
			for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements(); ) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				if (networkInterface.isUp()) {
					for (Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses(); inetAddresses.hasMoreElements(); ) {
						InetAddress addr = inetAddresses.nextElement();
						addresses.add(addr.getHostAddress());
					}
				}
			}
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
		log.info("All network addresses : {}", addresses);
	}

	static List<String> findLookupGroups() {
		return findProperty(PROP_LOOKUP_GROUPS_ENV, PROP_LOOKUP_GROUPS, "xap");
	}

	static List<String> findLookupLocators() {
		return findProperty(PROP_LOOKUP_LOCATORS_ENV, PROP_LOOKUP_LOCATORS, "localhost");
	}

	static List<String> findProperty(@NonNull String envPropertyName, @NonNull String systemPropertyName, @NonNull String defaultValue) {
		String envValue = System.getenv().get(envPropertyName);
		log.info("Value for {} ENV property is : {}", envPropertyName, envValue);
		String systemPropertyValue = System.getProperty(systemPropertyName);
		log.info("Value for {} System property is : {}", systemPropertyName, systemPropertyValue);
		String value = (systemPropertyValue != null) ? systemPropertyValue : envValue;
		if (value == null) {
			value = defaultValue;
		}
		return Arrays.asList(value.split(","));
	}

}
