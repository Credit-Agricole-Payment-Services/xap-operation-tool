package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.predicates.machine.MachineWithSameNamePredicate;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.zone.config.ExactZonesConfig;

import java.time.Duration;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class DefaultRebalanceProcessingUnitServiceTest {

	@InjectMocks
	private DefaultRebalanceProcessingUnitService service;

	@Mock
	private PuRelocateService puRelocateService;

	@Mock
	private XapService xapService;

	@Mock
	private UserConfirmationService userConfirmationService;

	@Mock
	private ProcessingUnit processingUnit;

	@Mock
	private ProcessingUnitInstance processingUnitInstance1;

	@Mock
	private ProcessingUnitInstance processingUnitInstance2;

	@Mock
	private ProcessingUnitInstance processingUnitInstance3;

	@Mock
	private ProcessingUnitInstance processingUnitInstance4;

	@Mock
	private ProcessingUnitInstance processingUnitInstance5;

	@Mock
	private ProcessingUnitInstance processingUnitInstance6;

	@Mock
	private GridServiceContainer gridServiceContainer1;

	@Mock
	private GridServiceContainer gridServiceContainer2;

	@Mock
	private GridServiceContainer gridServiceContainer3;

	@Mock
	private GridServiceContainer gridServiceContainer4;

	@Mock
	private GridServiceContainer gridServiceContainer5;

	@Mock
	private GridServiceContainer gridServiceContainer6;

	@Mock
	private Machine machine1;

	@Mock
	private Machine machine2;

	@Mock
	private Machine machine3;

	@Mock
	private Machine machine4;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		log.info("setUp()");
	}

	@Test
	public void should_not_rebalance_when_no_gsc_nor_no_puInstance() {
		final String processingUnitName = "my-pu";

		doReturn(new GridServiceContainer[0]).when(puRelocateService).findBestContainersToRelocate(same(processingUnit), any(Predicate.class), any(Predicate.class));
		doReturn(processingUnit).when(xapService).findProcessingUnitByName(processingUnitName);
		doReturn(new ProcessingUnitInstance[0]).when(processingUnit).getInstances();

		service.rebalanceProcessingUnit(processingUnitName, new RestartStrategy(Duration.ZERO));
	}

	@Test
	public void should_not_rebalance_when_only_one_container_and_only_one_puInstance() {
		final String processingUnitName = "my-pu";

		doReturn(new GridServiceContainer[]{gridServiceContainer1}).when(puRelocateService).findBestContainersToRelocate(same(processingUnit), any(Predicate.class), any(Predicate.class));
		doReturn(machine1).when(gridServiceContainer1).getMachine();
		//
		doReturn(processingUnit).when(xapService).findProcessingUnitByName(processingUnitName);
		doReturn(new ProcessingUnitInstance[]{processingUnitInstance1}).when(processingUnit).getInstances();
		//
		doReturn("machine1").when(machine1).getHostName();
		doReturn(gridServiceContainer1).when(processingUnitInstance1).getGridServiceContainer();
		doReturn(new ExactZonesConfig()).when(gridServiceContainer1).getExactZones();
		doReturn("machine1~1234").when(gridServiceContainer1).getId();

		service.rebalanceProcessingUnit(processingUnitName, new RestartStrategy(Duration.ZERO));
	}

	@Test
	public void should_rebalance_when_two_puInstance_onSameMachine_but_multiple_machines_available() {
		final String processingUnitName = "my-pu";

		doReturn(new GridServiceContainer[]{gridServiceContainer1, gridServiceContainer2, gridServiceContainer3, gridServiceContainer4}).when(puRelocateService).findBestContainersToRelocate(same(processingUnit), any(Predicate.class), any(Predicate.class));
		doReturn(machine1).when(gridServiceContainer1).getMachine();
		doReturn(machine1).when(gridServiceContainer2).getMachine();
		doReturn(machine2).when(gridServiceContainer3).getMachine();
		doReturn(machine2).when(gridServiceContainer4).getMachine();
		//
		doReturn(processingUnit).when(xapService).findProcessingUnitByName(processingUnitName);
		doReturn(new ProcessingUnitInstance[]{processingUnitInstance1, processingUnitInstance2}).when(processingUnit).getInstances();
		//
		doReturn(gridServiceContainer1).when(processingUnitInstance1).getGridServiceContainer();
		doReturn(gridServiceContainer2).when(processingUnitInstance2).getGridServiceContainer();
		//
		doReturn("machine1").when(machine1).getHostName();
		doReturn("machine2").when(machine2).getHostName();
		//
		doReturn(new ExactZonesConfig()).when(gridServiceContainer1).getExactZones();
		doReturn(new ExactZonesConfig()).when(gridServiceContainer2).getExactZones();
		doReturn(new ExactZonesConfig()).when(gridServiceContainer3).getExactZones();
		doReturn(new ExactZonesConfig()).when(gridServiceContainer4).getExactZones();
		//
		doReturn("machine1~1234").when(gridServiceContainer1).getId();
		doReturn("machine1~1235").when(gridServiceContainer2).getId();
		doReturn("machine2~3456").when(gridServiceContainer3).getId();
		doReturn("machine2~3457").when(gridServiceContainer4).getId();

		service.rebalanceProcessingUnit(processingUnitName, new RestartStrategy(Duration.ZERO));

		ArgumentMatcher matcher = o -> o == processingUnitInstance1 || o == processingUnitInstance2;

		verify(puRelocateService).relocatePuInstance(any(ProcessingUnitInstance.class), eq(new MachineWithSameNamePredicate("machine2")), eq(true));
	}

}
