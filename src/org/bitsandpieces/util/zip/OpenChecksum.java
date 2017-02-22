/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.zip;

import java.util.zip.Checksum;

/**
 * A {@link Checksum} whose value can be set arbitrarily.
 *
 * @author Jan Kebernik
 */
public interface OpenChecksum extends Checksum {

	/**
	 * Returns a new {@code OpenChecksum} instance with the same value.
	 *
	 * @return a new {@code OpenChecksum} instance with the same value.
	 */
	public OpenChecksum copy();

	/**
	 * Resets this {@code Checksum} to the specified initial value.
	 *
	 * @param value the new value of this {@code Checksum}.
	 * @return this {@code OpenChecksum}.
	 */
	public OpenChecksum reset(long value);
}
