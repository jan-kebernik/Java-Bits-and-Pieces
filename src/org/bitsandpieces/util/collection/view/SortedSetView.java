/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.Spliterator;

/**
 * A {@code SortedSetView} represents an unmodifiable group of sorted and
 * distinct objects, usually backed by a {@link java.util.SortedSet}. Generally
 * speaking, whether changes to the backing data-structure are reflected by the
 * {@code View} (and its iterators, spliterators and streams) depends on whether
 * the backing data-structure behaves the same way.
 *
 * @param <E> the type of elements maintained by this {@code SortedSetView}
 *
 * @see View
 * @see java.util.SortedSet
 *
 * @author Jan Kebernik
 */
public interface SortedSetView<E> extends SetView<E> {

	/**
	 * Returns the comparator used to order the elements in this
	 * {@code SetView}, or {@code null} if this {@code SetView} uses the {@linkplain Comparable
	 * natural ordering} of its elements.
	 *
	 * @return the comparator used to order the elements in this
	 * {@code SetView}, or {@code null} if this {@code SetView} uses the natural
	 * ordering of its elements
	 */
	Comparator<? super E> comparator();

	/**
	 * Returns a {@code SetView} of the portion of this {@code SetView} whose
	 * elements range from {@code fromElement}, inclusive, to {@code toElement},
	 * exclusive. (If {@code fromElement} and {@code toElement} are equal, the
	 * returned {@code SetView} is empty.) The returned {@code SetView} is
	 * backed by this {@code SetView}, so changes in this {@code SetView} are
	 * reflected in the returned {@code SetView}. The returned {@code SetView}
	 * supports all optional {@code SetView} operations that this
	 * {@code SetView} supports.
	 *
	 * @param fromElement low endpoint (inclusive) of the returned
	 * {@code SetView}
	 * @param toElement high endpoint (exclusive) of the returned
	 * {@code SetView}
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
	 * is {@code null} and this {@code SetView} does not permit {@code null} elements
	 * @throws IllegalArgumentException if {@code fromElement} is greater than
	 * {@code toElement}; or if this {@code SetView} itself has a restricted
	 * range, and {@code fromElement} or {@code toElement} lies outside the
	 * bounds of the range
	 */
	SortedSetView<E> subSet(E fromElement, E toElement);

	/**
	 * Returns a {@code SetView} of the portion of this {@code SetView} whose
	 * elements are strictly less than {@code toElement}. The returned
	 * {@code SetView} is backed by this {@code SetView}, so changes in this
	 * {@code SetView} are reflected in the returned {@code SetView}. The
	 * returned {@code SetView} supports all optional {@code SetView} operations
	 * that this {@code SetView} supports.
	 *
	 * @param toElement high endpoint (exclusive) of the returned
	 * {@code SetView}
	 * @return a {@code SetView} of the portion of this {@code SetView} whose
	 * elements are strictly less than {@code toElement}
	 * @throws ClassCastException if {@code toElement} is not compatible with
	 * this {@code SetView}'s comparator (or, if the {@code SetView} has no
	 * comparator, if {@code toElement} does not implement {@link Comparable}).
	 * Implementations may, but are not required to, throw this exception if
	 * {@code toElement} cannot be compared to elements currently in the
	 * {@code SetView}.
	 * @throws NullPointerException if {@code toElement} is {@code null} and this
	 * {@code SetView} does not permit {@code null} elements
	 * @throws IllegalArgumentException if this {@code SetView} itself has a
	 * restricted range, and {@code toElement} lies outside the bounds of the
	 * range
	 */
	SortedSetView<E> headSet(E toElement);

	/**
	 * Returns a {@code SetView} of the portion of this {@code SetView} whose
	 * elements are greater than or equal to {@code fromElement}. The returned
	 * {@code SetView} is backed by this {@code SetView}, so changes in the
	 * returned {@code SetView} are reflected in this {@code SetView}, and
	 * vice-versa. The returned {@code SetView} supports all optional
	 * {@code SetView} operations that this {@code SetView} supports.
	 *
	 * @param fromElement low endpoint (inclusive) of the returned
	 * {@code SetView}
	 * @return a {@code SetView} of the portion of this {@code SetView} whose
	 * elements are greater than or equal to {@code fromElement}
	 * @throws ClassCastException if {@code fromElement} is not compatible with
	 * this {@code SetView}'s comparator (or, if the {@code SetView} has no
	 * comparator, if {@code fromElement} does not implement
	 * {@link Comparable}). Implementations may, but are not required to, throw
	 * this exception if {@code fromElement} cannot be compared to elements
	 * currently in the {@code SetView}.
	 * @throws NullPointerException if {@code fromElement} is {@code null} and this
	 * {@code SetView} does not permit {@code null} elements
	 * @throws IllegalArgumentException if this {@code SetView} itself has a
	 * restricted range, and {@code fromElement} lies outside the bounds of the
	 * range
	 */
	SortedSetView<E> tailSet(E fromElement);

	/**
	 * Returns the first (lowest) element currently in this {@code SetView}.
	 *
	 * @return the first (lowest) element currently in this {@code SetView}
	 * @throws java.util.NoSuchElementException if this {@code SetView} is empty
	 */
	E first();

	/**
	 * Returns the last (highest) element currently in this {@code SetView}.
	 *
	 * @return the last (highest) element currently in this {@code SetView}
	 * @throws java.util.NoSuchElementException if this {@code SetView} is empty
	 */
	E last();

	/**
	 * Creates a {@code Spliterator} over the elements in this
	 * {@code SortedSetView}.
	 *
	 * <p>
	 * The {@code Spliterator} reports {@link Spliterator#DISTINCT},
	 * {@link Spliterator#SORTED} and {@link Spliterator#ORDERED}.
	 * Implementations should document the reporting of additional
	 * characteristic values.
	 *
	 * <p>
	 * The spliterator's comparator (see
	 * {@link java.util.Spliterator#getComparator()}) must be {@code null} if
	 * the {@code SortedSetView}'s comparator (see {@link #comparator()}) is
	 * {@code null}. Otherwise, the spliterator's comparator must be the same as
	 * or impose the same total ordering as the {@code SortedSetView}'s
	 * comparator.
	 *
	 * @return a {@code Spliterator} over the elements in this
	 * {@code SortedSetView}
	 */
	@Override
	Spliterator<E> spliterator();

	/**
	 * Returns an unmodifiable {@code SortedSet} containing the same elements as
	 * this {@code SortedSetView}.
	 *
	 * @return an unmodifiable {@code SortedSet} containing the same elements as
	 * this {@code SortedSetView}.
	 *
	 * @see Views#asView(SortedSet)
	 */
	@Override
	public SortedSet<E> asCollection();
}
