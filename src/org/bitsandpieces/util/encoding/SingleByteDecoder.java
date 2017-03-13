/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 *
 * @author pp
 */
abstract class SingleByteDecoder extends AbstractDecoder {

	static final char NO_DEF = 0xFFFF;
	
	abstract char convert(byte b);

	@Override
	final int _decode(byte[] src, char[] dest, int off, int len, int numCodePoints) {
		int _offsetOld = this.offset;
		int _offset = _offsetOld;
		int _limit = this.limit;
		int availIn = _limit - _offset;
		if (availIn == 0) {
			return Encoding.UNDERFLOW;
		}
		int availOut = Math.min(len, numCodePoints);
		int y = off;
		for (int m = _offset + Math.min(availIn, availOut); _offset < m;) {
			char r = convert(src[_offset++]);
			if (r == NO_DEF) {
				if (y == off) {
					this.bytes += (_offset - _offsetOld);
					this.offset = _offset;
					return Encoding.ERROR;
				}
				this.statePending = Encoding.ERROR;
				break;
			}
			dest[y++] = r;
		}
		this.bytes += (_offset - _offsetOld);
		this.offset = _offset;	// update internal state
		int n = y - off;
		this.codePoints += n;
		return n;
	}

	@Override
	final int _decode(byte[] src, Appendable dest, int len, int numCodePoints) throws UncheckedIOException {
		try {
			int _offsetOld = this.offset;
			int _offset = _offsetOld;
			int _limit = this.limit;
			int availIn = _limit - _offset;
			if (availIn == 0) {
				return Encoding.UNDERFLOW;
			}
			int availOut = Math.min(len, numCodePoints);
			int y = 0;
			for (int m = _offset + Math.min(availIn, availOut); _offset < m;) {
				char r = convert(src[_offset++]);
				if (r == NO_DEF) {
					if (y == 0) {
						this.bytes += (_offset - _offsetOld);
						this.offset = _offset;
						return Encoding.ERROR;
					}
					this.statePending = Encoding.ERROR;
					break;
				}
				dest.append(r);
				y++;
			}
			this.bytes += (_offset - _offsetOld);
			this.offset = _offset;	// update internal state
			this.codePoints += y;
			return y;
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public final int pendingOutput() {
		return 0;
	}

	@Override
	public final int needsInput() {
		// partial input impossible
		return 0;
	}

	@Override
	public final int pendingInput() {
		// partial input impossible
		return 0;
	}
}
