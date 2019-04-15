package gca.in.xap.tools.operationtool.predicates.pu;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.core.space.SpaceServiceDetails;

import java.util.Locale;
import java.util.function.Predicate;

@Slf4j
public class IsStatefulProcessingUnitPredicate implements Predicate<ProcessingUnitInstance> {

	@Override
	public boolean test(ProcessingUnitInstance pu) {
		final String puName = pu.getName();
		final boolean match = doTest(pu);
		log.debug("pu = {}, match = {}", puName, match);
		return match;
	}

	private boolean doTest(ProcessingUnitInstance pu) {
		boolean useEmbeddedSpaces = pu.isEmbeddedSpaces();
		if (useEmbeddedSpaces) {
			final SpaceServiceDetails[] embeddedSpacesDetails = pu.getEmbeddedSpacesDetails();
			for (SpaceServiceDetails spaceServiceDetails : embeddedSpacesDetails) {
				log.trace("spaceServiceDetails = {}", ToStringBuilder.reflectionToString(spaceServiceDetails));
				// il n'y a pas l'air d'y avoir une propriete qui indique que le space est un space specifique au mirror
				// on va donc partir du principe que par convention, le space interne du mirror contient la String "mirror" dans son ID
				if (!spaceServiceDetails.getId().toLowerCase(Locale.ENGLISH).contains("mirror")) {
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

}
