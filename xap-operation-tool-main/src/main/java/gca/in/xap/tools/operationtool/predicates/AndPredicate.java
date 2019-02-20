package gca.in.xap.tools.operationtool.predicates;

import lombok.ToString;

import java.util.Arrays;
import java.util.function.Predicate;

@ToString
public class AndPredicate<T> implements Predicate<T> {

	private final Predicate<T>[] predicates;

	public AndPredicate(Predicate<T>... predicates) {
		this.predicates = predicates;
	}

	@Override
	public boolean test(T t) {
		return Arrays.stream(predicates).allMatch(p -> p.test(t));
	}

}
