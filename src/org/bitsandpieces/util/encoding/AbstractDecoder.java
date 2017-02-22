/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

abstract class AbstractDecoder implements Decoder {

	static final char DUMMY = '\uFFFD';

	private static final Parser STOP = (int cp, int num) -> -1;
	private static final ErrorHandler ERR = (int num) -> -1;

	//private static final int DEFAULT_OUTPUT_LIMIT = -1;
	private static final long DEFAULT_INPUT_LIMIT = -1L;

	int codePoints;		// length of message in code points
	//int outputLimit;	// max num code points to produce
	long bytes;			// length of message in bytes
	long inputLimit;	// max num bytes to process
	long codePointsTotal;
	long bytesTotal;

	Parser stop;
	ErrorHandler err;

	char[] charBuffer = new char[256];
	int[] counts;

	AbstractDecoder(int initialCountsCap) {
		//this.outputLimit = DEFAULT_OUTPUT_LIMIT;
		this.inputLimit = DEFAULT_INPUT_LIMIT;
		this.stop = STOP;
		this.err = ERR;
		this.counts = new int[initialCountsCap];
	}

	final char[] preparePut(int index) {
		char[] a = this.charBuffer;
		if (index - a.length < 0) {
			// overflow-resistant
			return a;
		}
		// unsigned: index > MAX_VALUE - 1
		if (Integer.MIN_VALUE + index > Integer.MIN_VALUE + Integer.MAX_VALUE - 1) {
			throw new OutOfMemoryError();
		}
		int newCap = a.length << 1;
		if (newCap < 0) {
			newCap = Integer.MAX_VALUE;
		}
		char[] b = new char[newCap];
		System.arraycopy(a, 0, b, 0, a.length);
		return (this.charBuffer = b);
	}

	final void inc(int index, int len, int exp, int shift) {
		int[] a = this.counts;
		int x = index >>> exp;
		if (x < a.length) {
			a[x] = (a[x] << shift) | len;
			return;
		}
		if (a.length == Integer.MAX_VALUE) {
			// cannot grow. should never happen, though
			throw new OutOfMemoryError();
		}
		int newCap = a.length << 1;
		if (newCap < 0) {
			newCap = Integer.MAX_VALUE;
		}
		int[] b = new int[newCap];
		System.arraycopy(a, 0, b, 0, x);
		b[x] = len;
		this.counts = b;
	}

	static final int dec(int[] a, int index, int exp, int shift, int mask) {
		int x = index >>> exp;
		int c = a[x];
		a[x] = c >>> shift;
		return (c & mask) + 1;
	}

	static final long inputLimitReached(long ipLim) {
		if (ipLim < 0L) {
			// there is no limit (expecting more input).
			return -1L;	// not done
		}
		// limit reached. 
		return 0L;	// done
	}

	@Override
	public int codePoints() {
		return this.codePoints;
	}

	//@Override
	//public int outputLimit() {
	//	return this.outputLimit;
	//}
	//@Override
	//public Decoder outputLimit(int limit) {
	//	if (limit < this.codePointsTotal) {
	//		throw new IllegalArgumentException("limit must be >= codePointsTotal(): " + limit);
	//	}
	//	this.outputLimit = limit;
	//	return this;
	//}
	@Override
	public long bytes() {
		return this.bytes;
	}

	@Override
	public long codePointsTotal() {
		return this.codePointsTotal;
	}

	@Override
	public long bytesTotal() {
		return this.bytesTotal;
	}

	@Override
	public long inputLimit() {
		return this.inputLimit;
	}

	@Override
	public Decoder inputLimit(long limit) {
		if (limit < this.bytesTotal) {
			throw new IllegalArgumentException("limit must be >= bytesTotal(): " + limit);
		}
		this.inputLimit = limit;
		return this;
	}

	@Override
	public Decoder parser(Parser parser) {
		this.stop = parser == null ? STOP : parser;
		return this;
	}

	@Override
	public Parser parser() {
		Parser p = this.stop;
		return p == STOP ? null : p;
	}

	@Override
	public Decoder errorHandler(ErrorHandler errorHandler) {
		this.err = errorHandler == null ? ERR : errorHandler;
		return this;
	}

	@Override
	public ErrorHandler errorHandler() {
		ErrorHandler p = this.err;
		return p == ERR ? null : p;
	}

	@Override
	public Decoder reset() {
		// no need to clear counts.
		// new entries always push out the old ones first.
		this.bytes = 0L;
		this.bytesTotal = 0L;
		this.codePoints = 0;
		this.codePointsTotal = 0L;
		return this;
	}
}
