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
	final int _decode(byte[] src, char[] dest, int off, int len) {
		int _offset = this.offset;
		int _limit = this.limit;
		int availIn = _limit - _offset;
		if (availIn == 0) {
			return UNDERFLOW;
		}
		int y = off;
		for (int m = _offset + Math.min(availIn, len); _offset < m;) {
			char r = convert(src[_offset++]);
			if (r == NO_DEF) {
				if (y == off) {
					this.offset = _offset;
					return ERROR;
				}
				this.statePending = ERROR;
				break;
			}
			dest[y++] = r;
		}
		this.offset = _offset;	// update internal state
		return y - off;
	}

	@Override
	final int _decode(byte[] src, Appendable dest, int len) throws UncheckedIOException {
		int _offset = this.offset;
		try {
			int _limit = this.limit;
			int availIn = _limit - _offset;
			if (availIn == 0) {
				return UNDERFLOW;
			}
			int y = 0;
			for (int m = _offset + Math.min(availIn, len); _offset < m;) {
				char r = convert(src[_offset++]);
				if (r == NO_DEF) {
					if (y == 0) {
						return ERROR;
					}
					this.statePending = ERROR;
					break;
				}
				dest.append(r);
			}
			return y;
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		} finally {
			this.offset = _offset;
		}
	}

	@Override
	public final int pendingOutput() {
		return 0;
	}
}
