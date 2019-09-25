package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.service.IdExtractor;
import gca.in.xap.tools.operationtool.service.XapService;
import lombok.Setter;

import java.util.Iterator;
import java.util.Set;

public class HostnamesCandidates implements Iterable<String> {

	@Setter
	private static XapService xapService;

	@Setter
	private static IdExtractor idExtractor;

	public HostnamesCandidates() {
	}

	public Iterator<String> iterator() {
		Set<String> containersHostsNames = idExtractor.extractHostNames(xapService.findAllMachines());
		return containersHostsNames.iterator();
	}
}
