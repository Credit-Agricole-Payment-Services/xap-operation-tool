package gca.in.xap.tools.operationtool.commandoptions;

import picocli.CommandLine;

import java.util.List;

public class PuNamesFilteringOptions {
	@CommandLine.Option(names = {"--puIncludes"}, split = ",", description = "List of names of the Processing Units to include. If you only want to act on a subset of the Processing Units, you can specify 1 or more processing units to include in this deployment.")
	public List<String> processingUnitsIncludes;

	@CommandLine.Option(names = {"--puExcludes"}, split = ",", description = "List of names of the Processing Units to exclude. If you only want to act on a subset of the Processing Units, you can specify 1 or more processing units to exclude from this deployment.")
	public List<String> processingUnitsExcludes;
}
