package gca.in.xap.tools.operationtool.util.collectionvisit;

public interface CollectionVisitingStrategy<T> {

	interface ItemVisitor<T> {
		void visit(T item);
	}

	void perform(T[] items, ItemVisitor<T> itemVisitor);

}
