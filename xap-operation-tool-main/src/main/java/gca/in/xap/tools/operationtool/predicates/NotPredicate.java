package gca.in.xap.tools.operationtool.predicates;

import java.util.function.Predicate;

public class NotPredicate<T> implements Predicate<T> {

	private final Predicate<T> delegate;

	public NotPredicate(Predicate<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean test(T t) {
		return !delegate.test(t);
	}
}
