package gca.in.xap.tools.operationtool;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
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
	private static final String PROP_LOOKUP_LOCATORS = "lookup.locators";

	private static final String PROP_TIMEOUT = "timeout";
	private static final String PROP_TIMEOUT_DEFAULT = "PT1M";

	final String username = System.getProperty(PROP_CREDENTIAL_USERNAME);

	final String password = System.getProperty(PROP_CREDENTIAL_SECRET, "");

	final List<String> locators = findLookupLocators();

	final List<String> groups = findLookupGroups();

	final Duration timeoutDuration = Duration.parse(System.getProperty(PROP_TIMEOUT, PROP_TIMEOUT_DEFAULT));

	@Nullable
	final List<String> commandLineArgs;

	public ApplicationArguments(@Nullable List<String> commandLineArgs) {
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

	public void checkMinimalNumberOfCommandLineArgs(int minArgsCount) {
		if (minArgsCount < 0) {
			throw new IllegalArgumentException("minArgsCount should be at least zero : minArgsCount = " + minArgsCount);
		}
		int actualArgsCount = commandLineArgs == null ? 0 : commandLineArgs.size();
		if (actualArgsCount < minArgsCount) {
			throw new IllegalArgumentException("Expected at least " + minArgsCount + " args, found " + actualArgsCount);
		}
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
		return findProperty("XAP_LOOKUP_GROUPS", "lookup.groups", "xap");
	}

	static List<String> findLookupLocators() {
		return findProperty("XAP_MANAGER_SERVERS", "lookup.locators", "localhost");
	}

	static List<String> findProperty(@NonNull String envVariableName, @NonNull String systemPropertyName, @NonNull String defaultValue) {
		String envVariableValue = System.getenv().get(envVariableName);
		String systemPropertyValue = System.getProperty(systemPropertyName);
		log.info("Value for ENV property {} is : {}, Value for System property {} is : {}", envVariableName, envVariableValue, systemPropertyName, systemPropertyValue);
		String value = (systemPropertyValue != null) ? systemPropertyValue : envVariableValue;
		if (value == null) {
			value = defaultValue;
			log.info("Using default value {} because neither ENV variable {} nor System property {} are set", defaultValue, envVariableName, systemPropertyName);
			log.info("It is recommended to run the following command before using the tool, in order to have ENV variables set : source $XAP_ROOT_PATH/bin/setenv.sh");
		}
		return Arrays.asList(value.split(","));
	}

	public static String generateUsageString() {
		return "args: <zipFile> (<propsFile>)"
				+ "\nAvailable system properties:"
				+ "\n -D" + PROP_LOOKUP_GROUPS + " (comma separated multi-values. Default value (cf. env:XAP_LOOKUP_GROUPS) : " + findLookupGroups() + ")"
				+ "\n -D" + PROP_LOOKUP_LOCATORS + " (comma separated multi-values. Default (cf. env:XAP_MANAGER_SERVERS) : " + findLookupGroups() + ")"
				+ "\n -D" + PROP_CREDENTIAL_USERNAME + " (URL Encoded value)"
				+ "\n -D" + PROP_CREDENTIAL_SECRET + " (URL Encoded value)"
				+ "\n -D" + PROP_TIMEOUT + " (ISO-8601 Duration. Default value: " + PROP_TIMEOUT_DEFAULT + ")";
	}

}
