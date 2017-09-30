/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import java.util.zip.Inflater;
import org.bitsandpieces.util.Encoding.Decoder;
import org.bitsandpieces.util.Encoding.Encoder;
import org.bitsandpieces.util.Endian;

/**
 * Provides an efficient buffer strategy for an IOAddress, providing
 * quasi-random access and the ability to read and write any kind of data in any
 * order from an underlying I/O source. Quasi-random, in this context, simply
 * means that performance generally tends to degrades the more random the access
 * requests are. Sequential (in either direction) reading and writing will
 * perform on a level indistinguishable from any buffered I/O solution.
 * Performing short accesses within half of a buffer's specified size (specified
 * at instantiation) will generally performed completely in memory.
 * <p>
 * In addition, this class provides performace-oriented convenience methods that
 * can significantly speed up many common tasks.
 * <p>
 * Changes made to the contents of a buffer may not be reflected in another
 * buffer of the same address and are only guaranteed to be flushed to the
 * underlying source when the buffer is closed. In other words, buffers of the
 * same address are, unless they are ALL read-only, not safe for any type of
 * concurrent use (even single threaded, as they might overwrite each other's
 * contents when flushed). This API and its implementations make no attempt to
 * ensure that such buffers are Read-Only. Correct usage is left entirely to the
 * discretion of the programmer.
 * <p>
 * For the default implementation, any allocated arrays are shared and re-used
 * among buffers in a thread-safe and efficient manner, meaning that allocation
 * costs amortize very quickly. Said arrays are eligible to be garbage collected
 * if unused for longer periods of time, and do not require special clean-up.
 *
 * @author Jan Kebernik
 */
public abstract class IOBuffer implements AutoCloseable {
	// implemented methods mostly deal with bounds/nullity checks and 
	// leave the actual implementation fairly open-ended.

	/**
	 * The default {@code Endian} installed when an {@code IOBuffer} is first
	 * instantiated.
	 */
	public static final Endian DEFAULT_ENDIAN = Endian.BIG;

	long pos;
	long size;
	final IOSource source;
	private Endian endian;
	final AtomicInteger shared;

	IOBuffer(IOSource source) throws IOException {
		this.endian = DEFAULT_ENDIAN;
		this.pos = 0L;
		long s = source.size();
		this.size = s;
		this.source = source;
		this.shared = new AtomicInteger();
	}

	IOBuffer(IOSource source, AtomicInteger shared) throws IOException {
		this.endian = DEFAULT_ENDIAN;
		this.pos = 0L;
		long s = source.size();
		this.size = s;
		this.source = source;
		this.shared = shared;
		while (true) {
			int c = shared.get();
			if (c == -2) {
				throw new IllegalStateException("Too many siblings open.");
			}
			if (c == -1) {
				throw new IllegalStateException("Buffer was closed.");
			}
			if (shared.compareAndSet(c, c + 1)) {
				break;
			}
		}
	}

	// must always be called inside close() when impl class closes itself.
	protected final void closeSource() throws IOException {
		while (true) {
			int c = this.shared.get();
			if (this.shared.compareAndSet(c, c - 1)) {
				// won thread-race
				if (c == 0) {
					// no siblings remain
					this.source.close();
				}
				break;
			}
		}
	}

	/**
	 * Returns the underlying {@code IOAddress}.
	 *
	 * @return the underlying {@code IOAddress}.
	 */
	public final IOAddress address() {
		return this.source.address();
	}

	/**
	 * Creates a new {@code IOBuffer} with the exact same I/O source backing it
	 * as this {@code IOBuffer}.
	 * <p>
	 * This is generally only useful for creating multiple read-only buffers for
	 * the same address, as no two buffers are guaranteed to share synchronized
	 * contents. The new sibling will momentarily be synhronized to this
	 * IOBuffer (by flushin this buffer), but not to any others. Modifying the
	 * contents of two or more siblings leads to undefined behaviour, even on a
	 * single thread.
	 * <p>
	 * The I/O source backing all shared siblings will only be closed when all
	 * openend siblings are closed.
	 *
	 * @return a new {@code IOBuffer} with the exact same I/O source backing it
	 * as this {@code IOBuffer}.
	 */
	// package-private because too esoteric for general purposes.
	abstract IOBuffer createSibling() throws IOException;

	/**
	 * Returns the length in {@code byte}s of the buffered source.
	 * <p>
	 * Note that the value returned by this method is not strictly synchronized
	 * with the underlying source and may return differing values. However, it
	 * may be called even after the {@code IOBuffer} has been closed.
	 *
	 * @return the length in {@code byte}s of the buffered source.
	 */
	public final long size() {
		return this.size;
	}

	/**
	 * Will truncate the underlying source to the new size, if the specified
	 * size is less than the current size.
	 * <p>
	 * Note that like every other write operation, the effects may only take
	 * place once the {@code IOBuffer} is closed.
	 *
	 * @param size the new size.
	 * @return this {@code IOBuffer}.
	 */
	public final IOBuffer truncate(long size) throws IOException {
		_ensureOpen();
		if (size < 0L) {
			throw new IllegalArgumentException("size < 0: " + size);
		}
		return doTruncate(size);
	}

	final IOBuffer doTruncate(long size) throws IOException {
		_truncate(size);
		return this;
	}

	/**
	 * Returns the current position along the underlying source.
	 * <p>
	 * Note that this method will function even if the {@code IOBuffer} has
	 * already been closed.
	 *
	 * @return the current position along the underlying source.
	 */
	public final long pos() {
		return this.pos;
	}

	/**
	 * Sets the current position along the underlying source to the specified
	 * value.
	 * <p>
	 * The specified position is allowed to be larger than the
	 * {@code IOBuffer}'s current {@link #size() size()}. Any write operation to
	 * such a position will grow the underlying source to the sum of the
	 * position and the length of the write operation. Any resulting gap between
	 * the current size and the resulting size is filled with {@code 0}-value
	 * {@code byte}s.
	 * <p>
	 * Note that this method will function (but have no effect) even if the
	 * {@code IOBuffer} has already been closed.
	 *
	 * @param pos the new position.
	 * @return this {@code IOBuffer}.
	 * @throws IllegalArgumentException if {@code pos} is negative.
	 */
	public final IOBuffer pos(long pos) {
		if (pos < 0L) {
			throw new IllegalArgumentException("pos < 0: " + pos);
		}
		this.pos = pos;
		return this;
	}

	/**
	 * Returns this {@code IOBuffer}'s installed {@code Endian}.
	 *
	 * @return this {@code IOBuffer}'s installed {@code Endian}.
	 */
	public final Endian endian() {
		return this.endian;
	}

	/**
	 * Sets this {@code IOBuffer}'s installed {@code Endian} to the specified
	 * {@code Endian}.
	 *
	 * @param endian the new installed {@code Endian}.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final IOBuffer endian(Endian endian) {
		if (endian == null) {
			throw new NullPointerException();
		}
		this.endian = endian;
		return this;
	}

	///////////////////////////////////////////////////////////////
	// I/O METHODS
	/////////////////////////////////////////////////////////////// 
	/**
	 * Reads and returns a single {@code byte} at the current position. The
	 * buffer's position is advanced by one {@code byte}.
	 *
	 * @return the {@code byte} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	public final byte readByte() throws IOException {
		_ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 1L) {
			throw new IndexOutOfBoundsException("pos() + 1 > size(). pos(): " + p + ", size: " + s);
		}
		long pos_len = p + 1L;
		byte n = _readByte(p, pos_len, s);
		this.pos = pos_len;
		return n;
	}

	final byte doReadByte() {
		long p = this.pos;
		long pos_len = p + 1L;
		byte n = _readByte(p, pos_len, this.size);
		this.pos = pos_len;
		return n;
	}

	/**
	 * Reads and returns a single {@code byte} at the specified position. The
	 * buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @return the {@code byte} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 */
	public final byte readByte(long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 1L) {
			throw new IndexOutOfBoundsException("pos + 1 > size(). pos: " + pos + ", size: " + s);
		}
		return _readByte(pos, pos + 1L, s);
	}

	final byte doReadByte(long pos) throws IOException {
		return _readByte(pos, pos + 1L, this.size);
	}

	/**
	 * Reads and returns a single {@code char} at the current position, using
	 * the default {@code Endian}. The buffer's position is advanced by two
	 * {@code byte}s.
	 *
	 * @return the {@code char} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	public final char readChar() throws IOException {
		_ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 2L) {
			throw new IndexOutOfBoundsException("pos() + 2 > size(). pos(): " + p + ", size: " + s);
		}
		long pos_len = p + 2L;
		char n = _readChar(p, pos_len, s, this.endian);
		this.pos = pos_len;
		return n;
	}

	final char doReadChar() throws IOException {
		return doReadChar(this.endian);
	}

	/**
	 * Reads and returns a single {@code char} at the current position, using
	 * the specified {@code Endian}. The buffer's position is advanced by two
	 * {@code byte}s.
	 *
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code char}.
	 * @return the {@code char} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final char readChar(Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 2L) {
			throw new IndexOutOfBoundsException("pos() + 2 > size(). pos(): " + p + ", size: " + s);
		}
		long pos_len = p + 2L;
		char n = _readChar(p, pos_len, s, endian);
		this.pos = pos_len;
		return n;
	}

	final char doReadChar(Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 2L;
		char n = _readChar(p, pos_len, this.size, endian);
		this.pos = pos_len;
		return n;
	}

	/**
	 * Reads and returns a single {@code char} at the specified position, using
	 * the default {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @return the {@code char} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 */
	public final char readChar(long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 2L) {
			throw new IndexOutOfBoundsException("pos + 2 > size(). pos: " + pos + ", size: " + s);
		}
		return _readChar(pos, pos + 2L, s, this.endian);
	}

	final char doReadChar(long pos) throws IOException {
		return doReadChar(pos, this.endian);
	}

	/**
	 * Reads and returns a single {@code char} at the specified position, using
	 * the specified {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code char}.
	 * @return the {@code char} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final char readChar(long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 2L) {
			throw new IndexOutOfBoundsException("pos + 2 > size(). pos: " + pos + ", size: " + s);
		}
		return _readChar(pos, pos + 2L, s, endian);
	}

	final char doReadChar(long pos, Endian endian) throws IOException {
		return _readChar(pos, pos + 2L, this.size, endian);
	}

	/**
	 * Reads and returns a single {@code short} at the current position, using
	 * the default {@code Endian}. The buffer's position is advanced by two
	 * {@code byte}s.
	 *
	 * @return the {@code short} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	public final short readShort() throws IOException {
		_ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 2L) {
			throw new IndexOutOfBoundsException("pos() + 2 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 2L;
		short n = _readShort(p, pos_len, s, this.endian);
		this.pos = pos_len;
		return n;
	}

	final short doReadShort() throws IOException {
		return doReadShort(this.endian);
	}

	/**
	 * Reads and returns a single {@code short} at the current position, using
	 * the specified {@code Endian}. The buffer's position is advanced by two
	 * {@code byte}s.
	 *
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code short}.
	 * @return the {@code short} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final short readShort(Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 2L) {
			throw new IndexOutOfBoundsException("pos() + 2 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 2L;
		short n = _readShort(p, pos_len, s, endian);
		this.pos = pos_len;
		return n;
	}

	final short doReadShort(Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 2L;
		short n = _readShort(p, pos_len, this.size, endian);
		this.pos = pos_len;
		return n;
	}

	/**
	 * Reads and returns a single {@code short} at the specified position, using
	 * the default {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @return the {@code short} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 */
	public final short readShort(long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 2L) {
			throw new IndexOutOfBoundsException("pos + 2 > size(). pos: " + pos + ", size: " + s);
		}
		return _readShort(pos, pos + 2L, s, this.endian);
	}

	final short doReadShort(long pos) throws IOException {
		return doReadShort(pos, this.endian);
	}

	/**
	 * Reads and returns a single {@code short} at the specified position, using
	 * the specified {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code short}.
	 * @return the {@code short} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final short readShort(long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 2L) {
			throw new IndexOutOfBoundsException("pos + 2 > size(). pos: " + pos + ", size: " + s);
		}
		return _readShort(pos, pos + 2L, s, endian);
	}

	final short doReadShort(long pos, Endian endian) throws IOException {
		return _readShort(pos, pos + 2L, this.size, endian);
	}

	/**
	 * Reads and returns a single {@code int} at the current position, using the
	 * default {@code Endian}. The buffer's position is advanced by four
	 * {@code byte}s.
	 *
	 * @return the {@code int} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	public final int readInt() throws IOException {
		_ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 4L) {
			throw new IndexOutOfBoundsException("pos() + 4 > size(). pos(): " + p + ", size: " + s);
		}
		long pos_len = p + 4L;
		int n = _readInt(p, pos_len, s, this.endian);
		this.pos = pos_len;
		return n;
	}

	final int doReadInt() throws IOException {
		return doReadInt(this.endian);
	}

	/**
	 * Reads and returns a single {@code int} at the current position, using the
	 * specified {@code Endian}. The buffer's position is advanced by four
	 * {@code byte}s.
	 *
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code int}.
	 * @return the {@code int} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final int readInt(Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 4L) {
			throw new IndexOutOfBoundsException("pos() + 4 > size(). pos(): " + p + ", size: " + s);
		}
		long pos_len = p + 4L;
		int n = _readInt(p, pos_len, s, endian);
		this.pos = pos_len;
		return n;
	}

	final int doReadInt(Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 4L;
		int n = _readInt(p, pos_len, this.size, endian);
		this.pos = pos_len;
		return n;
	}

	/**
	 * Reads and returns a single {@code int} at the specified position, using
	 * the default {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @return the {@code int} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 */
	public final int readInt(long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 4L) {
			throw new IndexOutOfBoundsException("pos + 4 > size(). pos: " + pos + ", size: " + s);
		}
		return _readInt(pos, pos + 4L, s, this.endian);
	}

	final int doReadInt(long pos) throws IOException {
		return doReadInt(pos, this.endian);
	}

	/**
	 * Reads and returns a single {@code int} at the specified position, using
	 * the specified {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code int}.
	 * @return the {@code int} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final int readInt(long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 4L) {
			throw new IndexOutOfBoundsException("pos + 4 > size(). pos: " + pos + ", size: " + s);
		}
		return _readInt(pos, pos + 4L, s, endian);
	}

	final int doReadInt(long pos, Endian endian) throws IOException {
		return _readInt(pos, pos + 4L, this.size, endian);
	}

	/**
	 * Reads and returns a single {@code float} at the current position, using
	 * the default {@code Endian}. The buffer's position is advanced by four
	 * {@code byte}s.
	 *
	 * @return the {@code float} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	public final float readFloat() throws IOException {
		_ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 4L) {
			throw new IndexOutOfBoundsException("pos() + 4 > size(). pos(): " + p + ", size: " + s);
		}
		long pos_len = p + 4L;
		float n = _readFloat(p, pos_len, s, this.endian);
		this.pos = pos_len;
		return n;
	}

	final float doReadFloat() throws IOException {
		return doReadFloat(this.endian);
	}

	/**
	 * Reads and returns a single {@code float} at the current position, using
	 * the specified {@code Endian}. The buffer's position is advanced by four
	 * {@code byte}s.
	 *
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code float}.
	 * @return the {@code float} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final float readFloat(Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 4L) {
			throw new IndexOutOfBoundsException("pos() + 4 > size(). pos(): " + p + ", size: " + s);
		}
		long pos_len = p + 4L;
		float n = _readFloat(p, pos_len, s, endian);
		this.pos = pos_len;
		return n;
	}

	final float doReadFloat(Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 4L;
		float n = _readFloat(p, pos_len, this.size, endian);
		this.pos = pos_len;
		return n;
	}

	/**
	 * Reads and returns a single {@code float} at the specified position, using
	 * the default {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @return the {@code float} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 */
	public final float readFloat(long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 4L) {
			throw new IndexOutOfBoundsException("pos + 4 > size(). pos: " + pos + ", size: " + s);
		}
		return _readFloat(pos, pos + 4L, s, this.endian);
	}

	final float doReadFloat(long pos) throws IOException {
		return doReadFloat(pos, this.endian);
	}

	/**
	 * Reads and returns a single {@code float} at the specified position, using
	 * the specified {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code float}.
	 * @return the {@code float} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final float readFloat(long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 4L) {
			throw new IndexOutOfBoundsException("pos + 4 > size(). pos: " + pos + ", size: " + s);
		}
		return _readFloat(pos, pos + 4L, s, endian);
	}

	final float doReadFloat(long pos, Endian endian) throws IOException {
		return _readFloat(pos, pos + 4L, this.size, endian);
	}

	/**
	 * Reads and returns a single {@code long} at the current position, using
	 * the default {@code Endian}. The buffer's position is advanced by eight
	 * {@code byte}s.
	 *
	 * @return the {@code long} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	public final long readLong() throws IOException {
		_ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 8L) {
			throw new IndexOutOfBoundsException("pos() + 8 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 8L;
		long n = _readLong(p, pos_len, s, this.endian);
		this.pos = pos_len;
		return n;
	}

	final long doReadLong() throws IOException {
		return doReadLong(this.endian);
	}

	/**
	 * Reads and returns a single {@code long} at the current position, using
	 * the specified {@code Endian}. The buffer's position is advanced by eight
	 * {@code byte}s.
	 *
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code long}.
	 * @return the {@code long} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final long readLong(Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 8L) {
			throw new IndexOutOfBoundsException("pos() + 8 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 8L;
		long n = _readLong(p, pos_len, s, endian);
		this.pos = pos_len;
		return n;
	}

	final long doReadLong(Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 8L;
		long n = _readLong(p, pos_len, this.size, endian);
		this.pos = pos_len;
		return n;
	}

	/**
	 * Reads and returns a single {@code long} at the specified position, using
	 * the default {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @return the {@code long} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 */
	public final long readLong(long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 8L) {
			throw new IndexOutOfBoundsException("pos + 8 > size(). pos: " + pos + ", size: " + s);
		}
		return _readLong(pos, pos + 8L, s, this.endian);
	}

	final long doReadLong(long pos) throws IOException {
		return doReadLong(pos, this.endian);
	}

	/**
	 * Reads and returns a single {@code long} at the specified position, using
	 * the specified {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code long}.
	 * @return the {@code long} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final long readLong(long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 8L) {
			throw new IndexOutOfBoundsException("pos + 8 > size(). pos: " + pos + ", size: " + s);
		}
		return _readLong(pos, pos + 8L, s, endian);
	}

	final long doReadLong(long pos, Endian endian) throws IOException {
		return _readLong(pos, pos + 8L, this.size, endian);
	}

	/**
	 * Reads and returns a single {@code double} at the current position, using
	 * the default {@code Endian}. The buffer's position is advanced by eight
	 * {@code byte}s.
	 *
	 * @return the {@code double} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	public final double readDouble() throws IOException {
		_ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 8L) {
			throw new IndexOutOfBoundsException("pos() + 8 > size(). pos(): " + p + ", size: " + s);
		}
		long pos_len = p + 8L;
		double n = _readDouble(p, pos_len, s, this.endian);
		this.pos = pos_len;
		return n;
	}

	final double doReadDouble() throws IOException {
		return doReadDouble(this.endian);
	}

	/**
	 * Reads and returns a single {@code double} at the current position, using
	 * the specified {@code Endian}. The buffer's position is advanced by eight
	 * {@code byte}s.
	 *
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code double}.
	 * @return the {@code double} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final double readDouble(Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 8L) {
			throw new IndexOutOfBoundsException("pos() + 8 > size(). pos(): " + p + ", size: " + s);
		}
		long pos_len = p + 8L;
		double n = _readDouble(p, pos_len, s, endian);
		this.pos = pos_len;
		return n;
	}

	final double doReadDouble(Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 8L;
		double n = _readDouble(p, pos_len, this.size, endian);
		this.pos = pos_len;
		return n;
	}

	/**
	 * Reads and returns a single {@code double} at the specified position,
	 * using the default {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @return the {@code double} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 */
	public final double readDouble(long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 8L) {
			throw new IndexOutOfBoundsException("pos + 8 > size(). pos: " + pos + ", size: " + s);
		}
		return _readDouble(pos, pos + 8L, s, this.endian);
	}

	final double doReadDouble(long pos) throws IOException {
		return doReadDouble(pos, this.endian);
	}

	/**
	 * Reads and returns a single {@code double} at the specified position,
	 * using the specified {@code Endian}. The buffer's position is unchanged.
	 *
	 * @param pos the position from which to read.
	 * @param endian the {@code Endian} used to construct the returned
	 * {@code double}.
	 * @return the {@code double} read at the specified position.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if there is not enough input available at the specified position to
	 * complete the operation.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final double readDouble(long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 8L) {
			throw new IndexOutOfBoundsException("pos + 8 > size(). pos: " + pos + ", size: " + s);
		}
		return _readDouble(pos, pos + 8L, s, endian);
	}

	final double doReadDouble(long pos, Endian endian) throws IOException {
		return _readDouble(pos, pos + 8L, this.size, endian);
	}

	/**
	 * Reads {@code byte}s from the underlying source into the specified array,
	 * starting at the current position. The {@code IOBuffer}'s position is
	 * advanced by the actual number of bytes read.
	 *
	 * @param buf the array into which to read.
	 * @return the actual number of {@code byte}s read or {@code -1} if the end
	 * of the buffer was reached.
	 */
	public final int read(byte[] buf) throws IOException {
		_ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		return doRead(buf);
	}

	final int doRead(byte[] buf) throws IOException {
		return doRead(buf, 0, buf.length);
	}

	/**
	 * Reads {@code byte}s from the underlying source into the specified array,
	 * starting at the current position. The {@code IOBuffer}'s position is
	 * advanced by the actual number of bytes read.
	 *
	 * @param buf the array into which to read.
	 * @param off the offset into the array.
	 * @param len the number of {@code byte}s to read.
	 * @return the actual number of {@code byte}s read or {@code -1} if the end
	 * of the buffer was reached.
	 * @throws IndexOutOfBoundsException if the specified array range is
	 * illegal.
	 */
	public final int read(byte[] buf, int off, int len) throws IOException {
		_ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return doRead(buf, off, len);
	}

	final int doRead(byte[] buf, int off, int len) throws IOException {
		if (len == 0) {
			return 0;
		}
		long p = this.pos;
		long s = this.size;
		if (p >= s) {
			return -1;
		}
		int n = (int) Math.min(s - p, len);
		long pos_len = p + n;
		_read(p, pos_len, s, buf, off, n);
		this.pos = pos_len;
		return n;
	}

	/**
	 * Reads {@code byte}s from the underlying source into the specified array.
	 * The {@code IOBuffer}'s position is unchanged.
	 *
	 * @param pos the position from which to start reading.
	 * @param buf the array into which to read.
	 * @return the actual number of {@code byte}s read or {@code -1} if the end
	 * of the buffer was reached.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 */
	public final int read(long pos, byte[] buf) throws IOException {
		_ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		return doRead(pos, buf);
	}

	final int doRead(long pos, byte[] buf) throws IOException {
		return doRead(pos, buf, 0, buf.length);
	}

	/**
	 * Reads {@code byte}s from the underlying source into the specified array.
	 * The {@code IOBuffer}'s position is unchanged.
	 *
	 * @param pos the position from which to start reading.
	 * @param buf the array into which to read.
	 * @param off the offset into the array.
	 * @param len the number of {@code byte}s to read.
	 * @return the actual number of {@code byte}s read or {@code -1} if the end
	 * of the buffer was reached.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if the specified array range is illegal.
	 */
	public final int read(long pos, byte[] buf, int off, int len) throws IOException {
		_ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return doRead(pos, buf, off, len);
	}

	final int doRead(long pos, byte[] buf, int off, int len) throws IOException {
		if (len == 0) {
			return 0;
		}
		long s = this.size;
		if (pos >= s) {
			return -1;
		}
		int n = (int) Math.min(s - pos, len);
		_read(pos, pos + n, s, buf, off, n);
		return n;
	}

	/**
	 * Writes the specified {@code byte} to the underlying source at the current
	 * position. The {@code IOBuffer}'s position is advanced by one
	 * {@code byte}. If the current position is larger than the current size,
	 * the source will be grown and the resulting gap will be filled with
	 * {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code byte} to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeByte(byte n) throws IOException {
		_ensureOpen();
		long p = this.pos;
		long pos_len = p + 1L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 1 > Long.MAX_VALUE: " + p);
		}
		_writeByte(n, p, pos_len);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteByte(byte n) throws IOException {
		long p = this.pos;
		long pos_len = p + 1L;
		_writeByte(n, p, pos_len);
		this.pos = pos_len;
		return this;
	}

	/**
	 * Writes the specified {@code byte} to the underlying source at the
	 * specified position. The {@code IOBuffer}'s position is unchanged. If the
	 * specified position is larger than the current size, the source will be
	 * grown and the resulting gap will be filled with {@code 0}-value
	 * {@code byte}s.
	 *
	 * @param n the {@code byte} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 */
	public final IOBuffer writeByte(byte n, long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 1L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 1 > Long.MAX_VALUE: " + pos);
		}
		_writeByte(n, pos, pos_len);
		return this;
	}

	final IOBuffer doWriteByte(byte n, long pos) throws IOException {
		_writeByte(n, pos, pos + 1L);
		return this;
	}

	/**
	 * Writes the specified {@code char} to the underlying source at the current
	 * position, using the default {@code Endian}. The {@code IOBuffer}'s
	 * position is advanced by two {@code byte}s. If the current position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code char} to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeChar(char n) throws IOException {
		_ensureOpen();
		long p = this.pos;
		long pos_len = p + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 2 > Long.MAX_VALUE: " + p);
		}
		_writeChar(n, p, pos_len, this.endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteChar(char n) throws IOException {
		return doWriteChar(n, this.endian);
	}

	/**
	 * Writes the specified {@code char} to the underlying source at the current
	 * position, using the specified {@code Endian}. The {@code IOBuffer}'s
	 * position is advanced by two {@code byte}s. If the current position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code char} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeChar(char n, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 2 > Long.MAX_VALUE: " + p);
		}
		_writeChar(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteChar(char n, Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 2L;
		_writeChar(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	/**
	 * Writes the specified {@code char} to the underlying source at the
	 * specified position, using the default {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code char} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 */
	public final IOBuffer writeChar(char n, long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 2 > Long.MAX_VALUE: " + pos);
		}
		_writeChar(n, pos, pos_len, this.endian);
		return this;
	}

	final IOBuffer doWriteChar(char n, long pos) throws IOException {
		return doWriteChar(n, pos, this.endian);
	}

	/**
	 * Writes the specified {@code char} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code char} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final IOBuffer writeChar(char n, long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 2 > Long.MAX_VALUE: " + pos);
		}
		_writeChar(n, pos, pos_len, endian);
		return this;
	}

	final IOBuffer doWriteChar(char n, long pos, Endian endian) throws IOException {
		_writeChar(n, pos, pos + 2L, endian);
		return this;
	}

	/**
	 * Writes the specified {@code short} to the underlying source at the
	 * current position, using the default {@code Endian}. The
	 * {@code IOBuffer}'s position is advanced by two {@code byte}s. If the
	 * current position is larger than the current size, the source will be
	 * grown and the resulting gap will be filled with {@code 0}-value
	 * {@code byte}s.
	 *
	 * @param n the {@code short} to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeShort(short n) throws IOException {
		_ensureOpen();
		long p = this.pos;
		long pos_len = p + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 2 > Long.MAX_VALUE: " + p);
		}
		_writeShort(n, p, pos_len, this.endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteShort(short n) throws IOException {
		return doWriteShort(n, this.endian);
	}

	/**
	 * Writes the specified {@code short} to the underlying source at the
	 * current position, using the specified {@code Endian}. The
	 * {@code IOBuffer}'s position is advanced by two {@code byte}s. If the
	 * current position is larger than the current size, the source will be
	 * grown and the resulting gap will be filled with {@code 0}-value
	 * {@code byte}s.
	 *
	 * @param n the {@code short} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeShort(short n, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 2 > Long.MAX_VALUE: " + p);
		}
		_writeShort(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteShort(short n, Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 2L;
		_writeShort(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	/**
	 * Writes the specified {@code short} to the underlying source at the
	 * specified position, using the default {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code short} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 */
	public final IOBuffer writeShort(short n, long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 2 > Long.MAX_VALUE: " + pos);
		}
		_writeShort(n, pos, pos_len, this.endian);
		return this;
	}

	final IOBuffer doWriteShort(short n, long pos) throws IOException {
		return doWriteShort(n, pos, this.endian);
	}

	/**
	 * Writes the specified {@code short} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code short} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final IOBuffer writeShort(short n, long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 2 > Long.MAX_VALUE: " + pos);
		}
		_writeShort(n, pos, pos_len, endian);
		return this;
	}

	final IOBuffer doWriteShort(short n, long pos, Endian endian) throws IOException {
		_writeShort(n, pos, pos + 2L, endian);
		return this;
	}

	/**
	 * Writes the specified {@code int} to the underlying source at the current
	 * position, using the default {@code Endian}. The {@code IOBuffer}'s
	 * position is advanced by four {@code byte}s. If the current position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code int} to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeInt(int n) throws IOException {
		_ensureOpen();
		long p = this.pos;
		long pos_len = p + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 4 > Long.MAX_VALUE: " + p);
		}
		_writeInt(n, p, pos_len, this.endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteInt(int n) throws IOException {
		return doWriteInt(n, this.endian);
	}

	/**
	 * Writes the specified {@code int} to the underlying source at the current
	 * position, using the specified {@code Endian}. The {@code IOBuffer}'s
	 * position is advanced by four {@code byte}s. If the current position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code int} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeInt(int n, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 4 > Long.MAX_VALUE: " + p);
		}
		_writeInt(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteInt(int n, Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 4L;
		_writeInt(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	/**
	 * Writes the specified {@code int} to the underlying source at the
	 * specified position, using the default {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code int} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 */
	public final IOBuffer writeInt(int n, long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 4 > Long.MAX_VALUE: " + pos);
		}
		_writeInt(n, pos, pos_len, this.endian);
		return this;
	}

	final IOBuffer doWriteInt(int n, long pos) throws IOException {
		return doWriteInt(n, pos, this.endian);
	}

	/**
	 * Writes the specified {@code int} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code int} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final IOBuffer writeInt(int n, long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 4 > Long.MAX_VALUE: " + pos);
		}
		_writeInt(n, pos, pos_len, endian);
		return this;
	}

	final IOBuffer doWriteInt(int n, long pos, Endian endian) throws IOException {
		_writeInt(n, pos, pos + 4L, endian);
		return this;
	}

	/**
	 * Writes the specified {@code float} to the underlying source at the
	 * current position, using the default {@code Endian}. The
	 * {@code IOBuffer}'s position is advanced by two {@code byte}s. If the
	 * current position is larger than the current size, the source will be
	 * grown and the resulting gap will be filled with {@code 0}-value
	 * {@code byte}s.
	 *
	 * @param n the {@code float} to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeFloat(float n) throws IOException {
		_ensureOpen();
		long p = this.pos;
		long pos_len = p + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 4 > Long.MAX_VALUE: " + p);
		}
		_writeFloat(n, p, pos_len, this.endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteFloat(float n) throws IOException {
		return doWriteFloat(n, this.endian);
	}

	/**
	 * Writes the specified {@code float} to the underlying source at the
	 * current position, using the specified {@code Endian}. The
	 * {@code IOBuffer}'s position is advanced by two {@code byte}s. If the
	 * current position is larger than the current size, the source will be
	 * grown and the resulting gap will be filled with {@code 0}-value
	 * {@code byte}s.
	 *
	 * @param n the {@code float} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeFloat(float n, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 4 > Long.MAX_VALUE: " + p);
		}
		_writeFloat(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteFloat(float n, Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 4L;
		_writeFloat(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	/**
	 * Writes the specified {@code float} to the underlying source at the
	 * specified position, using the default {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code float} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.,
	 * or if no further bytes can be written.
	 */
	public final IOBuffer writeFloat(float n, long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 4 > Long.MAX_VALUE: " + pos);
		}
		_writeFloat(n, pos, pos_len, this.endian);
		return this;
	}

	final IOBuffer doWriteFloat(float n, long pos) throws IOException {
		return doWriteFloat(n, pos, this.endian);
	}

	/**
	 * Writes the specified {@code float} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code float} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final IOBuffer writeFloat(float n, long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 4 > Long.MAX_VALUE: " + pos);
		}
		_writeFloat(n, pos, pos_len, endian);
		return this;
	}

	final IOBuffer doWriteFloat(float n, long pos, Endian endian) throws IOException {
		_writeFloat(n, pos, pos + 4L, endian);
		return this;
	}

	/**
	 * Writes the specified {@code long} to the underlying source at the current
	 * position, using the default {@code Endian}. The {@code IOBuffer}'s
	 * position is advanced by eight {@code byte}s. If the current position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code long} to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeLong(long n) throws IOException {
		_ensureOpen();
		long p = this.pos;
		long pos_len = p + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 8 > Long.MAX_VALUE: " + p);
		}
		_writeLong(n, p, pos_len, this.endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteLong(long n) throws IOException {
		return doWriteLong(n, this.endian);
	}

	/**
	 * Writes the specified {@code long} to the underlying source at the current
	 * position, using the specified {@code Endian}. The {@code IOBuffer}'s
	 * position is advanced by eight {@code byte}s. If the current position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code long} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeLong(long n, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 8 > Long.MAX_VALUE: " + p);
		}
		_writeLong(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteLong(long n, Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 8L;
		_writeLong(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	/**
	 * Writes the specified {@code long} to the underlying source at the
	 * specified position, using the default {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code long} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 */
	public final IOBuffer writeLong(long n, long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 8 > Long.MAX_VALUE: " + pos);
		}
		_writeLong(n, pos, pos_len, this.endian);
		return this;
	}

	final IOBuffer doWriteLong(long n, long pos) throws IOException {
		return doWriteLong(n, pos, this.endian);
	}

	/**
	 * Writes the specified {@code long} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code long} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final IOBuffer writeLong(long n, long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 8 > Long.MAX_VALUE: " + pos);
		}
		_writeLong(n, pos, pos_len, endian);
		return this;
	}

	final IOBuffer doWriteLong(long n, long pos, Endian endian) throws IOException {
		_writeLong(n, pos, pos + 8L, endian);
		return this;
	}

	/**
	 * Writes the specified {@code double} to the underlying source at the
	 * current position, using the default {@code Endian}. The
	 * {@code IOBuffer}'s position is advanced by eight {@code byte}s. If the
	 * current position is larger than the current size, the source will be
	 * grown and the resulting gap will be filled with {@code 0}-value
	 * {@code byte}s.
	 *
	 * @param n the {@code double} to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeDouble(double n) throws IOException {
		_ensureOpen();
		long p = this.pos;
		long pos_len = p + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 8 > Long.MAX_VALUE: " + p);
		}
		_writeDouble(n, p, pos_len, this.endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteDouble(double n) throws IOException {
		return doWriteDouble(n, this.endian);
	}

	/**
	 * Writes the specified {@code double} to the underlying source at the
	 * current position, using the specified {@code Endian}. The
	 * {@code IOBuffer}'s position is advanced by eight {@code byte}s. If the
	 * current position is larger than the current size, the source will be
	 * grown and the resulting gap will be filled with {@code 0}-value
	 * {@code byte}s.
	 *
	 * @param n the {@code double} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 * @throws IndexOutOfBoundsException if no further bytes can be written.
	 */
	public final IOBuffer writeDouble(double n, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + 8 > Long.MAX_VALUE: " + p);
		}
		_writeDouble(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWriteDouble(double n, Endian endian) throws IOException {
		long p = this.pos;
		long pos_len = p + 8L;
		_writeDouble(n, p, pos_len, endian);
		this.pos = pos_len;
		return this;
	}

	/**
	 * Writes the specified {@code double} to the underlying source at the
	 * specified position, using the default {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code double} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 */
	public final IOBuffer writeDouble(double n, long pos) throws IOException {
		_ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 8 > Long.MAX_VALUE: " + pos);
		}
		_writeDouble(n, pos, pos_len, this.endian);
		return this;
	}

	final IOBuffer doWriteDouble(double n, long pos) throws IOException {
		return doWriteDouble(n, pos, this.endian);
	}

	/**
	 * Writes the specified {@code double} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The
	 * {@code IOBuffer}'s position is unchanged. If the specified position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code double} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * or if no further bytes can be written.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	public final IOBuffer writeDouble(double n, long pos, Endian endian) throws IOException {
		_ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 8 > Long.MAX_VALUE: " + pos);
		}
		_writeDouble(n, pos, pos_len, endian);
		return this;
	}

	final IOBuffer doWriteDouble(double n, long pos, Endian endian) throws IOException {
		_writeDouble(n, pos, pos + 8L, endian);
		return this;
	}

	/**
	 * Writes the specified {@code byte}s to the underlying source at the
	 * current position. The {@code IOBuffer}'s position is advanced by the
	 * number {@code byte}s written.
	 *
	 * @param buf the array whose {@code byte}s to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the position overflows.
	 */
	public final IOBuffer write(byte[] buf) throws IOException {
		_ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (buf.length == 0) {
			return this;
		}
		long p = this.pos;
		long pos_len = p + buf.length;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + buf.length > Long.MAX_VALUE. pos() = " + p + ", buf.length = " + buf.length);
		}
		_write(p, pos_len, buf, 0, buf.length);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWrite(byte[] buf) throws IOException {
		return doWrite(buf, 0, buf.length);
	}

	/**
	 * Writes the specified {@code byte}s to the underlying source at the
	 * current position. The {@code IOBuffer}'s position is advanced by the
	 * number {@code byte}s written.
	 *
	 * @param buf the array whose {@code byte}s to write.
	 * @param off the offset into the array.
	 * @param len the number of {@code byte}s to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified array range is illegal
	 * or if the position overflows.
	 */
	public final IOBuffer write(byte[] buf, int off, int len) throws IOException {
		_ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (len == 0) {
			return this;
		}
		long p = this.pos;
		long pos_len = p + len;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos() + len > Long.MAX_VALUE. pos() = " + p + ", len = " + len);
		}
		_write(p, pos_len, buf, off, len);
		this.pos = pos_len;
		return this;
	}

	final IOBuffer doWrite(byte[] buf, int off, int len) throws IOException {
		if (len == 0) {
			return this;
		}
		long p = this.pos;
		long pos_len = p + len;
		_write(p, pos_len, buf, off, len);
		this.pos = pos_len;
		return this;
	}

	/**
	 * Writes the specified {@code byte}s to the underlying source at the
	 * specified position. The {@code IOBuffer}'s position is unchanged.
	 *
	 * @param pos the position to which to write.
	 * @param buf the array whose {@code byte}s to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative
	 * or if the position overflows.
	 */
	public final IOBuffer write(long pos, byte[] buf) throws IOException {
		_ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		if (buf.length == 0) {
			return this;
		}
		long pos_len = pos + buf.length;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + buf.length > Long.MAX_VALUE. pos = " + pos + ", buf.length = " + buf.length);
		}
		_write(pos, pos_len, buf, 0, buf.length);
		return this;
	}

	final IOBuffer doWrite(long pos, byte[] buf) throws IOException {
		return doWrite(pos, buf, 0, buf.length);
	}

	/**
	 * Writes the specified {@code byte}s to the underlying source at the
	 * specified position. The {@code IOBuffer}'s position is unchanged.
	 *
	 * @param pos the position to which to write.
	 * @param buf the array whose {@code byte}s to write.
	 * @param off the offset into the array.
	 * @param len the number of {@code byte}s to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative,
	 * if the specified array range is illegal or if the position overflows.
	 */
	public final IOBuffer write(long pos, byte[] buf, int off, int len) throws IOException {
		_ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		if (len == 0) {
			return this;
		}
		long pos_len = pos + len;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + len > Long.MAX_VALUE. pos = " + pos + ", len = " + len);
		}
		_write(pos, pos_len, buf, off, len);
		return this;
	}

	final IOBuffer doWrite(long pos, byte[] buf, int off, int len) throws IOException {
		if (len == 0) {
			return this;
		}
		_write(pos, pos + len, buf, off, len);
		return this;
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified
	 * {@code Appendable}, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the specified number of input {@code byte}s
	 * has been consumed, when the specified number of {@code char}s has been
	 * decoded, when the specified number of code points has been resolved or
	 * when a decoding error occurs. The method may terminate early and must be
	 * called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param inputBytes the maximum number of input bytes to decode. Must be
	 * non-negative.
	 * @param dest the {@code Appendable} to which to decode.
	 * @param maxChars the maximum number of {@code char}s to append.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, int inputBytes, Appendable dest, int maxChars, int maxCodePoints) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException("inputBytes < 0: " + inputBytes);
		}
		if (maxChars < 0) {
			throw new IllegalArgumentException("maxChars < 0: " + maxChars);
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException("maxCodePoints < 0: " + maxCodePoints);
		}
		return doDecode(dec, inputBytes, dest, maxChars, maxCodePoints);
	}

	final int doDecode(Decoder dec, int inputBytes, Appendable dest, int maxChars, int maxCodePoints) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, maxChars, maxCodePoints, p, p + Math.min(inputBytes, s - p), s);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified
	 * {@code Appendable}, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the specified number of input {@code byte}s
	 * has been consumed or when a decoding error occurs. The method may
	 * terminate early and must be called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param inputBytes the desired number of input bytes to decode. Must be
	 * non-negative.
	 * @param dest the {@code Appendable} to which to decode.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, int inputBytes, Appendable dest) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException("inputBytes < 0: " + inputBytes);
		}
		return doDecode(dec, inputBytes, dest);
	}

	final int doDecode(Decoder dec, int inputBytes, Appendable dest) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, p, p + Math.min(inputBytes, s - p), s);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified
	 * {@code Appendable}, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the buffer is out of input or when a decoding
	 * error occurs. The method may terminate early and must be called in a
	 * loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param dest the {@code Appendable} to which to decode.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, Appendable dest) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		return doDecode(dec, dest);
	}

	final int doDecode(Decoder dec, Appendable dest) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, p, s, s);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified
	 * {@code Appendable}, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the specified number of input {@code byte}s
	 * has been consumed, when the specified number of {@code char}s has been
	 * decoded, when the specified number of code points has been resolved, when
	 * a decoding error occurs or when the specified {@code IntPredicate}
	 * returns {@code true} for the last resolved code point. The method may
	 * terminate early and must be called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param inputBytes the maximum number of input bytes to decode. Must be
	 * non-negative.
	 * @param dest the {@code Appendable} to which to decode.
	 * @param maxChars the maximum number of {@code char}s to append.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @param stop all resolved code points are passed to this stop condition.
	 * Decoding stops when it returns {@code true}.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>Integer.MIN_VALUE: Indicates that encoding stopped because the stop
	 * condition was triggered for the last resolved code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, int inputBytes, Appendable dest, int maxChars, int maxCodePoints, IntPredicate stop) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (stop == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException("inputBytes < 0: " + inputBytes);
		}
		if (maxChars < 0) {
			throw new IllegalArgumentException("maxChars < 0: " + maxChars);
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException("maxCodePoints < 0: " + maxCodePoints);
		}
		return doDecode(dec, inputBytes, dest, maxChars, maxCodePoints, stop);
	}

	final int doDecode(Decoder dec, int inputBytes, Appendable dest, int maxChars, int maxCodePoints, IntPredicate stop) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, maxChars, maxCodePoints, p, p + Math.min(inputBytes, s - p), s, stop);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified
	 * {@code Appendable}, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the specified number of input {@code byte}s
	 * has been consumed, when a decoding error occurs or when the specified
	 * {@code IntPredicate} returns {@code true} for the last resolved code
	 * point. The method may terminate early and must be called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param inputBytes the desired number of input bytes to decode. Must be
	 * non-negative.
	 * @param dest the {@code Appendable} to which to decode.
	 * @param stop all resolved code points are passed to this stop condition.
	 * Decoding stops when it returns {@code true}.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>Integer.MIN_VALUE: Indicates that encoding stopped because the stop
	 * condition was triggered for the last resolved code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, int inputBytes, Appendable dest, IntPredicate stop) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (stop == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException("inputBytes < 0: " + inputBytes);
		}
		return doDecode(dec, inputBytes, dest, stop);
	}

	final int doDecode(Decoder dec, int inputBytes, Appendable dest, IntPredicate stop) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, p, p + Math.min(inputBytes, s - p), s, stop);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified
	 * {@code Appendable}, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the buffer is out of input, when a decoding
	 * error occurs or when the specified {@code IntPredicate} returns
	 * {@code true} for the last resolved code point. The method may terminate
	 * early and must be called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param dest the {@code Appendable} to which to decode.
	 * @param stop all resolved code points are passed to this stop condition.
	 * Decoding stops when it returns {@code true}.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>Integer.MIN_VALUE: Indicates that encoding stopped because the stop
	 * condition was triggered for the last resolved code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, Appendable dest, IntPredicate stop) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (stop == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		return doDecode(dec, dest, stop);
	}

	final int doDecode(Decoder dec, Appendable dest, IntPredicate stop) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, p, s, s, stop);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified {@code char}
	 * array, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the specified number of input {@code byte}s
	 * has been consumed, when no more output can be produced, when the
	 * specified number of {@code char}s has been decoded, when the specified
	 * number of code points has been resolved or when a decoding error occurs.
	 * The method may terminate early and must be called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param inputBytes the maximum number of input bytes to decode. Must be
	 * non-negative.
	 * @param dest the {@code char} array to which to decode.
	 * @param off the offset into the array.
	 * @param maxChars the maximum number of {@code char}s to append.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, int inputBytes, char[] dest, int off, int maxChars, int maxCodePoints) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException("inputBytes < 0: " + inputBytes);
		}
		if (maxChars < 0) {
			throw new IllegalArgumentException("maxChars < 0: " + maxChars);
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException("maxCodePoints < 0: " + maxCodePoints);
		}
		return doDecode(dec, inputBytes, dest, off, maxChars, maxCodePoints);
	}

	final int doDecode(Decoder dec, int inputBytes, char[] dest, int off, int maxChars, int maxCodePoints) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, off, maxChars, maxCodePoints, p, p + Math.min(inputBytes, s - p), s);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified {@code char}
	 * array, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the specified number of input {@code byte}s
	 * has been consumed, when no more output can be produced or when a decoding
	 * error occurs. The method may terminate early and must be called in a
	 * loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param inputBytes the desired number of input bytes to decode. Must be
	 * non-negative.
	 * @param dest the {@code char} array to which to decode.
	 * @param off the offset into the array.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, int inputBytes, char[] dest, int off) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException("inputBytes < 0: " + inputBytes);
		}
		return doDecode(dec, inputBytes, dest, off);
	}

	final int doDecode(Decoder dec, int inputBytes, char[] dest, int off) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, off, Integer.MAX_VALUE, Integer.MAX_VALUE, p, p + Math.min(inputBytes, s - p), s);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified {@code char}
	 * array, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the buffer is out of input, when no more
	 * output can be produced or when a decoding error occurs. The method may
	 * terminate early and must be called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param dest the {@code char} array to which to decode.
	 * @param off the offset into the array.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, char[] dest, int off) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		return doDecode(dec, dest, off);
	}

	final int doDecode(Decoder dec, char[] dest, int off) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, off, Integer.MAX_VALUE, Integer.MAX_VALUE, p, s, s);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified {@code char}
	 * array, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the specified number of input {@code byte}s
	 * has been consumed, when no more output can be produced, when the
	 * specified number of {@code char}s has been decoded, when the specified
	 * number of code points has been resolved, when a decoding error occurs or
	 * when the specified {@code IntPredicate} returns {@code true} for the last
	 * resolved code point. The method may terminate early and must be called in
	 * a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param inputBytes the maximum number of input bytes to decode. Must be
	 * non-negative.
	 * @param dest the {@code char} array to which to decode.
	 * @param off the offset into the array.
	 * @param maxChars the maximum number of {@code char}s to append.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @param stop all resolved code points are passed to this stop condition.
	 * Decoding stops when it returns {@code true}.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>Integer.MIN_VALUE: Indicates that encoding stopped because the stop
	 * condition was triggered for the last resolved code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, int inputBytes, char[] dest, int off, int maxChars, int maxCodePoints, IntPredicate stop) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (stop == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException("inputBytes < 0: " + inputBytes);
		}
		if (maxChars < 0) {
			throw new IllegalArgumentException("maxChars < 0: " + maxChars);
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException("maxCodePoints < 0: " + maxCodePoints);
		}
		return doDecode(dec, inputBytes, dest, off, maxChars, maxCodePoints, stop);
	}

	final int doDecode(Decoder dec, int inputBytes, char[] dest, int off, int maxChars, int maxCodePoints, IntPredicate stop) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, off, maxChars, maxCodePoints, p, p + Math.min(inputBytes, s - p), s, stop);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified {@code char}
	 * array, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the specified number of input {@code byte}s
	 * has been consumed, when no more output can be produced, when a decoding
	 * error occurs or when the specified {@code IntPredicate} returns
	 * {@code true} for the last resolved code point. The method may terminate
	 * early and must be called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param inputBytes the desired number of input bytes to decode. Must be
	 * non-negative.
	 * @param dest the {@code char} array to which to decode.
	 * @param off the offset into the array.
	 * @param stop all resolved code points are passed to this stop condition.
	 * Decoding stops when it returns {@code true}.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>Integer.MIN_VALUE: Indicates that encoding stopped because the stop
	 * condition was triggered for the last resolved code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, int inputBytes, char[] dest, int off, IntPredicate stop) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (stop == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException("inputBytes < 0: " + inputBytes);
		}
		return doDecode(dec, inputBytes, dest, off, stop);
	}

	final int doDecode(Decoder dec, int inputBytes, char[] dest, int off, IntPredicate stop) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, off, Integer.MAX_VALUE, Integer.MAX_VALUE, p, p + Math.min(inputBytes, s - p), s, stop);
	}

	/**
	 * Decodes {@code byte}s from this buffer to the specified {@code char}
	 * array, using the specified {@code Decoder}.
	 * <p>
	 * Decoding starts at this buffer's current position. The buffer's position
	 * is advanced by the number of input {@code byte}s consumed from the
	 * buffer.
	 * <p>
	 * The {@code Decoder} must not have any input remaining.
	 * <p>
	 * The method terminates when the buffer is out of input, when no more
	 * output can be produced, when a decoding error occurs or when the
	 * specified {@code IntPredicate} returns {@code true} for the last resolved
	 * code point. The method may terminate early and must be called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of {@code 0} or {@code Integer.MIN_VALUE}. To ensure
	 * that no pending errors are swallowed and that all pending input and
	 * output is fully processed, they must always be called in a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes.
	 * @param dest the {@code char} array to which to decode.
	 * @param off the offset into the array.
	 * @param stop all resolved code points are passed to this stop condition.
	 * Decoding stops when it returns {@code true}.
	 * @return <ul><li>&gt= 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Decoder} until more input is provided. If
	 * {@link Decoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>Integer.MIN_VALUE: Indicates that encoding stopped because the stop
	 * condition was triggered for the last resolved code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 */
	public final int decode(Decoder dec, char[] dest, int off, IntPredicate stop) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (stop == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		return doDecode(dec, dest, off, stop);
	}

	final int doDecode(Decoder dec, char[] dest, int off, IntPredicate stop) throws IOException {
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, off, Integer.MAX_VALUE, Integer.MAX_VALUE, p, s, s, stop);
	}

	/**
	 * Encodes {@code char}s from the specified {@code Encoder} to this buffer.
	 * <p>
	 * The method starts at this buffer's current position. The buffer's
	 * position is advanced by the number of {@code byte}s written to it by the
	 * {@code Decoder}. Any pending output held by the {@code Encoder} is
	 * written to the buffer before any further encoding takes place.
	 * <p>
	 * Like all other write operations, if the buffer's current position is
	 * larger than its size, the underlying source will be grown and the
	 * resulting gap filled with {@code 0}-value {@code byte}s.
	 * <p>
	 * The method terminates when the specified number of input {@code char}s
	 * has been consumed, when the specified number of {@code byte}s has been
	 * encoded, when the specified number of code points has been resolved or
	 * when an encoding error occurs. The method may terminate early and must be
	 * called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. To ensures that no pending errors are swallowed
	 * and that all input and output if fully processed, they must be called in
	 * a loop.
	 *
	 * @param enc the {@code Encoder} used to encode bytes to this buffer.
	 * @param inputChars the desired number of input {@code char}s to encode.
	 * Must be non-negative.
	 * @param maxBytes the maximum number of bytes to write to the buffer.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @return <ul><li>&gt= 0: The actual number of {@code byte}s written.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Encoder} until more input is provided or more output is allowed.
	 * If {@link Encoder#needsInput() needsInput()} reports a non-zero value,
	 * more input is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code char}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code byte}s were produced.
	 */
	public final int encode(Encoder enc, int inputChars, int maxBytes, int maxCodePoints) throws IOException {
		_ensureOpen();
		if (enc == null) {
			throw new NullPointerException();
		}
		if (inputChars < 0) {
			throw new IllegalArgumentException("inputChars < 0: " + inputChars);
		}
		if (maxBytes < 0) {
			throw new IllegalArgumentException("maxChars < 0: " + maxBytes);
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException("maxCodePoints < 0: " + maxCodePoints);
		}
		return doEncode(enc, inputChars, maxBytes, maxCodePoints);
	}

	final int doEncode(Encoder enc, int inputChars, int maxBytes, int maxCodePoints) throws IOException {
		long p = this.pos;
		return _encode(enc, Math.min(inputChars, enc.inputRemaining()), p, p + Math.min(maxBytes, Long.MAX_VALUE - p), maxCodePoints);
	}

	/**
	 * Encodes {@code char}s from the specified {@code Encoder} to this buffer.
	 * <p>
	 * The method starts at this buffer's current position. The buffer's
	 * position is advanced by the number of {@code byte}s written to it by the
	 * {@code Decoder}. Any pending output held by the {@code Encoder} is
	 * written to the buffer before any further encoding takes place.
	 * <p>
	 * Like all other write operations, if the buffer's current position is
	 * larger than its size, the underlying source will be grown and the
	 * resulting gap filled with {@code 0}-value {@code byte}s.
	 * <p>
	 * The method terminates when the specified number of input {@code char}s
	 * has been consumed or when an encoding error occurs. The method may
	 * terminate early and must be called in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. To ensures that no pending errors are swallowed
	 * and that all input and output if fully processed, they must be called in
	 * a loop.
	 *
	 * @param enc the {@code Encoder} used to encode bytes to this buffer.
	 * @param inputChars the desired number of input {@code char}s to encode.
	 * Must be non-negative.
	 * @return <ul><li>&gt= 0: The actual number of {@code byte}s written.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Encoder} until more input is provided. If
	 * {@link Encoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code char}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code byte}s were produced.
	 */
	public final int encode(Encoder enc, int inputChars) throws IOException {
		_ensureOpen();
		if (enc == null) {
			throw new NullPointerException();
		}
		if (inputChars < 0) {
			throw new IllegalArgumentException("inputChars < 0: " + inputChars);
		}
		return doEncode(enc, inputChars);
	}

	final int doEncode(Encoder enc, int inputChars) throws IOException {
		return _encode(enc, Math.min(inputChars, enc.inputRemaining()), this.pos, Long.MAX_VALUE, Integer.MAX_VALUE);
	}

	/**
	 * Encodes {@code char}s from the specified {@code Encoder} to this buffer.
	 * <p>
	 * The method starts at this buffer's current position. The buffer's
	 * position is advanced by the number of {@code byte}s written to it by the
	 * {@code Decoder}. Any pending output held by the {@code Encoder} is
	 * written to the buffer before any further encoding takes place.
	 * <p>
	 * Like all other write operations, if the buffer's current position is
	 * larger than its size, the underlying source will be grown and the
	 * resulting gap filled with {@code 0}-value {@code byte}s.
	 * <p>
	 * The method terminates when the {@code Encoder} is out of input or when an
	 * encoding error occurs. The method may terminate early and must be called
	 * in a loop.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. To ensures that no pending errors are swallowed
	 * and that all input and output if fully processed, they must be called in
	 * a loop.
	 *
	 * @param enc the {@code Encoder} used to encode bytes to this buffer.
	 * @return <ul><li>&gt= 0: The actual number of {@code byte}s written.</li>
	 * <li>== 0: Indicates that no further work can be done by the
	 * {@code Encoder} until more input is provided. If
	 * {@link Encoder#needsInput() needsInput()} reports a non-zero value, more
	 * input is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code char}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code byte}s were produced.
	 */
	public final int encode(Encoder enc) throws IOException {
		_ensureOpen();
		if (enc == null) {
			throw new NullPointerException();
		}
		return doEncode(enc);
	}

	final int doEncode(Encoder enc) throws IOException {
		return _encode(enc, enc.inputRemaining(), this.pos, Long.MAX_VALUE, Integer.MAX_VALUE);
	}

	/**
	 * Decodes the next available line of text, using the specified
	 * {@code Decoder}. Decoding starts at the buffer's current position. The
	 * buffer's position is advanced by the number of {@code byte}s consumed.
	 * <p>
	 * The {@code Decoder} must have no
	 * {@link Decoder#inputRemaining() input remaining} and no
	 * {@link Decoder#pendingOutput() output pending}.
	 * <p>
	 * Note that the calling class must consider there to be an additional empty
	 * line at the end of the buffer either if the buffer is empty or if the
	 * last {@code byte} is {@code '\r'} or {@code '\n'}. This pseudo-line will
	 * <em>not</em> be returned by this method.
	 * <p>
	 * Calling this method is equivalent to calling
	 * {@link #nextLine(Decoder, String) nextLine(dec, "&#92;uFFFD")}.
	 *
	 * @param dec the {@code Decoder} used for decoding.
	 * @return the next available line or {@code null} if the end of the buffer
	 * is reached.
	 * @throws NullPointerException if the {@code Decoder} or the replacement
	 * {@code String} is {@code null}.
	 * @throws IllegalStateException if the {@code Decoder} has input remaining
	 * or output pending.
	 */
	public final String nextLine(Decoder dec) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (dec.pendingOutput() != 0) {
			throw new IllegalStateException("Decoder has pending output.");
		}
		return doNextLine(dec);
	}

	final String doNextLine(Decoder dec) throws IOException {
		return _nextLine(dec, "\uFFFD");
	}

	/**
	 * Decodes the next available line of text, using the specified
	 * {@code Decoder}. Decoding starts at the buffer's current position. The
	 * buffer's position is advanced by the number of {@code byte}s consumed.
	 * <p>
	 * The {@code Decoder} must have no
	 * {@link Decoder#inputRemaining() input remaining} and no
	 * {@link Decoder#pendingOutput() output pending}.
	 * <p>
	 * Note that the calling class must consider there to be an additional empty
	 * line at the end of the buffer either if the buffer is empty or if the
	 * last {@code byte} is {@code '\r'} or {@code '\n'}. This pseudo-line will
	 * <em>not</em> be returned by this method.
	 *
	 * @param dec the {@code Decoder} used for decoding.
	 * @param replace the String used to replace malformed or unmappable code
	 * points.
	 * @return the next available line or {@code null} if the end of the buffer
	 * is reached.
	 * @throws NullPointerException if the {@code Decoder} or the replacement
	 * {@code String} is null.
	 * @throws IllegalStateException if the {@code Decoder} has input remaining
	 * or output pending.
	 */
	public final String nextLine(Decoder dec, String replace) throws IOException {
		_ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (replace == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (dec.pendingOutput() != 0) {
			throw new IllegalStateException("Decoder has pending output.");
		}
		return doNextLine(dec, replace);
	}

	final String doNextLine(Decoder dec, String replace) throws IOException {
		return _nextLine(dec, replace);
	}

	/**
	 * Inflates compressed bytes from this buffer to the specified byte-array.
	 * Returns the number of uncompressed bytes written to the target
	 * byte-array.
	 * <p>
	 * Inflation starts at the buffer's current position. The buffer's position
	 * is advanced by the number of input bytes consumed.
	 * <p>
	 * This method terminates when the specified number of input bytes has been
	 * consumed, when the specified number of output bytes has been produced,
	 * when the zip stream ends or when the {@code Inflater} requires a special
	 * dictionary to proceed.
	 * <p>
	 * The {@code Inflater} must not have any input remaining.
	 *
	 * @param numBytesIn the number of compressed input bytes to consume.
	 * @param dest the byte-array into which to inflate.
	 * @param off the offset into the target byte-array.
	 * @param len the maximum number of uncompressed bytes to produce.
	 * @param inf the {@code Inflater} used for inflation.
	 * @return the number of uncompressed bytes written to the target
	 * byte-array. A value of {@code 0} may indicate that no further bytes are
	 * available for decompression or that no further bytes can be written.
	 * @throws IllegalArgumentException if {@code numBytesIn} is negative.
	 */
	public final int inflate(long numBytesIn, byte[] dest, int off, int len, Inflater inf) throws IOException, DataFormatException {
		_ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (numBytesIn < 0L) {
			throw new IllegalArgumentException("numBytesIn < 0: " + numBytesIn);
		}
		if (off < 0 || len < 0 || off > dest.length - len) {
			throw new IndexOutOfBoundsException();
		}
		if (inf.getRemaining() != 0) {
			throw new IllegalStateException("Inflater has input remaining.");
		}
		return doInflate(numBytesIn, dest, off, len, inf);
	}

	final int doInflate(long numBytesIn, byte[] dest, int off, int len, Inflater inf) throws IOException, DataFormatException {
		long p = this.pos;
		long s = this.size;
		return _inflate(dest, off, len, inf, p, p + Math.min(numBytesIn, s - p), s);
	}

	/**
	 * Inflates compressed bytes from this buffer to the specified byte-array.
	 * Returns the number of uncompressed bytes written to the target
	 * byte-array.
	 * <p>
	 * Inflation starts at the buffer's current position. The buffer's position
	 * is advanced by the number of input bytes consumed.
	 * <p>
	 * This method terminates when the buffer is out of input , when the
	 * specified number of output bytes has been produced, when the zip stream
	 * ends or when the {@code Inflater} requires a special dictionary to
	 * proceed.
	 * <p>
	 * The {@code Inflater} must not have any input remaining.
	 *
	 * @param dest the byte-array into which to inflate.
	 * @param off the offset into the target byte-array.
	 * @param len the maximum number of uncompressed bytes to produce.
	 * @param inf the {@code Inflater} used for inflation. Must not have any
	 * input remaining.
	 * @return the number of uncompressed bytes written to the target
	 * byte-array. A value of {@code 0} may indicate that no further bytes are
	 * available for decompression or that no further bytes can be written.
	 */
	public final int inflate(byte[] dest, int off, int len, Inflater inf) throws IOException, DataFormatException {
		_ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > dest.length - len) {
			throw new IndexOutOfBoundsException();
		}
		if (inf.getRemaining() != 0) {
			throw new IllegalStateException("Inflater has input remaining.");
		}
		return doInflate(dest, off, len, inf);
	}

	final int doInflate(byte[] dest, int off, int len, Inflater inf) throws IOException, DataFormatException {
		long s = this.size;
		return _inflate(dest, off, len, inf, this.pos, s, s);
	}

	/**
	 * Inflates compressed bytes from this buffer to the specified IOBuffer.
	 * Returns the number of uncompressed bytes written to the target buffer.
	 * <p>
	 * Input bytes are consumed starting at this buffer's current position. This
	 * buffer's position is advanced by the number of bytes consumed. Output
	 * bytes are produced starting at the destination buffer's current position.
	 * Its position is advanced by the number of bytes written.
	 * <p>
	 * This method terminates when the specified number of input bytes has been
	 * consumed, when the specified number of output bytes has been produced,
	 * when the zip stream ends or when the {@code Inflater} requires a special
	 * dictionary to proceed.
	 * <p>
	 * The {@code Inflater} must not have any input remaining.
	 *
	 * @param numBytesIn the number of compressed input bytes to consume.
	 * @param dest the byte-array into which to inflate.
	 * @param numBytesOut the maximum number of uncompressed bytes to produce.
	 * @param inf the {@code Inflater} used for inflation. Must not have any
	 * input remaining.
	 * @return the number of uncompressed bytes written to the target
	 * byte-array. A value of {@code 0} may indicate that no further bytes are
	 * available for decompression or that no further bytes can be written.
	 * @throws IllegalArgumentException if {@code numBytesIn} is negative.
	 */
	public final long inflate(long numBytesIn, IOBuffer dest, long numBytesOut, Inflater inf) throws IOException, DataFormatException {
		_ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dest == this) {
			throw new IllegalArgumentException("Destination IOBuffer must not be this IOBuffer");
		}
		if (numBytesIn < 0L) {
			throw new IllegalArgumentException("numBytesIn < 0: " + numBytesIn);
		}
		if (numBytesOut < 0L) {
			throw new IllegalArgumentException("numBytesOut < 0: " + numBytesOut);
		}
		if (inf.getRemaining() != 0) {
			throw new IllegalStateException("Inflater has input remaining.");
		}
		return doInflate(numBytesIn, dest, numBytesOut, inf);
	}

	final long doInflate(long numBytesIn, IOBuffer dest, long numBytesOut, Inflater inf) throws IOException, DataFormatException {
		long p = this.pos;
		long s = this.size;
		return _inflate(dest, numBytesOut, inf, p, p + Math.min(numBytesIn, s - p), s);
	}

	/**
	 * Inflates compressed bytes from this buffer to the specified IOBuffer.
	 * Returns the number of uncompressed bytes written to the target buffer.
	 * <p>
	 * Input bytes are consumed starting at this buffer's current position. This
	 * buffer's position is advanced by the number of bytes consumed. Output
	 * bytes are produced starting at the destination buffer's current position.
	 * Its position is advanced by the number of bytes written.
	 * <p>
	 * This method terminates when this buffer is out of input, when the zip
	 * stream ends or when the {@code Inflater} requires a special dictionary to
	 * proceed.
	 * <p>
	 * The {@code Inflater} must not have any input remaining.
	 *
	 * @param dest the byte-array into which to inflate.
	 * @param inf the {@code Inflater} used for inflation. Must not have any
	 * input remaining.
	 * @return the number of uncompressed bytes written to the target
	 * byte-array. A value of {@code 0} may indicate that no further bytes are
	 * available for decompression or that no further bytes can be written.
	 * @throws IllegalArgumentException if {@code numBytesIn} is negative.
	 */
	public final long inflate(IOBuffer dest, Inflater inf) throws IOException, DataFormatException {
		_ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dest == this) {
			throw new IllegalArgumentException("Destination IOBuffer must not be this IOBuffer");
		}
		if (inf.getRemaining() != 0) {
			throw new IllegalStateException("Inflater has input remaining.");
		}
		return doInflate(dest, inf);
	}

	final long doInflate(IOBuffer dest, Inflater inf) throws IOException, DataFormatException {
		long s = this.size;
		return _inflate(dest, Long.MAX_VALUE, inf, this.pos, s, s);
	}

	/**
	 * Inflates bytes to this IOBuffer using the specified Inflater, starting at
	 * the current position. The position is advanced by the number of bytes
	 * written to the IOBuffer. The method terminates when the specified number
	 * of bytes has been written, when the Inflater needs more input, when the
	 * inflater needs a special dictionary or when the zip stream ends.
	 *
	 * @param inf the Inflater whose input to decompress.
	 * @param len the desired number of decompressed bytes to write to the
	 * IOBuffer.
	 * @return the number of bytes written to the IOBuffer.
	 */
	public final int inflateTo(Inflater inf, int len) {
		_ensureOpen();
		if (inf == null) {
			throw new NullPointerException();
		}
		if (len < 0) {
			throw new IllegalArgumentException("len < 0: " + len);
		}
		long p = this.pos;
		if (p + len < 0L) {
			throw new IllegalArgumentException("pos() + len > Long.MAX_VAKUE. pos(): " + p + ", len: " + len);
		}
		return _inflateTo(inf, len, p);
	}

	final int doInflateTo(Inflater inf, int len) {
		return _inflateTo(inf, len, this.pos);
	}

	/**
	 * Writes bytes from this buffer to the destination buffer. Returns the
	 * number of {@code byte}s written, possibly {@code 0} if this buffer has no
	 * further {@code byte}s available at its current position, or if no further
	 * {@code byte}s can be written to the destination buffer.
	 * <p>
	 * Input {@code byte}s are read starting at this buffer's current position
	 * and written to the destination buffer's position. Both buffers' positions
	 * are advanced by the number of {@code byte}s transferred.
	 *
	 * @param numBytes the maximum number of {@code byte}s to transfer.
	 * @param dest the buffer to write to.
	 * @return the number of {@code byte}s written, possibly {@code 0} if this
	 * buffer has no further {@code byte}s available at its current position, or
	 * if no further {@code byte}s can be written to the destination buffer.
	 */
	public final long transfer(IOBuffer dest, long numBytes) throws IOException {
		_ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dest == this) {
			throw new IllegalArgumentException("Destination IOBuffer must not be this IOBuffer");
		}
		if (numBytes < 0L) {
			throw new IllegalArgumentException("numBytes < 0: " + numBytes);
		}
		return doTransfer(dest, numBytes);
	}

	final long doTransfer(IOBuffer dest, long numBytes) throws IOException {
		long p = this.pos;
		long s = this.size;
		if (p >= s) {
			return 0L;
		}
		return _transfer(Math.min(numBytes, s - p), dest, p, s);
	}

	/**
	 * Writes bytes from this buffer to the destination buffer. Returns the
	 * number of {@code byte}s written, possibly {@code 0} if this buffer has no
	 * further {@code byte}s available at its current position, or if no further
	 * {@code byte}s can be written to the destination buffer.
	 * <p>
	 * Input {@code byte}s are read starting at this buffer's current position
	 * and written to the destination buffer's position. Both buffers' positions
	 * are advanced by the number of {@code byte}s transferred.
	 *
	 * @param dest the buffer to write to.
	 * @return the number of {@code byte}s written, possibly {@code 0} if this
	 * buffer has no further {@code byte}s available at its current position, or
	 * if no further {@code byte}s can be written to the destination buffer.
	 */
	public final long transfer(IOBuffer dest) throws IOException {
		_ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dest == this) {
			throw new IllegalArgumentException("Destination IOBuffer must not be this IOBuffer");
		}
		return doTransfer(dest);
	}

	final long doTransfer(IOBuffer dest) throws IOException {
		long p = this.pos;
		long s = this.size;
		if (p >= s) {
			return 0L;
		}
		return _transfer(s - p, dest, p, s);
	}

	///////////////////////////////////////////////////////////////
	// IMPL METHODS
	/////////////////////////////////////////////////////////////// 
	//
	// methods to be implemented by sub-classes.
	// All relevant bounds-checks have already been performed.
	// Generally, they are fully bounded, ie. they are always called such that they point to 
	// byte ranges guaranteed to be accessible (barring IO errors).
	//
	// ensure that the buffer has not been closed
	abstract void _ensureOpen() throws IOException;
	abstract void _truncate(long size) throws IOException;
	// 
	abstract byte _readByte(long pos, long pos_len, long s) throws IOException;
	abstract char _readChar(long pos, long pos_len, long s, Endian endian) throws IOException;
	abstract short _readShort(long pos, long pos_len, long s, Endian endian) throws IOException;
	abstract int _readInt(long pos, long pos_len, long s, Endian endian) throws IOException;
	abstract float _readFloat(long pos, long pos_len, long s, Endian endian) throws IOException;
	abstract long _readLong(long pos, long pos_len, long s, Endian endian) throws IOException;
	abstract double _readDouble(long pos, long pos_len, long s, Endian endian) throws IOException;
	abstract void _read(long pos, long pos_len, long s, byte[] buf, int off, int len) throws IOException;
	//
	abstract void _writeByte(byte n, long pos, long pos_len) throws IOException;
	abstract void _writeChar(char n, long pos, long pos_len, Endian endian) throws IOException;
	abstract void _writeShort(short n, long pos, long pos_len, Endian endian) throws IOException;
	abstract void _writeInt(int n, long pos, long pos_len, Endian endian) throws IOException;
	abstract void _writeFloat(float n, long pos, long pos_len, Endian endian) throws IOException;
	abstract void _writeLong(long n, long pos, long pos_len, Endian endian) throws IOException;
	abstract void _writeDouble(double n, long pos, long pos_len, Endian endian) throws IOException;
	abstract void _write(long pos, long pos_len, byte[] buf, int off, int len) throws IOException;
	// pos = starting position along source
	// end = ending position (exclusive) along source
	abstract int _decode(Decoder dec, Appendable dest, int maxChars, int maxCodePoints, long pos, long end, long s) throws IOException;
	abstract int _decode(Decoder dec, Appendable dest, int maxChars, int maxCodePoints, long pos, long end, long s, IntPredicate stop) throws IOException;
	abstract int _decode(Decoder dec, char[] dest, int off, int maxChars, int maxCodePoints, long pos, long end, long s) throws IOException;
	abstract int _decode(Decoder dec, char[] dest, int off, int maxChars, int maxCodePoints, long pos, long end, long s, IntPredicate stop) throws IOException;
	//	
	abstract int _encode(Encoder enc, int inputChars, long pos, long end, int maxCodePoints) throws IOException;
	//
	abstract String _nextLine(Decoder dec, String replace) throws IOException;
	//
	abstract int _inflate(byte[] dest, int off, int len, Inflater inf, long pos, long end, long s) throws IOException, DataFormatException;
	abstract long _inflate(IOBuffer dest, long numBytesOut, Inflater inf, long pos, long end, long s) throws IOException, DataFormatException;
	//
	abstract int _inflateTo(Inflater inf, int len, long pos) throws IOException;
	//
	abstract long _transfer(long numBytes, IOBuffer dest, long pos, long s) throws IOException;

	// must ALWAYS call closeSource()
	@Override
	public abstract void close() throws IOException;
}
