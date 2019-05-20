package gca.in.xap.tools.operationtool.service.deployer;

import gca.in.xap.tools.operationtool.deploymentdescriptors.DeploymentDescriptor;
import gca.in.xap.tools.operationtool.deploymentdescriptors.json.DeploymentDescriptorMarshaller;
import gca.in.xap.tools.operationtool.deploymentdescriptors.puconfig.ProcessingUnitConfigToDeploymentDescriptorMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class HttpProcessingUnitDeployer implements ProcessingUnitDeployer {

	private final Admin admin;

	private final DeploymentDescriptorMarshaller deploymentDescriptorMarshaller;

	private final Vertx vertx;

	private final WebClient client;

	private final ProcessingUnitConfigToDeploymentDescriptorMapper processingUnitConfigToDeploymentDescriptorMapper;

	@Setter
	private int httpPort = 8090;

	@Setter
	private String deployPuEndpoint = "/v2/pus";

	@Setter
	private String uploadResourceEndpoint = "v2/pus/resources";

	@Setter
	private Duration uploadHttpRequestTimeout = Duration.ofSeconds(15);

	@Setter
	private Duration deployHttpRequestTimeout = Duration.ofSeconds(15);

	@Setter
	private Duration deployWaitTimeout = Duration.ofSeconds(120);

	public HttpProcessingUnitDeployer(
			Admin admin,
			Vertx vertx,
			DeploymentDescriptorMarshaller deploymentDescriptorMarshaller,
			ProcessingUnitConfigToDeploymentDescriptorMapper processingUnitConfigToDeploymentDescriptorMapper
	) {
		this.admin = admin;
		this.vertx = vertx;
		this.deploymentDescriptorMarshaller = deploymentDescriptorMarshaller;
		this.processingUnitConfigToDeploymentDescriptorMapper = processingUnitConfigToDeploymentDescriptorMapper;
		//
		WebClientOptions options = new WebClientOptions()
				.setUserAgent("xap-operation-tool");
		options.setKeepAlive(false);
		client = WebClient.create(vertx);
	}

	@Override
	public ProcessingUnit deploy(String puName, ProcessingUnitConfig processingUnitConfig) {
		GridServiceManagers gridServiceManagers = admin.getGridServiceManagers();
		GridServiceManager[] managers = gridServiceManagers.getManagers();
		GridServiceManager firstManager = managers[0];
		String managerHostName = firstManager.getMachine().getHostName();

		final DeploymentDescriptor deploymentDescriptor = processingUnitConfigToDeploymentDescriptorMapper.map(processingUnitConfig);
		log.info("deploymentDescriptor = {}", deploymentDescriptor);

		String processingUnitArchiveFilePath = processingUnitConfig.getProcessingUnit();
		log.info("processingUnitArchiveFilePath = {}", processingUnitArchiveFilePath);

		doUploadResourcesWithRetries(managerHostName, puName, deploymentDescriptor, 5);

		sleepALittleBit(30);

		deploymentDescriptor.setResource(puName + ".jar");
		final String processingUnitJson = marshall(deploymentDescriptor);
		log.info("processingUnitJson = {}", processingUnitJson);
		doDeploy(managerHostName, processingUnitJson);

		sleepALittleBit(20);

		log.info("Waiting {} for PU to become available ...", deployWaitTimeout);
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
		CountDownLatch requestFinished = new CountDownLatch(1);
		AtomicBoolean requestSucceeded = new AtomicBoolean(false);
		AtomicReference<Throwable> error = new AtomicReference<>();

		MultipartForm form = MultipartForm.create()
				.binaryFileUpload("file", processingUnitName + ".jar", deploymentDescriptor.getResource(), "application/octet-stream");

		log.info("Sending HTTP POST request to {} ...", uploadResourceEndpoint);
		client
				.post(httpPort, managerHostName, uploadResourceEndpoint)
				//.putHeader("Content-Type", "multipart/form-data")
				.putHeader("Accept", "text/plain")
				.sendMultipartForm(form, new Handler<AsyncResult<HttpResponse<Buffer>>>() {
					@Override
					public void handle(AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult) {
						Throwable cause = httpResponseAsyncResult.cause();
						log.info("httpResponseAsyncResult.succeeded() = {}, httpResponseAsyncResult.failed() = {}", httpResponseAsyncResult.succeeded(), httpResponseAsyncResult.failed(), cause);
						requestSucceeded.set(httpResponseAsyncResult.succeeded());
						error.set(cause);
						requestFinished.countDown();
					}
				});
		try {
			requestFinished.await(uploadHttpRequestTimeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!requestSucceeded.get()) {
				log.error("HTTP request failed", error.get());
				throw new RuntimeException("HTTP request failed, Maybe the Manager service is down ?", error.get());
			}
			log.info("HTTP Post request to {} was successful", uploadResourceEndpoint);
		} catch (InterruptedException e) {
			throw new RuntimeException("Timeout while waiting for HTTP request to complete", e);
		}
	}

	private void doDeploy(String managerHostName, String processingUnitJson) {
		CountDownLatch requestFinished = new CountDownLatch(1);
		AtomicBoolean requestSucceeded = new AtomicBoolean(false);
		AtomicReference<Throwable> error = new AtomicReference<>();

		log.info("Sending HTTP POST request to {} ...", deployPuEndpoint);
		client
				.post(httpPort, managerHostName, deployPuEndpoint)
				.putHeader("Content-Type", "application/json")
				.putHeader("Accept", "text/plain")
				.sendBuffer(Buffer.buffer(processingUnitJson), httpResponseAsyncResult -> {
					Throwable cause = httpResponseAsyncResult.cause();
					log.info("httpResponseAsyncResult.succeeded() = {}, httpResponseAsyncResult.failed() = {}", httpResponseAsyncResult.succeeded(), httpResponseAsyncResult.failed(), cause);
					requestSucceeded.set(httpResponseAsyncResult.succeeded());
					error.set(cause);
					requestFinished.countDown();
				});

		try {
			requestFinished.await(deployHttpRequestTimeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!requestSucceeded.get()) {
				log.error("HTTP request failed", error.get());
				throw new RuntimeException("HTTP request failed, Maybe the Manager service is down ?", error.get());
			}
			log.info("HTTP Post request to {} was successful", deployPuEndpoint);
		} catch (InterruptedException e) {
			throw new RuntimeException("Timeout while waiting for HTTP request to complete", e);
		}
	}

}
