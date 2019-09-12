package gca.in.xap.tools.operationtool.service.rebalance;

import lombok.Data;
import picocli.CommandLine;

import java.util.Set;
import java.util.TreeSet;

@Data
public class ZonesGroup {

	@CommandLine.Option(names = {"--zonesGroup"}, split = ",", description = "Only rebalance 1 Processing Unit Instance. If the option is set, then only 1 instance will be relocated. If the option is omited, then it will relocate as many instance os needed until the Processing Unit is balanced.")
	Set<String> zones;

	public ZonesGroup() {
		this(new TreeSet<>());
	}

	public ZonesGroup(Set<String> zones) {
		super();
		this.zones = zones;
	}

}
