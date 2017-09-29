/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * A {@code SetView} represents an unmodifiable group of distinct objects,
 * usually backed by a {@link java.util.Set}. Generally speaking, whether changes
 * to the backing data-structure are reflected by the {@code View} (and its
 * iterators, spliterators and streams) depends on whether the backing
 * data-structure behaves the same way.
 *
 * @param <E> the type of elements maintained by this {@code SetView}
 *
 * @see View
 * @see java.util.Set
 *
 * @author Jan Kebernik
 */
public interface SetView<E> extends View<E> {

	/**
	 * Returns the number of elements in this {@code SetView} (its cardinality).
	 * If this {@code SetView} contains more than {@code Integer.MAX_VALUE}
	 * elements, returns {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of elements in this {@code SetView} (its cardinality)
	 */
	@Override
	int size();

	/**
	 * Returns {@code true} if this {@code SetView} contains no elements.
	 *
	 * @return {@code true} if this {@code SetView} contains no elements
	 */
	@Override
	boolean isEmpty();

	/**
	 * Returns {@code true} if this {@code SetView} contains the specified
	 * element. More formally, returns {@code true} if and only if this
	 * {@code SetView} contains an element {@code e} such that
	 * {@code (o == null ? e == null : o.equals(e))}.
	 *
	 * @param o element whose presence in this {@code SetView} is to be tested
	 * @return {@code true} if this {@code SetView} contains the specified
	 * element
	 * @throws ClassCastException if the type of the specified element is
	 * incompatible with this {@code SetView} (optional)
	 * @throws NullPointerException if the specified element is {@code null} and
	 * this {@code SetView} does not permit {@code null} elements (optional)
	 */
	@Override
	boolean contains(Object o);

	/**
	 * Returns an iterator over the elements in this {@code SetView}. The
	 * elements are returned in no particular order (unless this {@code SetView}
	 * is an instance of or is backed by some class that provides a guarantee).
	 *
	 * @apiNote {@code Iterator}s derived from any implentation of the
	 * {@code SetView} interface are not permitted to support the
	 * {@link Iterator#remove() remove()} method.
	 *
	 * @return an iterator over the elements in this {@code SetView}
	 */
	@Override
	Iterator<E> iterator();

	/**
	 * Returns an array containing all of the elements in this {@code SetView}.
	 * If this {@code SetView} or its backing class make any guarantees as to
	 * what order its elements are returned by its iterator, this method must
	 * return the elements in the same order.
	 *
	 * <p>
	 * The returned array will be "safe" in that no references to it are
	 * maintained by this {@code SetView}. (In other words, this method must
	 * allocate a new array even if this {@code SetView} is backed by an array).
	 * The caller is thus free to modify the returned array.
	 *
	 * <p>
	 * This method acts as bridge between array-based and {@code View}-based
	 * APIs.
	 *
	 * @return an array containing all the elements in this {@code SetView}
	 */
	@Override
	Object[] toArray();

	/**
	 * Returns an array containing all of the elements in this {@code SetView};
	 * the runtime type of the returned array is that of the specified array. If
	 * the {@code SetView} fits in the specified array, it is returned therein.
	 * Otherwise, a new array is allocated with the runtime type of the
	 * specified array and the size of this {@code SetView}.
	 *
	 * <p>
	 * If this {@code SetView} fits in the specified array with room to spare
	 * (i.e., the array has more elements than this {@code SetView}), the
	 * element in the array immediately following the end of the {@code SetView}
	 * is set to {@code null}. (This is useful in determining the length of this
	 * {@code SetView}
	 * <i>only</i> if the caller knows that this {@code SetView} does not
	 * contain any {@code null} elements.)
	 *
	 * <p>
	 * If this {@code SetView} or its backing class make any guarantees as to
	 * what order its elements are returned by its iterator, this method must
	 * return the elements in the same order.
	 *
	 * <p>
	 * Like the {@link #toArray()} method, this method acts as bridge between
	 * array-based and {@code View}-based APIs. Further, this method allows
	 * precise control over the runtime type of the output array, and may, under
	 * certain circumstances, be used to save allocation costs.
	 *
	 * <p>
	 * Suppose {@code x} is a {@code SetView} known to contain only
	 * {@code String}s. The following code can be used to dump the
	 * {@code SetView} into a newly allocated array of {@code String}:
	 *
	 * <pre>
	 *     String[] y = x.toArray(new String[0]);</pre>
	 *
	 * Note that {@code toArray(new Object[0])} is identical in function to
	 * {@code toArray()}.
	 *
	 * @param a the array into which the elements of this {@code SetView} are to
	 * be stored, if it is big enough; otherwise, a new array of the same
	 * runtime type is allocated for this purpose.
	 * @return an array containing all the elements in this {@code SetView}
	 * @throws ArrayStoreException if the runtime type of the specified array is
	 * not a supertype of the runtime type of every element in this
	 * {@code SetView}
	 * @throws NullPointerException if the specified array is {@code null}
	 */
	@Override
	<T> T[] toArray(T[] a);

	/**
	 * Returns {@code true} if this {@code SetView} contains all of the elements
	 * of the specified {@code View}. If the specified {@code View} is also a
	 * {@code SetView}, this method returns {@code true} if it is a
	 * <i>subset</i> of this {@code SetView}.
	 *
	 * @param c {@code View} to be checked for containment in this
	 * {@code SetView}
	 * @return {@code true} if this {@code SetView} contains all of the elements
	 * of the specified {@code View}
	 * @throws ClassCastException if the types of one or more elements in the
	 * specified {@code View} are incompatible with this {@code SetView}
	 * (optional)
	 * @throws NullPointerException if the specified {@code View} contains one
	 * or more {@code null} elements and this {@code SetView} does not permit
	 * {@code null} elements (optional), or if the specified {@code View} is
	 * {@code null}
	 * @see #contains(Object)
	 */
	@Override
	boolean containsAll(View<?> c);

	/**
	 * Compares the specified object with this {@code SetView} for equality.
	 * Returns {@code true} if the specified object is also a {@code SetView},
	 * the two {@code SetView}s have the same size, and every member of the
	 * specified {@code SetView} is contained in this {@code SetView} (or
	 * equivalently, every member of this {@code SetView} is contained in the
	 * specified {@code SetView}). This definition ensures that the equals
	 * method works properly across different implementations of the
	 * {@code SetView} interface.
	 *
	 * @param o object to be compared for equality with this {@code SetView}
	 * @return {@code true} if the specified object is equal to this
	 * {@code SetView}
	 */
	@Override
	boolean equals(Object o);

	/**
	 * Returns the hash code value for this {@code SetView}. The hash code of a
	 * {@code SetView} is defined to be the sum of the hash codes of the
	 * elements in the {@code SetView}, where the hash code of a {@code null}
	 * element is defined to be zero. This ensures that {@code s1.equals(s2)}
	 * implies that {@code s1.hashCode()==s2.hashCode()} for any two
	 * {@code SetView}s {@code s1} and {@code s2}, as required by the general
	 * contract of {@link Object#hashCode}.
	 *
	 * @return the hash code value for this {@code SetView}
	 * @see Object#equals(Object)
	 * @see Set#equals(Object)
	 */
	@Override
	int hashCode();

	/**
	 * Creates a {@code Spliterator} over the elements in this {@code SetView}.
	 *
	 * <p>
	 * The {@code Spliterator} reports {@link Spliterator#DISTINCT}.
	 * Implementations should document the reporting of additional
	 * characteristic values.
	 *
	 * @implSpec The default implementation creates a
	 * <em>late-binding</em> spliterator from the {@code SetView}'s
	 * {@code Iterator}. The spliterator inherits the
	 * <em>fail-fast</em> properties of the {@code SetView}'s iterator.
	 * <p>
	 * The created {@code Spliterator} additionally reports
	 * {@link Spliterator#SIZED}.
	 *
	 * @implNote The created {@code Spliterator} additionally reports
	 * {@link Spliterator#SUBSIZED}.
	 *
	 * @return a {@code Spliterator} over the elements in this {@code SetView}
	 */
	@Override
	default Spliterator<E> spliterator() {
		return Spliterators.spliterator(iterator(), size(), Spliterator.DISTINCT
		);
	}

	/**
	 * Returns an unmodifiable {@code Set} containing the same elements as this
	 * {@code SetView}.
	 *
	 * @return an unmodifiable {@code Set} containing the same elements as this
	 * {@code SetView}.
	 *
	 * @see Views#asView(Set)
	 */
	@Override
	public Set<E> asCollection();
}
