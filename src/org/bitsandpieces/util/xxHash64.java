/*
 * Copyright (c) 2012-2014, Yann Collet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.bitsandpieces.util;

import java.util.zip.Checksum;

/**
 * Pure Java 64-bit implementation of Yann Collet's amazing
 * <a href="https://github.com/Cyan4973/xxHash">xxHash</a> algorithm, based on
 * Stephan Brumme's
 * <a href="http://create.stephan-brumme.com/xxhash/">simplified</a> version.
 * <p>
 * Obligatory warning: This is not a cryptographically secure hash.
 *
 * IMPORTANT: This implementation has not been tested against the reference
 * implementations as of yet. Results may vary!
 *
 * @implNote On an Intel Core i5 CPU @ 3.30 GHz using DDR3 RAM, this
 * implementation has a through-put of around 7,5 GB/s, which is not nearly as
 * fast as a fully optimized C based solution, but is about twice as fast as
 * CRC32 (as a performant baseline comparison) on the same machine. On a big
 * endian processor, the performance might suffer (maybe ~30% slower).
 * Performance is closely tied to the implementation specifics of the Endian
 * enum, specifically its usage of the java.misc.Unsafe class. If the target
 * environment, for whatever reason, does not have access to the Unsafe class,
 * the algorithm is expected to be up to four times slower (making it slower
 * than CRC32 again, but still practical as a 64-bit checksum).
 *
 * @author Jan Kebernik
 */
public class xxHash64 implements Checksum {

	// two's-complement arithmetic is sign-agnostic
	private static final long PRIME1 = -7046029288634856825L;	// unsigned: 11400714785074694791
	private static final long PRIME2 = -4417276706812531889L;	// unsigned: 14029467366897019727
	private static final long PRIME3 = 1609587929392839161L;	// unsigned:  1609587929392839161
	private static final long PRIME4 = -8796714831421723037L;	// unsigned:  9650029242287828579
	private static final long PRIME5 = 2870177450012600261L;	// unsigned:  2870177450012600261

	private static final int MAX_BUFFER_SIZE = 32;
	private static final int MAX_BUFFER_IDX = MAX_BUFFER_SIZE - 1;
	private static final long MAX_BUFFER_SIZE_COMP = ((long) MAX_BUFFER_SIZE) + Long.MIN_VALUE;

	private static final Endian ENDIAN = Endian.LITTLE;

	private long state0, state1, state2, state3;
	private final byte[] buffer = new byte[MAX_BUFFER_SIZE];
	private int bufferSize;
	private long totalLength;	// there is a risk of overflow. theoretically.

	public xxHash64() {
		_reset(0L);
	}

	public xxHash64(long seed) {
		_reset(seed);
	}

	@Override
	public void reset() {
		_reset(0L);
	}

	public void reset(long seed) {
		_reset(seed);
	}

	private void _reset(long seed) {
		this.state0 = seed + PRIME1 + PRIME2;
		this.state1 = seed + PRIME2;
		this.state2 = seed;
		this.state3 = seed - PRIME1;
		this.bufferSize = 0;
		this.totalLength = 0L;
	}

	@Override
	public void update(int b) {
		this.totalLength++;
		int bs = this.bufferSize;
		this.buffer[bs] = (byte) b;						// always store new byte in buffer
		this.bufferSize = (bs + 1) & MAX_BUFFER_IDX;	// and update its size (0..31)
		if (bs == MAX_BUFFER_IDX) {
			// process 32 (4x8) bytes in parallel
			this.state0 = processSingle(this.state0, ENDIAN._getLong(this.buffer, 0));
			this.state1 = processSingle(this.state1, ENDIAN._getLong(this.buffer, 8));
			this.state2 = processSingle(this.state2, ENDIAN._getLong(this.buffer, 16));
			this.state3 = processSingle(this.state3, ENDIAN._getLong(this.buffer, 24));
		}
	}

	@Override
	public void update(byte[] buf, int off, int len) {
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new IndexOutOfBoundsException();
		}
		if (len == 0) {
			return;
		}
		this.totalLength += len;
		int bs = this.bufferSize;
		if (bs + len < MAX_BUFFER_SIZE) {
			// new bytes fit into buffer (still not full)
			System.arraycopy(buf, off, this.buffer, bs, len);
			this.bufferSize = bs + len;
			return;
		}
		int max = off + len;
		long s0 = this.state0, s1 = this.state1, s2 = this.state2, s3 = this.state3;
		if (bs != 0) {
			// fill buffer if partially full
			int n = MAX_BUFFER_SIZE - bs;
			if (n > 0) {
				System.arraycopy(buf, off, this.buffer, bs, n);
				off += n;
			}
			// process 32 (4x8) bytes in parallel, from buffer
			s0 = processSingle(s0, ENDIAN._getLong(this.buffer, 0));
			s1 = processSingle(s1, ENDIAN._getLong(this.buffer, 8));
			s2 = processSingle(s2, ENDIAN._getLong(this.buffer, 16));
			s3 = processSingle(s3, ENDIAN._getLong(this.buffer, 24));
		}
		// process 32 (4x8) bytes in parallel, from source
		for (int m = max - MAX_BUFFER_IDX; off < m; off += MAX_BUFFER_SIZE) {
			s0 = processSingle(s0, ENDIAN._getLong(buf, off));
			s1 = processSingle(s1, ENDIAN._getLong(buf, off + 8));
			s2 = processSingle(s2, ENDIAN._getLong(buf, off + 16));
			s3 = processSingle(s3, ENDIAN._getLong(buf, off + 24));
		}
		this.state0 = s0;
		this.state1 = s1;
		this.state2 = s2;
		this.state3 = s3;
		// fill buffer with remainder
		System.arraycopy(buf, off, this.buffer, 0, this.bufferSize = max - off);
	}

	@Override
	public long getValue() {
		long result;
		long s0 = this.state0, s1 = this.state1, s2 = this.state2, s3 = this.state3;
		long total = this.totalLength;
		// Unsigned comparison. 
		// Total could still overflow, but would require more than 2^64-1 bytes
		// to be processed without resetting. Never going to happen.
		if (total + Long.MIN_VALUE >= MAX_BUFFER_SIZE_COMP) {
			result = Long.rotateLeft(s0, 1)
					+ Long.rotateLeft(s1, 7)
					+ Long.rotateLeft(s2, 12)
					+ Long.rotateLeft(s3, 18);
			result = (result ^ processSingle(0L, s0)) * PRIME1 + PRIME4;
			result = (result ^ processSingle(0L, s1)) * PRIME1 + PRIME4;
			result = (result ^ processSingle(0L, s2)) * PRIME1 + PRIME4;
			result = (result ^ processSingle(0L, s3)) * PRIME1 + PRIME4;
		} else {
			result = s2 + PRIME5;
		}
		result += total;
		int bs = this.bufferSize;
		int r = bs & 7;	// remaining 0..7 bytes
		int i = 0;
		for (int m = bs - r; i < m; i += 8) {
			result = Long.rotateLeft(result ^ processSingle(0, ENDIAN._getLong(this.buffer, i)), 27) * PRIME1 + PRIME4;
		}
		// do remainder
		if (r >= 4) {
			result = Long.rotateLeft(result ^ (ENDIAN._getInt(this.buffer, i) & 0xffffffffL) * PRIME1, 23) * PRIME2 + PRIME3;
			i += 4;
		}
		for (; i < bs; i++) {
			result = Long.rotateLeft(result ^ (this.buffer[i] & 0xffL) * PRIME5, 11) * PRIME1;
		}
		// mix result
		result ^= result >>> 33;
		result *= PRIME2;
		result ^= result >>> 29;
		result *= PRIME3;
		result ^= result >>> 32;
		return result;
	}

	private static long processSingle(long v, long n) {
		return Long.rotateLeft(v + n * PRIME2, 31) * PRIME1;
	}
}
