/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

import java.util.zip.Checksum;
import java.util.zip.Inflater;
import org.bitsandpieces.util.Endian;
import org.bitsandpieces.util.encoding.Decoder;
import org.bitsandpieces.util.encoding.Encoder;

/**
 * Provides an efficient buffer strategy for an IOAddress, providing semi-random
 * access and the ability to read and write any kind of data in any order from
 * an underlying I/O source. Semi-random, in this context, simply means that
 * performance degrades the more random the access requests are. Sequential (in
 * either direction) reading and writing will perform on a level
 * indistinguishable from any buffered I/O solution. Performing short accesses
 * within half of a buffer's specified size (specified at instantiation) will
 * generally performed completely in memory.
 * <p>
 * In addition, this interface provides performace-oriented convenience methods
 * that can significantly speed up many common tasks.
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
 * costs amortize to a neglibigle constant cost if many buffers are used. Said
 * arrays are eligible to be garbage collected if unused for any longer period
 * of time, and as such do not require special clean-up.
 *
 * @author Jan Kebernik
 */
public interface IOBuffer extends AutoCloseable {

	/**
	 * Returns the underlying {@code IOAddress}.
	 *
	 * @return the underlying {@code IOAddress}.
	 */
	IOAddress address();

	/**
	 * Returns the current position along the underlying source.
	 *
	 * @return the current position along the underlying source.
	 */
	long pos();

	/**
	 * Sets the current position along the underlying source to the specified
	 * value.
	 *
	 * @param pos the new position.
	 * @return this {@code IOBuffer}.
	 * @throws IllegalArgumentException if {@code pos} is negative.
	 */
	IOBuffer pos(long pos);

	/**
	 * Returns this {@code IOBuffer}'s default {@code Endian}.
	 *
	 * @return this {@code IOBuffer}'s default {@code Endian}.
	 */
	Endian endian();

	/**
	 * Sets this {@code IOBuffer}'s default {@code Endian} to the specified
	 * {@code Endian}.
	 *
	 * @param endian the new default {@code Endian}.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	IOBuffer endian(Endian endian);

	/**
	 * Returns the length in bytes of the buffered source. Note that the value
	 * returned by this method may differ from values reported by the underlying
	 * source until this buffer is flushed by closing it.
	 *
	 * @return the length in bytes of the buffered source.
	 */
	long size() throws IOException;

	/**
	 * Reads and returns a single {@code byte} at the current position. The
	 * buffer's position is advanced by one {@code byte}.
	 *
	 * @return the {@code byte} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	byte readByte() throws IOException;

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
	byte readByte(long pos) throws IOException;

	/**
	 * Reads and returns a single {@code char} at the current position, using
	 * the default {@code Endian}. The buffer's position is advanced by two
	 * {@code byte}s.
	 *
	 * @return the {@code char} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	char readChar() throws IOException;

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
	char readChar(Endian endian) throws IOException;

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
	char readChar(long pos) throws IOException;

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
	char readChar(long pos, Endian endian) throws IOException;

	/**
	 * Reads and returns a single {@code short} at the current position, using
	 * the default {@code Endian}. The buffer's position is advanced by two
	 * {@code byte}s.
	 *
	 * @return the {@code short} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	short readShort() throws IOException;

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
	short readShort(Endian endian) throws IOException;

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
	short readShort(long pos) throws IOException;

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
	short readShort(long pos, Endian endian) throws IOException;

	/**
	 * Reads and returns a single {@code int} at the current position, using the
	 * default {@code Endian}. The buffer's position is advanced by four
	 * {@code byte}s.
	 *
	 * @return the {@code int} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	int readInt() throws IOException;

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
	int readInt(Endian endian) throws IOException;

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
	int readInt(long pos) throws IOException;

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
	int readInt(long pos, Endian endian) throws IOException;

	/**
	 * Reads and returns a single {@code float} at the current position, using
	 * the default {@code Endian}. The buffer's position is advanced by four
	 * {@code byte}s.
	 *
	 * @return the {@code float} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	default float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
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
	default float readFloat(Endian endian) throws IOException {
		return Float.intBitsToFloat(readInt(endian));
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
	default float readFloat(long pos) throws IOException {
		return Float.intBitsToFloat(readInt(pos));
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
	default float readFloat(long pos, Endian endian) throws IOException {
		return Float.intBitsToFloat(readInt(pos, endian));
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
	long readLong() throws IOException;

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
	long readLong(Endian endian) throws IOException;

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
	long readLong(long pos) throws IOException;

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
	long readLong(long pos, Endian endian) throws IOException;

	/**
	 * Reads and returns a single {@code double} at the current position, using
	 * the default {@code Endian}. The buffer's position is advanced by eight
	 * {@code byte}s.
	 *
	 * @return the {@code double} read at the current position.
	 * @throws IndexOutOfBoundsException if there is not enough input available
	 * at the current position to complete the operation.
	 */
	default double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
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
	default double readDouble(Endian endian) throws IOException {
		return Double.longBitsToDouble(readLong(endian));
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
	default double readDouble(long pos) throws IOException {
		return Double.longBitsToDouble(readLong(pos));
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
	default double readDouble(long pos, Endian endian) throws IOException {
		return Double.longBitsToDouble(readLong(pos, endian));
	}

	/**
	 * Reads {@code byte}s from the underlying source into the specified array,
	 * starting at the current position. The buffer's position is advanced by
	 * the actual number of bytes read.
	 *
	 * @param buf the array into which to read.
	 * @return the actual number of {@code byte}s read or {@code -1} if the end
	 * of the buffer was reached.
	 */
	int read(byte[] buf) throws IOException;

	/**
	 * Reads {@code byte}s from the underlying source into the specified array,
	 * starting at the current position. The buffer's position is advanced by
	 * the actual number of bytes read.
	 *
	 * @param buf the array into which to read.
	 * @param off the offset into the array.
	 * @param len the number of {@code byte}s to read.
	 * @return the actual number of {@code byte}s read or {@code -1} if the end
	 * of the buffer was reached.
	 * @throws IndexOutOfBoundsException if the specified array range is
	 * illegal.
	 */
	int read(byte[] buf, int off, int len) throws IOException;

	/**
	 * Reads {@code byte}s from the underlying source into the specified array.
	 * The buffer's position is unchanged.
	 *
	 * @param pos the position from which to start reading.
	 * @param buf the array into which to read.
	 * @return the actual number of {@code byte}s read or {@code -1} if the end
	 * of the buffer was reached.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 */
	int read(long pos, byte[] buf) throws IOException;

	/**
	 * Reads {@code byte}s from the underlying source into the specified array.
	 * The buffer's position is unchanged.
	 *
	 * @param pos the position from which to start reading.
	 * @param buf the array into which to read.
	 * @param off the offset into the array.
	 * @param len the number of {@code byte}s to read.
	 * @return the actual number of {@code byte}s read or {@code -1} if the end
	 * of the buffer was reached.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 * @throws IndexOutOfBoundsException if the specified array range is
	 * illegal.
	 */
	int read(long pos, byte[] buf, int off, int len) throws IOException;

	/**
	 * Writes the specified {@code byte} to the underlying source at the current
	 * position. The buffer's position is advanced by one {@code byte}. If the
	 * current position is larger than the current size, the source will be
	 * grown and the resulting gap will be filled with {@code 0}-value
	 * {@code byte}s.
	 *
	 * @param n the {@code byte} to write.
	 * @return this {@code IOBuffer}.
	 */
	IOBuffer writeByte(byte n) throws IOException;

	/**
	 * Writes the specified {@code byte} to the underlying source at the
	 * specified position. The buffer's position is unchanged. If the specified
	 * position is larger than the current size, the source will be grown and
	 * the resulting gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code byte} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 */
	IOBuffer writeByte(byte n, long pos) throws IOException;

	/**
	 * Writes the specified {@code char} to the underlying source at the current
	 * position, using the default {@code Endian}. The buffer's position is
	 * advanced by two {@code byte}s. If the current position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code char} to write.
	 * @return this {@code IOBuffer}.
	 */
	IOBuffer writeChar(char n) throws IOException;

	/**
	 * Writes the specified {@code char} to the underlying source at the current
	 * position, using the specified {@code Endian}. The buffer's position is
	 * advanced by two {@code byte}s. If the current position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code char} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	IOBuffer writeChar(char n, Endian endian) throws IOException;

	/**
	 * Writes the specified {@code char} to the underlying source at the
	 * specified position, using the default {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code char} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 */
	IOBuffer writeChar(char n, long pos) throws IOException;

	/**
	 * Writes the specified {@code char} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code char} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	IOBuffer writeChar(char n, long pos, Endian endian) throws IOException;

	/**
	 * Writes the specified {@code short} to the underlying source at the
	 * current position, using the default {@code Endian}. The buffer's position
	 * is advanced by two {@code byte}s. If the current position is larger than
	 * the current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code short} to write.
	 * @return this {@code IOBuffer}.
	 */
	IOBuffer writeShort(short n) throws IOException;

	/**
	 * Writes the specified {@code short} to the underlying source at the
	 * current position, using the specified {@code Endian}. The buffer's
	 * position is advanced by two {@code byte}s. If the current position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code short} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	IOBuffer writeShort(short n, Endian endian) throws IOException;

	/**
	 * Writes the specified {@code short} to the underlying source at the
	 * specified position, using the default {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code short} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 */
	IOBuffer writeShort(short n, long pos) throws IOException;

	/**
	 * Writes the specified {@code short} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code short} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	IOBuffer writeShort(short n, long pos, Endian endian) throws IOException;

	/**
	 * Writes the specified {@code int} to the underlying source at the current
	 * position, using the default {@code Endian}. The buffer's position is
	 * advanced by four {@code byte}s. If the current position is larger than
	 * the current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code int} to write.
	 * @return this {@code IOBuffer}.
	 */
	IOBuffer writeInt(int n) throws IOException;

	/**
	 * Writes the specified {@code int} to the underlying source at the current
	 * position, using the specified {@code Endian}. The buffer's position is
	 * advanced by four {@code byte}s. If the current position is larger than
	 * the current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code int} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	IOBuffer writeInt(int n, Endian endian) throws IOException;

	/**
	 * Writes the specified {@code int} to the underlying source at the
	 * specified position, using the default {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code int} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 */
	IOBuffer writeInt(int n, long pos) throws IOException;

	/**
	 * Writes the specified {@code int} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code int} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	IOBuffer writeInt(int n, long pos, Endian endian) throws IOException;

	/**
	 * Writes the specified {@code float} to the underlying source at the
	 * current position, using the default {@code Endian}. The buffer's position
	 * is advanced by two {@code byte}s. If the current position is larger than
	 * the current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code float} to write.
	 * @return this {@code IOBuffer}.
	 */
	default IOBuffer writeFloat(float n) throws IOException {
		return writeInt(Float.floatToRawIntBits(n));
	}

	/**
	 * Writes the specified {@code float} to the underlying source at the
	 * current position, using the specified {@code Endian}. The buffer's
	 * position is advanced by two {@code byte}s. If the current position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code float} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	default IOBuffer writeFloat(float n, Endian endian) throws IOException {
		return writeInt(Float.floatToRawIntBits(n), endian);
	}

	/**
	 * Writes the specified {@code float} to the underlying source at the
	 * specified position, using the default {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code float} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 */
	default IOBuffer writeFloat(float n, long pos) throws IOException {
		return writeInt(Float.floatToRawIntBits(n), pos);
	}

	/**
	 * Writes the specified {@code float} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code float} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	default IOBuffer writeFloat(float n, long pos, Endian endian) throws IOException {
		return writeInt(Float.floatToRawIntBits(n), pos, endian);
	}

	/**
	 * Writes the specified {@code long} to the underlying source at the current
	 * position, using the default {@code Endian}. The buffer's position is
	 * advanced by eight {@code byte}s. If the current position is larger than
	 * the current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code long} to write.
	 * @return this {@code IOBuffer}.
	 */
	IOBuffer writeLong(long n) throws IOException;

	/**
	 * Writes the specified {@code long} to the underlying source at the current
	 * position, using the specified {@code Endian}. The buffer's position is
	 * advanced by eight {@code byte}s. If the current position is larger than
	 * the current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code long} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	IOBuffer writeLong(long n, Endian endian) throws IOException;

	/**
	 * Writes the specified {@code long} to the underlying source at the
	 * specified position, using the default {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code long} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 */
	IOBuffer writeLong(long n, long pos) throws IOException;

	/**
	 * Writes the specified {@code long} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code long} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	IOBuffer writeLong(long n, long pos, Endian endian) throws IOException;

	/**
	 * Writes the specified {@code double} to the underlying source at the
	 * current position, using the default {@code Endian}. The buffer's position
	 * is advanced by eight {@code byte}s. If the current position is larger
	 * than the current size, the source will be grown and the resulting gap
	 * will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code double} to write.
	 * @return this {@code IOBuffer}.
	 */
	default IOBuffer writeDouble(double n) throws IOException {
		return writeLong(Double.doubleToRawLongBits(n));
	}

	/**
	 * Writes the specified {@code double} to the underlying source at the
	 * current position, using the specified {@code Endian}. The buffer's
	 * position is advanced by eight {@code byte}s. If the current position is
	 * larger than the current size, the source will be grown and the resulting
	 * gap will be filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code double} to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	default IOBuffer writeDouble(double n, Endian endian) throws IOException {
		return writeLong(Double.doubleToRawLongBits(n), endian);
	}

	/**
	 * Writes the specified {@code double} to the underlying source at the
	 * specified position, using the default {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code double} to write.
	 * @param pos the position to which to write.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 */
	default IOBuffer writeDouble(double n, long pos) throws IOException {
		return writeLong(Double.doubleToRawLongBits(n), pos);
	}

	/**
	 * Writes the specified {@code double} to the underlying source at the
	 * specified position, using the specified {@code Endian}. The buffer's
	 * position is unchanged. If the specified position is larger than the
	 * current size, the source will be grown and the resulting gap will be
	 * filled with {@code 0}-value {@code byte}s.
	 *
	 * @param n the {@code double} to write.
	 * @param pos the position to which to write.
	 * @param endian the {@code Endian} determining the {@code byte} order.
	 * @return this {@code IOBuffer}.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 * @throws NullPointerException if the specified {@code Endian} is
	 * {@code null}.
	 */
	default IOBuffer writeDouble(double n, long pos, Endian endian) throws IOException {
		return writeLong(Double.doubleToRawLongBits(n), pos, endian);
	}

	/**
	 * Writes the specified {@code byte}s to the underlying source at the
	 * current position. The buffer's position is advanced by the number
	 * {@code byte}s written.
	 *
	 * @param buf the array whose {@code byte}s to write.
	 * @return the number of {@code byte}s written.
	 */
	int write(byte[] buf) throws IOException;

	/**
	 * Writes the specified {@code byte}s to the underlying source at the
	 * current position. The buffer's position is advanced by the number
	 * {@code byte}s written.
	 *
	 * @param buf the array whose {@code byte}s to write.
	 * @param off the offset into the array.
	 * @param len the number of {@code byte}s to write.
	 * @return the number of {@code byte}s written.
	 * @throws IndexOutOfBoundsException if the specified array range is
	 * illegal.
	 */
	int write(byte[] buf, int off, int len) throws IOException;

	/**
	 * Writes the specified {@code byte}s to the underlying source at the
	 * specified position. The buffer's position is unchanged.
	 *
	 * @param pos the position to which to write.
	 * @param buf the array whose {@code byte}s to write.
	 * @return the number of {@code byte}s written.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 */
	int write(long pos, byte[] buf) throws IOException;

	/**
	 * Writes the specified {@code byte}s to the underlying source at the
	 * specified position. The buffer's position is unchanged.
	 *
	 * @param pos the position to which to write.
	 * @param buf the array whose {@code byte}s to write.
	 * @param off the offset into the array.
	 * @param len the number of {@code byte}s to write.
	 * @return the number of {@code byte}s written.
	 * @throws IndexOutOfBoundsException if the specified position is negative.
	 * @throws IndexOutOfBoundsException if the specified array range is
	 * illegal.
	 */
	int write(long pos, byte[] buf, int off, int len) throws IOException;

	/**
	 * If the specified size is less than the current size, will truncate the
	 * underlying source to the new size.
	 *
	 * @param size the new size.
	 * @return this {@code IOBuffer}.
	 */
	IOBuffer truncate(long size) throws IOException;

	@Override
	public void close() throws IOException;

	/**
	 * Returns the number of bytes between the current position and the current
	 * size. If the current position is larger than the current size, will
	 * return a negative result.
	 *
	 * @return the number of bytes between the current position and the current
	 * size.
	 */
	long bytesRemaining();

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
	 * when a decoding error occurs.
	 * <p>
	 * Note that, depending on the implementation, not all input bytes may be
	 * processed in a single invocation.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. To ensures that no pending errors are swallowed
	 * and that all input and output if fully processed, they must be called in
	 * a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes
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
	int decode(Decoder dec, int inputBytes, Appendable dest, int maxChars, int maxCodePoints) throws IOException;

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
	 * has been consumed or when a decoding error occurs.
	 * <p>
	 * Note that, depending on the implementation, not all input bytes may be
	 * processed in a single invocation.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. To ensures that no pending errors are swallowed
	 * and that all input and output if fully processed, they must be called in
	 * a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes
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
	int decode(Decoder dec, int inputBytes, Appendable dest) throws IOException;

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
	 * error occurs.
	 * <p>
	 * Note that, depending on the implementation, not all input bytes may be
	 * processed in a single invocation.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. To ensures that no pending errors are swallowed
	 * and that all input and output if fully processed, they must be called in
	 * a loop.
	 *
	 * @param dec the {@code Decoder} used to decode input bytes
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
	int decode(Decoder dec, Appendable dest) throws IOException;

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
	 * when an encoding error occurs.
	 * <p>
	 * Note that, depending on the implementation, not all output bytes may be
	 * processed in a single invocation.
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
	int encode(Encoder enc, int inputChars, int maxBytes, int maxCodePoints) throws IOException;

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
	 * has been consumed or when an encoding error occurs.
	 * <p>
	 * Note that, depending on the implementation, not all output bytes may be
	 * processed in a single invocation.
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
	int encode(Encoder enc, int inputChars) throws IOException;

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
	 * encoding error occurs.
	 * <p>
	 * Note that, depending on the implementation, not all output bytes may be
	 * processed in a single invocation.
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
	int encode(Encoder enc) throws IOException;

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
	default String nextLine(Decoder dec) throws IOException {
		return nextLine(dec, "\uFFFD");
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
	String nextLine(Decoder dec, String replace) throws IOException;

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
	 * byte-array.
	 * @throws IllegalArgumentException if {@code numBytesIn} is negative or
	 * greater than bytesRemaining().
	 */
	public int inflate(long numBytesIn, byte[] dest, int off, int len, Inflater inf) throws IOException, DataFormatException;

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
	 * byte-array.
	 */
	public int inflate(byte[] dest, int off, int len, Inflater inf) throws IOException, DataFormatException;

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
	 * byte-array.
	 * @throws IllegalArgumentException if {@code numBytesIn} is negative or
	 * greater than bytesRemaining().
	 */
	public long inflate(long numBytesIn, IOBuffer dest, long numBytesOut, Inflater inf) throws IOException, DataFormatException;

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
	 * byte-array.
	 * @throws IllegalArgumentException if {@code numBytesIn} is negative or
	 * greater than bytesRemaining().
	 */
	public long inflate(IOBuffer dest, Inflater inf) throws IOException, DataFormatException;

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
	public long transfer(long numBytes, IOBuffer dest) throws IOException;

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
	public long transfer(IOBuffer dest) throws IOException;

	/**
	 * Creates a new {@code IOBuffer} with the exact same I/O source backing it
	 * as this {@code IOBuffer}.
	 * <p>
	 * This is generally only useful for creating multiple read-only buffers for
	 * the same address, as no two buffers are guaranteed to share synchronized
	 * contents. The new sibling will momentarily be synhronized to this
	 * IOBuffer, but not to any others. Modifying the contents of two or more
	 * siblings leads to undefined behaviour, even on a single thread.
	 * <p>
	 * The I/O source backing all shared siblings will only be closed when all
	 * openend siblings are closed.
	 *
	 * @return a new {@code IOBuffer} with the exacty same I/O source backing it
	 * as this {@code IOBuffer}.
	 */
	public IOBuffer createSibling() throws IOException;

	/**
	 * Updates the specified {@code Checksum} with {@code byte}s from this
	 * buffer.
	 * <p>
	 * Advances the buffer's position by the number of {@code byte}s consumed.
	 *
	 * @param sum the {@code Checksum} to update.
	 * @return the actual number of {@code byte}s consumed to update the
	 * {@code Checksum}, or {@code -1} if no {@code byte}s are available at the
	 * current position.
	 */
	public long updateChecksum(Checksum sum) throws IOException;

	/**
	 * Updates the specified {@code Checksum} with {@code byte}s from this
	 * buffer.
	 * <p>
	 * The buffer's position is not modified.
	 *
	 * @param pos the position from which to start updating the
	 * {@code Checksum}.
	 * @param sum the {@code Checksum} to update.
	 * @return the actual number of {@code byte}s consumed to update the
	 * {@code Checksum}, or {@code -1} if no {@code byte}s are available at the
	 * specified position.
	 */
	public long updateChecksum(long pos, Checksum sum) throws IOException;

	/**
	 * Updates the specified {@code Checksum} with {@code byte}s from this
	 * buffer.
	 * <p>
	 * Advances the buffer's position by the number of {@code byte}s consumed.
	 *
	 * @param sum the {@code Checksum} to update.
	 * @param len the maximum number of {@code byte}s with which to update the
	 * {@code Checksum}.
	 * @return the actual number of {@code byte}s consumed to update the
	 * {@code Checksum}, or {@code -1} if no {@code byte}s are available at the
	 * current position.
	 */
	public long updateChecksum(Checksum sum, long len) throws IOException;

	/**
	 * Updates the specified {@code Checksum} with {@code byte}s from this
	 * buffer.
	 * <p>
	 * The buffer's position is not modified.
	 *
	 * @param pos the position from which to start updating the
	 * {@code Checksum}.
	 * @param sum the {@code Checksum} to update.
	 * @param len the maximum number of {@code byte}s with which to update the
	 * {@code Checksum}.
	 * @return the actual number of {@code byte}s consumed to update the
	 * {@code Checksum}, or {@code -1} if no {@code byte}s are available at the
	 * specified position.
	 */
	public long updateChecksum(long pos, Checksum sum, long len) throws IOException;
}
