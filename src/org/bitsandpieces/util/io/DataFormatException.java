/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

/**
 * Unchecked variant. Signals that a data format error has occurred.
 *
 * @author Jan Kebernik
 */
public class DataFormatException extends RuntimeException {

	/**
	 * Constructs an {@code DataFormatException} with {@code null} as its error
	 * detail message.
	 */
	public DataFormatException() {
	}

	/**
	 * Constructs an {@code DataFormatException} with the specified detail
	 * message.
	 *
	 * @param message The detail message (which is saved for later retrieval by
	 * the {@link #getMessage()} method)
	 */
	public DataFormatException(String message) {
		super(message);
	}

	/**
	 * Constructs an {@code DataFormatException} with the specified detail
	 * message and cause.
	 * <p>
	 * Note that the detail message associated with {@code cause} is
	 * <i>not</i> automatically incorporated into this exception's detail
	 * message.
	 *
	 * @param message The detail message (which is saved for later retrieval by
	 * the {@link #getMessage()} method)
	 *
	 * @param cause The cause (which is saved for later retrieval by the
	 * {@link #getCause()} method). (A null value is permitted, and indicates
	 * that the cause is nonexistent or unknown.)
	 */
	public DataFormatException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs an {@code DataFormatException} with the specified cause and a
	 * detail message of {@code (cause == null ? null : cause.toString())}
	 * (which typically contains the class and detail message of {@code cause}).
	 * This constructor is useful for exceptions that are little more than
	 * wrappers for other throwables.
	 *
	 * @param cause The cause (which is saved for later retrieval by the
	 * {@link #getCause()} method). (A null value is permitted, and indicates
	 * that the cause is nonexistent or unknown.)
	 */
	public DataFormatException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new {@code DataFormatException} with the specified detail
	 * message, cause, suppression enabled or disabled, and writable stack trace
	 * enabled or disabled.
	 *
	 * @param message the detail message.
	 * @param cause the cause. (A {@code null} value is permitted, and indicates
	 * that the cause is nonexistent or unknown.)
	 * @param enableSuppression whether or not suppression is enabled or
	 * disabled
	 * @param writableStackTrace whether or not the stack trace should be
	 * writable
	 */
	protected DataFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
