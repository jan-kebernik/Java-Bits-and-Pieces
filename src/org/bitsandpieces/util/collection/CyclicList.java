/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Very fast random access List.
 * 
 * @author Jan Kebernik
 * @param <E>
 */
public class CyclicList<E>
		extends AbstractCyclicList<Object[]>
		implements List<E>, Cloneable, RandomAccess, java.io.Serializable {

	private static final long serialVersionUID = 8541895687880205550L;

	// TODO Make a note about a CyclicList that has thrown a CME is no longer safe to use.
	// TODO verify that all struct mods always fail during iter and always update state.
	private static final int CLEAR_LEN = 64;
	private static final Object[] CLEAR = new Object[CLEAR_LEN];

	private static final Object[] EMPTY = {};

	private static final Transfer<Object[], Collection<?>, long[]> CONTAINS = (Object[] a, int aOff, Collection<?> b, int bOff, int len, long[] attachment) -> {
		for (int m = aOff + len; aOff < m; aOff++, bOff++) {
			if (b.contains(a[aOff])) {
				attachment[bOff >>> 6] |= 1L << bOff;
			}
		}
	};

	private static final Transfer<Object[], Collection<?>, long[]> NOT_CONTAINS = (Object[] a, int aOff, Collection<?> b, int bOff, int len, long[] attachment) -> {
		for (int m = aOff + len; aOff < m; aOff++, bOff++) {
			if (!b.contains(a[aOff])) {
				attachment[bOff >>> 6] |= 1L << bOff;
			}
		}
	};

	public CyclicList() {
		super(0, 0, 0, EMPTY);
	}

	public CyclicList(int initialCapacity) {
		super(0, 0, 0, new Object[initialCapacity]);
	}

	private void writeObject(java.io.ObjectOutputStream str) throws java.io.IOException {
		long exp = this._state;
		int s = this._size;
		Object[] a = this._array;
		str.defaultWriteObject();	// effects should be irrelevant to custom format
		str.writeInt(s);	// write element count
		// write stored elements in proper order
		_readChecked(a, a.length, s, this._idx, this._len, 0, str, 0, s,
				(Object[] a1, int aOff, java.io.ObjectOutputStream b, int bOff, int len, Object attachment) -> {
					for (int m = aOff + len; aOff < m; aOff++) {
						b.writeObject(a1[aOff]);
					}
				}, null, _CME());	// throws CME on failure due to corruption
		_checkState(exp);			// throws CME if structural change detected
	}

	private void readObject(java.io.ObjectInputStream str) throws java.io.IOException, ClassNotFoundException {
		this._array = EMPTY;		// in case of failure
		str.defaultReadObject();	// effects should be irrelevant to custom format
		int s = str.readInt();		// read element count
		// read serialized array contents into the new array in proper order
		Object[] a = new Object[s];
		for (int i = 0; i < s; i++) {
			a[i] = str.readObject();
		}
		this._array = a;			// only not EMPTY if no errors occur
		this._size = s;				// only not zero if no errors occur
		this._idx = 0;				// imitate constructor
		this._len = 0;				// imitate constructor
		this._state = 0L;			// imitate constructor
		this._iteration = 0L;		// imitate constructor
	}

	@Override
	final int _arrayLength(Object[] array) {
		return array.length;
	}

	@Override
	protected final Object[] _newArray(int len) {
		return new Object[len];
	}

	private Object getElement(int index) {
		Object[] a = this._array;
		return a[_physicalIndex(a.length, this._size, this._idx, this._len, index)];
	}

	private void _remove(int s, int off, int len) {
		Object[] a = this._array;
		_remove(a, a.length, s, this._idx, this._len, off, len, CLEAR, CLEAR_LEN);
		this._state += len;
	}

	private Object[] _toArray(int s, int off, int len) {
		try {
			Object[] a = this._array;
			Object[] b = new Object[len];
			_read(a, a.length, s, this._idx, this._len, off, b, 0, len);
			return b;
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	private <T> T[] _toArray(int s, int off, int len, T[] b) {
		try {
			Objects.requireNonNull(b);
			Object[] a = this._array;
			if (b.length < len) {
				b = (Object) b.getClass() == (Object) Object[].class
						? (T[]) new Object[len]
						: (T[]) Array.newInstance(b.getClass().getComponentType(), len);
			}
			_read(a, a.length, s, this._idx, this._len, off, b, 0, len);
			return b;
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	private E _get(int s, int off, int len, int index) {
		try {
			_checkIndex(len, index);
			Object[] a = this._array;
			return (E) a[_physicalIndex(a.length, s, this._idx, this._len, off + index)];
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	private E _set(int s, int off, int len, int index, E element) {
		try {
			_checkIndex(len, index);
			Object[] a = this._array;
			int x = _physicalIndex(a.length, s, this._idx, this._len, off + index);
			E old = (E) a[x];
			a[x] = element;
			return old;
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	private int _indexOf(Object[] a, int s, int i, int n, int off, int len, Object o) {
		if (o == null) {
			try {
				return _indexOf(a, a.length, s, i, n, off, len, false,
						(Object[] array, int i1, int i2, Object attachment, boolean attachment2) -> {
							for (int m = i1 + i2; i1 < m; i1++) {
								if (null == array[i1]) {
									return i1;
								}
							}
							return -1;
						}, null, false);
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}
		this._iteration++;	// prevent Object#equals() side-effects
		try {
			return _indexOf(a, a.length, s, i, n, off, len, false,
					(Object[] array, int i1, int i2, Object attachment, boolean attachment2) -> {
						for (int m = i1 + i2; i1 < m; i1++) {
							if (attachment.equals(array[i1])) {
								return i1;
							}
						}
						return -1;
					}, o, false, _CME());
		} finally {
			this._iteration--;
		}
	}

	private int _lastIndexOf(Object[] a, int s, int i, int n, int off, int len, Object o) {
		if (o == null) {
			try {
				return _lastIndexOf(a, a.length, s, i, n, off, len, false,
						(Object[] array, int i1, int i2, Object attachment, boolean attachment2) -> {
							for (int m = i1 + i2 - 1, x = m; x >= i1; x--) {
								if (null == array[x]) {
									return x;
								}
							}
							return -1;
						}, null, false);
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}
		this._iteration++;	// prevent Object#equals() side-effects
		try {
			return _lastIndexOf(a, a.length, s, i, n, off, len, false,
					(Object[] array, int i1, int i2, Object attachment, boolean attachment2) -> {
						for (int m = i1 + i2 - 1, x = m; x >= i1; x--) {
							if (attachment.equals(array[x])) {
								return x;
							}
						}
						return -1;
					}, o, false, _CME());
		} finally {
			this._iteration--;
		}
	}

	private void _add(int s, int index, E e) {
		int n = this._len;
		Object[] old = this._array;
		Object[] a = _ensureInternal(old, old.length, s, this._idx, n, s + 1);	// may modify this._idx
		a[_insertIndex(a, a.length, s, this._idx, n, index, CLEAR, CLEAR_LEN)] = e;
		this._state++;
	}

	private E _remove(int s, int off, int len, int index) {
		_checkIndex(len, index);
		int i = this._idx;
		int n = this._len;
		Object[] a = this._array;
		int x = off + index;
		E old = (E) a[_physicalIndex(a.length, s, i, n, x)];
		_remove(a, a.length, s, i, n, x, 1, CLEAR, CLEAR_LEN);
		this._state++;
		return old;
	}

	private boolean _remove(int s, int off, int len, Object o) {
		int i = this._idx;
		int n = this._len;
		Object[] a = this._array;
		int x = _indexOf(a, s, i, n, off, len, o);
		if (x < 0) {
			return false;
		}
		_remove(a, a.length, s, i, n, off + x, 1, CLEAR, CLEAR_LEN);
		this._state++;
		return true;
	}

	private void _forEach(int s, int off, int len, Consumer<? super E> action) {
		_forEach(s, this._idx, this._len, off, len, action);
	}

	private void _forEach(int s, int i, int n, int off, int len, Consumer<? super E> action) {
		Object[] a = this._array;
		_read(a, a.length, s, i, n, off, action, 0, len,
				(Object[] src, int srcOff, Consumer<? super E> dest, int destOff, int len1, Object attachment) -> {
					for (int m = srcOff + len1; srcOff < m; srcOff++) {
						dest.accept((E) src[srcOff]);
					}
				}, null, _CME());
	}

	private boolean _containsAll(int s, int off, int len, final Collection<?> c) {
		// neither are empty
		this._iteration++;	// prevent Collection#contains() interference
		try {
			Object[] a = this._array;
			return -1 != _indexOf(a, a.length, s, this._idx, this._len, off, len, true,
					(Object[] array, int i1, int i2, Collection<?> attachment, boolean attachment2) -> {
						for (int m = i1 + i2; i1 < m; i1++) {
							if (!attachment.contains(array[i1])) {
								return i1;
							}
						}
						return -1;
					}, c, false, _CME());
			// ConcurrentModificationException will be thrown if the internal logic
			// fails. Any erros in #contains() will simply bubble up the stack.
		} finally {
			this._iteration--;
		}
	}

	private void _addCyclicList(int s, int index, CyclicList<?> cyc, int s2, int offset, int length) {
		int n = this._len;
		Object[] old = this._array;
		Object[] a = _ensureInternal(old, old.length, s, this._idx, n, s + length);	// may modify this._idx
		Object[] b = cyc._array;
		_insert(a, a.length, s, this._idx, n, index, cyc, b, b.length, s2, offset, length, CLEAR, CLEAR_LEN);
		this._state += length;
		// read as: insert at "index" the cyclic array from "offset" for "length"
	}

	private int _addAll(int s, int index, final Collection<? extends E> c) {
		// some optimizations for known implementations
		// note that we cannot simply ask for "instanceof CyclicList", because
		// even it is is a sub class, because its logical ordering 
		// characteristics might be different.
		int cs = c.size();	// throws if invalid sublist
		Class<?> clazz = c.getClass();
		if (CyclicList.class == clazz) {
			// wether this list is the other list makes no difference.
			try {
				_addCyclicList(s, index, (CyclicList<?>) c, cs, 0, cs);
				return cs;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}
		if (SubList.class == clazz) {
			// whether this list backs the sub-list makes no difference.
			try {
				SubList sub = (SubList) c;
				CyclicList<?> cyc = sub.owner();
				_addCyclicList(s, index, cyc, cyc._size, sub.offset, cs);
				// Note that the sublist may immediately become outdated if 
				// it was backed by this list. Which is the expected behaviour, 
				// because its backing list was modified outside of its scope.
				return cs;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}
		// general iterator solution (also works for collections backed by this list)
		this._iteration++;	// prevent Iterator#hasNext() and Iterator#next() interference
		try {
			Iterator<? extends E> it = c.iterator();
			// Note that there is a possible concurrency issue here.
			// The iterator() and size() calls may happen during an 
			// update to c. However, there is no practical way to even 
			// verify such a case before the Iterator is consumed.
			int n = this._len;
			Object[] old = this._array;
			Object[] a = _ensureInternal(old, old.length, s, this._idx, n, s + cs);	// may modify this._idx
			_insert(a, a.length, s, this._idx, n, index, it, 0, cs,
					(Iterator<? extends E> src, int srcOff, Object[] dest, int destOff, int len, Object attachment) -> {
						for (int m = destOff + len; destOff < m; destOff++) {
							dest[destOff] = src.next();
						}
					}, null, _CME(), CLEAR, CLEAR_LEN);
			this._state += cs;
			if (it.hasNext()) {
				// apparently the calls to iterator() and size() happened 
				// for different states of c.
				throw new ConcurrentModificationException();
			}
			return cs;
		} finally {
			this._iteration--;
		}
	}

	private void _replaceAll(int s, int off, int len, UnaryOperator<E> operator) {
		this._iteration++;	// prevent Consumer#accept() interference
		try {
			Object[] a = this._array;
			_read(a, a.length, s, this._idx, this._len, off, operator, 0, len,
					(Object[] a1, int aOff, UnaryOperator<E> b, int bOff, int len1, Object attachment) -> {
						for (int m = aOff + len1; aOff < m; aOff++) {
							a1[aOff] = (b.apply((E) a1[aOff]));
						}
					}, null, _CME());
		} finally {
			this._iteration--;
		}
	}

	private boolean _cyclicListEquals(int s1, CyclicList<?> g, int s2, int off1, int off2, int len) {
		Object[] a = this._array;	// dest
		Object[] b = g._array;		// src
		return _intersect(
				a, a.length, s1, this._idx, this._len, off1,
				b, b.length, s2, g._idx, g._len, off2,
				len,
				(Object[] a1, int aOff, Object[] b1, int bOff, int len1, Object attachment) -> {
					for (int m = aOff + len1; aOff < m; aOff++, bOff++) {
						if (!Objects.equals(a1[aOff], b1[bOff])) {
							// num elements tranferred so far
							return aOff - m + len1;
						}
					}
					return len1;
				}, null, _CME()) == len;	// all elements matched
	}

	private int _hashCode(int s, int off, int len) {
		this._iteration++;	// prevent Object#hashCode() interference.
		try {
			Object[] a = this._array;
			return _accumulate(a, a.length, s, this._idx, this._len, off, len, 0,
					(Object[] array, int off1, int len1, int currentValue) -> {
						for (int m = off1 + len1; off1 < m; off1++) {
							currentValue = currentValue * 31 + (Objects.hashCode(array[off1]));
						}
						return currentValue;
					}, _CME());
		} finally {
			this._iteration--;
		}
	}

	@Override
	@SuppressWarnings("CloneDeclaresCloneNotSupported")
	public CyclicList<E> clone() {
		try {
			return (CyclicList<E>) super.clone();
		} catch (CloneNotSupportedException ex) {
			// cannot happen.
			throw new InternalError(ex);
		}
	}

	@Override
	public void clear() {
		try {
			_failDuringIteration();
			int s = this._size;
			if (s != 0) {
				_remove(s, 0, s);
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	@Override
	public Object[] toArray() {
		int s = this._size;
		return _toArray(s, 0, s);
	}

	@Override
	public <T> T[] toArray(T[] a) {
		int s = this._size;
		return _toArray(s, 0, s, a);
	}

	@Override
	public E get(int index) {
		int s = this._size;
		return _get(s, 0, s, index);
	}

	@Override
	public E set(int index, E element) {
		int s = this._size;
		return _set(s, 0, s, index, element);
	}

	@Override
	public int indexOf(Object o) {
		int s = this._size;
		return _indexOf(this._array, s, this._idx, this._len, 0, s, o);
	}

	@Override
	public int lastIndexOf(Object o) {
		int s = this._size;
		return _lastIndexOf(this._array, s, this._idx, this._len, 0, s, o);
	}

	@Override
	public boolean contains(Object o) {
		int s = this._size;
		return -1 != _indexOf(this._array, s, this._idx, this._len, 0, s, o);
	}

	@Override
	public boolean add(E e) {
		try {
			_failDuringIteration();
			int s = this._size;
			_add(s, s, e);
			return true;
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	@Override
	public void add(int index, E element) {
		try {
			_failDuringIteration();
			int s = this._size;
			_checkIndexInclusive(s, index);
			_add(s, index, element);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	@Override
	public E remove(int index) {
		try {
			_failDuringIteration();
			int s = this._size;
			return _remove(s, 0, s, index);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	@Override
	public boolean remove(Object o) {
		try {
			_failDuringIteration();
			int s = this._size;
			return _remove(s, 0, s, o);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		Objects.requireNonNull(action);
		this._iteration++;	// prevent Consumer#accept() interference
		try {
			int s = this._size;
			_forEach(s, 0, s, action);
		} finally {
			this._iteration--;
		}
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		// throws if invalid sublist
		if (c.isEmpty() || c == this) {
			// every collection always contains at least nothing.
			return true;
		}
		int s = this._size;
		if (s == 0) {
			// the range contains nothing
			return false;
		}
		if (SubList.class == c.getClass()) {
			if (this == ((SubList) c).owner()) {
				// every backing list always contains all of its sub-sets
				return true;
			}
		}
		return _containsAll(s, 0, s, c);
	}

	@Override
	public boolean addAll(final Collection<? extends E> c) {
		_failDuringIteration();
		Objects.requireNonNull(c);
		if (c.isEmpty()) {
			return false;
		}
		int s = this._size;
		_addAll(s, s, c);
		return true;
	}

	@Override
	public boolean addAll(int index, final Collection<? extends E> c) {
		_failDuringIteration();
		Objects.requireNonNull(c);
		int s = this._size;
		_checkIndexInclusive(s, index);
		if (c.isEmpty()) {
			return false;
		}
		_addAll(s, index, c);
		return true;
	}

	// works even if c is backed by this list
	@Override
	public boolean removeAll(final Collection<?> c) {
		_failDuringIteration();
		Objects.requireNonNull(c);
		int s = this._size;
		if (s == 0) {
			// nothing can change.
			return false;
		}
		if (this == c) {
			// remove everything
			_remove(s, 0, s);
			return true;
		}
		// throws if invalid sublist
		if (c.isEmpty()) {
			// there is nothing to remove
			return false;
		}
		//Class<?> clazz = c.getClass();
		//if (SubList.class == clazz) {
		//	return _removeAllSubList(s, c, false, CONTAINS, REMOVE) != 0;
		//}
		return _removeAll(s, 0, s, c, false, CONTAINS) != 0;
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		_failDuringIteration();
		Objects.requireNonNull(c);
		int s = this._size;
		if (this == c || s == 0) {
			// nothing can change.
			return false;
		}
		if (c.isEmpty()) {
			// remove everything.
			_remove(s, 0, s);
			return true;
		}

		//Class<?> clazz = c.getClass();
		//if (SubList.class == clazz) {
		//	return _removeAllSubList(s, c, true, NOT_CONTAINS, RETAIN) != 0;
		//}
		return _removeAll(s, 0, s, c, true, NOT_CONTAINS) != 0;
	}

	/*
	private int _removeAllSubList(int s, Collection<?> c, boolean retain, Transfer<Object[], Collection<?>, long[]> contains, Evaluator<Object[], Collection<?>> rem) {
		try {
			int i = this._idx;
			int n = this._len;
			SubList sub = (SubList) c;
			if (this == sub.owner()) {
				// because the view may lose elements during bulk removal, 
				// contains() would no longer work as expected. 
				// therefore all items that have to be removed
				// must be collected beforehand.
				// we omit all elements inside the view from the contains()
				// logic.
				int len = sub.length;
				int left = sub.offset;
				int rightOff = left + len;
				int right = s - rightOff;
				long[] leftBits = new long[(left + 63) >>> 6];
				long[] rightBits = new long[(right + 63) >>> 6];
				Object[] a = this._array;
				_read(a, a.length, s, i, n, 0, c, 0, left, contains, leftBits);
				_read(a, a.length, s, i, n, rightOff, c, 0, right, contains, rightBits);
				this._state += _remove(a, a.length, s, i, n, rightOff, rightBits, CLEAR, CLEAR_LEN);
				if (!retain) {
					_remove(a, a.length, this._size, this._idx, this._len, left, len, CLEAR, CLEAR_LEN);
					this._state += len;
				}
				int r = _remove(a, a.length, this._size, this._idx, this._len, 0, leftBits, CLEAR, CLEAR_LEN);
				this._state += r;
				return r;
			}
			// can be removed using batchRemove
			//return _remove(s, i, n, 0, s, c, retain);
			return _batchRemove(s, i, n, 0, s, c, retain, rem);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}*/
	private int _removeAll(int s, int off, int len, Collection<?> c, boolean retain, Transfer<Object[], Collection<?>, long[]> contains) {
		// general solution (works even if c is backed by this list)
		this._iteration++;	// prevent Collection#contains() interference
		try {
			int i = this._idx;
			int n = this._len;
			long[] bits = new long[(len + 63) >>> 6];
			Object[] a = this._array;
			_read(a, a.length, s, i, n, off, c, 0, len, contains, bits, _CME());
			int r = _remove(a, a.length, s, i, n, off, bits, CLEAR, CLEAR_LEN);
			this._state += r;
			return r;
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		} finally {
			this._iteration--;
		}
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		_failDuringIteration();
		Objects.requireNonNull(filter);
		int s = this._size;
		if (s == 0) {
			// nothing can be removed.
			return false;
		}
		return _removeIf(s, 0, s, filter) != 0;
	}

	private int _removeIf(int s, int off, int len, Predicate<? super E> filter) {
		this._iteration++;	// prevent Object#equals() interference
		try {
			return _batchRemove(s, this._idx, this._len, off, len, filter, false,
					(Object[] array, int i1, int i2, Predicate<? super E> c, boolean comp) -> {
						for (int m = i1 + i2; i1 < m; i1++) {
							if (c.test((E) array[i1]) == comp) {
								return i1;
							}
						}
						return -1;
					});
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		} finally {
			this._iteration--;
		}
	}

	private <T> int _batchRemove(int s, int i, int n, int off, int len, T attachment, boolean comp, Evaluator<Object[], T> ev) throws ArrayIndexOutOfBoundsException {
		// removes all elements whose evaluation matches the current complement.
		// instead of removing one element at a time, the algorithm counts 
		// elements in sequence (using the more efficient indexOf() 
		// iteration strategy) that can be removed in one bulk operation.
		int old = s;
		Object[] a = this._array;
		// throws ConcurrentModificationException if the internal logic fails.
		// Erros occuring inside the Evaluator will bubble up the stack.
		for (int j; -1 != (j = _indexOf(a, a.length, s, i, n, off, len, false, ev, attachment, comp, _CME()));) {
			if (comp) {
				// keep run
				off += j;
				comp = false;
			} else {
				// remove run
				_remove(a, a.length, s, i, n, off, j, CLEAR, CLEAR_LEN);
				s = this._size;	// update due to modification
				i = this._idx;	// update due to modification
				n = this._len;	// update due to modification
				comp = true;
			}
			len -= j;	// consume run
		}
		// handle remaining run
		if (!comp) {
			_remove(a, a.length, s, i, n, off, len, CLEAR, CLEAR_LEN);
			s -= len;	// update due to modification
		}
		int d = old - s;
		_state += d;
		return d;
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		Objects.requireNonNull(operator);
		int s = this._size;
		_replaceAll(s, 0, s, operator);
	}

	@Override
	public void sort(Comparator<? super E> c) {
		// _failDuringIteration();	// algorithm adapts
		int s = this._size;
		_sort(s, 0, s, c);
	}

	private void _sort(int s, int off, int len, Comparator<? super E> c) {
		long it = this._iteration++;	// prevent Comparator#compare() interference
		try {
			E[] a = (E[]) this._array;
			_sortArray(a, a.length, s, this._idx, this._len,
					off, len, it == 0L, c, Arrays::sort, _CME(),
					CLEAR, CLEAR_LEN);
		} finally {
			this._iteration--;
		}
	}

	@Override
	public String toString() {
		int s = this._size;
		return _toString(s, 0, s, this);
	}

	private String _toString(int s, int off, int len, Object self) {
		if (len == 0) {
			return "[]";
		}
		this._iteration++;	// prevent Object#toString() interference
		try {
			StringBuilder sb = new StringBuilder().append('[');
			Object[] a = this._array;
			_read(a, a.length, s, this._idx, this._len, off, sb, 0, len,
					(Object[] a1, int aOff, StringBuilder b, int bOff, int len1, Object attachment) -> {
						for (int m = aOff + len1; aOff < m; aOff++) {
							Object e = a1[aOff];
							b.append(e == attachment ? "(this Collection)" : e).append(',').append(' ');
						}
					}, self, _CME());
			sb.deleteCharAt(sb.length() - 1).setCharAt(sb.length() - 1, ']');
			return sb.toString();
		} finally {
			this._iteration--;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof List)) {
			return false;
		}
		int s = this._size;
		List<?> list = (List<?>) obj;
		if (s == 0 && list.isEmpty()) {
			return true;
		}
		if (s != list.size()) {
			// throws if invalid sublist
			return false;
		}
		this._iteration++;	// prevent Object#equals() interference
		try {
			Class<?> clazz = obj.getClass();
			if (SubList.class == clazz) {
				SubList sub = (SubList) obj;
				CyclicList<?> cyc = sub.owner();
				if (this == cyc) {
					// the sublist covers all elements (length == size).
					return true;
				}
				return _cyclicListEquals(s, cyc, cyc._size, 0, sub.offset, s);
			}
			if (CyclicList.class == clazz) {
				CyclicList<?> cyc = (CyclicList<?>) obj;
				return _cyclicListEquals(s, cyc, s, 0, 0, s);
			}
			return _equals(s, 0, s, list);
		} finally {
			this._iteration--;
		}
	}

	private boolean _equals(int s, int off, int len, List<?> list) {
		Iterator<?> it = list.iterator();
		Object[] a = this._array;
		int x = _indexOf(a, a.length, s, this._idx, this._len, off, len, true,
				(Object[] array, int off1, int len1, Iterator<?> attachment1, boolean attachment2) -> {
					for (int m = off1 + len1; off1 < m; off1++) {
						if (!Objects.equals(array[off1], attachment1.next())) {
							return off1;
						}
					}
					return -1;
				}, it, false, _CME());
		if (x >= 0) {
			// early abort
			return false;
		}
		// all processed
		if (it.hasNext()) {
			// Iterator and size() out-of-sync
			throw new ConcurrentModificationException();
		}
		return true;
	}

	@Override
	public int hashCode() {
		int s = this._size;
		return _hashCode(s, 0, s);
	}

	@Override
	public Iterator<E> iterator() {
		return new BoundListItr(0, 0, this._size, this._state);
	}

	@Override
	public ListIterator<E> listIterator() {
		return new BoundListItr(0, 0, this._size, this._state);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		int s = this._size;
		_checkIndexInclusive(s, index);
		return new BoundListItr(0, index, s, this._state);
	}

	@Override
	public Spliterator<E> spliterator() {
		return new AbstractSplit() {
			private long state;

			private int bind(int i, Object action) {
				if (i < 0) {
					Objects.requireNonNull(action);	// takes precedence over binding
					this.index = 0;
					this.maxIndex = CyclicList.this._size;
					this.state = CyclicList.this._state;
					return 0;
				}
				_checkIsIterating(this.isIterating);	// takes precedence over null-check
				Objects.requireNonNull(action);
				_checkState(this.state);
				return i;
			}

			@Override
			public final boolean tryAdvance(Consumer<? super E> action) {
				int x = bind(this.index, action);
				int m = this.maxIndex;
				if (x >= m) {
					return false;
				}
				action.accept((E) getElement(x));
				this.index = x + 1;
				return true;
			}

			@Override
			public final void forEachRemaining(Consumer<? super E> action) {
				int x = bind(this.index, action);
				int m = this.maxIndex;
				if (x >= m) {
					return;
				}
				this.isIterating = true;
				CyclicList.this._iteration++;
				try {
					_forEach(CyclicList.this._size, x, m - x, action);
					this.index = m;
					_checkState(this.state);
				} finally {
					this.isIterating = false;
					CyclicList.this._iteration--;
				}
			}

			@Override
			public final Spliterator<E> trySplit() {
				int i = this.index;
				if (i < 0) {
					// not yet bound
					int m = CyclicList.this._size;
					long st = CyclicList.this._state;
					this.index = 0;
					this.state = st;
					int k = m >>> 1;
					if (k == 0) {
						this.maxIndex = m;
						return null;
					}
					int j = m - k;
					this.maxIndex = j;
					return new BoundSplit(CyclicList.this._array, m, CyclicList.this._idx, CyclicList.this._len, j, k, st);
				}
				_checkIsIteratingSplit(this.isIterating);	// cannot iterate if not bound yet
				//checkState(this.state);	// no point
				int k = (this.maxIndex - i) >>> 1;
				return k == 0 ? null : new BoundSplit(CyclicList.this._array,
						CyclicList.this._size, CyclicList.this._idx, CyclicList.this._len,
						this.maxIndex -= k, k, this.state);
			}

			@Override
			public final long estimateSize() {
				int i = this.index;
				if (i < 0) {
					this.index = 0;
					this.state = CyclicList.this._state;
					return (long) (this.maxIndex = CyclicList.this._size);
				}
				return (long) (this.maxIndex - i);
			}
		};
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		int s = this._size;
		return new SubList(fromIndex,
				_checkRange(fromIndex, toIndex, s), this._state);
	}

	private final class BoundListItr implements ListIterator<E> {

		private final int offset;
		int index;
		int ret = -1;
		int maxIndex;
		long state;
		boolean isIterating;

		private BoundListItr(int offset, int index, int length, long state) {
			this.offset = offset;
			this.index = offset + index;
			this.maxIndex = offset + index + length;
			this.state = state;
		}

		@Override
		public final int nextIndex() {
			return index;
		}

		@Override
		public final int previousIndex() {
			return index - 1;
		}

		@Override
		public final boolean hasNext() {
			return index < maxIndex;
		}

		@Override
		public final boolean hasPrevious() {
			return index > offset;
		}

		@Override
		public final E next() {
			try {
				_checkIsIterating(this.isIterating);
				_checkState(this.state);
				int i = this.index;
				if (i >= this.maxIndex) {
					throw new NoSuchElementException();
				}
				E e = (E) getElement(i);
				this.ret = i;
				this.index = i + 1;
				return e;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public final E previous() {
			try {
				_checkIsIterating(this.isIterating);
				_checkState(this.state);
				int i = this.index - 1;
				if (i < this.offset) {
					throw new NoSuchElementException();
				}
				E e = (E) getElement(i);
				this.index = i;
				this.ret = i;
				return e;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public final void set(E e) {
			try {
				int r = this.ret;
				if (r < 0) {
					throw new IllegalStateException();
				}
				_checkState(this.state);
				Object[] a = CyclicList.this._array;
				a[_physicalIndex(a.length, CyclicList.this._size, CyclicList.this._idx, CyclicList.this._len, r)] = e;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public final void add(E e) {
			try {
				_failDuringIteration();
				long exp = this.state;
				_checkState(exp);
				int i = this.index;
				_add(CyclicList.this._size, i, e);
				this.state = exp + 1;
				this.index = i + 1;
				this.ret = -1;
				this.maxIndex++;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public final void remove() {
			try {
				_failDuringIteration();
				int r = ret;
				if (r < 0) {
					throw new IllegalStateException();
				}
				long exp = this.state;
				_checkState(exp);
				_remove(CyclicList.this._size, r, 1);
				this.state = exp + 1;
				this.index = r;
				this.ret = -1;
				this.maxIndex--;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public final void forEachRemaining(Consumer<? super E> action) {
			_checkIsIterating(this.isIterating);
			Objects.requireNonNull(action);
			long exp = this.state;
			_checkState(exp);
			int x = this.index;
			int m = this.maxIndex;
			if (x >= m) {
				return;
			}
			this.isIterating = true;
			CyclicList.this._iteration++;
			try {
				_forEach(CyclicList.this._size, x, m - x, action);
				this.index = m;
				this.ret = m - 1;
				_checkState(exp);
			} finally {
				this.isIterating = false;
				CyclicList.this._iteration--;
			}
		}
	}

	private abstract class AbstractSplit implements Spliterator<E> {

		boolean isIterating;
		int maxIndex;
		int index = -1;

		@Override
		public final int characteristics() {
			return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
		}

		@Override
		public final long getExactSizeIfKnown() {
			return estimateSize();
		}
	}

	private final class BoundSplit extends AbstractSplit implements Spliterator<E> {

		private final Object[] _a;
		private final int _s, _i, _n;	// no need to re-read them at all
		private final long state;

		private BoundSplit(Object[] a, int s, int i, int n, int index, int len, long state) {
			this._a = a;
			this._s = s;
			this._i = i;
			this._n = n;
			this.maxIndex = index + len;
			this.index = index;
			this.state = state;
		}

		@Override
		public final boolean tryAdvance(Consumer<? super E> action) {
			_checkIsIterating(this.isIterating);
			Objects.requireNonNull(action);
			_checkState(this.state);
			int x = this.index;
			int m = this.maxIndex;
			if (x >= m) {
				return false;
			}
			action.accept((E) this._a[_physicalIndex(this._a.length, this._s, this._i, this._n, x)]);
			this.index = x + 1;
			return true;
		}

		@Override
		public final void forEachRemaining(Consumer<? super E> action) {
			_checkIsIterating(this.isIterating);
			Objects.requireNonNull(action);
			long exp = this.state;
			_checkState(exp);
			int x = this.index;
			int m = this.maxIndex;
			if (x >= m) {
				return;
			}
			this.isIterating = true;
			CyclicList.this._iteration++;
			try {
				_forEach(this._s, this._i, this._n, x, m - x, action);
				this.index = m;
				_checkState(exp);
			} finally {
				this.isIterating = false;
				CyclicList.this._iteration--;
			}
		}

		@Override
		public final Spliterator<E> trySplit() {
			_checkIsIteratingSplit(this.isIterating);
			//_checkState(this.state);	// ?
			int m = this.maxIndex;
			int k = (m - this.index) >>> 1;
			return k == 0 ? null : new BoundSplit(this._a, this._s, this._i, this._n, (this.maxIndex = m - k), k, this.state);
		}

		@Override
		public final long estimateSize() {
			return (long) (this.maxIndex - this.index);
		}
	}

	private final class SubList implements List<E>, RandomAccess {

		private final int offset;
		private int length;
		private long state;

		public SubList(int offset, int length, long state) {
			this.offset = offset;
			this.length = length;
			this.state = state;		// should maintain its own state history, so that 
			// any sublist derived from an invalid sublist cannot
			// easily become valid again even if the other parameters are valid
		}

		private CyclicList<E> owner() {
			return CyclicList.this;
		}

		private void checkState() {
			_checkState(this.state);
		}

		@Override
		public boolean isEmpty() {
			checkState();
			return 0 == this.length;
		}

		@Override
		public int size() {
			checkState();
			return this.length;
		}

		@Override
		public void clear() {
			try {
				_failDuringIteration();
				checkState();
				int len = this.length;
				if (len != 0) {
					int s = CyclicList.this._size;
					_remove(s, this.offset, len);
					this.state += len;
					this.length = 0;
				}
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public Object[] toArray() {
			checkState();
			return _toArray(CyclicList.this._size, this.offset, this.length);
		}

		@Override
		public <T> T[] toArray(T[] a) {
			checkState();
			return _toArray(CyclicList.this._size, this.offset, this.length, a);
		}

		@Override
		public E get(int index) {
			checkState();
			return _get(CyclicList.this._size, this.offset, this.length, index);
		}

		@Override
		public E set(int index, E element) {
			checkState();
			return _set(CyclicList.this._size, this.offset, this.length, index, element);
		}

		@Override
		public int indexOf(Object o) {
			checkState();
			return _indexOf(CyclicList.this._array, CyclicList.this._size, CyclicList.this._idx, CyclicList.this._len,
					this.offset, this.length, o);
		}

		@Override
		public int lastIndexOf(Object o) {
			checkState();
			return _lastIndexOf(CyclicList.this._array, CyclicList.this._size, CyclicList.this._idx, CyclicList.this._len,
					this.offset, this.length, o);
		}

		@Override
		public boolean contains(Object o) {
			return indexOf(o) != -1;
		}

		@Override
		public boolean add(E e) {
			try {
				_failDuringIteration();
				long exp = this.state;
				_checkState(exp);
				_add(CyclicList.this._size, this.offset + this.length++, e);
				this.state = exp + 1;
				return true;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public void add(int index, E element) {
			try {
				_failDuringIteration();
				long exp = this.state;
				_checkState(exp);
				int len = this.length;
				_checkIndexInclusive(len, index);
				_add(CyclicList.this._size, this.offset + index, element);
				this.state = exp + 1;
				this.length = len + 1;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public E remove(int index) {
			try {
				_failDuringIteration();
				long exp = this.state;
				_checkState(exp);
				int len = this.length;
				E old = _remove(CyclicList.this._size, this.offset, len, index);
				this.state = exp + 1;
				this.length = len - 1;
				return old;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public boolean remove(Object o) {
			try {
				_failDuringIteration();
				long exp = this.state;
				_checkState(exp);
				int len = this.length;
				if (_remove(CyclicList.this._size, this.offset, len, o)) {
					this.state = exp + 1;
					this.length = len - 1;
					return true;
				}
				return false;
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public void forEach(Consumer<? super E> action) {
			Objects.requireNonNull(action);
			checkState();
			CyclicList.this._iteration++;
			try {
				_forEach(CyclicList.this._size, this.offset, this.length, action);
			} finally {
				CyclicList.this._iteration--;
			}
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			Objects.requireNonNull(c);
			checkState();
			if (c == this || c.isEmpty()) {
				return true;
			}
			int off = this.offset;
			int len = this.length;
			if (len == 0) {
				// can't contain other
				return false;
			}
			if (SubList.class == c.getClass()) {
				SubList sub = (SubList) c;
				if (this.owner() == sub.owner() && off <= sub.offset && len >= sub.length) {
					// this view contains the other view
					return true;
				}
			}
			return _containsAll(CyclicList.this._size, off, len, c);
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			_failDuringIteration();
			Objects.requireNonNull(c);
			long exp = this.state;
			_checkState(exp);
			if (c.isEmpty()) {
				return false;
			}
			int len = this.length;
			int n = _addAll(CyclicList.this._size, this.offset + len, c);
			this.state = exp + n;
			this.length = len + n;
			return true;
		}

		@Override
		public boolean addAll(int index, Collection<? extends E> c) {
			_failDuringIteration();
			Objects.requireNonNull(c);
			long exp = this.state;
			_checkState(exp);
			int len = this.length;
			_checkIndexInclusive(len, index);
			if (c.isEmpty()) {
				return false;
			}
			int n = _addAll(CyclicList.this._size, this.offset + index, c);
			this.state = exp + n;
			this.length = len + n;
			return true;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			_failDuringIteration();
			Objects.requireNonNull(c);
			long exp = this.state;
			_checkState(exp);
			int len = this.length;
			if (len == 0) {
				// nothing can change
				return false;
			}
			if (this == c || this.owner() == c) {
				// remove self
				_remove(CyclicList.this._size, this.offset, len);
				this.state = exp + len;
				this.length = 0;
				return true;
			}
			if (c.isEmpty()) {
				// there is nothing to remove
				return false;
			}
			// further optimizations for other sublist are only marginally effective
			int r = _removeAll(CyclicList.this._size, this.offset, len, c, false, CONTAINS);
			if (r == 0) {
				return false;
			}
			this.state = exp + r;
			this.length = len - r;
			return true;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			_failDuringIteration();
			Objects.requireNonNull(c);
			long exp = this.state;
			_checkState(exp);
			int len = this.length;
			if (this == c || this.owner() == c || len == 0) {
				// nothing can change.
				return false;
			}
			if (c.isEmpty()) {
				// retain nothing
				_remove(CyclicList.this._size, this.offset, len);
				this.state = exp + len;
				this.length = 0;
				return true;
			}
			// further optimizations for other sublist are only marginally effective
			int r = _removeAll(CyclicList.this._size, this.offset, len, c, true, NOT_CONTAINS);
			if (r == 0) {
				return false;
			}
			this.state = exp + r;
			this.length = len - r;
			return true;
		}

		@Override
		public boolean removeIf(Predicate<? super E> filter) {
			_failDuringIteration();
			Objects.requireNonNull(filter);
			long exp = this.state;
			_checkState(exp);
			int len = this.length;
			if (len == 0) {
				// nothing can be removed
				return false;
			}
			int n = _removeIf(CyclicList.this._size, this.offset, len, filter);
			if (n == 0) {
				return false;
			}
			this.state = exp + n;
			this.length = len - n;
			return true;
		}

		@Override
		public void replaceAll(UnaryOperator<E> operator) {
			Objects.requireNonNull(operator);
			_checkState(this.state);
			_replaceAll(CyclicList.this._size, this.offset, this.length, operator);
		}

		@Override
		public void sort(Comparator<? super E> c) {
			_checkState(this.state);
			_sort(CyclicList.this._size, this.offset, this.length, c);
		}

		@Override
		public List<E> subList(int fromIndex, int toIndex) {
			int len = this.length;
			return new SubList(this.offset + fromIndex,
					_checkRange(fromIndex, toIndex, len), this.state);
		}

		@Override
		public Iterator<E> iterator() {
			long exp = this.state;
			_checkState(exp);
			return new BoundListItr(this.offset, 0, this.length, exp);
		}

		@Override
		public ListIterator<E> listIterator() {
			long exp = this.state;
			_checkState(exp);
			return new BoundListItr(this.offset, 0, this.length, exp);
		}

		@Override
		public ListIterator<E> listIterator(int index) {
			long exp = this.state;
			_checkState(exp);
			int len = this.length;
			_checkIndexInclusive(len, index);
			return new BoundListItr(this.offset, index, len, exp);
		}

		@Override
		public Spliterator<E> spliterator() {
			long exp = this.state;
			_checkState(exp);
			return new BoundSplit(CyclicList.this._array,
					CyclicList.this._size, CyclicList.this._idx, CyclicList.this._len,
					this.offset, this.length, exp);
		}

		@Override
		public String toString() {
			_checkState(this.state);
			return _toString(CyclicList.this._size, this.offset, this.length, this);
		}

		@Override
		public boolean equals(Object obj) {
			_checkState(this.state);
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof List)) {
				return false;
			}
			int len = this.length;
			List<?> list = (List<?>) obj;
			if (len == 0 && list.isEmpty()) {
				return true;
			}
			if (len != list.size()) {
				return false;
			}
			CyclicList.this._iteration++;	// prevent Object#equals() interference
			try {
				Class<?> clazz = obj.getClass();
				if (SubList.class == clazz) {
					SubList sub = (SubList) obj;
					CyclicList<?> cyc = sub.owner();
					if (this.owner() == cyc) {
						// true if both lists provide the 
						// same view of the same list
						return sub.offset == this.offset;
					}
					return _cyclicListEquals(CyclicList.this._size, cyc, cyc._size, this.offset, sub.offset, len);
				}
				if (CyclicList.class == clazz) {
					if (this.owner() == obj) {
						// sublist covers all elements
						return true;
					}
					CyclicList<?> cyc = (CyclicList<?>) obj;
					return _cyclicListEquals(CyclicList.this._size, cyc, cyc._size, this.offset, 0, len);
				}
				return _equals(CyclicList.this._size, this.offset, this.length, list);
			} finally {
				CyclicList.this._iteration--;
			}
		}

		@Override
		public int hashCode() {
			_checkState(this.state);
			return _hashCode(CyclicList.this._size, this.offset, this.length);
		}
	}
}
