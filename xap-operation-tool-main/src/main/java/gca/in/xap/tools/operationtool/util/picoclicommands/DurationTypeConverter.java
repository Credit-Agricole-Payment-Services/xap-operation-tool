package gca.in.xap.tools.operationtool.util.picoclicommands;

import picocli.CommandLine;

import java.time.Duration;
import java.time.format.DateTimeParseException;

public class DurationTypeConverter implements CommandLine.ITypeConverter<Duration> {
	@Override
	public Duration convert(String value) {
		try {
			return Duration.parse(value);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Value '" + value + "' is not a valid Duration", e);
		}
	}
}
