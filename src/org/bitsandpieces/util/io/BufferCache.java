/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

/**
 * Provides caching for byte-arrays with lengths equalling every positive power
 * of two (at most 2^30), as well as Integer.MAX_VALUE - 8 (limit on some VMs).
 * <p>
 * Requesting an array of a specific size returns an array with a length of the
 * next sufficiently large power of two, or the VM limit.
 *
 * @author Jan Kebernik
 */
final class BufferCache {

	// 64 kb, fairly large and rebuffering is expensive, but 
	// it also means that inflaters become faster because they can make use
	// of the "inflate_fast" sub-routine
	// and it means that small files will fit into a single buffer, 
	// which can speed things up a lot.
	// plus allocation costs amortize quickly without too much nesting/concurrency
	// so it's still cheap.
	private static final int DEFAULT_INDEX = 16;
	static final int DEFAULT_SIZE = 1 << DEFAULT_INDEX;
	private static final int MAX_LENGTH = Integer.MAX_VALUE - 8;	// limit on some VMs

	private static final Cache[] CACHE_TABLE = {
		new Cache(1),
		new Cache(1 << 1),
		new Cache(1 << 2),
		new Cache(1 << 3),
		new Cache(1 << 4),
		new Cache(1 << 5),
		new Cache(1 << 6),
		new Cache(1 << 7),
		new Cache(1 << 8),
		new Cache(1 << 9),
		new Cache(1 << 10),
		new Cache(1 << 11),
		new Cache(1 << 12),
		new Cache(1 << 13),
		new Cache(1 << 14),
		new Cache(1 << 15),
		new Cache(1 << 16),
		new Cache(1 << 17),
		new Cache(1 << 18),
		new Cache(1 << 19),
		new Cache(1 << 20),
		new Cache(1 << 21),
		new Cache(1 << 22),
		new Cache(1 << 23),
		new Cache(1 << 24),
		new Cache(1 << 25),
		new Cache(1 << 26),
		new Cache(1 << 27),
		new Cache(1 << 28),
		new Cache(1 << 29),
		new Cache(1 << 30)
	};

	private static final Cache DEFAULT_CACHE = CACHE_TABLE[DEFAULT_INDEX];
	private static final Cache MAX_CACHE = new Cache(MAX_LENGTH);

	private static final int MAX_POW = 1 << 30;

	private static final byte[] EMPTY = {};

	private static int bufIdx(int size) {
		// this is actually much faster than expected, probably because
		// numberOfLeadingZeros() gets special treatment by 
		// the VM or is somehow optimally pre-compiled. Either way, the exact 
		// same java code is much slower if inlined.
		return 32 - Integer.numberOfLeadingZeros(size - 1);
	}

	// requests a buffer of the default size
	public static byte[] requestBuffer() {
		return DEFAULT_CACHE.requestInstance();
	}

	// returns an array of at least "len"
	public static byte[] requestBuffer(int size) {
		if (size < 1) {
			if (size == 0) {
				return EMPTY;
			}
			throw new IllegalArgumentException("size < 0: " + size);
		}
		if (size > MAX_POW) {
			if (size > MAX_LENGTH) {
				throw new IllegalArgumentException("requested size exceeds VM limit (" + MAX_LENGTH + "): " + size);
			}
			return MAX_CACHE.requestInstance();
		}
		return CACHE_TABLE[bufIdx(size)].requestInstance();
	}

	// puts the specified array into the shared pool
	public static void releaseBuffer(byte[] buf) {
		if (buf != null) {
			if (buf.length == 0) {
				return;
			}
			if (buf.length > MAX_POW) {
				MAX_CACHE.releaseInstance(buf);
				return;
			}
			CACHE_TABLE[bufIdx(buf.length)].releaseInstance(buf);
		}
	}

	// simple impl
	private static final class Cache extends ObjectCache<byte[]> {

		public Cache(int size) {
			super(() -> new byte[size]);
		}
	}

	private BufferCache() {
	}
}
