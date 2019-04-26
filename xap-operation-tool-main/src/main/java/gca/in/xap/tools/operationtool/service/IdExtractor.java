package gca.in.xap.tools.operationtool.service;

import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class IdExtractor {

	public Collection<String> extractContainerIds(ProcessingUnit existingProcessingUnit) {
		return extractIds(existingProcessingUnit.getGridServiceContainers());
	}

	public Collection<String> extractIds(Collection<GridServiceContainer> containers) {
		Set<String> gscIds = new TreeSet<>();
		for (GridServiceContainer gsc : containers) {
			gscIds.add(gsc.getId());
		}
		return gscIds;
	}

	public Collection<String> extractIds(GridServiceContainer[] containers) {
		Set<String> gscIds = new TreeSet<>();
		for (GridServiceContainer gsc : containers) {
			gscIds.add(gsc.getId());
		}
		return gscIds;
	}

	public Collection<String> extractIds(GridServiceManager[] managers) {
		Set<String> gscIds = new TreeSet<>();
		for (GridServiceManager gsm : managers) {
			gscIds.add(gsm.getMachine().getHostName());
		}
		return gscIds;
	}

	public Collection<String> extractIds(GridServiceAgent[] agents) {
		Set<String> gscIds = new TreeSet<>();
		for (GridServiceAgent gsa : agents) {
			gscIds.add(gsa.getMachine().getHostName());
		}
		return gscIds;
	}

	public Set<String> extractContainersHostsNames(GridServiceContainer[] containers) {
		TreeSet<String> result = Arrays.stream(containers).map(container -> container.getMachine().getHostName()).collect(Collectors.toCollection(TreeSet::new));
		log.info("extractContainersHostsNames() : result = {}", result);
		return result;
	}

	public Set<String> extractHostNames(Machine[] machines) {
		TreeSet<String> result = Arrays.stream(machines).map(Machine::getHostName).collect(Collectors.toCollection(TreeSet::new));
		log.info("extractHostNames() : result = {}", result);
		return result;
	}

	public Collection<String> extractRunningProcessingUnitsNames(GridServiceContainer gsc) {
		ProcessingUnitInstance[] puInstances = gsc.getProcessingUnitInstances();
		return extractProcessingUnitsNames(puInstances);
	}

	public Collection<String> extractProcessingUnitsNames(ProcessingUnitInstance[] puInstances) {
		List<String> names = new ArrayList<>();
		for (ProcessingUnitInstance pu : puInstances) {
			names.add(pu.getName());
		}
		Collections.sort(names);
		return names;
	}

}
