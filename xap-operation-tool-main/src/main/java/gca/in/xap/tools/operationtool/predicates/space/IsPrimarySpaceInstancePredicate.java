package gca.in.xap.tools.operationtool.predicates.space;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import org.openspaces.admin.space.SpaceInstance;

import java.util.function.Predicate;

public class IsPrimarySpaceInstancePredicate implements Predicate<SpaceInstance> {
	@Override
	public boolean test(SpaceInstance spaceInstance) {
		SpaceMode spaceMode = spaceInstance.getMode();
		if (spaceMode == null) {
			return false;
		}
		switch (spaceMode) {
			case PRIMARY:
				return true;
			case NONE:
			case BACKUP:
			default:
				return false;
		}
	}
}
