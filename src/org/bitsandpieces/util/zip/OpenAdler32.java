/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.zip;

import java.nio.ByteBuffer;

/**
 * Pure-Java implementation of {@link java.util.zip.Adler32}.
 * <p>
 * For arrays, this implementation is competitive with the JDK version in terms
 * of throughput. For {@link ByteBuffer}s, the throughput of the JDK vesions is
 * significantly higher.
 * </p>
 * <p>
 * Access to {@code OpenAdler32}'s internal value can be granted
 * non-reflectively, via object-inheritence.
 * </p>
 *
 * @author Jan Kebernik
 */
public class OpenAdler32 implements OpenChecksum, Cloneable {

	private static final int BASE = 65521;		// largest prime smaller than 65536
	private static final long BASEL = BASE;		// largest prime smaller than 65536 (to avoid casting costs)
	/*
	 * Largest n such that 255n(n+1)/2+(n+1)(BASE-1) <= 2^31-1, which is also
	 * a multiple of 16. Using the default value of 5552 would result in
	 * inconsistent behaviour due to Java's signed modulo arithmetic.
	 */
	private static final int NMAX = 3840;

	// 256 MiB
	private static final int NMAXL = 1 << 28;

	/**
	 * Updates the speficied sum with the specified bytes from the provided byte
	 * array. This implementation is about 80% as fast as the (native) JDK
	 * version. Further improvements are unlikely at this point.
	 */
	static final int updateAdler32(int sum, byte[] b, int off, int len) {
		if (len < 16) {
			// using at most one modulo operation
			int sum1 = sum >>> 16;
			int sum0 = sum & 0xffff;
			if (len == 1) {
				return _update(sum1, sum0, b[off]);
			}
			for (int m = off + len; off < m; off++) {
				sum1 += (sum0 += b[off] & 0xff);
			}
			if (sum0 >= BASE) {
				sum0 -= BASE;
			}
			return ((sum1 % BASE) << 16) | sum0;
		}
		if (len < NMAX) {
			// Using 32-bits and only two modulo operations.
			// The JIT will automatically unroll int-based loops if necessary.
			int sum1 = sum >>> 16;
			int sum0 = sum & 0xffff;
			for (int m = off + len; off < m; off++) {
				sum1 += (sum0 += b[off] & 0xff);
			}
			return ((sum1 % BASE) << 16) | (sum0 % BASE);
		}
		// Using 64-bits with unrolling is significantly faster for long runs.
		long sum1 = sum >>> 16;
		long sum0 = sum & 0xffff;
		// Do the first 0-7 unaligned bytes first
		for (int m = off + (len & 7); off < m; off++) {
			sum1 += (sum0 += b[off] & 0xffL);
		}
		// NMAXL is well below the overflow edge, so the potential 
		// extra 7 bytes won't matter.
		// All loops will be unrolled from here on out, because 
		// the JIT refuses to do so on its own for long-based loops.
		// Unrolling gains us a speedup of almost 50%.
		len -= len & 7;
		while (len > NMAXL) {
			// Using only two modulo operations for every 256 MiB of input.
			len -= NMAXL;
			for (int m = off + NMAXL; off < m; off += 8) {
				sum1 += (sum0 += b[off + 0] & 0xffL);
				sum1 += (sum0 += b[off + 1] & 0xffL);
				sum1 += (sum0 += b[off + 2] & 0xffL);
				sum1 += (sum0 += b[off + 3] & 0xffL);
				sum1 += (sum0 += b[off + 4] & 0xffL);
				sum1 += (sum0 += b[off + 5] & 0xffL);
				sum1 += (sum0 += b[off + 6] & 0xffL);
				sum1 += (sum0 += b[off + 7] & 0xffL);
			}
			sum1 %= BASEL;
			sum0 %= BASEL;
		}
		// Remaining bytes.
		for (int m = off + len; off < m; off += 8) {
			sum1 += (sum0 += b[off + 0] & 0xffL);
			sum1 += (sum0 += b[off + 1] & 0xffL);
			sum1 += (sum0 += b[off + 2] & 0xffL);
			sum1 += (sum0 += b[off + 3] & 0xffL);
			sum1 += (sum0 += b[off + 4] & 0xffL);
			sum1 += (sum0 += b[off + 5] & 0xffL);
			sum1 += (sum0 += b[off + 6] & 0xffL);
			sum1 += (sum0 += b[off + 7] & 0xffL);
		}
		return (int) (((sum1 % BASEL) << 16) | (sum0 % BASEL));
	}

	/**
	 * Updates the speficied sum with the specified bytes from the provided
	 * buffer. The buffer position is updated. Because loop unrolling is less
	 * effective for non-array types, this version is much slower for direct
	 * buffers (no underlying array).
	 */
	static final int updateAdler32(int sum, ByteBuffer buffer, int len) {
		int off = buffer.position();
		if (off < 0) {
			throw new IllegalStateException("Provided buffer has a negative position: " + off);
		}
		buffer.position(off + len);
		if (buffer.hasArray()) {
			// Use the much faster array version, if possible.
			return updateAdler32(sum, buffer.array(), buffer.arrayOffset() + off, len);
		}
		if (len < 16) {
			int sum1 = sum >>> 16;
			int sum0 = sum & 0xffff;
			if (len == 1) {
				return _update(sum1, sum0, buffer.get(off));
			}
			for (int m = off + len; off < m; off++) {
				sum1 += (sum0 += buffer.get(off) & 0xff);
			}
			if (sum0 >= BASE) {
				sum0 -= BASE;
			}
			return ((sum1 % BASE) << 16) | sum0;
		}
		if (len < NMAX) {
			int sum1 = sum >>> 16;
			int sum0 = sum & 0xffff;
			for (int m = off + len; off < m; off++) {
				sum1 += (sum0 += buffer.get(off) & 0xff);
			}
			return ((sum1 % BASE) << 16) | (sum0 % BASE);
		}
		long sum1 = sum >>> 16;
		long sum0 = sum & 0xffff;
		for (int m = off + (len & 7); off < m; off++) {
			sum1 += (sum0 += buffer.get(off) & 0xffL);
		}
		len -= len & 7;
		while (len > NMAXL) {
			len -= NMAXL;
			for (int m = off + NMAXL; off < m; off += 8) {
				sum1 += (sum0 += buffer.get(off) & 0xffL);
				sum1 += (sum0 += buffer.get(off + 1) & 0xffL);
				sum1 += (sum0 += buffer.get(off + 2) & 0xffL);
				sum1 += (sum0 += buffer.get(off + 3) & 0xffL);
				sum1 += (sum0 += buffer.get(off + 4) & 0xffL);
				sum1 += (sum0 += buffer.get(off + 5) & 0xffL);
				sum1 += (sum0 += buffer.get(off + 6) & 0xffL);
				sum1 += (sum0 += buffer.get(off + 7) & 0xffL);
			}
			sum1 %= BASEL;
			sum0 %= BASEL;
		}
		for (int m = off + len; off < m; off += 8) {
			sum1 += (sum0 += buffer.get(off) & 0xffL);
			sum1 += (sum0 += buffer.get(off + 1) & 0xffL);
			sum1 += (sum0 += buffer.get(off + 2) & 0xffL);
			sum1 += (sum0 += buffer.get(off + 3) & 0xffL);
			sum1 += (sum0 += buffer.get(off + 4) & 0xffL);
			sum1 += (sum0 += buffer.get(off + 5) & 0xffL);
			sum1 += (sum0 += buffer.get(off + 6) & 0xffL);
			sum1 += (sum0 += buffer.get(off + 7) & 0xffL);
		}
		return (int) (((sum1 % BASEL) << 16) | (sum0 % BASEL));
	}

	/**
	 * Updates the speficied sum with the specified byte.
	 */
	static final int updateAdler32(int sum, int b) {
		return _update(sum >>> 16, sum & 0xffff, b);
	}

	private static int _update(int sum1, int sum0, int b) {
		sum1 = _update(sum1, sum0 = _update(sum0, b & 0xff));
		return (sum1 << 16) | sum0;
	}

	private static int _update(int s, int b) {
		if ((s += b) >= BASE) {
			s -= BASE;
		}
		return s;
	}

	/**
	 * The internal checksum. Access to this field may be granted
	 * non-reflectively by sub-classing OpenAdler32.
	 */
	protected int sum;

	/**
	 * Creates a new {@code OpenAdler32} object.
	 */
	public OpenAdler32() {
		this(1);
	}

	/**
	 * Creates a new {@code OpenAdler32} object with a pre-determined checksum.
	 *
	 * @param sum the pre-determined checksum that this {@code OpenAdler32}
	 * starts out with.
	 */
	protected OpenAdler32(int sum) {
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
		this.sum = updateAdler32(this.sum, b);
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
		this.sum = updateAdler32(this.sum, b, off, len);
	}

	/**
	 * Updates this {@code Checksum} with the specified array of bytes.
	 *
	 * @param b the byte array to update the checksum with.
	 */
	public void update(byte[] b) {
		this.sum = updateAdler32(this.sum, b, 0, b.length);
	}

	/**
	 * Updates this {@code Checksum} with the bytes from the specified buffer.
	 *
	 * This {@code Checksum} is updated using
	 * {@link java.nio.Buffer#remaining() Buffer#remaining()} bytes starting at
	 * {@link java.nio.Buffer#position() Buffer#position()}. Upon return, the
	 * buffer's position will be updated to its limit; its limit will remain
	 * unchanged.
	 *
	 * @param buffer the {@code ByteBuffer} to update the checksum with.
	 * @throws IllegalStateException if the {@code ByteBuffer} is in a faulty
	 * state.
	 */
	public void update(ByteBuffer buffer) {
		int len = buffer.remaining();
		if (len < 0) {
			throw new IllegalStateException("Provided buffer has a negative number of bytes remaining: " + len);
		}
		this.sum = updateAdler32(this.sum, buffer, len);
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
		this.sum = 1;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implNote Only the lower four bytes of the provided value will be used.
	 */
	@Override
	public OpenAdler32 reset(long value) {
		this.sum = (int) value;
		return this;
	}

	@Override
	public OpenAdler32 copy() {
		return new OpenAdler32(this.sum);
	}

	@Override
	@SuppressWarnings("CloneDeclaresCloneNotSupported")
	public OpenAdler32 clone() {
		try {
			return (OpenAdler32) super.clone();
		} catch (CloneNotSupportedException ex) {
			// Can never happen.
			throw new InternalError(ex);
		}
	}
}
