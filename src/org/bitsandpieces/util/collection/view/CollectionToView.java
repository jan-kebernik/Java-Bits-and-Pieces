/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

/**
 *
 * @author Jan Kebernik
 */
final class CollectionToView {

	private CollectionToView() {
	}

	static interface ToView<E> {

		View<E> toView();
	}

	static interface ToSetView<E> extends ToView<E> {

		@Override
		SetView<E> toView();
	}

	static interface ToSortedSetView<E> extends ToSetView<E> {

		@Override
		SortedSetView<E> toView();
	}

	static interface ToNavigableSetView<E> extends ToSortedSetView<E> {

		@Override
		NavigableSetView<E> toView();
	}

	static interface ToListView<E> extends ToView<E> {

		@Override
		ListView<E> toView();
	}

	static interface ToMapView<K, V> {

		MapView<K, V> toView();
	}

	static interface ToSortedMapView<K, V> extends ToMapView<K, V> {

		@Override
		SortedMapView<K, V> toView();
	}
	
	static interface ToNavigableMapView<K, V> extends ToSortedMapView<K, V> {

		@Override
		NavigableMapView<K, V> toView();
	}
}
