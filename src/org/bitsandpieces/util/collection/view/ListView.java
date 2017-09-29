/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * A {@code ListView} represents an unmodifiable group of objects in proper
 * sequence, usually backed by a {@link java.util.List}.
 *
 * @param <E> the type of elements maintained by this {@code ListView}
 *
 * @see View
 * @see java.util.List
 *
 * @author Jan Kebernik
 */
public interface ListView<E> extends View<E> {

	/**
	 * Returns the number of elements in this {@code ListView}. If this
	 * {@code ListView} contains more than {@code Integer.MAX_VALUE} elements,
	 * returns {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of elements in this {@code ListView}
	 */
	@Override
	int size();

	/**
	 * Returns {@code true} if this {@code ListView} contains no elements.
	 *
	 * @return {@code true} if this {@code ListView} contains no elements
	 */
	@Override
	boolean isEmpty();

	/**
	 * Returns {@code true} if this {@code ListView} contains the specified
	 * element. More formally, returns {@code true} if and only if this
	 * {@code ListView} contains at least one element {@code e} such that
	 * {@code (o == null ? e == null : o.equals(e))}.
	 *
	 * @param o element whose presence in this {@code ListView} is to be tested
	 * @return {@code true} if this {@code ListView} contains the specified
	 * element
	 * @throws ClassCastException if the type of the specified element is
	 * incompatible with this {@code ListView} (optional)
	 * @throws NullPointerException if the specified element is {@code null} and
	 * this {@code ListView} does not permit {@code null} elements (optional)
	 */
	@Override
	boolean contains(Object o);

	/**
	 * Returns an iterator over the elements in this {@code ListView} in proper
	 * sequence.
	 *
	 * @apiNote {@code Iterator}s derived from any implentation of the
	 * {@code ListView} interface are not permitted to support the
	 * {@link Iterator#remove() remove()} method.
	 *
	 * @return an iterator over the elements in this {@code ListView} in proper
	 * sequence
	 */
	@Override
	Iterator<E> iterator();

	/**
	 * Returns an array containing all of the elements in this {@code ListView}
	 * in proper sequence (from first to last element).
	 *
	 * <p>
	 * The returned array will be "safe" in that no references to it are
	 * maintained by this {@code ListView}. (In other words, this method must
	 * allocate a new array even if this {@code ListView} is backed by an
	 * array). The caller is thus free to modify the returned array.
	 *
	 * <p>
	 * This method acts as bridge between array-based and {@code View}-based
	 * APIs.
	 *
	 * @return an array containing all of the elements in this {@code ListView}
	 * in proper sequence
	 * @see Views#asListView(Object[])
	 */
	@Override
	Object[] toArray();

	/**
	 * Returns an array containing all of the elements in this {@code ListView}
	 * in proper sequence (from first to last element); the runtime type of the
	 * returned array is that of the specified array. If the {@code ListView}
	 * fits in the specified array, it is returned therein. Otherwise, a new
	 * array is allocated with the runtime type of the specified array and the
	 * size of this {@code ListView}.
	 *
	 * <p>
	 * If the {@code ListView} fits in the specified array with room to spare
	 * (i.e., the array has more elements than the {@code ListView}), the
	 * element in the array immediately following the end of the
	 * {@code ListView} is set to {@code null}. (This is useful in determining
	 * the length of the {@code ListView} <i>only</i> if the caller knows that
	 * the {@code ListView} does not contain any {@code null} elements.)
	 *
	 * <p>
	 * Like the {@link #toArray()} method, this method acts as bridge between
	 * array-based and {@code View}-based APIs. Further, this method allows
	 * precise control over the runtime type of the output array, and may, under
	 * certain circumstances, be used to save allocation costs.
	 *
	 * <p>
	 * Suppose {@code x} is a {@code ListView} known to contain only
	 * {@code String}s. The following code can be used to dump the
	 * {@code ListView} into a newly allocated array of {@code Srtring}:
	 *
	 * <pre>{@code
	 *     String[] y = x.toArray(new String[0]);
	 * }</pre>
	 *
	 * Note that {@code toArray(new Object[0])} is identical in function to
	 * {@code toArray()}.
	 *
	 * @param a the array into which the elements of this {@code ListView} are
	 * to be stored, if it is big enough; otherwise, a new array of the same
	 * runtime type is allocated for this purpose.
	 * @return an array containing the elements of this {@code ListView}
	 * @throws ArrayStoreException if the runtime type of the specified array is
	 * not a supertype of the runtime type of every element in this
	 * {@code ListView}
	 * @throws NullPointerException if the specified array is {@code null}
	 */
	@Override
	<T> T[] toArray(T[] a);

	/**
	 * Returns {@code true} if this {@code ListView} contains all of the
	 * elements of the specified {@code View}.
	 *
	 * @param c {@code View} to be checked for containment in this
	 * {@code ListView}
	 * @return {@code true} if this {@code ListView} contains all of the
	 * elements of the specified {@code View}
	 * @throws ClassCastException if the types of one or more elements in the
	 * specified {@code View} are incompatible with this {@code ListView}
	 * (optional)
	 * @throws NullPointerException if the specified {@code View} contains one
	 * or more {@code null} elements and this {@code ListView} does not permit
	 * {@code null} elements (optional), or if the specified {@code View} is
	 * {@code null}
	 * @see #contains(Object)
	 */
	@Override
	boolean containsAll(View<?> c);

	/**
	 * Compares the specified object with this {@code ListView} for equality.
	 * Returns {@code true} if and only if the specified object is also a
	 * {@code ListView}, both {@code ListView}s have the same size, and all
	 * corresponding pairs of elements in the two {@code ListView}s are
	 * <i>equal</i>. (Two elements {@code e1} and {@code e2} are <i>equal</i> if
	 * {@code (e1 == null ? e2 == null : e1.equals(e2))}.) In other words, two
	 * {@code ListView}s are defined to be equal if they contain the same
	 * elements in the same order. This definition ensures that the equals
	 * method works properly across different implementations of the
	 * {@code ListView} interface.
	 *
	 * @param o the object to be compared for equality with this
	 * {@code ListView}
	 * @return {@code true} if the specified object is equal to this
	 * {@code ListView}
	 */
	@Override
	boolean equals(Object o);

	/**
	 * Returns the hash code value for this {@code ListView}. The hash code of a
	 * {@code ListView} is defined to be the result of the following
	 * calculation:
	 * <pre>{@code
	 *     int hashCode = 1;
	 *     for (E e : listView)
	 *         hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
	 * }</pre> This ensures that {@code list1.equals(list2)} implies that
	 * {@code list1.hashCode() == list2.hashCode()} for any two lists,
	 * {@code list1} and {@code list2}, as required by the general contract of
	 * {@link Object#hashCode}.
	 *
	 * @return the hash code value for this {@code ListView}
	 * @see Object#equals(Object)
	 * @see #equals(Object)
	 */
	@Override
	int hashCode();

	/**
	 * Returns the element at the specified position in this {@code ListView}.
	 *
	 * @param index index of the element to return
	 * @return the element at the specified position in this {@code ListView}
	 * @throws IndexOutOfBoundsException if the index is out of range ({@code index
	 * < 0 || index >= size()})
	 */
	E get(int index);

	/**
	 * Returns the index of the first occurrence of the specified element in
	 * this {@code ListView}, or {@code -1} if this {@code ListView} does not
	 * contain the element. More formally, returns the lowest index {@code i}
	 * such that {@code (o == null ? get(i) == null : o.equals(get(i)))}, or
	 * {@code -1} if there is no such index.
	 *
	 * @param o element to search for
	 * @return the index of the first occurrence of the specified element in
	 * this {@code ListView}, or {@code -1} if this {@code ListView} does not
	 * contain the element
	 * @throws ClassCastException if the type of the specified element is
	 * incompatible with this {@code ListView} (optional)
	 * @throws NullPointerException if the specified element is {@code null} and
	 * this {@code ListView} does not permit {@code null} elements (optional)
	 */
	int indexOf(Object o);

	/**
	 * Returns the index of the last occurrence of the specified element in this
	 * {@code ListView}, or {@code -1} if this {@code ListView} does not contain
	 * the element. More formally, returns the highest index {@code i} such that
	 * {@code (o == null ? get(i) == null : o.equals(get(i)))}, or {@code -1} if
	 * there is no such index.
	 *
	 * @param o element to search for
	 * @return the index of the last occurrence of the specified element in this
	 * {@code ListView}, or {@code -1} if this {@code ListView} does not contain
	 * the element
	 * @throws ClassCastException if the type of the specified element is
	 * incompatible with this {@code ListView} (optional)
	 * @throws NullPointerException if the specified element is {@code null} and
	 * this {@code ListView} does not permit {@code null} elements (optional)
	 */
	int lastIndexOf(Object o);

	/**
	 * Returns a {@code ListIterator} over the elements in this {@code ListView}
	 * (in proper sequence).
	 *
	 * @apiNote {@code ListIterator}s derived from any implentation of the
	 * {@code ListView} interface are not permitted to support the
	 * {@link Iterator#remove() remove()}, {@link ListIterator#add(Object) add()}
	 * and {@link ListIterator#set(Object) set()} methods.
	 *
	 * @return a {@code ListView} iterator over the elements in this
	 * {@code ListView} (in proper sequence)
	 */
	ListIterator<E> listIterator();

	/**
	 * Returns a {@code ListIterator} over the elements in this {@code ListView}
	 * (in proper sequence), starting at the specified position in the
	 * {@code ListView}. The specified index indicates the first element that
	 * would be returned by an initial call to {@link ListIterator#next next}.
	 * An initial call to {@link ListIterator#previous previous} would return
	 * the element with the specified index minus one.
	 *
	 * @apiNote {@code ListIterator}s derived from any implentation of the
	 * {@code ListView} interface are not permitted to support the
	 * {@link Iterator#remove() remove()}, {@link ListIterator#add(Object) add()}
	 * and {@link ListIterator#set(Object) set()} methods.
	 *
	 * @param index index of the first element to be returned from the
	 * {@code ListView} iterator (by a call to {@link ListIterator#next next})
	 * @return a {@code ListView} iterator over the elements in this
	 * {@code ListView} (in proper sequence), starting at the specified position
	 * in the {@code ListView}
	 * @throws IndexOutOfBoundsException if the index is out of range
	 * ({@code index < 0 || index > size()})
	 */
	ListIterator<E> listIterator(int index);

	/**
	 * Returns a view of the portion of this {@code ListView} between the
	 * specified {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
	 * (If {@code fromIndex} and {@code toIndex} are equal, the returned
	 * {@code ListView} is empty.) The returned {@code ListView} is backed by
	 * this {@code ListView}, so non-structural changes in the returned
	 * {@code ListView} are reflected in this {@code ListView}, and vice-versa.
	 * The returned {@code ListView} supports all of the optional
	 * {@code ListView} operations supported by this {@code ListView}
	 * .<p>
	 *
	 * This method eliminates the need for explicit range operations (of the
	 * sort that commonly exist for arrays). Any operation that expects a
	 * {@code ListView} can be used as a range operation by passing a subList
	 * view instead of a whole {@code ListView}. For example, the following
	 * idiom iterates only over a range of elements from a {@code ListView}:
	 * <pre>{@code
	 *      for (Object obj : list.subList(from, to))
	 * }</pre> Similar idioms may be constructed for {@code indexOf} and
	 * {@code lastIndexOf}, and all of the algorithms in the {@code Views} class
	 * can be applied to a subList.<p>
	 *
	 * The semantics of the {@code ListView} returned by this method become
	 * undefined if the backing {@code ListView} (i.e., this {@code ListView})
	 * is <i>structurally modified</i> in any way other than via the returned
	 * {@code ListView}. (Structural modifications are those that change the
	 * size of this {@code ListView}, or otherwise perturb it in such a fashion
	 * that iterations in progress may yield incorrect results.) Such
	 * modifications may occur if the data-structre backing this {@code View} is
	 * modified from outside the {@code Views}-api. Whether these changes are
	 * reflected depends on the backing data-structure.
	 *
	 * @param fromIndex low endpoint (inclusive) of the subList
	 * @param toIndex high endpoint (exclusive) of the subList
	 * @return a view of the specified range within this {@code ListView}
	 * @throws IndexOutOfBoundsException for an illegal endpoint index value
	 * ({@code fromIndex < 0 || toIndex > size || fromIndex > toIndex})
	 */
	ListView<E> subList(int fromIndex, int toIndex);

	/**
	 * Creates a {@link Spliterator} over the elements in this {@code ListView}.
	 *
	 * <p>
	 * The {@code Spliterator} reports {@link Spliterator#SIZED} and
	 * {@link Spliterator#ORDERED}. Implementations should document the
	 * reporting of additional characteristic values.
	 *
	 * @implSpec The default implementation creates a
	 * <em>late-binding</em> spliterator from the {@code ListView}'s
	 * {@code Iterator}. The spliterator inherits the
	 * <em>fail-fast</em> properties of the {@code ListView}'s iterator.
	 *
	 * @implNote The created {@code Spliterator} additionally reports
	 * {@link Spliterator#SUBSIZED}.
	 *
	 * @return a {@code Spliterator} over the elements in this {@code ListView}
	 */
	@Override
	default Spliterator<E> spliterator() {
		return Spliterators.spliterator(iterator(), size(), Spliterator.ORDERED);
	}

	/**
	 * Returns an unmodifiable {@code List} containing the same elements as this
	 * {@code ListView}. If this {@code ListView} or its backing {@code List}
	 * implements {@code RandomAccess}, the returned {@code List} will do so as
	 * well.
	 *
	 * @return an unmodifiable {@code List} containing the same elements as this
	 * {@code ListView}
	 *
	 * @see Views#asView(List)
	 */
	@Override
	public List<E> asCollection();
}
