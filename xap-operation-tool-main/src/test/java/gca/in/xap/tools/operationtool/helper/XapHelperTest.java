package gca.in.xap.tools.operationtool.helper;

import org.junit.Test;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.pu.topology.ProcessingUnitConfigHolder;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class XapHelperTest {

	@Test
	public void should_await_deployment_ends_success() throws TimeoutException {
		ApplicationConfig applicationConfig = mock(ApplicationConfig.class, DefaultUnexpectedMockInvocationAnswer.singleton);
		Application dataApp = mock(Application.class, DefaultUnexpectedMockInvocationAnswer.singleton);

		ProcessingUnitConfigHolder[] processingUnitsToDeploy = new ProcessingUnitConfigHolder[3];
		processingUnitsToDeploy[0] = mock(ProcessingUnitConfigHolder.class, DefaultUnexpectedMockInvocationAnswer.singleton);
		processingUnitsToDeploy[1] = mock(ProcessingUnitConfigHolder.class, DefaultUnexpectedMockInvocationAnswer.singleton);
		processingUnitsToDeploy[2] = mock(ProcessingUnitConfigHolder.class, DefaultUnexpectedMockInvocationAnswer.singleton);

		doReturn("my-stateful-pu1").when(processingUnitsToDeploy[0]).getName();
		doReturn("my-stateless-pu2").when(processingUnitsToDeploy[1]).getName();
		doReturn("my-mirror-pu").when(processingUnitsToDeploy[2]).getName();
		doReturn(processingUnitsToDeploy).when(applicationConfig).getProcessingUnits();

		doReturn("AwesomeApp").when(applicationConfig).getName();

		ProcessingUnits processingUnits = mock(ProcessingUnits.class, DefaultUnexpectedMockInvocationAnswer.singleton);

		ProcessingUnit pu1 = mock(ProcessingUnit.class, DefaultUnexpectedMockInvocationAnswer.singleton);
		ProcessingUnit pu2 = mock(ProcessingUnit.class, DefaultUnexpectedMockInvocationAnswer.singleton);
		ProcessingUnit pu3 = mock(ProcessingUnit.class, DefaultUnexpectedMockInvocationAnswer.singleton);

		doReturn(2).when(pu1).getPlannedNumberOfInstances();
		doReturn(2).when(pu2).getPlannedNumberOfInstances();
		doReturn(1).when(pu3).getPlannedNumberOfInstances();

		doReturn(new ProcessingUnitInstance[2]).when(pu1).getInstances();
		doReturn(new ProcessingUnitInstance[2]).when(pu2).getInstances();
		doReturn(new ProcessingUnitInstance[1]).when(pu3).getInstances();

		doReturn("my-stateful-pu1").when(pu1).getName();
		doReturn("my-stateless-pu2").when(pu2).getName();
		doReturn("my-mirror-pu").when(pu3).getName();

		doAnswer(invocationOnMock -> {
			Thread.sleep(4000);
			return true;
		}).when(pu1).waitFor(eq(2), anyLong(), any());
		doAnswer(invocationOnMock -> {
			Thread.sleep(2000);
			return true;
		}).when(pu2).waitFor(eq(2), anyLong(), any());
		doAnswer(invocationOnMock -> {
			Thread.sleep(1000);
			return true;
		}).when(pu3).waitFor(eq(1), anyLong(), any());

		ProcessingUnit[] processingUnitsArray = new ProcessingUnit[]{pu1, pu2, pu3};

		doReturn(processingUnits).when(dataApp).getProcessingUnits();
		doReturn(processingUnitsArray).when(processingUnits).getProcessingUnits();

		doReturn(pu1).when(processingUnits).getProcessingUnit("my-stateful-pu1");
		doReturn(pu2).when(processingUnits).getProcessingUnit("my-stateless-pu2");
		doReturn(pu3).when(processingUnits).getProcessingUnit("my-mirror-pu");

		long deploymentStartTime = System.currentTimeMillis();

		XapHelper.awaitDeployment(applicationConfig, dataApp, deploymentStartTime, Duration.of(10, ChronoUnit.SECONDS));
	}

}
