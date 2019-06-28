package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.predicates.punames.FilterPuNamesPredicate;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Predicate;

@Component
@CommandLine.Command(name = "undeploy")
@Slf4j
public class UndeployCommand extends AbstractAppCommand implements Runnable {

	public static class WholeModeOptions {

		@CommandLine.Option(names = {"--whole"}, description = "Undeploy the application in whole.")
		boolean wholeMode;

		@CommandLine.Parameters(index = "0", arity = "0", description = "Application Name")
		String applicationName;

	}

	public static class MutuallyExclusiveOptions {

		@CommandLine.Option(names = {"--all"}, description = "Undeploy all processing units")
		boolean all;

		@CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
		WholeModeOptions wholeModeOptions;

		@CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
		PuNamesFilteringOptions puNamesFilteringOptions;

	}

	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	private UserConfirmationService userConfirmationService;

	@CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
	private MutuallyExclusiveOptions mutuallyExclusiveOptions;

	private static boolean isNullOrEmpty(List list) {
		return list == null || list.isEmpty();
	}

	@Override
	public void run() {
		xapService.printReportOnContainersAndProcessingUnits();

		if (mutuallyExclusiveOptions.wholeModeOptions != null && mutuallyExclusiveOptions.wholeModeOptions.wholeMode) {
			if (mutuallyExclusiveOptions.wholeModeOptions.applicationName == null) {
				throw new IllegalArgumentException("Application Name is required");
			}
			log.warn("Will undeploy Application : {}", mutuallyExclusiveOptions.wholeModeOptions.applicationName);
			userConfirmationService.askConfirmationAndWait();

			xapService.undeployIfExists(mutuallyExclusiveOptions.wholeModeOptions.applicationName);
		} else {
			if (mutuallyExclusiveOptions.all) {
				log.warn("Will undeploy ALL Processing Units");
				userConfirmationService.askConfirmationAndWait();

				final Predicate<String> processingUnitsPredicate = (pu -> true);
				xapService.undeployProcessingUnits(processingUnitsPredicate);
			} else {
				if (mutuallyExclusiveOptions.puNamesFilteringOptions != null) {
					log.warn("Will undeploy Processing Units matching the provided filter : processingUnitsIncludes = {}, processingUnitsExcludes = {}", mutuallyExclusiveOptions.puNamesFilteringOptions.processingUnitsIncludes, mutuallyExclusiveOptions.puNamesFilteringOptions.processingUnitsExcludes);
					userConfirmationService.askConfirmationAndWait();

					final Predicate<String> processingUnitsPredicate = FilterPuNamesPredicate.createProcessingUnitsPredicate(mutuallyExclusiveOptions.puNamesFilteringOptions.processingUnitsIncludes, mutuallyExclusiveOptions.puNamesFilteringOptions.processingUnitsExcludes);
					xapService.undeployProcessingUnits(processingUnitsPredicate);
				}
			}
		}

	}

}
