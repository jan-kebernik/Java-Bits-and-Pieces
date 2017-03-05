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
final class DecoderISO_8859_1 extends SingleByteDecoder {

	@Override
	char convert(byte b) {
		return ((char) (b & 0xff));
	}

	@Override
	public Encoding encoding() {
		return Encoding.ISO_8859_1;
	}
}
