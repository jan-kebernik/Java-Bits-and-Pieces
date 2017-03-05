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
	final int _encode(char[] src, byte[] buf, int off, int len) {
		int _offset = this.offset;
		int _limit = this.limit;
		char hs = this.surr;
		if (hs != NONE) {
			// have a high surrogate waiting
			if (_offset == _limit) {
				return UNDERFLOW;
			}
			this.surr = NONE;
			if (Character.isLowSurrogate(src[_offset])) {
				// two-char error
				this.offset = ++_offset;
			}
			return ERROR;
		}
		int y = off;
		int m = off + len;
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
						if (y == off) {
							return UNDERFLOW;
						}
						return y - off;
					}
					if (Character.isLowSurrogate(src[_offset])) {
						// two-char error
						_offset++;
					}
				}
				this.offset = _offset;
				if (y == off) {
					return ERROR;
				}
				this.statePending = ERROR;
				return y - off;
			}
			buf[y++] = (byte) (r & 0xff);
		}
		this.offset = _offset;
		if (_offset == _limit && y == off) {
			return UNDERFLOW;
		}
		return y - off;
	}

	@Override
	final int _encode(CharSequence src, byte[] buf, int off, int len) {
		int _offset = this.offset;
		int _limit = this.limit;
		char hs = this.surr;
		if (hs != NONE) {
			// have a high surrogate waiting
			if (_offset == _limit) {
				return UNDERFLOW;
			}
			this.surr = NONE;
			if (Character.isLowSurrogate(src.charAt(_offset))) {
				// two-char error
				this.offset = ++_offset;
			}
			return ERROR;
		}
		int y = off;
		int m = off + len;
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
						if (y == off) {
							return UNDERFLOW;
						}
						return y - off;
					}
					if (Character.isLowSurrogate(src.charAt(_offset))) {
						// two-char error
						_offset++;
					}
				}
				this.offset = _offset;
				if (y == off) {
					return ERROR;
				}
				this.statePending = ERROR;
				return y - off;
			}
			buf[y++] = (byte) (r & 0xff);
		}
		this.offset = _offset;
		if (_offset == _limit && y == off) {
			return UNDERFLOW;
		}
		return y - off;
	}

	@Override
	public final int pendingOutput() {
		return 0;
	}
}
