/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

/**
 * Signals that an I/O exception of some sort has occurred. This class is the
 * general class of exceptions produced by failed or interrupted I/O operations.
 *
 * @author pp
 */
public class IOException extends RuntimeException {

	/**
	 * Constructs an {@code IOException} with {@code null} as its error detail
	 * message.
	 */
	public IOException() {
	}

	/**
	 * Constructs an {@code IOException} with the specified detail message.
	 *
	 * @param message The detail message (which is saved for later retrieval by
	 * the {@link #getMessage()} method)
	 */
	public IOException(String message) {
		super(message);
	}

	/**
	 * Constructs an {@code IOException} with the specified detail message and
	 * cause.
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
	public IOException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs an {@code IOException} with the specified cause and a detail
	 * message of {@code (cause == null ? null : cause.toString())} (which
	 * typically contains the class and detail message of {@code cause}). This
	 * constructor is useful for exceptions that are little more than wrappers
	 * for other throwables.
	 *
	 * @param cause The cause (which is saved for later retrieval by the
	 * {@link #getCause()} method). (A null value is permitted, and indicates
	 * that the cause is nonexistent or unknown.)
	 */
	public IOException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new {@code IOException} with the specified detail message,
	 * cause, suppression enabled or disabled, and writable stack trace enabled
	 * or disabled.
	 *
	 * @param message the detail message.
	 * @param cause the cause. (A {@code null} value is permitted, and indicates
	 * that the cause is nonexistent or unknown.)
	 * @param enableSuppression whether or not suppression is enabled or
	 * disabled
	 * @param writableStackTrace whether or not the stack trace should be
	 * writable
	 */
	protected IOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
