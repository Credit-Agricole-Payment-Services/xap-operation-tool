package gca.in.xap.tools.operationtool.service;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

@Component
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

}
