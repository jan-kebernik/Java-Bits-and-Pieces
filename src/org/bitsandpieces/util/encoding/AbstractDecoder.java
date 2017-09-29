/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

/**
 *
 * @author pp
 */
abstract class AbstractDecoder implements Decoder {

	long bytes, chars;
	int offset, limit;
	int pendingError;

	byte[] src;

	abstract int _decode(byte[] src, Appendable dest, int maxChars, int maxCodePoints, int _offset, int _limit);

	abstract int _decode(byte[] src, char[] dest, int off, int maxChars, int maxCodePoints, int _offset, int _limit);

	@Override
	public final int decode(char[] dest, int off) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		int s = this.pendingError;
		if (s != 0) {
			// error must be returned even if no chars are to be produced
			this.pendingError = 0;
			return s;
		}
		int n = dest.length - off;
		if (n == 0) {
			// produce no chars
			return 0;
		}
		return _decode(this.src, dest, off, n, Integer.MAX_VALUE, this.offset, this.limit);
	}

	@Override
	public final int decode(int inputBytes, char[] dest, int off) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException();
		}
		int _offset = this.offset;
		int _limit = this.limit;
		if (inputBytes > _limit - _offset) {
			throw new IllegalArgumentException();
		}
		int s = this.pendingError;
		if (s != 0) {
			// error must be returned even if no chars are to be produced
			this.pendingError = 0;
			return s;
		}
		int n = dest.length - off;
		if (n == 0) {
			// produce no chars
			return 0;
		}
		return _decode(this.src, dest, off, n, Integer.MAX_VALUE, _offset, _offset + inputBytes);
	}

	@Override
	public final int decode(char[] dest, int off, int maxChars, int maxCodePoints) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (maxChars < 0) {
			throw new IllegalArgumentException();
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException();
		}
		int s = this.pendingError;
		if (s != 0) {
			// error must be returned even if no chars are to be produced
			this.pendingError = 0;
			return s;
		}
		int n = Math.min(dest.length - off, maxChars);
		if (n == 0 || maxCodePoints == 0) {
			// produce no chars, or resolve no code points
			return 0;
		}
		return _decode(this.src, dest, off, n, maxCodePoints, this.offset, this.limit);
	}
	
	@Override
	public final int decode(int inputBytes, char[] dest, int off, int maxChars, int maxCodePoints) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException();
		}
		int _offset = this.offset;
		int _limit = this.limit;
		if (inputBytes > _limit - _offset) {
			throw new IllegalArgumentException();
		}
		if (maxChars < 0) {
			throw new IllegalArgumentException();
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException();
		}
		int s = this.pendingError;
		if (s != 0) {
			// error must be returned even if no chars are to be produced
			this.pendingError = 0;
			return s;
		}
		int n = Math.min(dest.length - off, maxChars);
		if (n == 0 || maxCodePoints == 0) {
			// produce no chars, or resolve no code points
			return 0;
		}
		return _decode(this.src, dest, off, n, maxCodePoints, _offset, _offset + inputBytes);
	}

	@Override
	public final int decode(Appendable dest) {
		if (dest == null) {
			throw new NullPointerException();
		}
		int s = this.pendingError;
		if (s != 0) {
			this.pendingError = 0;
			return s;
		}
		return _decode(this.src, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, this.offset, this.limit);
	}

	@Override
	public final int decode(int inputBytes, Appendable dest) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException();
		}
		int _offset = this.offset;
		int _limit = this.limit;
		if (inputBytes > _limit - _offset) {
			throw new IllegalArgumentException();
		}
		int s = this.pendingError;
		if (s != 0) {
			// error must be returned even if no chars are to be produced
			this.pendingError = 0;
			return s;
		}
		return _decode(this.src, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, _offset, _offset + inputBytes);
	}

	@Override
	public final int decode(int inputBytes, Appendable dest, int maxChars, int maxCodePoints) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException();
		}
		int _offset = this.offset;
		int _limit = this.limit;
		if (inputBytes > _limit - _offset) {
			throw new IllegalArgumentException();
		}
		if (maxChars < 0) {
			throw new IllegalArgumentException();
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException();
		}
		int s = this.pendingError;
		if (s != 0) {
			// error must be returned even if no chars are to be produced
			this.pendingError = 0;
			return s;
		}
		if (maxChars == 0 || maxCodePoints == 0) {
			// produce no chars, or resolve no code points
			return 0;
		}
		return _decode(this.src, dest, maxChars, maxCodePoints, _offset, _offset + inputBytes);
	}
	
	@Override
	public final int decode(Appendable dest, int maxChars, int maxCodePoints) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (maxChars < 0) {
			throw new IllegalArgumentException();
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException();
		}
		int s = this.pendingError;
		if (s != 0) {
			// error must be returned even if no chars are to be produced
			this.pendingError = 0;
			return s;
		}
		if (maxChars == 0 || maxCodePoints == 0) {
			// produce no chars, or resolve no code points
			return 0;
		}
		return _decode(this.src, dest, maxChars, maxCodePoints, this.offset, this.limit);
	}

	@Override
	public final Decoder setInput(byte[] src) {
		if (src == null) {
			throw new NullPointerException();
		}
		this.src = src;
		this.offset = 0;
		this.limit = src.length;
		return this;
	}

	@Override
	public final Decoder setInput(byte[] src, int off, int len) {
		if (src == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > src.length - len) {
			throw new IndexOutOfBoundsException();
		}
		this.src = src;
		this.offset = off;
		this.limit = off + len;
		return this;
	}

	@Override
	public final int inputRemaining() {
		return this.limit - this.offset;
	}

	@Override
	public final long charsProduced() {
		return this.chars;
	}

	@Override
	public final long bytesConsumed() {
		return this.bytes;
	}

	//@Override
	//public final long codePointsResolved() {
	//	return this.codePoints;
	//}

	@Override
	public final Decoder dropInput() {
		this.offset = 0;
		this.limit = 0;
		this.bytes = 0;
		this.src = null;
		return this;
	}

	protected final void _reset() {
		this.bytes = 0L;
		this.chars = 0L;
		//this.codePoints = 0L;
		this.pendingError = 0;
	}

	@Override
	public Decoder reset() {
		_reset();
		return dropInput();
	}
}
