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
abstract class DualByteEncoder extends AbstractEncoder {

	private static final char NONE = 0;

	private int bytePending;

	private char surr = NONE;

	abstract char get(int i);

	@Override
	int _encode(char[] src, byte[] buf, int off, int len, int numCodePoints) {
		int _numCP = 0;
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
		int m = off + len;
		int p = this.bytePending;
		if (p != 0) {
			buf[y++] = (byte) (p & 0xff);
			this.bytePending = 0;
			_numCP++;
		}
		for (; _offset < _limit && y < m && _numCP < numCodePoints;) {
			char c = src[_offset++];
			char r = get(c);
			if (r == DualByteDecoder.NO_DEF) {
				// unmappable
				if (Character.isHighSurrogate(c)) {
					if (_offset == _limit) {
						this.surr = c;
						this.offset = _offset;
						this.chars += (_offset - _offsetOld);
						if (y == off) {
							return Encoding.UNDERFLOW;
						}
						return y - off;
					}
					if (Character.isLowSurrogate(src[_offset])) {
						// two-char error
						_offset++;
					}
				}
				this.offset = _offset;	// update internal state
				this.chars += (_offset - _offsetOld);
				if (y == off) {
					// no bytes produced yet
					return Encoding.ERROR;
				}
				this.statePending = Encoding.ERROR;
				return y - off;
			}
			int lead = r >>> 8;
			if (lead == 0) {
				// one byte
				buf[y++] = (byte) r;
				_numCP++;
				continue;
			}
			// two bytes
			int trail = r & 0xff;
			buf[y++] = (byte) lead;
			if (y == m) {
				this.offset = _offset;	// update internal state
				this.chars += (_offset - _offsetOld);
				this.bytePending = 0x80000000 | trail;
				return y - off;
			}
			buf[y++] = (byte) trail;
			_numCP++;
		}
		this.chars += (_offset - _offsetOld);
		this.offset = _offset;	// update internal state
		if (_offset == _limit && y == off) {
			return Encoding.UNDERFLOW;
		}
		return y - off;
	}

	@Override
	int _encode(CharSequence src, byte[] buf, int off, int len, int numCodePoints) {
		int _numCP = 0;
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
		int m = off + len;
		int p = this.bytePending;
		if (p != 0) {
			buf[y++] = (byte) (p & 0xff);
			this.bytePending = 0;
			_numCP++;
		}
		for (; _offset < _limit && y < m && _numCP < numCodePoints;) {
			char c = src.charAt(_offset++);
			char r = get(c);
			if (r == DualByteDecoder.NO_DEF) {
				// unmappable
				if (Character.isHighSurrogate(c)) {
					if (_offset == _limit) {
						this.surr = c;
						this.offset = _offset;
						this.chars += (_offset - _offsetOld);
						if (y == off) {
							return Encoding.UNDERFLOW;
						}
						return y - off;
					}
					if (Character.isLowSurrogate(src.charAt(_offset))) {
						// two-char error
						_offset++;
					}
				}
				this.offset = _offset;	// update internal state
				this.chars += (_offset - _offsetOld);
				if (y == off) {
					// no bytes produced yet
					return Encoding.ERROR;
				}
				this.statePending = Encoding.ERROR;
				return y - off;
			}
			int lead = r >>> 8;
			if (lead == 0) {
				// one byte
				buf[y++] = (byte) r;
				_numCP++;
				continue;
			}
			// two bytes
			int trail = r & 0xff;
			buf[y++] = (byte) lead;
			if (y == m) {
				this.offset = _offset;	// update internal state
				this.chars += (_offset - _offsetOld);
				this.bytePending = 0x80000000 | trail;
				return y - off;
			}
			buf[y++] = (byte) trail;
			_numCP++;
		}
		this.offset = _offset;	// update internal state
		this.chars += (_offset - _offsetOld);
		if (_offset == _limit && y == off) {
			return Encoding.UNDERFLOW;
		}
		return y - off;
	}

	@Override
	public int pendingOutput() {
		// always 0 or 1 byte is pending
		return this.bytePending >>> 31;
	}

	@Override
	public Encoder reset() {
		super.reset();
		this.surr = NONE;
		this.bytePending = 0;
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
