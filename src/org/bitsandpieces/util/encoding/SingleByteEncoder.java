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
abstract class SingleByteEncoder extends AbstractEncoder {

	static final int INVALID_ID = 0;

	int convert(int c) {
		return INVALID_ID;
	}

	boolean isInDirectRange(char c) {
		return c < '\u0080';
	}

	private static final char NONE = 0;

	private char surr = NONE;

	@Override
	final int _encode(char[] src, byte[] buf, int off, int len, int numCodePoints) {
		int _offsetOld = this.offset;
		int _offset = _offsetOld;
		int _limit = this.limit;
		char hs = this.surr;
		if (hs != NONE) {
			// have a high surrogate waiting
			if (_offset == _limit) {
				return Encoding.UNDERFLOW;
			}
			this.surr = NONE;
			if (Character.isLowSurrogate(src[_offset])) {
				// two-char error
				this.offset = ++_offset;
				this.chars++;
			}
			return Encoding.ERROR;
		}
		int y = off;
		int m = off + Math.min(len, numCodePoints);
		for (; _offset < _limit && y < m;) {
			char c = src[_offset++];
			if (isInDirectRange(c)) {
				buf[y++] = (byte) c;
				continue;
			}
			int r = convert(c);
			if (r == INVALID_ID || (r >>> 16) != c) {
				// unmappable
				if (Character.isHighSurrogate(c)) {
					if (_offset == _limit) {
						this.surr = c;
						this.offset = _offset;
						this.chars += (_offset - _offsetOld);
						if (y == off) {
							return Encoding.UNDERFLOW;
						}
						int n = y - off;
						this.codePoints += n;
						return n;
					}
					if (Character.isLowSurrogate(src[_offset])) {
						// two-char error
						_offset++;
					}
				}
				this.offset = _offset;
				this.chars += (_offset - _offsetOld);
				if (y == off) {
					return Encoding.ERROR;
				}
				this.statePending = Encoding.ERROR;
				int n = y - off;
				this.codePoints += n;
				return n;
			}
			buf[y++] = (byte) (r & 0xff);
		}
		this.offset = _offset;
		this.chars += (_offset - _offsetOld);
		if (_offset == _limit && y == off) {
			return Encoding.UNDERFLOW;
		}
		int n = y - off;
		this.codePoints += n;
		return n;
	}

	@Override
	final int _encode(CharSequence src, byte[] buf, int off, int len, int numCodePoints) {
		int _offsetOld = this.offset;
		int _offset = _offsetOld;
		int _limit = this.limit;
		char hs = this.surr;
		if (hs != NONE) {
			// have a high surrogate waiting
			if (_offset == _limit) {
				return Encoding.UNDERFLOW;
			}
			this.surr = NONE;
			if (Character.isLowSurrogate(src.charAt(_offset))) {
				// two-char error
				this.offset = ++_offset;
				this.chars++;
			}
			return Encoding.ERROR;
		}
		int y = off;
		int m = off + Math.min(len, numCodePoints);
		for (; _offset < _limit && y < m;) {
			char c = src.charAt(_offset++);
			if (isInDirectRange(c)) {
				buf[y++] = (byte) c;
				continue;
			}
			int r = convert(c);
			if (r == INVALID_ID || (r >>> 16) != c) {
				// unmappable
				if (Character.isHighSurrogate(c)) {
					if (_offset == _limit) {
						this.surr = c;
						this.offset = _offset;
						this.chars += (_offset - _offsetOld);
						if (y == off) {
							return Encoding.UNDERFLOW;
						}
						int n = y - off;
						this.codePoints += n;
						return n;
					}
					if (Character.isLowSurrogate(src.charAt(_offset))) {
						// two-char error
						_offset++;
					}
				}
				this.offset = _offset;
				this.chars += (_offset - _offsetOld);
				if (y == off) {
					return Encoding.ERROR;
				}
				this.statePending = Encoding.ERROR;
				int n = y - off;
				this.codePoints += n;
				return n;
			}
			buf[y++] = (byte) (r & 0xff);
		}
		this.offset = _offset;
		this.chars += (_offset - _offsetOld);
		if (_offset == _limit && y == off) {
			return Encoding.UNDERFLOW;
		}
		int n = y - off;
		this.codePoints += n;
		return n;
	}

	@Override
	public final int pendingOutput() {
		return 0;
	}

	@Override
	public Encoder reset() {
		super.reset();
		this.surr = NONE;
		return this;
	}

	@Override
	public final int pendingInput() {
		return this.surr != NONE ? 1 : 0;
	}

	@Override
	public final int needsInput() {
		return this.surr != NONE ? 1 : 0;
	}
}
