package gca.in.xap.tools.operationtool.commandoptions;

import org.junit.Test;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.vm.VirtualMachine;
import org.openspaces.admin.vm.VirtualMachineDetails;

import java.time.Duration;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ContainersUptimeFilterOptionsTest {

	@Test
	public void test_between_5min_and_10min() {
		ContainersUptimeFilterOptions filterOptions = new ContainersUptimeFilterOptions();
		filterOptions.setUptimeGreaterThanDuration(Duration.ofMinutes(5));
		filterOptions.setUptimeLessThanDuration(Duration.ofMinutes(10));
		//
		Predicate<GridServiceContainer> predicate = filterOptions.toPredicate();
		//
		VirtualMachineDetails virtualMachineDetails1 = mock(VirtualMachineDetails.class);
		doReturn(System.currentTimeMillis() - 5 * 60 * 1000 - 1000).when(virtualMachineDetails1).getStartTime();
		VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
		doReturn(virtualMachineDetails1).when(virtualMachine1).getDetails();
		GridServiceContainer gsc1 = mock(GridServiceContainer.class);
		doReturn(virtualMachine1).when(gsc1).getVirtualMachine();
		//
		boolean test = predicate.test(gsc1);
		assertTrue(test);
	}

	@Test
	public void test_older_than_1_day() {
		ContainersUptimeFilterOptions filterOptions = new ContainersUptimeFilterOptions();
		filterOptions.setUptimeGreaterThanDuration(Duration.ofDays(1));
		//
		Predicate<GridServiceContainer> predicate = filterOptions.toPredicate();
		//
		VirtualMachineDetails virtualMachineDetails1 = mock(VirtualMachineDetails.class);
		doReturn(System.currentTimeMillis() - 24 * 60 * 60 * 1000 - 5000).when(virtualMachineDetails1).getStartTime();
		VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
		doReturn(virtualMachineDetails1).when(virtualMachine1).getDetails();
		GridServiceContainer gsc1 = mock(GridServiceContainer.class);
		doReturn(virtualMachine1).when(gsc1).getVirtualMachine();
		//
		boolean test = predicate.test(gsc1);
		assertTrue(test);
	}

	@Test
	public void test_not_older_than_1_day() {
		ContainersUptimeFilterOptions filterOptions = new ContainersUptimeFilterOptions();
		filterOptions.setUptimeGreaterThanDuration(Duration.ofDays(1));
		//
		Predicate<GridServiceContainer> predicate = filterOptions.toPredicate();
		//
		VirtualMachineDetails virtualMachineDetails1 = mock(VirtualMachineDetails.class);
		doReturn(System.currentTimeMillis() - 23 * 60 * 60 * 1000 - 5000).when(virtualMachineDetails1).getStartTime();
		VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
		doReturn(virtualMachineDetails1).when(virtualMachine1).getDetails();
		GridServiceContainer gsc1 = mock(GridServiceContainer.class);
		doReturn(virtualMachine1).when(gsc1).getVirtualMachine();
		//
		boolean test = predicate.test(gsc1);
		assertFalse(test);
	}

	@Test
	public void test_less_than_2_day() {
		ContainersUptimeFilterOptions filterOptions = new ContainersUptimeFilterOptions();
		filterOptions.setUptimeLessThanDuration(Duration.ofDays(2));
		//
		Predicate<GridServiceContainer> predicate = filterOptions.toPredicate();
		//
		VirtualMachineDetails virtualMachineDetails1 = mock(VirtualMachineDetails.class);
		doReturn(System.currentTimeMillis() - 24 * 60 * 60 * 1000 - 5000).when(virtualMachineDetails1).getStartTime();
		VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
		doReturn(virtualMachineDetails1).when(virtualMachine1).getDetails();
		GridServiceContainer gsc1 = mock(GridServiceContainer.class);
		doReturn(virtualMachine1).when(gsc1).getVirtualMachine();
		//
		boolean test = predicate.test(gsc1);
		assertTrue(test);
	}

	@Test
	public void test_not_less_than_2_day() {
		ContainersUptimeFilterOptions filterOptions = new ContainersUptimeFilterOptions();
		filterOptions.setUptimeLessThanDuration(Duration.ofDays(2));
		//
		Predicate<GridServiceContainer> predicate = filterOptions.toPredicate();
		//
		VirtualMachineDetails virtualMachineDetails1 = mock(VirtualMachineDetails.class);
		doReturn(System.currentTimeMillis() - 49 * 60 * 60 * 1000 - 5000).when(virtualMachineDetails1).getStartTime();
		VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
		doReturn(virtualMachineDetails1).when(virtualMachine1).getDetails();
		GridServiceContainer gsc1 = mock(GridServiceContainer.class);
		doReturn(virtualMachine1).when(gsc1).getVirtualMachine();
		//
		boolean test = predicate.test(gsc1);
		assertFalse(test);
	}

	@Test
	public void test_no_constraint() {
		ContainersUptimeFilterOptions filterOptions = new ContainersUptimeFilterOptions();
		//
		Predicate<GridServiceContainer> predicate = filterOptions.toPredicate();
		//
		VirtualMachineDetails virtualMachineDetails1 = mock(VirtualMachineDetails.class);
		doReturn(System.currentTimeMillis() - 24 * 60 * 60 * 1000 - 5000).when(virtualMachineDetails1).getStartTime();
		VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
		doReturn(virtualMachineDetails1).when(virtualMachine1).getDetails();
		GridServiceContainer gsc1 = mock(GridServiceContainer.class);
		doReturn(virtualMachine1).when(gsc1).getVirtualMachine();
		//
		boolean test = predicate.test(gsc1);
		assertTrue(test);
	}

}
