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
final class DecoderASCII extends SingleByteDecoder {

	@Override
	char convert(byte b) {
		char c = ((char) (b & 0xff));
		return c < '\u0080' ? c : NO_DEF;
	}

	@Override
	public Encoding encoding() {
		return Encoding.ASCII;
	}
}
