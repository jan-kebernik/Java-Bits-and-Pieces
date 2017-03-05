/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection;

import java.util.ConcurrentModificationException;
import java.util.function.Supplier;

/**
 *
 * @author Jan Kebernik
 */
abstract class AbstractCyclicList<A> extends AbstractCyclicArray<A> implements Cloneable {

	static final int DEFAULT_CAPACITY = 10;

	// _state should always be modified by the amount of change. That way, 
	// a single insertion will have different signature than a bulk insertion.
	transient long _state;
	transient long _iteration;

	transient A _array;

	// note: the only non-transient field should by default be the size field.
	// the array object itself is not necessary, nor is the physical structure.
	// all that matters is the logical structure, which is simply a list of 
	// objects/privimites with the same length as the size field.
	AbstractCyclicList(int _size, int _idx, int _len, A array) {
		super(_size, _idx, _len);
		this._array = array;
	}

	abstract int _arrayLength(A array);

	@Override
	protected Object clone() throws CloneNotSupportedException {
		try {
			AbstractCyclicList<A> g = (AbstractCyclicList<A>) super.clone();
			g._state = 0L;		// imitate constructor
			g._iteration = 0L;	// imitate constructor
			int s = this._size;
			A a = this._array;
			A b = _newArray(s);
			// copy elements into new, trimmed, array
			_read(a, _arrayLength(a), s, this._idx, this._len, 0, b, 0, s);
			g._array = b;
			g._idx = 0;			// overwrite super.clone()
			g._len = 0;			// overwrite super.clone()
			return g;
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	static final Supplier<ConcurrentModificationException> _CME() {
		return () -> new ConcurrentModificationException();
	}

	// any action that would alter the layout of the underlying array
	// would also disrupt the correct execution of any iteration currently taking place.
	// any method that has the potential to do so must fail during iteration, 
	// even if it would ultimately not have any effects, if only to indicate
	// incorrect usage.
	final void _failDuringIteration() {
		if (0 != this._iteration) {
			throw new IllegalStateException(
					"Structural modification is not permitted during iteration.");
		}
	}

	final void _checkIsIteratingSplit(boolean isIterating) {
		if (isIterating) {
			throw new IllegalStateException("Cannot split during iteration.");
		}
	}

	final void _checkState(long expected) {
		if (this._state != expected) {
			throw new ConcurrentModificationException();
		}
	}

	final A _ensureInternal(A a, int aLen, int s, int i, int n, int minCap) {
		if (minCap < 0) {
			throw new OutOfMemoryError();
		}
		if (minCap <= aLen) {
			return a;
		}
		A b = _newArray(newCap(aLen, minCap));
		this._idx = _growUnfair(a, aLen, b, s, i, n);
		return (this._array = b);
	}

	public void ensureCapacity(int minCapacity) {
		try {
			_failDuringIteration();
			A a = this._array;
			int aLen = _arrayLength(a);
			if (minCapacity > aLen) {
				A b = _newArray(newCap(aLen, minCapacity));
				this._idx = _growUnfair(a, aLen, b, this._size, this._idx, this._len);
				this._array = b;
				this._state += minCapacity - aLen;
			}
			// no action if minCapacity <= 0 
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	private static int newCap(int aLen, int minCap) {
		int newCap = aLen + (aLen >>> 1);
		if (newCap < 0) {
			newCap = minCap > MAX_ARRAY_CAPACITY
					? Integer.MAX_VALUE
					: MAX_ARRAY_CAPACITY;
		}
		if (newCap < minCap) {
			newCap = minCap;
		}
		return newCap;
	}

	public boolean isEmpty() {
		return 0 == this._size;
	}

	public int size() {
		return this._size;
	}

	public void trimToSize() {
		try {
			_failDuringIteration();
			int s = this._size;
			A a = this._array;
			int aLen = _arrayLength(a);
			if (s < aLen) {
				A b = _newArray(s);
				_read(a, aLen, s, this._idx, this._len, 0, b, 0, s);
				this._array = b;
				this._idx = 0;
				this._len = 0;
				this._state += aLen - s;
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	static final void _checkIndex(int s, int index) throws IndexOutOfBoundsException {
		if (index < 0 || index >= s) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + s);
		}
	}

	static final void _checkIndexInclusive(int s, int index) throws IndexOutOfBoundsException {
		if (index < 0 || index > s) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + s);
		}
	}

	static final void _checkIsIterating(boolean isIterating) {
		if (isIterating) {
			throw new IllegalStateException("Nested iteration is unsupported.");
		}
	}

	/*static final boolean _isKnownImpl(java.util.Collection<?> c, Class<?> clazz) {
		return CyclicList.class == clazz
				|| java.util.ArrayList.class == clazz
				|| java.util.LinkedList.class == clazz
				|| java.util.HashSet.class == clazz
				|| java.util.TreeSet.class == clazz;
	}*/

	static final int _checkRange(int fromIndex, int toIndex, int s) {
		int len = toIndex - fromIndex;
		if (fromIndex < 0 || len < 0 || fromIndex > s - len) {
			throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + ", toIndex: " + toIndex);
		}
		return len;
	}

}
