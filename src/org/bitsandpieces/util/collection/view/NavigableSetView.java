/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Iterator;
import java.util.NavigableSet;

/**
 * A {@code NavigableSetView} represents an unmodifiable group of sorted and
 * distinct objects with additional search functionality, usually backed by a
 * {@link java.util.NavigableSet}. Generally speaking, whether changes to the
 * backing data-structure are reflected by the {@code View} (and its iterators,
 * spliterators and streams) depends on whether the backing data-structure
 * behaves the same way.
 *
 * @param <E> the type of elements maintained by this {@code NavigableSetView}
 *
 * @see View
 * @see java.util.NavigableSet
 *
 * @author Jan Kebernik
 */
public interface NavigableSetView<E> extends SortedSetView<E> {

	/**
	 * Returns the greatest element in this {@code SetView} strictly less than
	 * the given element, or {@code null} if there is no such element.
	 *
	 * @param e the value to match
	 * @return the greatest element less than {@code e}, or {@code null} if
	 * there is no such element
	 * @throws ClassCastException if the specified element cannot be compared
	 * with the elements currently in the {@code SetView}
	 * @throws NullPointerException if the specified element is {@code null} and
	 * this {@code SetView} does not permit {@code null} elements
	 */
	E lower(E e);

	/**
	 * Returns the greatest element in this {@code SetView} less than or equal
	 * to the given element, or {@code null} if there is no such element.
	 *
	 * @param e the value to match
	 * @return the greatest element less than or equal to {@code e}, or
	 * {@code null} if there is no such element
	 * @throws ClassCastException if the specified element cannot be compared
	 * with the elements currently in the {@code SetView}
	 * @throws NullPointerException if the specified element is {@code null} and
	 * this {@code SetView} does not permit {@code null} elements
	 */
	E floor(E e);

	/**
	 * Returns the least element in this {@code SetView} greater than or equal
	 * to the given element, or {@code null} if there is no such element.
	 *
	 * @param e the value to match
	 * @return the least element greater than or equal to {@code e}, or
	 * {@code null} if there is no such element
	 * @throws ClassCastException if the specified element cannot be compared
	 * with the elements currently in the {@code SetView}
	 * @throws NullPointerException if the specified element is {@code null} and
	 * this {@code SetView} does not permit {@code null} elements
	 */
	E ceiling(E e);

	/**
	 * Returns the least element in this {@code SetView} strictly greater than
	 * the given element, or {@code null} if there is no such element.
	 *
	 * @param e the value to match
	 * @return the least element greater than {@code e}, or {@code null} if
	 * there is no such element
	 * @throws ClassCastException if the specified element cannot be compared
	 * with the elements currently in the {@code SetView}
	 * @throws NullPointerException if the specified element is {@code null} and
	 * this {@code SetView} does not permit {@code null} elements
	 */
	E higher(E e);

	/**
	 * Returns an iterator over the elements in this {@code SetView}, in
	 * ascending order.
	 *
	 * @return an iterator over the elements in this {@code SetView}, in
	 * ascending order
	 */
	@Override
	Iterator<E> iterator();

	/**
	 * Returns a reverse order {@code SetView} of the elements contained in this
	 * {@code SetView}. The descending {@code SetView} is backed by this
	 * {@code SetView}, so changes to this {@code SetView} are reflected in the
	 * descending {@code SetView}. If this {@code SetView} is modified while an
	 * iteration over the descending {@code SetView} is in progress, the results
	 * of the iteration are undefined.
	 *
	 * @return a reverse order view of this {@code SetView}
	 */
	NavigableSetView<E> descendingSet();

	/**
	 * Returns an iterator over the elements in this {@code SetView}, in
	 * descending order. Equivalent in effect to
	 * {@code descendingSet().iterator()}.
	 *
	 * @return an iterator over the elements in this {@code SetView}, in
	 * descending order
	 */
	Iterator<E> descendingIterator();

	/**
	 * Returns a {@code SetView} of the portion of this {@code SetView} whose
	 * elements range from {@code fromElement} to {@code toElement}. If
	 * {@code fromElement} and {@code toElement} are equal, the returned
	 * {@code SetView} is empty unless {@code
	 * fromInclusive} and {@code toInclusive} are both true. The returned
	 * {@code SetView} is backed by this {@code SetView}, so changes in this
	 * {@code SetView} are reflected in the returned {@code SetView}. The
	 * returned {@code SetView} supports all optional {@code SetView} operations
	 * that this {@code SetView} supports.
	 *
	 * @param fromElement low endpoint of the returned {@code SetView}
	 * @param fromInclusive {@code true} if the low endpoint is to be included
	 * in the returned {@code SetView}
	 * @param toElement high endpoint of the returned {@code SetView}
	 * @param toInclusive {@code true} if the high endpoint is to be included in
	 * the returned {@code SetView}
	 * @return a {@code SetView} of the portion of this {@code SetView} whose
	 * elements range from {@code fromElement}, inclusive, to {@code toElement},
	 * exclusive
	 * @throws ClassCastException if {@code fromElement} and {@code toElement}
	 * cannot be compared to one another using this {@code SetView}'s comparator
	 * (or, if the {@code SetView} has no comparator, using natural ordering).
	 * Implementations may, but are not required to, throw this exception if
	 * {@code fromElement} or {@code toElement} cannot be compared to elements
	 * currently in the {@code SetView}.
	 * @throws NullPointerException if {@code fromElement} or {@code toElement}
	 * is {@code null} and this {@code SetView} does not permit {@code null}
	 * elements
	 * @throws IllegalArgumentException if {@code fromElement} is greater than
	 * {@code toElement}; or if this {@code SetView} itself has a restricted
	 * range, and {@code fromElement} or {@code toElement} lies outside the
	 * bounds of the range.
	 */
	NavigableSetView<E> subSet(E fromElement, boolean fromInclusive,
			E toElement, boolean toInclusive);

	/**
	 * Returns a {@code SetView} of the portion of this {@code SetView} whose
	 * elements are less than (or equal to, if {@code inclusive} is true)
	 * {@code toElement}. The returned {@code SetView} is backed by this
	 * {@code SetView}, so changes in this{@code SetView} are reflected in the
	 * returned {@code SetView}. The returned {@code SetView} supports all
	 * optional {@code SetView} operations that this {@code SetView} supports.
	 *
	 * @param toElement high endpoint of the returned {@code SetView}
	 * @param inclusive {@code true} if the high endpoint is to be included in
	 * the returned {@code SetView}
	 * @return a {@code SetView} of the portion of this {@code SetView} whose
	 * elements are less than (or equal to, if {@code inclusive} is true)
	 * {@code toElement}
	 * @throws ClassCastException if {@code toElement} is not compatible with
	 * this {@code SetView}'s comparator (or, if the {@code SetView} has no
	 * comparator, if {@code toElement} does not implement {@link Comparable}).
	 * Implementations may, but are not required to, throw this exception if
	 * {@code toElement} cannot be compared to elements currently in the
	 * {@code SetView}.
	 * @throws NullPointerException if {@code toElement} is {@code null} and
	 * this {@code SetView} does not permit {@code null} elements
	 * @throws IllegalArgumentException if this {@code SetView} itself has a
	 * restricted range, and {@code toElement} lies outside the bounds of the
	 * range
	 */
	NavigableSetView<E> headSet(E toElement, boolean inclusive);

	/**
	 * Returns a {@code SetView} of the portion of this {@code SetView} whose
	 * elements are greater than (or equal to, if {@code inclusive} is true)
	 * {@code fromElement}. The returned {@code SetView} is backed by this
	 * {@code SetView}, so changes in the returned {@code SetView} are reflected
	 * in this {@code SetView}, and vice-versa. The returned {@code SetView}
	 * supports all optional {@code SetView} operations that this
	 * {@code SetView} supports.
	 *
	 * @param fromElement low endpoint of the returned {@code SetView}
	 * @param inclusive {@code true} if the low endpoint is to be included in
	 * the returned {@code SetView}
	 * @return a {@code SetView} of the portion of this {@code SetView} whose
	 * elements are greater than or equal to {@code fromElement}
	 * @throws ClassCastException if {@code fromElement} is not compatible with
	 * this {@code SetView}'s comparator (or, if the {@code SetView} has no
	 * comparator, if {@code fromElement} does not implement
	 * {@link Comparable}). Implementations may, but are not required to, throw
	 * this exception if {@code fromElement} cannot be compared to elements
	 * currently in the {@code SetView}.
	 * @throws NullPointerException if {@code fromElement} is {@code null} and
	 * this {@code SetView} does not permit {@code null} elements
	 * @throws IllegalArgumentException if this {@code SetView} itself has a
	 * restricted range, and {@code fromElement} lies outside the bounds of the
	 * range
	 */
	NavigableSetView<E> tailSet(E fromElement, boolean inclusive);

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Equivalent to {@code subSet(fromElement, true, toElement, false)}.
	 *
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	@Override
	SortedSetView<E> subSet(E fromElement, E toElement);

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Equivalent to {@code headSet(toElement, false)}.
	 *
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	@Override
	SortedSetView<E> headSet(E toElement);

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Equivalent to {@code tailSet(fromElement, true)}.
	 *
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	@Override
	SortedSetView<E> tailSet(E fromElement);

	/**
	 * Returns an unmodifiable {@code NavigableSet} containing the same elements
	 * as this {@code NavigableSetView}.
	 *
	 * @return an unmodifiable {@code NavigableSet} containing the same elements
	 * as this {@code NavigableSetView}.
	 *
	 * @see Views#asView(NavigableSet)
	 */
	@Override
	public NavigableSet<E> asCollection();
}
