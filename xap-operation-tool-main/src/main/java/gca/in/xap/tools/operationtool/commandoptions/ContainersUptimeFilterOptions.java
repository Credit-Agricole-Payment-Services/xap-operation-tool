package gca.in.xap.tools.operationtool.commandoptions;

import gca.in.xap.tools.operationtool.util.picoclicommands.DurationTypeConverter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import picocli.CommandLine;

import java.time.Duration;
import java.util.function.Predicate;

@Slf4j
public class ContainersUptimeFilterOptions {

	@CommandLine.Option(
			names = {"--uptime-greater-than"},
			converter = DurationTypeConverter.class,
			description = "If the Container is up for more than this duration, the action will be performed. Duration is expressed in ISO_8601 format (example : PT30S for a duration of 30 seconds, PT2M for a duration of 2 minutes)")
	@Setter
	public Duration uptimeGreaterThanDuration;

	@CommandLine.Option(
			names = {"--uptime-less-than"},
			converter = DurationTypeConverter.class,
			description = "If the Container is up for less than this duration, the action will be performed. Duration is expressed in ISO_8601 format (example : PT30S for a duration of 30 seconds, PT2M for a duration of 2 minutes)")
	@Setter
	public Duration uptimeLessThanDuration;

	public Predicate<GridServiceContainer> toPredicate() {
		//
		Predicate<GridServiceContainer> includePredicate;
		if (uptimeGreaterThanDuration != null) {
			includePredicate = gsc -> extractUptime(gsc).compareTo(uptimeGreaterThanDuration) > 0;
		} else {
			includePredicate = value -> true;
		}
		if (uptimeLessThanDuration != null) {
			return gsc -> extractUptime(gsc).compareTo(uptimeLessThanDuration) < 0 && includePredicate.test(gsc);
		} else {
			return includePredicate;
		}
	}

	public Duration extractUptime(GridServiceContainer gsc) {
		long startTime = gsc.getVirtualMachine().getDetails().getStartTime();
		long currentTime = System.currentTimeMillis();
		//
		long currentUptime = currentTime - startTime;
		log.info("currentUptime = {}", currentUptime);
		return Duration.ofMillis(currentUptime);
	}

}
