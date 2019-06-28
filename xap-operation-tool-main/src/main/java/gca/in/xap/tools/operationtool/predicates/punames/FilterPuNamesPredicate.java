package gca.in.xap.tools.operationtool.predicates.punames;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class FilterPuNamesPredicate {

	public static Predicate<String> createProcessingUnitsPredicate(@Nullable List<String> processingUnitsIncludes, @Nullable List<String> processingUnitsExcludes) {
		Predicate<String> includePredicate;
		if (processingUnitsIncludes != null) {
			includePredicate = value -> processingUnitsIncludes.contains(value);
		} else {
			includePredicate = value -> true;
		}
		if (processingUnitsExcludes != null) {
			return value -> !processingUnitsIncludes.contains(value) && includePredicate.test(value);
		} else {
			return includePredicate;
		}
	}

}
