/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.zip;

import java.nio.ByteBuffer;

/**
 * Pure Java implementation of {@link java.util.zip.CRC32}.
 * <p>
 * Performance is not quite competitive with the JDK version, although it is
 * fast enough to be useful.
 * </p>
 * <p>
 * Access to {@code OpenCRC32}'s internal value can be granted non-reflectively,
 * via object-inheritence.
 * </p>
 *
 * @author Jan Kebernik
 */
public class OpenCRC32 implements OpenChecksum, Cloneable {

	private static final int[] TABLE = genTable();

	// generates 8 CRC32 tables in a single array instance.
	private static int[] genTable() {
		int[] table = new int[2048];
		for (int i = 0; i < 256; i++) {
			int c = i;
			for (int j = 0; j < 8; j++) {
				c = (c >>> 1) ^ (-(c & 1) & 0xedb88320);
			}
			table[i] = c;
		}
		for (int i = 0; i < 256; i++) {
			table[0x100 + i] = (table[0x000 + i] >>> 8) ^ table[table[0x000 + i] & 0xff];
			table[0x200 + i] = (table[0x100 + i] >>> 8) ^ table[table[0x100 + i] & 0xff];
			table[0x300 + i] = (table[0x200 + i] >>> 8) ^ table[table[0x200 + i] & 0xff];
			table[0x400 + i] = (table[0x300 + i] >>> 8) ^ table[table[0x300 + i] & 0xff];
			table[0x500 + i] = (table[0x400 + i] >>> 8) ^ table[table[0x400 + i] & 0xff];
			table[0x600 + i] = (table[0x500 + i] >>> 8) ^ table[table[0x500 + i] & 0xff];
			table[0x700 + i] = (table[0x600 + i] >>> 8) ^ table[table[0x600 + i] & 0xff];
		}
		return table;
	}

	/**
	 * Updates the speficied sum with the specified bytes from the provided byte
	 * array.
	 */
	static final int updateCRC32(int sum, byte[] b, int off, int len) {
		sum = ~sum;
		int n = len & 7;
		// update 0-7 unaligned bytes
		for (int m = off + n; off < m; off++) {
			sum = TABLE[(sum ^ b[off]) & 0xff] ^ (sum >>> 8);
		}
		// update 8 bytes at a time.
		for (int m = off + len - n; off != m; off += 8) {
			int x = ((b[off] & 0xff) | ((b[off + 1] & 0xff) << 8) | ((b[off + 2] & 0xff) << 16) | (b[off + 3] << 24)) ^ sum;
			int y = ((b[off + 4] & 0xff) | ((b[off + 5] & 0xff) << 8) | ((b[off + 6] & 0xff) << 16) | (b[off + 7] << 24));
			sum = TABLE[0x000 + (y >>> 24)]
					^ TABLE[0x100 + ((y >>> 16) & 0xff)]
					^ TABLE[0x200 + ((y >>> 8) & 0xff)]
					^ TABLE[0x300 + (y & 0xff)]
					^ TABLE[0x400 + (x >>> 24)]
					^ TABLE[0x500 + ((x >>> 16) & 0xff)]
					^ TABLE[0x600 + ((x >>> 8) & 0xff)]
					^ TABLE[0x700 + (x & 0xff)];
		}
		return ~sum;
	}

	/**
	 * Updates the speficied sum with the specified bytes from the provided
	 * buffer. The buffer position is updated.
	 */
	static final int updateCRC32(int sum, ByteBuffer buffer, int len) {
		if (buffer.hasArray()) {
			// use the much faster array version, if possible.
			int off = buffer.position();
			if (off < 0) {
				throw new IllegalStateException("Provided buffer has a negative position: " + off);
			}
			buffer.position(off + len);
			return updateCRC32(sum, buffer.array(), buffer.arrayOffset() + off, len);
		}
		java.nio.ByteOrder ord = buffer.order();
		buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
		sum = ~sum;
		len -= len & 7;
		for (int n = len & 7; n != 0; n--) {
			sum = TABLE[(sum ^ buffer.get()) & 0xff] ^ (sum >>> 8);
		}
		for (; len != 0; len -= 8) {
			int x = buffer.getInt() ^ sum;
			int y = buffer.getInt();
			sum = TABLE[0x000 + (y >>> 24)]
					^ TABLE[0x100 + ((y >>> 16) & 0xff)]
					^ TABLE[0x200 + ((y >>> 8) & 0xff)]
					^ TABLE[0x300 + (y & 0xff)]
					^ TABLE[0x400 + (x >>> 24)]
					^ TABLE[0x500 + ((x >>> 16) & 0xff)]
					^ TABLE[0x600 + ((x >>> 8) & 0xff)]
					^ TABLE[0x700 + (x & 0xff)];
		}
		buffer.order(ord);	// reset the order to its previous value
		return ~sum;
	}

	/**
	 * Updates the speficied sum with the specified byte.
	 */
	static final int updateCRC32(int sum, int b) {
		int s = ~sum;
		return ~(TABLE[(s ^ b) & 0xff] ^ (s >>> 8));
	}

	/**
	 * The internal checksum.
	 */
	protected int sum;

	/**
	 * Creates a new {@code OpenCRC32} object.
	 */
	public OpenCRC32() {
		this(0);
	}

	/**
	 * Creates a new {@code OpenCRC32} object with a pre-determined checksum.
	 *
	 * @param sum the pre-determined checksum that this {@code OpenCRC32} starts
	 * out with.
	 */
	protected OpenCRC32(int sum) {
		this.sum = sum;
	}

	/**
	 * Updates this {@code Checksum} with the specified byte (the low eight bits
	 * of the argument b).
	 *
	 * @param b the byte to update this {@code Checksum} with.
	 */
	@Override
	public void update(int b) {
		this.sum = updateCRC32(this.sum, b);
	}

	/**
	 * Updates this {@code Checksum} with the specified array of bytes.
	 *
	 * @throws IndexOutOfBoundsException if {@code off} is negative, or
	 * {@code len} is negative, or {@code off+len} is greater than the length of
	 * the array {@code b}
	 */
	@Override
	public void update(byte[] b, int off, int len) {
		if (off < 0 || len < 0 || off > b.length - len) {
			throw new IndexOutOfBoundsException();
		}
		this.sum = updateCRC32(this.sum, b, off, len);
	}

	/**
	 * Updates this {@code Checksum} with the specified array of bytes.
	 *
	 * @param b the byte array to update the checksum with.
	 */
	public void update(byte[] b) {
		this.sum = updateCRC32(this.sum, b, 0, b.length);
	}

	/**
	 * Updates this {@code Checksum} with the bytes from the specified buffer.
	 *
	 * This {@code Checksum} is updated using
	 * buffer.{@link java.nio.Buffer#remaining() remaining()} bytes starting at
	 * buffer.{@link java.nio.Buffer#position() position()}. Upon return, the
	 * buffer's position will be updated to its limit; its limit will remain
	 * unchanged.
	 *
	 * @param buffer the {@code ByteBuffer} to update the checksum with.
	 * @throws IllegalStateException if the {@code ByteBuffer} is in a faulty
	 * state.
	 */
	public void update(ByteBuffer buffer) throws IllegalStateException {
		int len = buffer.remaining();
		if (len < 0) {
			throw new IllegalStateException("Provided buffer has a negative number of bytes remaining: " + len);
		}
		this.sum = updateCRC32(this.sum, buffer, len);
	}

	@Override
	public long getValue() {
		return this.sum & 0xffffffffL;
	}

	/**
	 * Resets this {@code Checksum} to an initial value of {@code 1}.
	 */
	@Override
	public void reset() {
		this.sum = 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implNote Only the lower four bytes of the provided value will be used.
	 */
	@Override
	public OpenCRC32 reset(long value) {
		this.sum = (int) value;
		return this;
	}

	@Override
	public OpenCRC32 copy() {
		return new OpenCRC32(this.sum);
	}

	@Override
	@SuppressWarnings("CloneDeclaresCloneNotSupported")
	public OpenCRC32 clone() {
		try {
			return (OpenCRC32) super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new InternalError(ex);
		}
	}
}
