package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.model.VirtualMachineDescription;

import java.util.Comparator;

public class VirtualMachineDescriptionComparator implements Comparator<VirtualMachineDescription> {
	@Override
	public int compare(VirtualMachineDescription o1, VirtualMachineDescription o2) {
		int result;
		result = o1.getComponentType().compareTo(o2.getComponentType());
		if (result == 0) {
			result = o1.getHostName().compareTo(o2.getHostName());
		}
		if (result == 0) {
			result = o1.getHeapSizeInMBMax().compareTo(o2.getHeapSizeInMBMax());
		}
		if (result == 0) {
			result = o1.getUptime().compareTo(o2.getUptime());
		}
		return result;
	}
}
