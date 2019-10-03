package gca.in.xap.tools.operationtool.service;

import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

@Component
@Slf4j
public class LocalMachineGridServiceLocator {

	public GridServiceManager pickManagerOnLocalMachine(GridServiceManager[] managers) {
		// if a manager is running on the same machine that we are using to run the tool
		// then we prefer to use it
		final Set<String> allNetworkAddresses = findAllNetworkAddresses();
		log.info("Local host network interfaces are : {}", allNetworkAddresses);
		return Arrays.stream(managers).filter(manager -> allNetworkAddresses.contains(manager.getMachine().getHostAddress())).findFirst().orElse(null);
	}

	public GridServiceAgent pickAgentOnLocalMachine(GridServiceAgent[] agents) {
		// if an agent is running on the same machine that we are using to run the tool
		// then we prefer to use it
		final Set<String> allNetworkAddresses = findAllNetworkAddresses();
		log.info("Local host network interfaces are : {}", allNetworkAddresses);
		return Arrays.stream(agents).filter(agent -> allNetworkAddresses.contains(agent.getMachine().getHostAddress())).findFirst().orElse(null);
	}

	private Set<String> findAllNetworkAddresses() {
		Enumeration<NetworkInterface> networkInterfaces;
		try {
			networkInterfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			return new HashSet<>();
		}

		Set<String> result = new HashSet<>();
		for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
			List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
			for (InterfaceAddress currentAddress : interfaceAddresses) {
				InetAddress localAddress = currentAddress.getAddress();
				String ifAddress = localAddress.getHostAddress();
				result.add(ifAddress);
			}
		}
		return result;
	}

}
