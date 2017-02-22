/*
 * zlib.h -- interface of the 'zlib' general purpose compression library
 * version 1.2.11, January 15th, 2017
 *
 * Copyright (C) 1995-2017 Jean-loup Gailly and Mark Adler
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 * claim that you wrote the original software. If you use this software
 * in a product, an acknowledgment in the product documentation would be
 * appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 *
 * Jean-loup Gailly        Mark Adler
 * jloup@gzip.org          madler@alumni.caltech.edu
 *
 *
 * The data format used by the zlib library is described by RFCs (Request for
 * Comments) 1950 to 1952 in the files http://tools.ietf.org/html/rfc1950
 * (zlib format), rfc1951 (deflate format) and rfc1952 (gzip format).
 */
package org.bitsandpieces.util.zip;

import java.util.zip.DataFormatException;

/**
 * Somewhat optimized pure-Java implementation of a
 * <a href="http://www.zlib.net/">ZLib</a> Inflater intended as a simple drop-in
 * for the {@link java.util.zip.Inflater JDK version}. Unlike the JDK version,
 * if {@code nowrap} is {@code false} it will detect and parse GZIP-formatted
 * data automatically, without requiring a {@link java.util.zip.GZIPInputStream}
 * wrapper.
 * <p>
 * Another benefit of this class over the JDK version is its ability to capture
 * and restore an inflater's internal {@link State} during inflation. This makes
 * it feasable to implement quasi-random access over any ZLib compressed
 * data-stream, by storing captured states when first inflating over fixed
 * intervals of sufficient length.
 * </p>
 * <p>
 * Performance is very close to the JDK version and even superior in some cases.
 * </p>
 * <p>
 * This class was mostly transcribed and refactored from Mark Adler's reference
 * implementation (as of version 1.2.8 of ZLib).
 * </p>
 *
 * @author Jan Kebernik
 */
public class OpenInflater {

	private static final int MAXBITS = 15;
	private static final int WSIZE = 1 << MAXBITS;	// state->wbits always ends up 15, anyway.
	private static final int ENOUGH_LENS = 852;
	private static final int ENOUGH_DISTS = 592;
	private static final int ENOUGH = ENOUGH_LENS + ENOUGH_DISTS;
	private static final byte[] EMPTY = new byte[0];
	private static final int[] ZEROS = new int[MAXBITS + 1];	// TODO this is so small that a loop might be faster

	private static final int[] ORDER = {
		16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15
	};

	private static final int[] LBASE = {
		0x0003, 0x0004, 0x0005, 0x0006, 0x0007, 0x0008, 0x0009, 0x000a,
		0x000b, 0x000d, 0x000f, 0x0011, 0x0013, 0x0017, 0x001b, 0x001f,
		0x0023, 0x002b, 0x0033, 0x003b, 0x0043, 0x0053, 0x0063, 0x0073,
		0x0083, 0x00a3, 0x00c3, 0x00e3, 0x0102, 0x0000, 0x0000
	};

	private static final int[] LEXT = {
		0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10,
		0x11, 0x11, 0x11, 0x11, 0x12, 0x12, 0x12, 0x12,
		0x13, 0x13, 0x13, 0x13, 0x14, 0x14, 0x14, 0x14,
		0x15, 0x15, 0x15, 0x15, 0x10, 0x48, 0x4e
	};

	private static final int[] DBASE = {
		0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0007, 0x0009, 0x000d,
		0x0011, 0x0019, 0x0021, 0x0031, 0x0041, 0x0061, 0x0081, 0x00c1,
		0x0101, 0x0181, 0x0201, 0x0301, 0x0401, 0x0601, 0x0801, 0x0c01,
		0x1001, 0x1801, 0x2001, 0x3001, 0x4001, 0x6001, 0x0000, 0x0000
	};

	private static final int[] DEXT = {
		0x10, 0x10, 0x10, 0x10, 0x11, 0x11, 0x12, 0x12,
		0x13, 0x13, 0x14, 0x14, 0x15, 0x15, 0x16, 0x16,
		0x17, 0x17, 0x18, 0x18, 0x19, 0x19, 0x1a, 0x1a,
		0x1b, 0x1b, 0x1c, 0x1c, 0x1d, 0x1d, 0x40, 0x40
	};

	// Every entry is a complete code (1 byte 'op', 1 byte 'bits', 2 bytes 'val').
	// This way of doing it limits both the space requirement and the number
	// of (in Java) relatively expensive array accesses to a minimum.
	// However, every value must be (dis)assembled via bit-wise operations,
	// the cost of which is mitigated by using effecitve memoization
	private static final int[] LENFIX = {
		0x00000760, 0x00500800, 0x00100800, 0x00730814, 0x001f0712, 0x00700800, 0x00300800, 0x00c00900,
		0x000a0710, 0x00600800, 0x00200800, 0x00a00900, 0x00000800, 0x00800800, 0x00400800, 0x00e00900,
		0x00060710, 0x00580800, 0x00180800, 0x00900900, 0x003b0713, 0x00780800, 0x00380800, 0x00d00900,
		0x00110711, 0x00680800, 0x00280800, 0x00b00900, 0x00080800, 0x00880800, 0x00480800, 0x00f00900,
		0x00040710, 0x00540800, 0x00140800, 0x00e30815, 0x002b0713, 0x00740800, 0x00340800, 0x00c80900,
		0x000d0711, 0x00640800, 0x00240800, 0x00a80900, 0x00040800, 0x00840800, 0x00440800, 0x00e80900,
		0x00080710, 0x005c0800, 0x001c0800, 0x00980900, 0x00530714, 0x007c0800, 0x003c0800, 0x00d80900,
		0x00170712, 0x006c0800, 0x002c0800, 0x00b80900, 0x000c0800, 0x008c0800, 0x004c0800, 0x00f80900,
		0x00030710, 0x00520800, 0x00120800, 0x00a30815, 0x00230713, 0x00720800, 0x00320800, 0x00c40900,
		0x000b0711, 0x00620800, 0x00220800, 0x00a40900, 0x00020800, 0x00820800, 0x00420800, 0x00e40900,
		0x00070710, 0x005a0800, 0x001a0800, 0x00940900, 0x00430714, 0x007a0800, 0x003a0800, 0x00d40900,
		0x00130712, 0x006a0800, 0x002a0800, 0x00b40900, 0x000a0800, 0x008a0800, 0x004a0800, 0x00f40900,
		0x00050710, 0x00560800, 0x00160800, 0x00000840, 0x00330713, 0x00760800, 0x00360800, 0x00cc0900,
		0x000f0711, 0x00660800, 0x00260800, 0x00ac0900, 0x00060800, 0x00860800, 0x00460800, 0x00ec0900,
		0x00090710, 0x005e0800, 0x001e0800, 0x009c0900, 0x00630714, 0x007e0800, 0x003e0800, 0x00dc0900,
		0x001b0712, 0x006e0800, 0x002e0800, 0x00bc0900, 0x000e0800, 0x008e0800, 0x004e0800, 0x00fc0900,
		0x00000760, 0x00510800, 0x00110800, 0x00830815, 0x001f0712, 0x00710800, 0x00310800, 0x00c20900,
		0x000a0710, 0x00610800, 0x00210800, 0x00a20900, 0x00010800, 0x00810800, 0x00410800, 0x00e20900,
		0x00060710, 0x00590800, 0x00190800, 0x00920900, 0x003b0713, 0x00790800, 0x00390800, 0x00d20900,
		0x00110711, 0x00690800, 0x00290800, 0x00b20900, 0x00090800, 0x00890800, 0x00490800, 0x00f20900,
		0x00040710, 0x00550800, 0x00150800, 0x01020810, 0x002b0713, 0x00750800, 0x00350800, 0x00ca0900,
		0x000d0711, 0x00650800, 0x00250800, 0x00aa0900, 0x00050800, 0x00850800, 0x00450800, 0x00ea0900,
		0x00080710, 0x005d0800, 0x001d0800, 0x009a0900, 0x00530714, 0x007d0800, 0x003d0800, 0x00da0900,
		0x00170712, 0x006d0800, 0x002d0800, 0x00ba0900, 0x000d0800, 0x008d0800, 0x004d0800, 0x00fa0900,
		0x00030710, 0x00530800, 0x00130800, 0x00c30815, 0x00230713, 0x00730800, 0x00330800, 0x00c60900,
		0x000b0711, 0x00630800, 0x00230800, 0x00a60900, 0x00030800, 0x00830800, 0x00430800, 0x00e60900,
		0x00070710, 0x005b0800, 0x001b0800, 0x00960900, 0x00430714, 0x007b0800, 0x003b0800, 0x00d60900,
		0x00130712, 0x006b0800, 0x002b0800, 0x00b60900, 0x000b0800, 0x008b0800, 0x004b0800, 0x00f60900,
		0x00050710, 0x00570800, 0x00170800, 0x00000840, 0x00330713, 0x00770800, 0x00370800, 0x00ce0900,
		0x000f0711, 0x00670800, 0x00270800, 0x00ae0900, 0x00070800, 0x00870800, 0x00470800, 0x00ee0900,
		0x00090710, 0x005f0800, 0x001f0800, 0x009e0900, 0x00630714, 0x007f0800, 0x003f0800, 0x00de0900,
		0x001b0712, 0x006f0800, 0x002f0800, 0x00be0900, 0x000f0800, 0x008f0800, 0x004f0800, 0x00fe0900,
		0x00000760, 0x00500800, 0x00100800, 0x00730814, 0x001f0712, 0x00700800, 0x00300800, 0x00c10900,
		0x000a0710, 0x00600800, 0x00200800, 0x00a10900, 0x00000800, 0x00800800, 0x00400800, 0x00e10900,
		0x00060710, 0x00580800, 0x00180800, 0x00910900, 0x003b0713, 0x00780800, 0x00380800, 0x00d10900,
		0x00110711, 0x00680800, 0x00280800, 0x00b10900, 0x00080800, 0x00880800, 0x00480800, 0x00f10900,
		0x00040710, 0x00540800, 0x00140800, 0x00e30815, 0x002b0713, 0x00740800, 0x00340800, 0x00c90900,
		0x000d0711, 0x00640800, 0x00240800, 0x00a90900, 0x00040800, 0x00840800, 0x00440800, 0x00e90900,
		0x00080710, 0x005c0800, 0x001c0800, 0x00990900, 0x00530714, 0x007c0800, 0x003c0800, 0x00d90900,
		0x00170712, 0x006c0800, 0x002c0800, 0x00b90900, 0x000c0800, 0x008c0800, 0x004c0800, 0x00f90900,
		0x00030710, 0x00520800, 0x00120800, 0x00a30815, 0x00230713, 0x00720800, 0x00320800, 0x00c50900,
		0x000b0711, 0x00620800, 0x00220800, 0x00a50900, 0x00020800, 0x00820800, 0x00420800, 0x00e50900,
		0x00070710, 0x005a0800, 0x001a0800, 0x00950900, 0x00430714, 0x007a0800, 0x003a0800, 0x00d50900,
		0x00130712, 0x006a0800, 0x002a0800, 0x00b50900, 0x000a0800, 0x008a0800, 0x004a0800, 0x00f50900,
		0x00050710, 0x00560800, 0x00160800, 0x00000840, 0x00330713, 0x00760800, 0x00360800, 0x00cd0900,
		0x000f0711, 0x00660800, 0x00260800, 0x00ad0900, 0x00060800, 0x00860800, 0x00460800, 0x00ed0900,
		0x00090710, 0x005e0800, 0x001e0800, 0x009d0900, 0x00630714, 0x007e0800, 0x003e0800, 0x00dd0900,
		0x001b0712, 0x006e0800, 0x002e0800, 0x00bd0900, 0x000e0800, 0x008e0800, 0x004e0800, 0x00fd0900,
		0x00000760, 0x00510800, 0x00110800, 0x00830815, 0x001f0712, 0x00710800, 0x00310800, 0x00c30900,
		0x000a0710, 0x00610800, 0x00210800, 0x00a30900, 0x00010800, 0x00810800, 0x00410800, 0x00e30900,
		0x00060710, 0x00590800, 0x00190800, 0x00930900, 0x003b0713, 0x00790800, 0x00390800, 0x00d30900,
		0x00110711, 0x00690800, 0x00290800, 0x00b30900, 0x00090800, 0x00890800, 0x00490800, 0x00f30900,
		0x00040710, 0x00550800, 0x00150800, 0x01020810, 0x002b0713, 0x00750800, 0x00350800, 0x00cb0900,
		0x000d0711, 0x00650800, 0x00250800, 0x00ab0900, 0x00050800, 0x00850800, 0x00450800, 0x00eb0900,
		0x00080710, 0x005d0800, 0x001d0800, 0x009b0900, 0x00530714, 0x007d0800, 0x003d0800, 0x00db0900,
		0x00170712, 0x006d0800, 0x002d0800, 0x00bb0900, 0x000d0800, 0x008d0800, 0x004d0800, 0x00fb0900,
		0x00030710, 0x00530800, 0x00130800, 0x00c30815, 0x00230713, 0x00730800, 0x00330800, 0x00c70900,
		0x000b0711, 0x00630800, 0x00230800, 0x00a70900, 0x00030800, 0x00830800, 0x00430800, 0x00e70900,
		0x00070710, 0x005b0800, 0x001b0800, 0x00970900, 0x00430714, 0x007b0800, 0x003b0800, 0x00d70900,
		0x00130712, 0x006b0800, 0x002b0800, 0x00b70900, 0x000b0800, 0x008b0800, 0x004b0800, 0x00f70900,
		0x00050710, 0x00570800, 0x00170800, 0x00000840, 0x00330713, 0x00770800, 0x00370800, 0x00cf0900,
		0x000f0711, 0x00670800, 0x00270800, 0x00af0900, 0x00070800, 0x00870800, 0x00470800, 0x00ef0900,
		0x00090710, 0x005f0800, 0x001f0800, 0x009f0900, 0x00630714, 0x007f0800, 0x003f0800, 0x00df0900,
		0x001b0712, 0x006f0800, 0x002f0800, 0x00bf0900, 0x000f0800, 0x008f0800, 0x004f0800, 0x00ff0900
	};

	private static final int[] DISTFIX = {
		0x00010510, 0x01010517, 0x00110513, 0x1001051b, 0x00050511, 0x04010519, 0x00410515, 0x4001051d,
		0x00030510, 0x02010518, 0x00210514, 0x2001051c, 0x00090512, 0x0801051a, 0x00810516, 0x00000540,
		0x00020510, 0x01810517, 0x00190513, 0x1801051b, 0x00070511, 0x06010519, 0x00610515, 0x6001051d,
		0x00040510, 0x03010518, 0x00310514, 0x3001051c, 0x000d0512, 0x0c01051a, 0x00c10516, 0x00000540
	};

	private static final int HEAD = 0, FLAGS = 1,
			TIME = 2, OS = 3, EXLEN = 4, EXTRA = 5,
			NAME = 6, COMMENT = 7, HCRC = 8, DICTID = 9,
			DICT = 10, TYPEDO = 11, STORED = 12, COPY = 13,
			TABLE = 14, LENLENS = 15, CODELENS = 16, LEN = 17,
			LENEXT = 18, DIST = 19, DISTEXT = 20, MATCH = 21,
			LIT = 22, CHECK = 23, LENGTH = 24, DONE = 25,
			BAD = 26;

	// these are ZLIB compiler flags dealing with invalid distance values
	private final boolean pkzipBugWorkaround;
	private final boolean inflateStrict;
	private final boolean inflateAllowInvalidDistanceToofarArrr;

	// this value also appears to deal with invalid distance values
	private final boolean sane;

	private final boolean wrap;
	private final boolean nativeCRC32;

	private OpenChecksum adler;
	private int mode;
	private int hold, bits, whave, wnext;
	private boolean lastBlock;
	private int flags;
	private final byte[] window;
	private int ncode, nlen, ndist, have, length, offset, extraBits, lenbits,
			distbits, nextInIndex, availIn;
	private final int[] lens, codes, work, offs, count;
	private int[] lencode;
	private int[] distcode;
	private int distcodeIndex;
	private long totalIn, totalOut;
	private byte[] nextIn = EMPTY;

	public OpenInflater() {
		this(false);
	}

	public OpenInflater(boolean nowrap) {
		this(nowrap, true, false, true, false, true);
	}

	public OpenInflater(boolean nowrap, boolean nativeChecksums) {
		this(nowrap, nativeChecksums, false, true, false, true);
	}

	/**
	 * Allows for setting some of the more subtle ZLIB compiler flags. Only
	 * recommended to be used by experts.
	 *
	 * @param nowrap wether to support GZIP compatible compression.
	 * @param nativeCRC32 wether to initially use the native CRC32
	 * implementation. If {@code false} or if reflection is disabled, this
	 * inflater will fall back on a pure-Java implementation.
	 * @param pkzipBugWorkaround please consult the original ZLIB source and
	 * documentation for details.
	 * @param inflateStrict please consult the original ZLIB source and
	 * documentation for details.
	 * @param inflateAllowInvalidDistanceToofarArrr please consult the original
	 * ZLIB source and documentation for details.
	 * @param sane please consult the original ZLIB source and documentation for
	 * details.
	 */
	public OpenInflater(boolean nowrap, boolean nativeCRC32, boolean pkzipBugWorkaround, boolean inflateStrict, boolean inflateAllowInvalidDistanceToofarArrr, boolean sane) {
		this.pkzipBugWorkaround = pkzipBugWorkaround;
		this.inflateStrict = inflateStrict;
		this.inflateAllowInvalidDistanceToofarArrr = inflateAllowInvalidDistanceToofarArrr;
		this.sane = sane;
		wrap = !nowrap;
		// adler = 1;	// not necessary
		mode = HEAD;
		lastBlock = false;
		//wsize = 1 << MAXBITS;
		window = new byte[WSIZE];
		lens = new int[320];
		codes = new int[ENOUGH];
		work = new int[288];
		offs = new int[MAXBITS + 1];
		count = new int[MAXBITS + 1];
		lencode = LENFIX;
		distcode = DISTFIX;
		totalIn = 0L;
		totalOut = 0L;
		this.nativeCRC32 = nativeCRC32;
		adler = new OpenAdler32();
	}

	private OpenInflater(State s) {
		nativeCRC32 = s.nativeCRC32;
		pkzipBugWorkaround = s.pkzipBugWorkaround;
		inflateStrict = s.inflateStrict;
		inflateAllowInvalidDistanceToofarArrr = s.inflateAllowInvalidDistanceToofarArrr;
		sane = s.sane;
		wrap = s.wrap;
		adler = s.adler.copy();
		//adler = new OpenAdler32().reset(s.adler);
		mode = s.mode;
		flags = s.flags;
		hold = s.hold;
		bits = s.bits;
		lastBlock = s.lastBlock;
		//wsize = 1 << MAXBITS;
		whave = s.window.length;
		wnext = s.wnext;
		//if (whave == wsize) {
		//	window = s.window.clone();
		//} else {
		window = new byte[WSIZE];
		System.arraycopy(s.window, 0, window, 0, whave);
		//}
		ncode = s.ncode;
		nlen = s.nlen;
		ndist = s.ndist;
		have = s.have;
		length = s.length;
		offset = s.offset;
		extraBits = s.extraBits;
		lenbits = s.lenbits;
		distbits = s.distbits;
		lens = s.lens.clone();
		codes = s.codes.clone();
		lencode = s.lencode == null ? codes : s.lencode;
		distcode = s.distcode == null ? codes : s.distcode;
		work = new int[288];
		offs = new int[MAXBITS + 1];
		count = new int[MAXBITS + 1];
		distcodeIndex = s.distcodeIndex;
		totalIn = s.totalIn;
		totalOut = s.totalOut;
	}

	/**
	 * Captures this {@code OpenInflater}'s internal state. Not thread-safe.
	 *
	 * @return a new {@code State} describing this {@code OpenInflater}.
	 */
	public State capture() {
		return new State(this);
	}

	/**
	 * Stores a {@code OpenInflater}'s internal state. Any number of equivalent
	 * but distinct {@code OpenInflater}s can be re-created from any one
	 * {@code State}. Note that these objects are immutable and as such suitable
	 * for concurrent use. However, capturing a state is only thread-safe when
	 * explicity synchronized.
	 * <p>
	 * Note that each {@code State} is expected to occupy around 32KB of memory.
	 * </p>
	 */
	public static final class State {

		private final boolean nativeCRC32;
		private final boolean pkzipBugWorkaround, inflateStrict,
				inflateAllowInvalidDistanceToofarArrr, sane,
				wrap, lastBlock;
		private final int mode;
		private final int flags;
		private final OpenChecksum adler;
		private final int hold, bits,
				wnext, ncode, nlen, ndist,
				have, length, offset,
				extraBits, lenbits,
				distbits, distcodeIndex;
		private final byte[] window;
		private final int[] lens, codes;
		private final int[] lencode;
		private final int[] distcode;
		private final long totalIn, totalOut;

		// state does reference the inflater's current input buffer
		private State(OpenInflater inf) {
			nativeCRC32 = inf.nativeCRC32;
			pkzipBugWorkaround = inf.pkzipBugWorkaround;
			inflateStrict = inf.inflateStrict;
			inflateAllowInvalidDistanceToofarArrr = inf.inflateAllowInvalidDistanceToofarArrr;
			sane = inf.sane;
			wrap = inf.wrap;
			adler = inf.adler.copy();

			//adler = (int) inf.adler.getValue();
			mode = inf.mode;
			flags = inf.flags;
			hold = inf.hold;
			bits = inf.bits;
			lastBlock = inf.lastBlock;
			wnext = inf.wnext;
			//if (inf.whave == inf.wsize) {
			//	window = inf.window.clone();
			//} else {
			window = new byte[inf.whave];
			System.arraycopy(inf.window, 0, window, 0, inf.whave);
			//}
			ncode = inf.ncode;
			nlen = inf.nlen;
			ndist = inf.ndist;
			have = inf.have;
			length = inf.length;
			offset = inf.offset;
			extraBits = inf.extraBits;
			lenbits = inf.lenbits;
			distbits = inf.distbits;
			lens = inf.lens.clone();
			codes = inf.codes.clone();
			lencode = inf.lencode == inf.codes ? null : LENFIX;
			distcode = inf.distcode == inf.codes ? null : DISTFIX;
			distcodeIndex = inf.distcodeIndex;
			totalIn = inf.totalIn;
			totalOut = inf.totalOut;
		}

		/**
		 * Restores a {@code OpenInflater} from the this captured {@code State}.
		 *
		 * @return a new {@code OpenInflater} instance restored from the this
		 * captured {@code State}.
		 */
		public OpenInflater restore() {
			return new OpenInflater(this);
		}
	}

	public void reset() {
		nextIn = EMPTY;
		nextInIndex
				= availIn
				= hold
				= bits
				//= wsize
				= whave
				= wnext
				= length
				= offset
				= extraBits
				= ncode
				= nlen
				= ndist
				= have
				= distcodeIndex
				= lenbits
				= distbits = 0;
		totalIn = totalOut = 0L;
		//adler.reset();
		//adler = 1;	// not necessary
		mode = HEAD;
		lastBlock = false;
	}

	private static int BITS(int h, int n) {
		return h & ((1 << n) - 1);
	}

	/**
	 * Returns the total number of bytes remaining in the input buffer. This can
	 * be used to find out what bytes still remain in the input buffer after
	 * decompression has finished.
	 *
	 * @return the total number of bytes remaining in the input buffer
	 */
	public int getRemaining() {
		return availIn;
	}

	/**
	 * Returns {@code true} if a preset dictionary is needed for decompression.
	 *
	 * @return {@code true} if a preset dictionary is needed for decompression
	 * @see OpenInflater#setDictionary
	 */
	public boolean needsDictionary() {
		return mode == DICT;
	}

	/**
	 * Returns {@code true} if the end of the compressed data stream has been
	 * reached.
	 *
	 * @return {@code true} if the end of the compressed data stream has been
	 * reached
	 */
	public boolean finished() {
		return mode == DONE;
	}

	/**
	 * Returns {@code true} if no data remains in the input buffer. This can be
	 * used to determine if #setInput should be called in order to provide more
	 * input.
	 *
	 * @return {@code true} if no data remains in the input buffer
	 */
	public boolean needsInput() {
		return availIn == 0;
	}

	/**
	 * Returns the ADLER-32 value of the uncompressed data.
	 *
	 * @return the ADLER-32 value of the uncompressed data
	 */
	public int getAdler() {
		// TODO this *might* actually return incomplete results during header parsing
		return (int) adler.getValue();
	}

	/**
	 * Returns the total number of compressed bytes input so far.
	 *
	 * @return the total (non-negative) number of compressed bytes input so far
	 */
	public long getBytesRead() {
		return totalIn;
	}

	/**
	 * Returns the total number of uncompressed bytes output so far.
	 *
	 * @return the total (non-negative) number of uncompressed bytes output so
	 * far
	 */
	public long getBytesWritten() {
		return totalOut;
	}

	/**
	 * Sets the preset dictionary to the given array of bytes. Should be called
	 * when inflate() returns 0 and needsDictionary() returns true indicating
	 * that a preset dictionary is required. The method getAdler() can be used
	 * to get the Adler-32 value of the dictionary needed.
	 *
	 * @param b the dictionary data bytes
	 * @param off the start offset of the data
	 * @param len the length of the data
	 * @see OpenInflater#needsDictionary
	 * @see OpenInflater#getAdler
	 */
	public void setDictionary(byte[] b, int off, int len) {
		if (off > b.length - len || off < 0 || len < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (mode != DICT) {
			throw new IllegalStateException("no ditionary is required");
		}
		int a = OpenAdler32.updateAdler32(1, b, off, len);

		//adler.update(b, off, len);
		//int a = (int) adler.getValue();
		//adler.reset();
		if (a != hold) {
			throw new IllegalArgumentException("incorrect dictionary checksum");
		}
		updatewindow(b, off + len, len);
		hold = bits = 0;
		mode = TYPEDO;
	}

	/**
	 * Sets the preset dictionary to the given array of bytes. Should be called
	 * when inflate() returns 0 and needsDictionary() returns true indicating
	 * that a preset dictionary is required. The method getAdler() can be used
	 * to get the Adler-32 value of the dictionary needed.
	 *
	 * @param b the dictionary data bytes
	 * @see OpenInflater#needsDictionary
	 * @see OpenInflater#getAdler
	 */
	public void setDictionary(byte[] b) {
		setDictionary(b, 0, b.length);
	}

	/**
	 * Closes the decompressor and discards any unprocessed input. This method
	 * should be called when the decompressor is no longer being used. Once this
	 * method is called, the behavior of the Inflater object is undefined.
	 */
	public void end() {
		nextIn = EMPTY;
		mode = DONE;
	}

	/**
	 * Sets input data for decompression. Should be called whenever needsInput()
	 * returns true indicating that more input data is required.
	 *
	 * @param b the input data bytes
	 * @param off the start offset of the input data
	 * @param len the length of the input data
	 * @see OpenInflater#needsInput
	 */
	public void setInput(byte[] b, int off, int len) {
		if (off > b.length - len || off < 0 || len < 0) {
			throw new IndexOutOfBoundsException();
		}
		nextIn = b;
		nextInIndex = off;
		availIn = len;
	}

	/**
	 * Sets input data for decompression. Should be called whenever needsInput()
	 * returns true indicating that more input data is required.
	 *
	 * @param b the input data bytes
	 * @see OpenInflater#needsInput
	 */
	public void setInput(byte[] b) {
		setInput(b, 0, b.length);
	}

	/**
	 * Uncompresses bytes into specified buffer. Returns actual number of bytes
	 * uncompressed. A return value of 0 indicates that needsInput() or
	 * needsDictionary() should be called in order to determine if more input
	 * data or a preset dictionary is required. In the latter case, getAdler()
	 * can be used to get the Adler-32 value of the dictionary required.
	 *
	 * @param b the buffer for the uncompressed data
	 * @param off the start offset of the data
	 * @param len the maximum number of uncompressed bytes
	 * @return the actual number of uncompressed bytes
	 * @exception DataFormatException if the compressed data format is invalid
	 * @see OpenInflater#needsInput
	 * @see OpenInflater#needsDictionary
	 */
	public int inflate(byte[] b, int off, int len) throws DataFormatException {
		if (off > b.length - len || off < 0 || len < 0) {
			throw new IndexOutOfBoundsException();
		}
		// memoize some member vars for speed
		int _mode = mode;
		int _availIn = availIn;
		int _bits = bits;
		int _hold = hold;
		final byte[] _nextIn = nextIn;
		int _nextInIndex = nextInIndex;

		final int in = _availIn;
		final int out = len;
		try {
			main:
			while (true) {
				modes:
				switch (_mode) {
					case HEAD: {
						if (!wrap) {
							_mode = TYPEDO;
							break;
						}
						while (_bits < 16) {
							if (_availIn == 0) {
								totalIn += in - _availIn;
								return 0;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						if (_hold == 0x8b1f) {
							adler = nativeCRC32 ? new HybridCRC32() : new OpenCRC32();
							adler.update(_hold & 0xff);
							adler.update((_hold >>> 8) & 0xff);
							//int a = OpenCRC32.updateCRC32(0, _hold);
							//adler = OpenCRC32.updateCRC32(a, _hold >>> 8);
							_hold = _bits = 0;
							_mode = FLAGS;
							break;
						}
						//adler = 1;	// init Adler32 sum
						flags = 0;
						if ((((_hold & 0xff) << 8) | (_hold >>> 8)) % 31 != 0) {
							throw new DataFormatException("incorrect header check");
						}
						if ((_hold & 0xf) != 8) {
							throw new DataFormatException("unknown compression method");
						}
						_hold >>>= 4;
						_bits -= 4;
						int _len = (_hold & 0xf) + 8;
						if (_len > MAXBITS) {
							throw new DataFormatException("invalid window size");
						}
						//wsize = 1 << len;	// only if state->wbits == 0, which is never the case
						_mode = (_hold & 0x200) != 0 ? DICTID : TYPEDO;
						_hold = _bits = 0;
						break;
					}
					case FLAGS: {
						while (_bits < 16) {
							if (_availIn == 0) {
								totalIn += in - _availIn;
								return 0;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						flags = _hold;
						if ((flags & 0xff) != 8) {
							throw new DataFormatException("unknown compression method");
						}
						if ((flags & 0xe000) != 0) {
							throw new DataFormatException("unknown header flags set");
						}
						if ((flags & 0x0200) != 0) {
							adler.update(_hold & 0xff);
							adler.update((_hold >>> 8) & 0xff);

							//	int a = OpenCRC32.updateCRC32(adler, _hold);
							//	adler = OpenCRC32.updateCRC32(a, _hold >>> 8);
						}
						_hold = _bits = 0;
						_mode = TIME;
						// fall-through
					}
					case TIME: {
						while (_bits < 32) {
							if (_availIn == 0) {
								totalIn += in - _availIn;
								return 0;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						if ((flags & 0x0200) != 0) {
							adler.update(_hold & 0xff);
							adler.update((_hold >>> 8) & 0xff);
							adler.update((_hold >>> 16) & 0xff);
							adler.update((_hold >>> 24) & 0xff);

							//	int a = OpenCRC32.updateCRC32(adler, _hold);
							//	a = OpenCRC32.updateCRC32(a, _hold >>> 8);
							//	a = OpenCRC32.updateCRC32(a, _hold >>> 16);
							//	adler = OpenCRC32.updateCRC32(a, _hold >>> 24);
						}
						_hold = _bits = 0;
						_mode = OS;
						// fall-through
					}
					case OS: {
						while (_bits < 16) {
							if (_availIn == 0) {
								totalIn += in - _availIn;
								return 0;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						if ((flags & 0x0200) != 0) {
							adler.update(_hold & 0xff);
							adler.update((_hold >>> 8) & 0xff);
							//	int a = OpenCRC32.updateCRC32(adler, _hold);
							//	adler = OpenCRC32.updateCRC32(a, _hold >>> 8);
						}
						_hold = _bits = 0;
						_mode = EXLEN;
						// fall-through
					}
					case EXLEN: {
						if ((flags & 0x0400) != 0) {
							while (_bits < 16) {
								if (_availIn == 0) {
									totalIn += in - _availIn;
									return 0;
								}
								_availIn--;
								_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
								_bits += 8;
							}
							length = _hold;
							if ((flags & 0x0200) != 0) {
								adler.update(_hold & 0xff);
								adler.update((_hold >>> 8) & 0xff);
								//int a = OpenCRC32.updateCRC32(adler, _hold);
								//adler = OpenCRC32.updateCRC32(a, _hold >>> 8);
							}
							_hold = _bits = 0;
						}
						_mode = EXTRA;
						// fall-through
					}
					case EXTRA: {
						if ((flags & 0x0400) != 0) {
							int copy = length;
							if (copy > _availIn) {
								copy = _availIn;
							}
							if (copy != 0) {
								if ((flags & 0x0200) != 0) {
									adler.update(_nextIn, _nextInIndex, copy);
									//adler = OpenCRC32.updateCRC32(adler, _nextIn, _nextInIndex, copy);
								}
								_availIn -= copy;
								_nextInIndex += copy;
								length -= copy;
							}
							if (length != 0) {
								totalIn += in - _availIn;
								return 0;
							}
						}
						length = 0;
						_mode = NAME;
						// fall-through
					}
					case NAME: {
						if ((flags & 0x0800) != 0) {
							// basically just skip past a 0-terminated string
							if (_availIn == 0) {
								totalIn += in - _availIn;
								return 0;
							}
							int copy = 0;
							int _len;
							do {
								_len = _nextIn[copy++];
							} while (_len != 0 && copy < _availIn);
							if ((flags & 0x0200) != 0) {
								adler.update(_nextIn, _nextInIndex, copy);
								//adler = OpenCRC32.updateCRC32(adler, _nextIn, _nextInIndex, copy);
							}
							_availIn -= copy;
							_nextInIndex += copy;
							if (_len != 0) {
								totalIn += in - _availIn;
								return 0;
							}
						}
						length = 0;
						_mode = COMMENT;
						// fall-through
					}
					case COMMENT: {
						if ((flags & 0x1000) != 0) {
							// basically just skip past a 0-terminated string
							if (_availIn == 0) {
								totalIn += in - _availIn;
								return 0;
							}
							int copy = 0;
							int _len;
							do {
								_len = _nextIn[copy++];
							} while (_len != 0 && copy < _availIn);
							if ((flags & 0x0200) != 0) {
								adler.update(_nextIn, _nextInIndex, copy);
								//adler = OpenCRC32.updateCRC32(adler, _nextIn, _nextInIndex, copy);
							}
							_availIn -= copy;
							_nextInIndex += copy;
							if (_len != 0) {
								totalIn += in - _availIn;
								return 0;
							}
						}
						_mode = HCRC;
						// fall-through
					}
					case HCRC: {
						// CRC check
						if ((flags & 0x0200) != 0) {
							while (_bits < 16) {
								if (_availIn == 0) {
									totalIn += in - _availIn;
									return 0;
								}
								_availIn--;
								_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
								_bits += 8;
							}
							if (_hold != ((int) adler.getValue() & 0xffff)) {
								//if (_hold != (adler & 0xffff)) {
								throw new DataFormatException("header crc mismatch");
							}
							_hold = _bits = 0;
						}

						// TODO use independent checksum, so as not to report values during header parsing
						//adler = 0;	// reset CRC32 sum
						adler.reset();
						_mode = TYPEDO;
						break;
					}
					case DICTID: {
						while (_bits < 32) {
							if (_availIn == 0) {
								totalIn += in - _availIn;
								return 0;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						totalIn += in - _availIn;
						_hold = Integer.reverseBytes(_hold);
						_mode = DICT;
						// fall-through
					}
					case DICT: {
						return 0;
					}
					case TYPEDO: {
						if (lastBlock) {
							_hold >>>= _bits & 7;
							_bits -= _bits & 7;
							if (!wrap) {
								_mode = DONE;
								break main;
							}
							_mode = CHECK;
							break;
						}
						if (_bits < 3) {
							if (_availIn == 0) {
								break main;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						lastBlock = (_hold & 1) != 0;
						switch ((_hold >>> 1) & 3) {
							case 0: {
								_hold >>>= 3;
								_bits -= 3;
								_hold >>>= _bits & 7;
								_bits -= _bits & 7;
								_mode = STORED;
								break;
							}
							case 1: {
								_hold >>>= 3;
								_bits -= 3;
								//fixedtables();
								lencode = LENFIX;
								lenbits = 9;
								distcode = DISTFIX;
								distcodeIndex = 0;
								distbits = 5;
								_mode = LEN;
								break;
							}
							case 2: {
								_hold >>>= 3;
								_bits -= 3;
								_mode = TABLE;
								break;
							}
							default: {
								//_hold >>>= 3;
								//_bits -= 3;
								throw new DataFormatException("invalid block type");
							}
						}
						break;
					}
					case STORED: {
						while (_bits < 32) {
							if (_availIn == 0) {
								break main;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						if ((_hold & 0xffff) != ((_hold >>> 16) ^ 0xffff)) {
							throw new DataFormatException("invalid stored block lengths");
						}
						length = _hold & 0xffff;
						_hold = _bits = 0;
						_mode = COPY;
						// fall-through
					}
					case COPY: {
						int copy = length;
						if (copy != 0) {
							if (copy > _availIn) {
								copy = _availIn;
							}
							if (copy > len) {
								copy = len;
							}
							if (copy == 0) {
								// return infLeave(in, out, b);
								break main;
							}
							System.arraycopy(_nextIn, _nextInIndex, b, off, copy);
							_availIn -= copy;
							_nextInIndex += copy;
							len -= copy;
							off += copy;
							length -= copy;
							break;
						}
						_mode = TYPEDO;
						break;
					}
					case TABLE: {
						while (_bits < 14) {
							if (_availIn == 0) {
								break main;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						nlen = (_hold & 0x1f) + 257;
						ndist = ((_hold >>> 5) & 0x1f) + 1;
						ncode = ((_hold >>> 10) & 0xf) + 4;
						_hold >>>= 14;
						_bits -= 14;
						if (!pkzipBugWorkaround) {
							if (nlen > 286 && ndist > 30) {
								throw new DataFormatException("too many length or distance symbols");
							}
						}
						have = 0;
						_mode = LENLENS;
						// fall-through
					}
					case LENLENS: {
						while (have < ncode) {
							if (_bits < 3) {
								if (_availIn == 0) {
									break main;
								}
								_availIn--;
								_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
								_bits += 8;
							}
							lens[ORDER[have++]] = _hold & 0x7;
							_hold >>>= 3;
							_bits -= 3;
						}
						while (have < 19) {
							lens[ORDER[have++]] = 0;
						}
						lenbits = inflateTableCodes();
						lencode = codes;
						have = 0;
						_mode = CODELENS;
						// fall-through
					}
					case CODELENS: {
						final int _nlen = nlen;
						final int _ndist = ndist;
						int m = _nlen + _ndist;
						while (have < m) {
							int here;
							int copy, _len;
							int hereBits;
							while (true) {
								here = lencode[BITS(_hold, lenbits)];
								hereBits = (here >>> 8) & 0xff;
								if (hereBits <= _bits) {
									break;
								}
								if (_availIn == 0) {
									// return infLeave(in, out, b);
									break main;
								}
								_availIn--;
								_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
								_bits += 8;
							}
							int hereVal = here >>> 16;
							if (hereVal < 16) {
								_hold >>>= hereBits;
								_bits -= hereBits;
								lens[have++] = hereVal;
							} else {
								switch (hereVal) {
									case 16: {
										int h2 = hereBits + 2;
										while (_bits < h2) {
											if (_availIn == 0) {
												break main;
											}
											_availIn--;
											_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
											_bits += 8;
										}
										if (have == 0) {
											throw new DataFormatException("invalid bit length repeat");
										}
										_len = lens[have - 1];
										copy = 3 + ((_hold >>> hereBits) & 0x3);
										_hold >>>= h2;
										_bits -= h2;
										break;
									}
									case 17: {
										int h3 = hereBits + 3;
										while (_bits < h3) {
											if (_availIn == 0) {
												break main;
											}
											_availIn--;
											_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
											_bits += 8;
										}
										_len = 0;
										copy = 3 + ((_hold >>> hereBits) & 0x7);
										_hold >>>= h3;
										_bits -= h3;
										break;
									}
									default: {
										int h7 = hereBits + 7;
										while (_bits < h7) {
											if (_availIn == 0) {
												break main;
											}
											_availIn--;
											_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
											_bits += 8;
										}
										_len = 0;
										copy = 11 + ((_hold >>> hereBits) & 0x7f);
										_hold >>>= h7;
										_bits -= h7;
										break;
									}
								}
								if (have + copy > m) {
									throw new DataFormatException("invalid bit length repeat");
								}
								while (copy-- != 0) {
									lens[have++] = _len;
								}
							}
						}
						if (lens[256] == 0) {
							throw new DataFormatException("invalid code -- missing end-of-block");
						}
						lenbits = inflateTableLens(_nlen);
						distbits = inflateTableDists(_nlen, _ndist);
						distcode = lencode = codes;
						_mode = LEN;
						// fall-through
					}
					case LEN: {
						int here;
						int hereBits;
						// inflate_fast
						final int _wnext = wnext;
						final int _whave = whave;
						final int _lenbits = lenbits;
						final int _distbits = distbits;
						final int _distcodeIndex = distcodeIndex;
						final int[] _distcode = distcode;
						final int[] _lencode = lencode;
						while (_availIn >= 6 && len >= 258) {
							int last = _nextInIndex + _availIn - 5;
							int beg = off + len - out;
							int end = off + len - 257;
							int lmask = (1 << _lenbits) - 1;
							int dmask = (1 << _distbits) - 1;
							outer:
							do {
								if (_bits <= 15) {
									_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
									_hold |= (_nextIn[_nextInIndex++] & 0xff) << (_bits += 8);
									_bits += 8;
								}
								here = _lencode[_hold & lmask];
								while (true) {	//doLen
									hereBits = (here >>> 8) & 0xff;
									_hold >>>= hereBits;
									_bits -= hereBits;
									if ((here & 0xff) == 0) {
										b[off++] = (byte) (here >>> 16);
										continue outer;
									}
									if ((here & 16) != 0) {
										int _len = here >>> 16;
										int op = here & 15;
										if (op != 0) {
											if (_bits < op) {
												_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
												_bits += 8;
											}
											_len += _hold & ((1 << op) - 1);
											_hold >>>= op;
											_bits -= op;
										}
										if (_bits <= 15) {
											_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
											_hold |= (_nextIn[_nextInIndex++] & 0xff) << (_bits += 8);
											_bits += 8;
										}
										here = _distcode[_distcodeIndex + (_hold & dmask)];
										while (true) {	// doDist
											hereBits = (here >>> 8) & 0xff;
											_hold >>>= hereBits;
											_bits -= hereBits;
											if ((here & 16) != 0) {
												int dist = here >>> 16;
												op = here & 15;
												if (_bits < op) {
													_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
													if ((_bits += 8) < op) {
														_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
														_bits += 8;
													}
												}
												dist += _hold & ((1 << op) - 1);
												if (inflateStrict) {
													if (dist > WSIZE) {
														throw new DataFormatException("invalid distance too far back");
													}
												}
												_hold >>>= op;
												_bits -= op;
												op = off - beg;
												if (dist > op) {
													op = dist - op;
													if (op > _whave) {
														if (sane) {
															throw new DataFormatException("invalid distance too far back");
														}
														if (inflateAllowInvalidDistanceToofarArrr) {
															if (_len <= op - _whave) {
																do {
																	b[off++] = 0;
																} while (--_len != 0);
																continue outer;
															}
															_len -= op - _whave;
															do {
																b[off++] = 0;
															} while (--op > _whave);
															if (op == 0) {
																int fromIndex = off - dist;
																do {
																	b[off++] = b[fromIndex++];
																} while (--_len != 0);
																continue outer;
															}
														}
													}
													byte[] from = window;
													int fromIndex = 0;
													if (_wnext == 0) {
														fromIndex += WSIZE - op;
														if (op < _len) {
															_len -= op;
															do {
																b[off++] = from[fromIndex++];
															} while (--op != 0);
															from = b;
															fromIndex = off - dist;
														}
													} else if (_wnext < op) {
														fromIndex += WSIZE + _wnext - op;
														op -= _wnext;
														if (op < _len) {
															_len -= op;
															do {
																b[off++] = from[fromIndex++];
															} while (--op != 0);
															fromIndex = 0;
															if (_wnext < _len) {
																op = _wnext;
																_len -= op;
																do {
																	b[off++] = from[fromIndex++];
																} while (--op != 0);
																from = b;
																fromIndex = off - dist;
															}
														}
													} else {
														fromIndex += _wnext - op;
														if (op < _len) {
															_len -= op;
															do {
																b[off++] = from[fromIndex++];
															} while (--op != 0);
															from = b;
															fromIndex = off - dist;
														}
													}
													while (_len > 2) {
														b[off++] = from[fromIndex++];
														b[off++] = from[fromIndex++];
														b[off++] = from[fromIndex++];
														_len -= 3;
													}
													if (_len != 0) {
														b[off++] = from[fromIndex++];
														if (_len > 1) {
															b[off++] = from[fromIndex++];
														}
													}
													continue outer;
												}
												int fromIndex = off - dist;
												do {
													b[off++] = b[fromIndex++];
													b[off++] = b[fromIndex++];
													b[off++] = b[fromIndex++];
													_len -= 3;
												} while (_len > 2);
												if (_len != 0) {
													b[off++] = b[fromIndex++];
													if (_len > 1) {
														b[off++] = b[fromIndex++];
													}
												}
												continue outer;
											}
											if ((here & 64) != 0) {
												throw new DataFormatException("invalid distance code");
											}
											here = _distcode[_distcodeIndex + ((here >>> 16) + (_hold & ((1 << (here & 0xff)) - 1)))];
										}	// goto dodist
									}
									if ((here & 64) == 0) {
										here = _lencode[(here >>> 16) + (_hold & ((1 << (here & 0xff)) - 1))];
										continue;	// goto dolen
									}
									if ((here & 32) == 0) {
										throw new DataFormatException("invalid literal/length code");
									}
									int _len = _bits >>> 3;
									_nextInIndex -= _len;
									_bits -= _len << 3;
									_hold &= (1 << _bits) - 1;
									_availIn = 5 + last - _nextInIndex;
									len = 257 + end - off;
									_mode = TYPEDO;
									break modes;	// goto TYPEDO next
								}	// goto dolen
							} while (_nextInIndex < last && off < end);
							int _len = _bits >>> 3;
							_nextInIndex -= _len;
							_bits -= _len << 3;
							_hold &= (1 << _bits) - 1;
							_availIn = 5 + last - _nextInIndex;
							len = 257 + end - off;
							// keep doing inflate_fast
						}
						while (true) {
							here = _lencode[BITS(_hold, _lenbits)];
							hereBits = (here >>> 8) & 0xff;
							if (hereBits <= _bits) {
								break;
							}
							if (_availIn == 0) {
								// return infLeave(in, out, b);
								break main;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						if ((here & 0xff) != 0 && (here & 0xf0) == 0) {
							int lastBits = (here >>> 8) & 0xff;
							int lastVal = here >>> 16;
							int n = lastBits + (here & 0xff);
							while (true) {
								here = _lencode[lastVal + (BITS(_hold, n) >>> lastBits)];
								hereBits = (here >>> 8) & 0xff;
								if (lastBits + hereBits <= _bits) {
									break;
								}
								if (_availIn == 0) {
									// return infLeave(in, out, b);
									break main;
								}
								_availIn--;
								_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
								_bits += 8;
							}
							_hold >>>= lastBits;
							_bits -= lastBits;
						}
						_hold >>>= hereBits;
						_bits -= hereBits;
						length = here >>> 16;
						if ((here & 0xff) == 0) {
							_mode = LIT;
							break;
						}
						if ((here & 32) != 0) {
							_mode = TYPEDO;
							break;
						}
						if ((here & 64) != 0) {
							throw new DataFormatException("invalid literal/length code");
						}
						extraBits = here & 15;
						_mode = LENEXT;
						// fall-through
					}
					case LENEXT: {
						int _extraBits = extraBits;
						if (_extraBits != 0) {
							while (_bits < _extraBits) {
								if (_availIn == 0) {
									break main;
								}
								_availIn--;
								_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
								_bits += 8;
							}
							length += BITS(_hold, _extraBits);
							_hold >>>= _extraBits;
							_bits -= _extraBits;
						}
						_mode = DIST;
						// fall-through
					}
					case DIST: {
						int here;
						int hereBits;
						final int[] _distcode = distcode;
						int _distcodeIndex = distcodeIndex;
						while (true) {
							here = _distcode[_distcodeIndex + BITS(_hold, distbits)];
							hereBits = (here >>> 8) & 0xff;
							if (hereBits <= _bits) {
								break;
							}
							if (_availIn == 0) {
								// return infLeave(in, out, b);
								break main;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						if ((here & 0xf0) == 0) {
							int lastBits = (here >>> 8) & 0xff;
							int lastVal = here >>> 16;
							int n = lastBits + (here & 0xff);
							while (true) {
								here = _distcode[_distcodeIndex + (lastVal + (BITS(_hold, n) >>> lastBits))];
								hereBits = (here >>> 8) & 0xff;
								if (lastBits + hereBits <= _bits) {
									break;
								}
								if (_availIn == 0) {
									// return infLeave(in, out, b);
									break main;
								}
								_availIn--;
								_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
								_bits += 8;
							}
							_hold >>>= lastBits;
							_bits -= lastBits;
						}
						_hold >>>= hereBits;
						_bits -= hereBits;
						if ((here & 64) != 0) {
							throw new DataFormatException("invalid distance code");
						}
						offset = here >>> 16;
						extraBits = here & 15;
						_mode = DISTEXT;
						// fall-through
					}
					case DISTEXT: {
						int _extraBits = extraBits;
						if (_extraBits != 0) {
							while (_bits < _extraBits) {
								if (_availIn == 0) {
									break main;
								}
								_availIn--;
								_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
								_bits += 8;
							}
							offset += BITS(_hold, _extraBits);
							_hold >>>= _extraBits;
							_bits -= _extraBits;
						}
						if (inflateStrict) {
							if (offset > WSIZE) {
								throw new DataFormatException("invalid distance too far back");
							}
						}
						_mode = MATCH;
						// fall-through
					}
					case MATCH: {
						if (len == 0) {
							// return infLeave(in, out, b);
							break main;
						}
						byte[] from;
						int fromIndex;
						int copy = out - len;
						if (offset > copy) {
							copy = offset - copy;
							if (copy > whave) {
								if (sane) {
									throw new DataFormatException("invalid distance too far back");
								}
								if (inflateAllowInvalidDistanceToofarArrr) {
									copy -= whave;
									if (copy > length) {
										copy = length;
									}
									if (copy > len) {
										copy = len;
									}
									len -= copy;
									do {
										b[off++] = 0;
									} while (--copy != 0);
									if (length == 0) {
										_mode = LEN;
									}
									break;
								}
							}
							if (copy > wnext) {
								copy -= wnext;
								from = window;
								fromIndex = WSIZE - copy;
							} else {
								from = window;
								fromIndex = wnext - copy;
							}
							if (copy > length) {
								copy = length;
							}
						} else {
							from = b;
							fromIndex = off - offset;
							copy = length;
						}
						if (copy > len) {
							copy = len;
						}
						length -= copy;
						len -= copy;
						do {
							b[off++] = from[fromIndex++];
						} while (--copy != 0);
						if (length == 0) {
							_mode = LEN;
						}
						break;
					}
					case LIT: {
						if (len == 0) {
							// return infLeave(in, out, b);
							break main;
						}
						b[off++] = (byte) length;
						len--;
						_mode = LEN;
						break;
					}
					case CHECK: {
						while (_bits < 32) {
							if (_availIn == 0) {
								break main;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						totalIn += in - _availIn;
						final int dif = out - len;
						totalOut += dif;
						if (flags != 0) {
							if (dif != 0) {
								//adler = OpenCRC32.updateCRC32(adler, b, off - dif, dif);
								adler.update(b, off - dif, dif);
							}
							//if (adler != _hold) {
							if ((int) adler.getValue() != _hold) {
								throw new DataFormatException("incorrect data check");
							}
							_hold = _bits = 0;
							if (_availIn >= 4) {
								// can finish in this invocation
								int _hold2 = (_nextIn[_nextInIndex++] & 0xff)
										| (_nextIn[_nextInIndex++] & 0xff) << 8
										| (_nextIn[_nextInIndex++] & 0xff) << 16
										| (_nextIn[_nextInIndex++] & 0xff) << 24;
								_availIn -= 4;
								totalIn += 4;
								if ((_hold2 & 0xffffffffL) != totalOut) {
									throw new DataFormatException("incorrect length check");
								}
								_mode = DONE;
								return dif;
							}
							_mode = LENGTH;
							return dif;
						}
						if (dif != 0) {
							//adler = OpenAdler32.updateAdler32(adler, b, off - dif, dif);
							adler.update(b, off - dif, dif);
						}
						//if (adler != reverse(_hold)) {
						if ((int) adler.getValue() != Integer.reverseBytes(_hold)) {
							throw new DataFormatException("incorrect data check");
						}
						_hold = _bits = 0;
						_mode = DONE;
						return dif;
					}
					case LENGTH: {
						while (_bits < 32) {
							if (_availIn == 0) {
								break main;
							}
							_availIn--;
							_hold |= (_nextIn[_nextInIndex++] & 0xff) << _bits;
							_bits += 8;
						}
						if ((_hold & 0xffffffffL) != totalOut) {
							throw new DataFormatException("incorrect length check");
						}
						_hold = _bits = 0;
						_mode = DONE;
						break main;
					}
					case DONE: {
						return 0;
					}
					default: {
						throw new InternalError("invalid inflate mode");
					}
				}
			}
			// inf leave
			updatewindow(b, off, out - len);
			int diff = out - len;
			totalIn += in - _availIn;
			totalOut += diff;
			if (wrap && diff != 0) {
				adler.update(b, off - diff, diff);
				//adler = flags != 0
				//	? OpenCRC32.updateCRC32(adler, b, off - diff, diff)
				//	: OpenAdler32.updateAdler32(adler, b, off - diff, diff);
			}
			return diff;
		} catch (DataFormatException ex) {
			_mode = BAD;
			throw ex;
		} finally {
			// restore from memoized values
			mode = _mode;
			availIn = _availIn;
			bits = _bits;
			hold = _hold;
			nextInIndex = _nextInIndex;
		}
	}

	/**
	 * Uncompresses bytes into specified buffer. Returns actual number of bytes
	 * uncompressed. A return value of 0 indicates that needsInput() or
	 * needsDictionary() should be called in order to determine if more input
	 * data or a preset dictionary is required. In the latter case, getAdler()
	 * can be used to get the Adler-32 value of the dictionary required.
	 *
	 * @param b the buffer for the uncompressed data
	 * @return the actual number of uncompressed bytes
	 * @exception DataFormatException if the compressed data format is invalid
	 * @see OpenInflater#needsInput
	 * @see OpenInflater#needsDictionary
	 */
	public int inflate(byte[] b) throws DataFormatException {
		return inflate(b, 0, b.length);
	}

	private void updatewindow(byte[] source, int end, int copy) {
		if (copy > WSIZE) {
			System.arraycopy(source, end - WSIZE, window, 0, WSIZE);
			wnext = 0;
			whave = WSIZE;
			return;
		}
		int dist = WSIZE - wnext;
		if (dist > copy) {
			System.arraycopy(source, end - copy, window, wnext, copy);
			if ((wnext += copy) == WSIZE) {
				wnext = 0;
			}
			if (whave < WSIZE) {
				whave += copy;
			}
			return;
		}
		System.arraycopy(source, end - copy, window, wnext, dist);
		copy -= dist;
		if (copy != 0) {
			System.arraycopy(source, end - copy, window, 0, copy);
			wnext = copy;
			whave = WSIZE;
			return;
		}
		if ((wnext += dist) == WSIZE) {
			wnext = 0;
		}
		if (whave < WSIZE) {
			whave += dist;
		}
	}

	// no side-effects
	private int getMin(int max) {
		for (int i = 1; i < max; i++) {
			if (count[i] != 0) {
				return i;
			}
		}
		return max;
	}

	// no side-effects
	private void calculateOffsets(int off, int m) {
		offs[1] = 0;
		for (int len = 1; len < MAXBITS; len++) {
			offs[len + 1] = offs[len] + count[len];
		}
		for (int sym = off; sym < m; sym++) {
			int len = lens[sym];
			if (len != 0) {
				work[offs[len]++] = sym - off;
			}
		}
	}

	// no side-effects
	private int inflateTableCodes() throws DataFormatException {
		System.arraycopy(ZEROS, 0, count, 0, count.length);
		for (int sym = 0; sym < 19; sym++) {
			count[lens[sym]]++;
		}
		for (int max = MAXBITS; max >= 1; max--) {
			if (count[max] != 0) {
				int root = 7;
				int min = getMin(max);
				if (root > max) {
					root = max;
				}
				if (root < min) {
					root = min;
				}
				int left = 1;
				for (int len = 1; len <= MAXBITS; len++) {
					left <<= 1;
					left -= count[len];
					if (left < 0) {
						throw new DataFormatException("invalid code lengths set");
					}
				}
				if (left > 0) {
					throw new DataFormatException("invalid code lengths set");
				}
				calculateOffsets(0, 19);
				int huff = 0;
				int sym = 0;
				int len = min;
				int nextIndex = 0;
				int curr = root;
				int drop = 0;
				int low = -1;
				int used = 1 << root;
				int mask = used - 1;
				while (true) {
					int here = (work[sym] << 16) | ((len - drop) << 8);
					int incr = 1 << (len - drop);
					int fill = 1 << curr;
					min = fill;
					do {
						fill -= incr;
						codes[nextIndex + ((huff >>> drop) + fill)] = here;
					} while (fill != 0);
					incr = 1 << (len - 1);
					while ((huff & incr) != 0) {
						incr >>>= 1;
					}
					if (incr != 0) {
						huff &= incr - 1;
						huff += incr;
					} else {
						huff = 0;
					}
					sym++;
					if (--count[len] == 0) {
						if (len == max) {
							break;
						}
						len = lens[work[sym]];
					}
					if (len > root && (huff & mask) != low) {
						if (drop == 0) {
							drop = root;
						}
						nextIndex += min;
						curr = len - drop;
						left = 1 << curr;
						while (curr + drop < max) {
							left -= count[curr + drop];
							if (left <= 0) {
								break;
							}
							curr++;
							left <<= 1;
						}
						used += 1 << curr;
						low = huff & mask;
						codes[low] = (nextIndex << 16) | (root << 8) | curr;
					}
				}
				if (huff != 0) {
					codes[nextIndex + huff] = ((len - drop) << 8) | 64;
				}
				return root;
			}
		}
		// max == 0
		codes[0] = 0x140; //code(64, 1, 0);
		codes[1] = 0x140; //code(64, 1, 0);
		return 1;
	}

	// side-effect: distcodeIndex
	private int inflateTableLens(int nlen) throws DataFormatException {
		System.arraycopy(ZEROS, 0, count, 0, count.length);
		for (int sym = 0; sym < nlen; sym++) {
			count[lens[sym]]++;
		}
		for (int max = MAXBITS; max >= 1; max--) {
			if (count[max] != 0) {
				int root = 9;
				int min = getMin(max);
				if (root > max) {
					root = max;
				}
				if (root < min) {
					root = min;
				}
				int left = 1;
				for (int len = 1; len <= MAXBITS; len++) {
					left <<= 1;
					left -= count[len];
					if (left < 0) {
						throw new DataFormatException("invalid literal/lengths set");
					}
				}
				if (left > 0 && max != 1) {
					throw new DataFormatException("invalid literal/lengths set");
				}
				calculateOffsets(0, nlen);
				int huff = 0;
				int sym = 0;
				int len = min;
				int nextIndex = 0;
				int curr = root;
				int drop = 0;
				int low = -1;
				int used = 1 << root;
				int mask = used - 1;
				if (used > ENOUGH_LENS) {
					throw new DataFormatException("invalid literal/lengths set");
				}
				while (true) {
					int here;
					if (work[sym] < 256) {
						here = (work[sym] << 16) | ((len - drop) << 8);
					} else if (work[sym] > 256) {
						here = (LBASE[work[sym] - 257] << 16) | ((len - drop) << 8) | LEXT[work[sym] - 257];
					} else {
						here = ((len - drop) << 8) | (32 + 64);
					}
					int incr = 1 << (len - drop);
					int fill = 1 << curr;
					min = fill;
					do {
						fill -= incr;
						codes[nextIndex + ((huff >>> drop) + fill)] = here;
					} while (fill != 0);
					incr = 1 << (len - 1);
					while ((huff & incr) != 0) {
						incr >>>= 1;
					}
					if (incr != 0) {
						huff &= incr - 1;
						huff += incr;
					} else {
						huff = 0;
					}
					sym++;
					if (--count[len] == 0) {
						if (len == max) {
							break;
						}
						len = lens[work[sym]];
					}
					if (len > root && (huff & mask) != low) {
						if (drop == 0) {
							drop = root;
						}
						nextIndex += min;
						curr = len - drop;
						left = 1 << curr;
						while (curr + drop < max) {
							left -= count[curr + drop];
							if (left <= 0) {
								break;
							}
							curr++;
							left <<= 1;
						}
						used += 1 << curr;
						if (used > ENOUGH_LENS) {
							throw new DataFormatException("invalid literal/lengths set");
						}
						low = huff & mask;
						codes[low] = (nextIndex << 16) | (root << 8) | curr;
					}
				}
				if (huff != 0) {
					codes[nextIndex + huff] = ((len - drop) << 8) | 64;
				}
				distcodeIndex = used;
				return root;
			}
		}
		// max == 0
		codes[0] = 0x140; //code(64, 1, 0);
		codes[1] = 0x140; //code(64, 1, 0);
		distcodeIndex = 2;
		return 1;
	}

	// no side-effects
	private int inflateTableDists(int nlen, int ndist) throws DataFormatException {
		int tableIndex = distcodeIndex;
		System.arraycopy(ZEROS, 0, count, 0, count.length);
		int m = nlen + ndist;
		for (int sym = nlen; sym < m; sym++) {
			count[lens[sym]]++;
		}
		for (int max = MAXBITS; max >= 1; max--) {
			if (count[max] != 0) {
				int root = 6;
				int min = getMin(max);
				if (root > max) {
					root = max;
				}
				if (root < min) {
					root = min;
				}
				int left = 1;
				for (int len = 1; len <= MAXBITS; len++) {
					left <<= 1;
					left -= count[len];
					if (left < 0) {
						throw new DataFormatException("invalid distances set");
					}
				}
				if (left > 0 && max != 1) {
					throw new DataFormatException("invalid distances set");
				}
				calculateOffsets(nlen, m);
				int huff = 0;
				int sym = 0;
				int len = min;
				int nextIndex = tableIndex;
				int curr = root;
				int drop = 0;
				int low = -1;
				int used = 1 << root;
				int mask = used - 1;
				if (used > ENOUGH_DISTS) {
					throw new DataFormatException("invalid distances set");
				}
				while (true) {
					int here = (DBASE[work[sym]] << 16) | ((len - drop) << 8) | DEXT[work[sym]];
					int incr = 1 << (len - drop);
					int fill = 1 << curr;
					min = fill;
					do {
						fill -= incr;
						codes[nextIndex + ((huff >>> drop) + fill)] = here;
					} while (fill != 0);
					incr = 1 << (len - 1);
					while ((huff & incr) != 0) {
						incr >>>= 1;
					}
					if (incr != 0) {
						huff &= incr - 1;
						huff += incr;
					} else {
						huff = 0;
					}
					sym++;
					if (--count[len] == 0) {
						if (len == max) {
							break;
						}
						len = lens[nlen + work[sym]];
					}
					if (len > root && (huff & mask) != low) {
						if (drop == 0) {
							drop = root;
						}
						nextIndex += min;
						curr = len - drop;
						left = 1 << curr;
						while (curr + drop < max) {
							left -= count[curr + drop];
							if (left <= 0) {
								break;
							}
							curr++;
							left <<= 1;
						}
						used += 1 << curr;
						if (used > ENOUGH_DISTS) {
							throw new DataFormatException("invalid distances set");
						}
						low = huff & mask;
						codes[tableIndex + low] = ((nextIndex - tableIndex) << 16) | (root << 8) | curr;
					}
				}
				if (huff != 0) {
					codes[nextIndex + huff] = ((len - drop) << 8) | 64;
				}
				tableIndex += used;
				return root;
			}
		}
		// max == 0
		codes[tableIndex++] = 0x140; //code(64, 1, 0);
		codes[tableIndex++] = 0x140; //code(64, 1, 0);
		return 1;
	}
}
