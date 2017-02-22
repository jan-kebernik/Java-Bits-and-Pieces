/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

/**
 *
 * @author Jan Kebernik
 */
final class DecoderISO_8859_1 extends SingleByteDecoder {
	
	@Override
	char translate(byte inputByte) {
		return (char) (inputByte & 0xff);
	}

	@Override
	boolean mappable(char i) {
		return true;
	}

	@Override
	public Encoding encoding() {
		return Encoding.ISO_8859_1;
	}
}
