/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.zip;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 * Dynamic wrapper for both native and pure-Java implementations.
 *
 * Even an 8-slice CRC32 pure-Java implementation only achieves around 66% of
 * the throughput of the native version. As such, while reflection is allowed,
 * the native version is preferred. If reflection fails for any reason, will
 * fall back on the pure-Java implementation.
 *
 * @author Jan Kebernik
 */
final class HybridCRC32 extends CRC32 implements OpenChecksum {

	private static final String FAIL_MESSAGE = "Reflection failed. Falling back on general solution.";

	private static final Field FIELD = getAccessibleField();

	private static Field getAccessibleField() {
		try {
			Field f = CRC32.class.getDeclaredField("crc");
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException | SecurityException ex) {
			Logger.getLogger(HybridCRC32.class.getName()).log(Level.WARNING, FAIL_MESSAGE, ex);
		}
		return null;
	}

	@Override
	public OpenChecksum copy() {
		if (FIELD != null) {
			try {
				int v = FIELD.getInt(this);
				HybridCRC32 b = new HybridCRC32();
				FIELD.setInt(b, v);
				return b;
			} catch (IllegalArgumentException | IllegalAccessException ex) {
				Logger.getLogger(CRC32.class.getName()).log(Level.WARNING, FAIL_MESSAGE, ex);
			}
		}
		return new OpenCRC32((int) this.getValue());
	}

	@Override
	public OpenChecksum reset(long value) {
		if (FIELD != null) {
			try {
				FIELD.setInt(this, (int) value);
				return this;
			} catch (IllegalArgumentException | IllegalAccessException ex) {
				Logger.getLogger(CRC32.class.getName()).log(Level.WARNING, FAIL_MESSAGE, ex);
			}
		}
		return new OpenCRC32((int) value);
	}
}
