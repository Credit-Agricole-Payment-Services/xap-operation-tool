package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.model.VirtualMachineDescription;

import java.util.Comparator;

public class VirtualMachineDescriptionComparator implements Comparator<VirtualMachineDescription> {
	@Override
	public int compare(VirtualMachineDescription o1, VirtualMachineDescription o2) {
		return Comparator
				.comparing(VirtualMachineDescription::getComponentType)
				.thenComparing(VirtualMachineDescription::getHostName)
				.thenComparing(VirtualMachineDescription::getHeapSizeInMBMax)
				.thenComparing(VirtualMachineDescription::getUptime)
				.compare(o1, o2);
	}
}
