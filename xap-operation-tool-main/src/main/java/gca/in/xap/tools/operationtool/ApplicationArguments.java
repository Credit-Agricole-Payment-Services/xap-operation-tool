package gca.in.xap.tools.operationtool;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

@Slf4j
public class ApplicationArguments {

	private static final String PROP_CREDENTIAL_USERNAME = "credential.username";
	private static final String PROP_CREDENTIAL_SECRET = "credential.password";

	private static final String PROP_LOOKUP_GROUPS = "lookup.groups";
	private static final String PROP_LOOKUP_GROUPS_ENV = "XAP_LOOKUP_GROUPS";
	private static final String PROP_LOOKUP_GROUPS_DEFAULT = System.getenv().getOrDefault(PROP_LOOKUP_GROUPS_ENV, "xap");

	private static final String PROP_LOOKUP_LOCATORS = "lookup.locators";
	private static final String PROP_LOOKUP_LOCATORS_ENV = "XAP_LOOKUP_LOCATORS";
	private static final String PROP_LOOKUP_LOCATORS_DEFAULT = System.getenv().getOrDefault(PROP_LOOKUP_LOCATORS_ENV, "localhost");

	private static final String PROP_LOG_LEVEL_ROOT = "log.level.root";
	private static final String PROP_LOG_LEVEL_ROOT_DEFAULT = Level.INFO.getName();

	private static final String PROP_TIMEOUT = "timeout";
	private static final String PROP_TIMEOUT_DEFAULT = "PT1M";

	private static final String USAGE = "args: <zipFile> (<propsFile>)"
			+ "\nAvailable system properties:"
			+ "\n -D" + PROP_LOOKUP_GROUPS + " (comma separated multi-values. Default value (cf. env:" + PROP_LOOKUP_GROUPS_ENV + ") : " + PROP_LOOKUP_GROUPS_DEFAULT + ")"
			+ "\n -D" + PROP_LOOKUP_LOCATORS + " (comma separated multi-values. Default (cf. env:" + PROP_LOOKUP_LOCATORS_ENV + ") : " + PROP_LOOKUP_LOCATORS_DEFAULT + ")"
			+ "\n -D" + PROP_CREDENTIAL_USERNAME + " (URL Encoded value)"
			+ "\n -D" + PROP_CREDENTIAL_SECRET + " (URL Encoded value)"
			+ "\n -D" + PROP_LOG_LEVEL_ROOT + " (Default value: " + PROP_LOG_LEVEL_ROOT_DEFAULT + ")"
			+ "\n -D" + PROP_TIMEOUT + " (ISO-8601 Duration. Default value: " + PROP_TIMEOUT_DEFAULT + ")";

	final String username = System.getProperty(PROP_CREDENTIAL_USERNAME);

	final String password = System.getProperty(PROP_CREDENTIAL_SECRET, "");

	final List<String> locators = Arrays.asList(System.getProperty(PROP_LOOKUP_LOCATORS, PROP_LOOKUP_LOCATORS_DEFAULT).split(","));

	final List<String> groups = Arrays.asList(System.getProperty(PROP_LOOKUP_GROUPS, PROP_LOOKUP_GROUPS_DEFAULT).split(","));

	final Duration timeoutDuration = Duration.parse(System.getProperty(PROP_TIMEOUT, PROP_TIMEOUT_DEFAULT));

	final String[] commandLineArgs;

	public ApplicationArguments(String... commandLineArgs) {
		this.commandLineArgs = commandLineArgs;
	}

	public void printInfo() {
		log.info("commandLineArgs = {}", Arrays.asList(commandLineArgs));

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

}
