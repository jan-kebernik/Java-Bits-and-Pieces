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
final class EncoderASCII extends SingleByteEncoder {

	@Override
	public Encoding encoding() {
		return Encoding.ASCII;
	}
}
