package gca.in.xap.tools.operationtool.service.deployer;

import gca.in.xap.tools.operationtool.deploymentdescriptors.DeploymentDescriptor;
import gca.in.xap.tools.operationtool.deploymentdescriptors.json.DeploymentDescriptorMarshaller;
import gca.in.xap.tools.operationtool.deploymentdescriptors.puconfig.ProcessingUnitConfigToDeploymentDescriptorMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpProcessingUnitDeployer implements ProcessingUnitDeployer {

	private final Admin admin;

	private final WebClient webClient;

	private final DeploymentDescriptorMarshaller deploymentDescriptorMarshaller;

	private final ProcessingUnitConfigToDeploymentDescriptorMapper processingUnitConfigToDeploymentDescriptorMapper;

	@Setter
	private int httpPort = 8090;

	@Setter
	private String deployPuEndpoint = "/v2/pus";

	@Setter
	private String uploadResourceEndpoint = "/v2/pus/resources";

	@Setter
	private Duration uploadHttpRequestTimeout = Duration.ofSeconds(15);

	@Setter
	private Duration deployHttpRequestTimeout = Duration.ofSeconds(15);

	@Setter
	private Duration deployWaitTimeout = Duration.ofSeconds(120);

	public HttpProcessingUnitDeployer(
			Admin admin,
			WebClient webClient,
			DeploymentDescriptorMarshaller deploymentDescriptorMarshaller,
			ProcessingUnitConfigToDeploymentDescriptorMapper processingUnitConfigToDeploymentDescriptorMapper
	) {
		this.admin = admin;
		this.webClient = webClient;
		this.deploymentDescriptorMarshaller = deploymentDescriptorMarshaller;
		this.processingUnitConfigToDeploymentDescriptorMapper = processingUnitConfigToDeploymentDescriptorMapper;
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

	private GridServiceManager pickManager() {
		final GridServiceManagers gridServiceManagers = admin.getGridServiceManagers();
		final GridServiceManager[] managers = gridServiceManagers.getManagers();

		// if a manager is running on the same machine that we are using to run the tool
		// then we prefer to use it
		final Set<String> allNetworkAddresses = findAllNetworkAddresses();
		log.info("Local host network interfaces are : {}", allNetworkAddresses);
		final GridServiceManager preferredManager = Arrays.stream(managers).filter(manager -> allNetworkAddresses.contains(manager.getMachine().getHostAddress())).findFirst().orElse(null);
		if (preferredManager != null) {
			log.info("A XAP Manager has been found on the local host where xap-operation-tool is executing, we will be using this manager to schedule deployments");
			return preferredManager;
		}

		// else we use the first manager in the list of managers
		// TODO implement a random pick (or something else?) in order to be able to pick a different manager than the first
		GridServiceManager firstManager = managers[0];
		return firstManager;
	}

	@Override
	public ProcessingUnit deploy(String puName, ProcessingUnitConfig processingUnitConfig) {
		GridServiceManager gridServiceManager = pickManager();
		String managerHostName = gridServiceManager.getMachine().getHostName();
		log.info("Using Manager on {}", managerHostName);

		final DeploymentDescriptor deploymentDescriptor = processingUnitConfigToDeploymentDescriptorMapper.map(processingUnitConfig);
		log.debug("deploymentDescriptor = {}", deploymentDescriptor);

		String processingUnitArchiveFilePath = processingUnitConfig.getProcessingUnit();
		log.debug("processingUnitArchiveFilePath = {}", processingUnitArchiveFilePath);

		doUploadResourcesWithRetries(managerHostName, puName, deploymentDescriptor, 5);

		sleepALittleBit(5);

		deploymentDescriptor.setResource(puName + ".jar");
		final String processingUnitJson = marshall(deploymentDescriptor);

		// warning : printing the JSON is not recommended for production use
		// because this may print sensitive info into the logs
		// printing in trace level only is OK
		log.trace("processingUnitJson = {}", processingUnitJson);

		doDeploy(managerHostName, processingUnitJson);

		//sleepALittleBit(10);

		log.info("Waiting up to {} for PU {} to start deployment ...", deployWaitTimeout, puName);
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(puName, deployWaitTimeout.toMillis(), TimeUnit.MILLISECONDS);
		//ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(puName);
		if (processingUnit == null) {
			String errorMessage = String.format("ProcessingUnit is still not deployed after timeout of %d ms", deployWaitTimeout.toMillis());
			log.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		return processingUnit;
	}

	private void sleepALittleBit(long durationInSeconds) {
		log.info("Waiting for {} seconds before proceeding ...", durationInSeconds);
		try {
			TimeUnit.SECONDS.sleep(durationInSeconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private String marshall(DeploymentDescriptor deploymentDescriptor) {
		try {
			ByteArrayOutputStream processingUnitJsonByteArrayOutputStream = new ByteArrayOutputStream();
			deploymentDescriptorMarshaller.marshall(deploymentDescriptor, processingUnitJsonByteArrayOutputStream);
			return processingUnitJsonByteArrayOutputStream.toString("UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void doUploadResourcesWithRetries(String managerHostName, String processingUnitName, DeploymentDescriptor deploymentDescriptor, int remainingAttempts) {
		if (remainingAttempts == 0) {
			throw new RuntimeException("Failed to upload resources, max number of attempt has been reached");
		}
		try {
			doUploadResources(managerHostName, processingUnitName, deploymentDescriptor);
		} catch (RuntimeException e) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e1) {
				throw new RuntimeException(e);
			}
			doUploadResourcesWithRetries(managerHostName, processingUnitName, deploymentDescriptor, remainingAttempts - 1);
		}
	}

	private void doUploadResources(String managerHostName, String processingUnitName, DeploymentDescriptor deploymentDescriptor) {
		AsyncResultHandler handler = new AsyncResultHandler(uploadResourceEndpoint, uploadHttpRequestTimeout);

		String partName = "file";
		String filename = processingUnitName + ".jar";
		String filepath = deploymentDescriptor.getResource();
		String mediaType = "application/octet-stream";
		MultipartForm form = MultipartForm.create()
				.binaryFileUpload(partName, filename, filepath, mediaType);

		log.info("Sending HTTP put request to {} ...", uploadResourceEndpoint);

		webClient
				.put(httpPort, managerHostName, uploadResourceEndpoint)
				.putHeader("Content-Type", "multipart/form-data")
				.putHeader("Accept", "text/plain")
				.sendMultipartForm(form, handler);

		handler.waitAndcheck(201);
	}

	private void doDeploy(String managerHostName, String processingUnitJson) {
		AsyncResultHandler handler = new AsyncResultHandler(deployPuEndpoint, deployHttpRequestTimeout);

		log.info("Sending HTTP post request to {} ...", deployPuEndpoint);
		webClient
				.post(httpPort, managerHostName, deployPuEndpoint)
				.putHeader("Content-Type", "application/json")
				.putHeader("Accept", "text/plain")
				.sendBuffer(Buffer.buffer(processingUnitJson), handler);

		handler.waitAndcheck(202);
	}


	@Data
	private static class RequestReport {

		private Boolean succeeded;

		private Throwable error;

		private Integer statusCode;

		private String statusMessage;

		private String responseBodyAsString;

	}

	private static class AsyncResultHandler implements Handler<AsyncResult<HttpResponse<Buffer>>> {

		private final CountDownLatch requestFinished = new CountDownLatch(1);

		private final String url;

		private final Duration httpRequestTimeout;

		private RequestReport requestReport;

		public AsyncResultHandler(String url, Duration httpRequestTimeout) {
			this.url = url;
			this.httpRequestTimeout = httpRequestTimeout;
		}

		@Override
		public void handle(AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult) {
			final Throwable cause = httpResponseAsyncResult.cause();
			final @Nullable HttpResponse<Buffer> result = httpResponseAsyncResult.result();

			RequestReport requestReport = new RequestReport();
			requestReport.setSucceeded(httpResponseAsyncResult.succeeded());
			requestReport.setError(cause);
			if (result != null) {
				requestReport.setStatusCode(result.statusCode());
				requestReport.setStatusMessage(result.statusMessage());
				requestReport.setResponseBodyAsString(result.bodyAsString("UTF-8"));
			}
			this.requestReport = requestReport;
			log.info("requestReport = {}", requestReport);

			requestFinished.countDown();
		}

		public void waitAndcheck(int expectedHttpStatusCode) {
			try {
				boolean requestIsFinished = requestFinished.await(httpRequestTimeout.toMillis(), TimeUnit.MILLISECONDS);
				if (requestIsFinished) {
					if (requestReport.getError() != null) {
						log.error("HTTP request failed", requestReport.getError());
						throw new RuntimeException("HTTP request failed, Maybe the Manager service is down ?", requestReport.getError());
					}
					if (requestReport.getStatusCode() >= 500) {
						String errorMessage = String.format("Server error : requestReport = %s", requestReport);
						log.error(errorMessage);
						throw new RuntimeException(errorMessage);
					}
					if (requestReport.getStatusCode() >= 400) {
						String errorMessage = String.format("Bad request : requestReport = %s", requestReport);
						log.error(errorMessage);
						throw new RuntimeException(errorMessage);
					}
					if (requestReport.getStatusCode() != expectedHttpStatusCode) {
						String errorMessage = String.format("HTTP status code is different than expected : expectedHttpStatusCode = %d, requestReport = %s", expectedHttpStatusCode, requestReport);
						log.error(errorMessage);
						throw new RuntimeException(errorMessage);
					}
					log.info("HTTP Post request to {} was successful : requestReport = {}", url, requestReport);
				} else {
					throw new RuntimeException("Timeout while waiting for HTTP request to complete");
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Timeout while waiting for HTTP request to complete", e);
			}
		}

	}
}
