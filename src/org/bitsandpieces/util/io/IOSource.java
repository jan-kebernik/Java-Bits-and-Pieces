/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

/**
 * Provides an IOAddress with basic I/O functionality, intended to serve as a
 * bridge between an address and a buffer. Instances implementing this interface
 * are not meant to be used directly, but only via their
 * {@link #buffer() buffers}.
 *
 * @author Jan Bebernik
 */
// TODO decide whether to hide this interface
public interface IOSource extends AutoCloseable {

	/**
	 * Returns the address of this source.
	 *
	 * @return the address of this source.
	 */
	IOAddress address();

	/**
	 * Returns the length in bytes of the underlying file.
	 *
	 * @return the length in bytes of the underlying file.
	 */
	long size() throws IOException;

	/**
	 * Reads the specified number of bytes into the specified byte-array,
	 * starting at the specified position.
	 *
	 * @param pos the position from which to start reading bytes.
	 * @param buf the byte-array into which to read bytes.
	 * @param off the offset into the destination array.
	 * @param len the maximum number of bytes to read.
	 * @return the actual number of bytes read, or -1 if the end of the file is
	 * reached.
	 */
	int read(long pos, byte[] buf, int off, int len) throws IOException;

	/**
	 * Writes the specified byte to the underlying file at the specified
	 * position.
	 *
	 * Note that the underlying file must grow in size if bytes are written past
	 * its current size. Writing to a position higher than the current size must
	 * cause the gap inbetween to be filled with 0-value bytes.
	 *
	 * @param pos the position to start writing to.
	 * @param buf the array whose bytes to write.
	 * @param off the offset into the array.
	 * @param len the number of bytes to write.
	 */
	void write(long pos, byte[] buf, int off, int len) throws IOException;

	/**
	 * If the specified size is less than the current size, will truncate the
	 * underlying file to the new size.
	 *
	 * @param size the new size.
	 */
	void truncate(long size) throws IOException;

	/**
	 * Wraps this source in a buffer of a size (if applicable) appropriate for
	 * most tasks.
	 *
	 * @return a new IOBuffer wrapping this source.
	 */
	default IOBuffer buffer() {
		return new GeneralIOBuffer(this);
	}

	/**
	 * Wraps this source in a buffer of the specified size (if applicable).
	 *
	 * @param bufferSize the size of the buffer.
	 * @return a new IOBuffer wrapping this source.
	 */
	default IOBuffer buffer(int bufferSize) {
		return new GeneralIOBuffer(this, bufferSize);
	}

	@Override
	void close() throws IOException;
}
