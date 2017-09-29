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
abstract class AbstractEncoder implements Encoder {

	long bytes, chars, codePoints;
	int offset, limit;
	int pendingError;

	char[] srcArr;
	CharSequence srcSeq;

	abstract int _encode(CharSequence src, byte[] dest, int off, int maxBytes, int maxCodePoints, int _offset, int _limit);

	abstract int _encode(char[] src, byte[] dest, int off, int maxBytes, int maxCodePoints, int _offset, int _limit);

	@Override
	public final int encode(byte[] dest, int off) {
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
		CharSequence src = this.srcSeq;
		return src != null
				? _encode(src, dest, off, n, Integer.MAX_VALUE, this.offset, this.limit)
				: _encode(this.srcArr, dest, off, n, Integer.MAX_VALUE, this.offset, this.limit);
	}

	@Override
	public final int encode(int inputChars, byte[] dest, int off) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (inputChars < 0) {
			throw new IllegalArgumentException();
		}
		int _offset = this.offset;
		int _limit = this.limit;
		if (inputChars > _limit - _offset) {
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
		CharSequence src = this.srcSeq;
		return src != null
				? _encode(src, dest, off, n, Integer.MAX_VALUE, _offset, _offset + inputChars)
				: _encode(this.srcArr, dest, off, n, Integer.MAX_VALUE, _offset, _offset + inputChars);
	}

	@Override
	public final int encode(byte[] dest, int off, int maxBytes, int maxCodePoints) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (maxBytes < 0) {
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
		int n = Math.min(dest.length - off, maxBytes);
		if (n == 0 || maxCodePoints == 0) {
			// produce no chars or resolve no code points
			return 0;
		}
		CharSequence src = this.srcSeq;
		return src != null
				? _encode(src, dest, off, n, maxCodePoints, this.offset, this.limit)
				: _encode(this.srcArr, dest, off, n, maxCodePoints, this.offset, this.limit);
	}

	@Override
	public final int encode(int inputChars, byte[] dest, int off, int maxBytes, int maxCodePoints) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > dest.length) {
			throw new IndexOutOfBoundsException();
		}
		if (inputChars < 0) {
			throw new IllegalArgumentException();
		}
		int _offset = this.offset;
		int _limit = this.limit;
		if (inputChars > _limit - _offset) {
			throw new IllegalArgumentException();
		}
		if (maxBytes < 0) {
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
		int n = Math.min(dest.length - off, maxBytes);
		if (n == 0 || maxCodePoints == 0) {
			// produce no chars or resolve no code points
			return 0;
		}
		CharSequence src = this.srcSeq;
		return src != null
				? _encode(src, dest, off, n, maxCodePoints, _offset, _offset + inputChars)
				: _encode(this.srcArr, dest, off, n, maxCodePoints, _offset, _offset + inputChars);
	}

	@Override
	public final Encoder setInput(CharSequence src) {
		if (src == null) {
			throw new NullPointerException();
		}
		this.srcArr = null;
		this.srcSeq = src;
		this.offset = 0;
		this.limit = src.length();
		return this;
	}

	@Override
	public final Encoder setInput(CharSequence src, int off, int len) {
		if (src == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > src.length() - len) {
			throw new IndexOutOfBoundsException();
		}
		this.srcArr = null;
		this.srcSeq = src;
		this.offset = off;
		this.limit = off + len;
		return this;
	}

	@Override
	public final Encoder setInput(char[] src) {
		if (src == null) {
			throw new NullPointerException();
		}
		this.srcArr = src;
		this.srcSeq = null;
		this.offset = 0;
		this.limit = src.length;
		return this;
	}

	@Override
	public final Encoder setInput(char[] src, int off, int len) {
		if (src == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > src.length - len) {
			throw new IndexOutOfBoundsException();
		}
		this.srcArr = src;
		this.srcSeq = null;
		this.offset = off;
		this.limit = off + len;
		return this;
	}

	@Override
	public final int inputRemaining() {
		return this.limit - this.offset;
	}

	@Override
	public final long bytesProduced() {
		return this.bytes;
	}

	@Override
	public final long charsConsumed() {
		return this.chars;
	}

	@Override
	public final long codePointsResolved() {
		return this.codePoints;
	}

	@Override
	public final Encoder dropInput() {
		this.offset = 0;
		this.limit = 0;
		this.srcArr = null;
		this.srcSeq = null;
		return this;
	}

	protected final void _reset() {
		this.bytes = 0L;
		this.chars = 0L;
		this.codePoints = 0L;
		this.pendingError = 0;
	}

	@Override
	public Encoder reset() {
		_reset();
		return dropInput();
	}
}
