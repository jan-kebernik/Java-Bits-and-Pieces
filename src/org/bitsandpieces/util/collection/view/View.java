/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@code View} represents an unmodifiable group of objects, usually backed by
 * a {@link java.util.Collection}. Generally speaking, whether changes to the
 * backing data-structure are reflected by the {@code View} (and its
 * {@code Iterator}s, {@code Spliterator}s and {@code Stream}s) depends on
 * whether the backing data-structure behaves this way for its own
 * {@code Iterator}s, {@code Spliterator}s and {@code Stream}s.
 *
 * @param <E> the type of elements in this {@code View}
 *
 * @author Jan Kebernik
 */
public interface View<E> extends Iterable<E> {

	/**
	 * Returns the number of elements in this {@code View}. If this {@code View}
	 * contains more than {@code Integer.MAX_VALUE} elements, returns
	 * {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of elements in this {@code View}
	 */
	int size();

	/**
	 * Returns {@code true} if this {@code View} contains no elements.
	 *
	 * @return {@code true} if this {@code View} contains no elements
	 */
	boolean isEmpty();

	/**
	 * Returns {@code true} if this {@code View} contains the specified element.
	 * More formally, returns {@code true} if and only if this {@code View}
	 * contains at least one element {@code e} such that
	 * {@code (o == null ? e == null : o.equals(e))}.
	 *
	 * @param o element whose presence in this {@code View} is to be tested
	 * @return {@code true} if this {@code View} contains the specified element
	 * @throws ClassCastException if the type of the specified element is
	 * incompatible with this {@code View} (optional)
	 * @throws NullPointerException if the specified element is {@code null} and
	 * this {@code View} does not permit {@code null} elements (optional)
	 */
	boolean contains(Object o);

	/**
	 * Returns an iterator over the elements in this {@code View}. There are no
	 * guarantees concerning the order in which the elements are returned
	 * (unless this {@code View} is an instance of or is backed by some class
	 * that provides a guarantee).
	 *
	 * @apiNote {@code Iterator}s derived from any implentation of the
	 * <tt>View</tt>
	 * interface are not permitted to support the
	 * {@link Iterator#remove() remove()} method.
	 *
	 * @return an <tt>Iterator</tt> over the elements in this {@code View}
	 */
	@Override
	Iterator<E> iterator();

	/**
	 * Returns an array containing all of the elements in this {@code View}. If
	 * this {@code View} makes any guarantees as to what order its elements are
	 * returned by its iterator, this method must return the elements in the
	 * same order.
	 *
	 * <p>
	 * The returned array will be "safe" in that no references to it are
	 * maintained by this {@code View}. (In other words, this method must
	 * allocate a new array even if this {@code View} is backed by an array).
	 * The caller is thus free to modify the returned array.
	 *
	 * <p>
	 * This method acts as bridge between array-based and {@code View}-based
	 * APIs.
	 *
	 * @return an array containing all of the elements in this {@code View}
	 */
	Object[] toArray();

	/**
	 * Returns an array containing all of the elements in this {@code View}; the
	 * runtime type of the returned array is that of the specified array. If the
	 * {@code View} fits in the specified array, it is returned therein.
	 * Otherwise, a new array is allocated with the runtime type of the
	 * specified array and the size of this {@code View}.
	 *
	 * <p>
	 * If this {@code View} fits in the specified array with room to spare
	 * (i.e., the array has more elements than this {@code View}), the element
	 * in the array immediately following the end of the {@code View} is set to
	 * {@code null}. (This is useful in determining the length of this
	 * {@code View}
	 * <i>only</i> if the caller knows that this {@code View} does not contain
	 * any {@code null} elements.)
	 *
	 * <p>
	 * If this {@code View} makes any guarantees as to what order its elements
	 * are returned by its iterator, this method must return the elements in the
	 * same order.
	 *
	 * <p>
	 * Like the {@link #toArray()} method, this method acts as bridge between
	 * array-based and {@code View}-based APIs. Further, this method allows
	 * precise control over the runtime type of the output array, and may, under
	 * certain circumstances, be used to save allocation costs.
	 *
	 * <p>
	 * Suppose <tt>x</tt> is a {@code View} known to contain only strings. The
	 * following code can be used to dump the {@code View} into a newly
	 * allocated array of
	 * <tt>String</tt>:
	 *
	 * <pre>
	 *     String[] y = x.toArray(new String[0]);</pre>
	 *
	 * Note that <tt>toArray(new Object[0])</tt> is identical in function to
	 * <tt>toArray()</tt>.
	 *
	 * @param <T> the runtime type of the array to contain the {@code View}
	 * @param a the array into which the elements of this {@code View} are to be
	 * stored, if it is big enough; otherwise, a new array of the same runtime
	 * type is allocated for this purpose.
	 * @return an array containing all of the elements in this {@code View}
	 * @throws ArrayStoreException if the runtime type of the specified array is
	 * not a supertype of the runtime type of every element in this {@code View}
	 * @throws NullPointerException if the specified array is {@code null}
	 */
	<T> T[] toArray(T[] a);

	/**
	 * Returns {@code true} if this {@code View} contains all of the elements in
	 * the specified {@code View}.
	 *
	 * @param c {@code View} to be checked for containment in this {@code View}
	 * @return {@code true} if this {@code View} contains all of the elements in
	 * the specified {@code View}
	 * @throws ClassCastException if the types of one or more elements in the
	 * specified {@code View} are incompatible with this {@code View} (optional)
	 * @throws NullPointerException if the specified {@code View} contains one
	 * or more {@code null} elements and this {@code View} does not permit
	 * {@code null} elements (optional), or if the specified {@code View} is
	 * {@code null}.
	 * @see #contains(Object)
	 */
	boolean containsAll(View<?> c);

	/**
	 * Compares the specified object with this {@code View} for equality.
	 * <p>
	 *
	 * While the <tt>View</tt> interface adds no stipulations to the general
	 * contract for the <tt>Object.equals</tt>, programmers who implement the
	 * <tt>View</tt> interface "directly" (in other words, create a class that
	 * is a <tt>View</tt> but is not a <tt>SetView</tt>
	 * or a <tt>ListView</tt>) must exercise care if they choose to override the
	 * <tt>Object.equals</tt>. It is not necessary to do so, and the simplest
	 * course of action is to rely on <tt>Object</tt>'s implementation, but the
	 * implementor may wish to implement a "value comparison" in place of the
	 * default "reference comparison." (The <tt>ListView</tt> and
	 * <tt>SetView</tt> interfaces mandate such value comparisons.)
	 * <p>
	 *
	 * The general contract for the <tt>Object.equals</tt> method states that
	 * equals must be symmetric (in other words, <tt>a.equals(b)</tt> if and
	 * only if <tt>b.equals(a)</tt>). The contracts for <tt>ListView.equals</tt>
	 * and <tt>SetView.equals</tt> state that {@code ListView}s are only equal
	 * to other {@code ListView}s, and {@code SetView}s to other
	 * {@code SetView}s. Thus, a custom
	 * <tt>equals</tt> method for a {@code View} class that implements neither
	 * the
	 * <tt>ListView</tt> nor
	 * <tt>SetView</tt> interface must return <tt>false</tt> when this
	 * {@code View} is compared to any {@code ListView} or {@code SetView}. (By
	 * the same logic, it is not possible to write a class that correctly
	 * implements both the
	 * <tt>SetView</tt> and
	 * <tt>ListView</tt> interfaces.)
	 *
	 * @param o object to be compared for equality with this {@code View}
	 * @return {@code true} if the specified object is equal to this
	 * {@code View}
	 *
	 * @see Object#equals(Object)
	 * @see SetView#equals(Object)
	 * @see ListView#equals(Object)
	 */
	@Override
	boolean equals(Object o);

	/**
	 * Returns the hash code value for this {@code View}. While the
	 * <tt>View</tt> interface adds no stipulations to the general contract for
	 * the <tt>Object.hashCode</tt> method, programmers should take note that
	 * any class that overrides the <tt>Object.equals</tt>
	 * method must also override the <tt>Object.hashCode</tt> method in order to
	 * satisfy the general contract for the <tt>Object.hashCode</tt> method. In
	 * particular, <tt>v1.equals(v2)</tt> implies that
	 * <tt>v1.hashCode()==v2.hashCode()</tt>.
	 *
	 * @return the hash code value for this View
	 *
	 * @see Object#hashCode()
	 * @see Object#equals(Object)
	 */
	@Override
	int hashCode();

	/**
	 * Creates a {@link Spliterator} over the elements in this {@code View}.
	 *
	 * Implementations should document characteristic values reported by the
	 * spliterator. Such characteristic values are not required to be reported
	 * if the spliterator reports {@link Spliterator#SIZED} and this
	 * {@code View} contains no elements.
	 *
	 * <p>
	 * The default implementation should be overridden by subclasses that can
	 * return a more efficient spliterator. In order to preserve expected
	 * laziness behavior for the {@link #stream()} and
	 * {@link #parallelStream()}} methods, spliterators should either have the
	 * characteristic of {@code IMMUTABLE} or {@code CONCURRENT}, or be
	 * <em><a href="Spliterator.html#binding">late-binding</a></em>. If none of
	 * these is practical, the overriding class should describe the
	 * spliterator's documented policy of binding and structural interference,
	 * and should override the {@link #stream()} and {@link #parallelStream()}
	 * methods to create streams using a {@code Supplier} of the spliterator, as
	 * in:
	 * <pre>{@code
	 *     Stream<E> s = StreamSupport.stream(() -> spliterator(), spliteratorCharacteristics)
	 * }</pre>
	 * <p>
	 * These requirements ensure that streams produced by the {@link #stream()}
	 * and {@link #parallelStream()} methods will reflect the contents of the
	 * {@code View} as of initiation of the terminal stream operation.
	 *
	 * @implSpec The default implementation creates a
	 * <em>late-binding</em> spliterator from the {@code View}'s
	 * {@code Iterator}. The spliterator inherits the
	 * <em>fail-fast</em> properties of the {@code View}'s iterator.
	 * <p>
	 * The created {@code Spliterator} reports {@link Spliterator#SIZED}.
	 *
	 * @implNote The created {@code Spliterator} additionally reports
	 * {@link Spliterator#SUBSIZED}.
	 *
	 * <p>
	 * If a spliterator covers no elements then the reporting of additional
	 * characteristic values, beyond that of {@code SIZED} and {@code SUBSIZED},
	 * does not aid clients to control, specialize or simplify computation.
	 * However, this does enable shared use of an immutable and empty
	 * spliterator instance (see {@link Spliterators#emptySpliterator()}) for
	 * empty {@code View}s, and enables clients to determine if such a
	 * spliterator covers no elements.
	 *
	 * @return a {@code Spliterator} over the elements in this {@code View}
	 */
	@Override
	default Spliterator<E> spliterator() {
		return Spliterators.spliterator(iterator(), size(), 0);
	}

	/**
	 * Returns a sequential {@code Stream} with this {@code View} as its source.
	 *
	 * <p>
	 * This method should be overridden when the {@link #spliterator()} method
	 * cannot return a spliterator that is {@code IMMUTABLE},
	 * {@code CONCURRENT}, or <em>late-binding</em>. (See {@link #spliterator()}
	 * for details.)
	 *
	 * @implSpec The default implementation creates a sequential {@code Stream}
	 * from the {@code View}'s {@code Spliterator}.
	 *
	 * @return a sequential {@code Stream} over the elements in this
	 * {@code View}
	 */
	default Stream<E> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Returns a possibly parallel {@code Stream} with this {@code View} as its
	 * source. It is allowable for this method to return a sequential stream.
	 *
	 * <p>
	 * This method should be overridden when the {@link #spliterator()} method
	 * cannot return a spliterator that is {@code IMMUTABLE},
	 * {@code CONCURRENT}, or <em>late-binding</em>. (See {@link #spliterator()}
	 * for details.)
	 *
	 * @implSpec The default implementation creates a parallel {@code Stream}
	 * from the {@code View}'s {@code Spliterator}.
	 *
	 * @return a possibly parallel {@code Stream} over the elements in this
	 * {@code View}
	 */
	default Stream<E> parallelStream() {
		return StreamSupport.stream(spliterator(), true);
	}

	/**
	 * Returns an unmodifiable {@code Collection} containing the same elements
	 * as this {@code View}.
	 *
	 * @return an unmodifiable {@code Collection} containing the same elements
	 * as this {@code View}
	 *
	 * @see Views#asView(Collection)
	 */
	public Collection<E> asCollection();
}
