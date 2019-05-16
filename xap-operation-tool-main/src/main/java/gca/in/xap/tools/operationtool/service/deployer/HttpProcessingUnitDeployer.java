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

	private final WebClient client;

	private final ProcessingUnitConfigToDeploymentDescriptorMapper processingUnitConfigToDeploymentDescriptorMapper;

	@Setter
	private int httpPort = 8090;

	@Setter
	private String deployPuEndpoint = "/v2/pus";

	@Setter
	private String uploadResourceEndpoint = "/pus/resources";

	@Setter
	private Duration uploadTimeout = Duration.ofSeconds(15);

	@Setter
	private Duration deployTimeout = Duration.ofSeconds(15);

	public HttpProcessingUnitDeployer(
			Admin admin,
			DeploymentDescriptorMarshaller deploymentDescriptorMarshaller,
			ProcessingUnitConfigToDeploymentDescriptorMapper processingUnitConfigToDeploymentDescriptorMapper
	) {
		this.admin = admin;
		this.deploymentDescriptorMarshaller = deploymentDescriptorMarshaller;
		this.processingUnitConfigToDeploymentDescriptorMapper = processingUnitConfigToDeploymentDescriptorMapper;
		//
		Vertx vertx = Vertx.vertx();
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

		doUploadResources(managerHostName, puName, deploymentDescriptor);

		deploymentDescriptor.setResource(puName + ".jar");
		final String processingUnitJson = marshall(deploymentDescriptor);
		log.info("processingUnitJson = {}", processingUnitJson);
		doDeploy(managerHostName, processingUnitJson);

		int timeoutDurationInSeconds = 60;
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(puName, timeoutDurationInSeconds, TimeUnit.SECONDS);
		if (processingUnit == null) {
			String errorMessage = String.format("ProcessingUnit is still not deployed after timeout of %d seconds", timeoutDurationInSeconds);
			log.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		return processingUnit;
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

	private void doUploadResources(String managerHostName, String processingUnitName, DeploymentDescriptor deploymentDescriptor) {
		CountDownLatch requestFinished = new CountDownLatch(1);
		AtomicBoolean requestSucceeded = new AtomicBoolean(false);
		AtomicReference<Throwable> error = new AtomicReference<>();

		MultipartForm form = MultipartForm.create()
				.binaryFileUpload("file", processingUnitName + ".jar", deploymentDescriptor.getResource(), "application/java-archive");

		client
				.post(httpPort, managerHostName, uploadResourceEndpoint)
				//.putHeader("Content-Type", "multipart/form-data")
				.putHeader("Accept", "text/plain")
				.sendMultipartForm(form, new Handler<AsyncResult<HttpResponse<Buffer>>>() {
					@Override
					public void handle(AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult) {
						log.info("httpResponseAsyncResult.succeeded() = {}, httpResponseAsyncResult.failed() = {}", httpResponseAsyncResult.succeeded(), httpResponseAsyncResult.failed());
						requestSucceeded.set(httpResponseAsyncResult.succeeded());
						error.set(httpResponseAsyncResult.cause());
						requestFinished.countDown();
					}
				});
		try {
			requestFinished.await(uploadTimeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!requestSucceeded.get()) {
				log.error("HTTP request failed", error.get());
				throw new RuntimeException("HTTP request failed", error.get());
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Timeout while waiting for HTTP request to complete", e);
		}
	}

	private void doDeploy(String managerHostName, String processingUnitJson) {
		CountDownLatch requestFinished = new CountDownLatch(1);
		AtomicBoolean requestSucceeded = new AtomicBoolean(false);
		AtomicReference<Throwable> error = new AtomicReference<>();

		client
				.post(httpPort, managerHostName, deployPuEndpoint)
				.putHeader("Content-Type", "application/json")
				.putHeader("Accept", "text/plain")
				.sendBuffer(Buffer.buffer(processingUnitJson), httpResponseAsyncResult -> {
					log.info("httpResponseAsyncResult.succeeded() = {}, httpResponseAsyncResult.failed() = {}", httpResponseAsyncResult.succeeded(), httpResponseAsyncResult.failed());
					requestSucceeded.set(httpResponseAsyncResult.succeeded());
					error.set(httpResponseAsyncResult.cause());
					requestFinished.countDown();
				});

		try {
			requestFinished.await(deployTimeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!requestSucceeded.get()) {
				log.error("HTTP request failed", error.get());
				throw new RuntimeException("HTTP request failed", error.get());
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Timeout while waiting for HTTP request to complete", e);
		}
	}

}
