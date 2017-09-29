/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.io.UncheckedIOException;

/**
 * {@code Decoder}s are objects that decode {@code byte}s to UTF-16
 * {@code char}s.
 * <p>
 * Both input and output are provided purely by the calling classes, making
 * {@code Decoder}s very light-weight. Calling classes are free to react to
 * indiviual errors as they are encountered.
 * </p>
 *
 * @author Jan Kebernik
 */
public interface Decoder {

	/**
	 * Returns this {@code Decoder}'s {@link Encoding}.
	 *
	 * @return this {@code Decoder}'s {@code Encoding}.
	 */
	Encoding encoding();

	/**
	 * Decodes the current input into the specified array.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. Partial code points
	 * may need to be stored by this {@code Decoder} until more input is made
	 * available or more output can be stored.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param dest the array into which to decode.
	 * @param off the offset into the array.
	 * @return <ul><li>&gt 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Decoder} until more input or output is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point. If
	 * {@link #pendingOutput() pendingOutput()} reports a non-zero value, then
	 * the provided array range did not provide enough space to fully store all
	 * output.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IndexOutOfBoundsException if {@code off < 0} or
	 * {@code off > dest.length}.
	 */
	int decode(char[] dest, int off);

	/**
	 * Decodes the current input into the specified array.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. Partial code points
	 * may need to be stored by this {@code Decoder} until more input is made
	 * available or more output can be stored.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param inputBytes the desired number of {@code byte}s to consume. Must be
	 * less than or equal to {@link #inputRemaining() inputRemaining()}.
	 * @param dest the array into which to decode.
	 * @param off the offset into the array.
	 * @return <ul><li>&gt 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Decoder} until more input or output is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point. If
	 * {@link #pendingOutput() pendingOutput()} reports a non-zero value, then
	 * the provided array range did not provide enough space to fully store all
	 * output.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IllegalArgumentException if {@code inputBytes < 0} or
	 * {@code inputBytes > inputRemaining()}.
	 * @throws IndexOutOfBoundsException if {@code off < 0} or
	 * {@code off > dest.length}.
	 */
	int decode(int inputBytes, char[] dest, int off);

	/**
	 * Decodes the current input into the specified array.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. Partial code points
	 * may need to be stored by this {@code Decoder} until more input is made
	 * available or more output can be stored.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param dest the array into which to decode.
	 * @param off the offset into the array.
	 * @param maxChars the maximum number of {@code char}s to store.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @return <ul><li>&gt 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Decoder} until more input or output is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point. If
	 * {@link #pendingOutput() pendingOutput()} reports a non-zero value, then
	 * the provided array range did not provide enough space to fully store all
	 * output.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IllegalArgumentException if {@code inputBytes < 0}, if
	 * {@code inputBytes > inputRemaining()}, if {@code maxChars < 0} or if
	 * {@code maxCodePoints < 0}.
	 * @throws IndexOutOfBoundsException if {@code off < 0} or
	 * {@code off > dest.length}.
	 */
	int decode(char[] dest, int off, int maxChars, int maxCodePoints);

	/**
	 * Decodes the current input into the specified array.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. Partial code points
	 * may need to be stored by this {@code Decoder} until more input is made
	 * available or more output can be stored.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param inputBytes the desired number of {@code byte}s to consume. Must be
	 * less than or equal to {@link #inputRemaining() inputRemaining()}.
	 * @param dest the array into which to decode.
	 * @param off the offset into the array.
	 * @param maxChars the maximum number of {@code char}s to store.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @return <ul><li>&gt 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Decoder} until more input or output is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point. If
	 * {@link #pendingOutput() pendingOutput()} reports a non-zero value, then
	 * the provided array range did not provide enough space to fully store all
	 * output.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IllegalArgumentException if {@code inputBytes < 0}, if
	 * {@code inputBytes > inputRemaining()}, if {@code maxChars < 0} or if
	 * {@code maxCodePoints < 0}.
	 * @throws IndexOutOfBoundsException if {@code off < 0} or
	 * {@code off > dest.length}.
	 */
	int decode(int inputBytes, char[] dest, int off, int maxChars, int maxCodePoints);

	/**
	 * Decodes the current input to the specified {@code Appendable}.
	 * <p>
	 * The method will terminate when all available input is processed or when
	 * an error occurs. Partial code points may need to be stored by this
	 * {@code Decoder} until more input is made available.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param dest the {@code Appendable} to which to decode.
	 * @return <ul><li>&gt 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Decoder} until more input is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 */
	int decode(Appendable dest) throws UncheckedIOException;

	/**
	 * Decodes the current input to the specified {@code Appendable}.
	 * <p>
	 * The method will terminate when all available input is processed or when
	 * an error occurs. Partial code points may need to be stored by this
	 * {@code Decoder} until more input is made available.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param inputBytes the desired number of {@code byte}s to consume. Must be
	 * less than or equal to {@link #inputRemaining() inputRemaining()}.
	 * @param dest the {@code Appendable} to which to decode.
	 * @return <ul><li>&gt 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Decoder} until more input is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IllegalArgumentException if {@code inputBytes < 0} or
	 * {@code inputBytes > inputRemaining()}.
	 */
	int decode(int inputBytes, Appendable dest) throws UncheckedIOException;

	/**
	 * Decodes the current input to the specified {@code Appendable}.
	 * <p>
	 * The method will terminate when all available input is processed or when
	 * an error occurs. Partial code points may need to be stored by this
	 * {@code Decoder} until more input is made available.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param dest the {@code Appendable} to which to decode.
	 * @param maxChars the maximum number of {@code char}s to append.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @return <ul><li>&gt 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Decoder} until more input is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IllegalArgumentException if {@code inputBytes < 0} or
	 * {@code inputBytes > inputRemaining()}.
	 * @throws IllegalArgumentException if {@code maxChars < 0} or
	 * {@code maxCodePoints < 0}.
	 */
	int decode(Appendable dest, int maxChars, int maxCodePoints) throws UncheckedIOException;

	/**
	 * Decodes the current input to the specified {@code Appendable}.
	 * <p>
	 * The method will terminate when all available input is processed or when
	 * an error occurs. Partial code points may need to be stored by this
	 * {@code Decoder} until more input is made available.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param inputBytes the desired number of {@code byte}s to consume. Must be
	 * less than or equal to {@link #inputRemaining() inputRemaining()}.
	 * @param dest the {@code Appendable} to which to decode.
	 * @param maxChars the maximum number of {@code char}s to append.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @return <ul><li>&gt 0: The actual number of {@code char}s decoded.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Decoder} until more input is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code byte}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code char}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IllegalArgumentException if {@code inputBytes < 0} or
	 * {@code inputBytes > inputRemaining()}.
	 * @throws IllegalArgumentException if {@code maxChars < 0} or
	 * {@code maxCodePoints < 0}.
	 */
	int decode(int inputBytes, Appendable dest, int maxChars, int maxCodePoints) throws UncheckedIOException;

	/**
	 * Instructs this {@code Decoder} to use the specified {@code byte}s as
	 * input. The current input is discarded. Pending input is unaffected.
	 *
	 * @param src the array whose {@code byte}s to use as input
	 * @return this {@code Decoder}.
	 * @throws NullPointerException if {@code src} is {@code null}
	 */
	default Decoder setInput(byte[] src) {
		return setInput(src, 0, src.length);
	}

	/**
	 * Instructs this {@code Decoder} to use the specified {@code byte}s as
	 * input. The current input is discarded. Pending input is unaffected.
	 *
	 * @param src the array whose {@code byte}s to use as input.
	 * @param off the offset into the byte-array.
	 * @param len the number of {@code byte}s to use.
	 * @return this {@code Decoder}.
	 * @throws NullPointerException if {@code src} is {@code null}
	 * @throws IndexOutOfBoundsException if the specified range is illegal
	 */
	Decoder setInput(byte[] src, int off, int len);

	/**
	 * Returns the number of input {@code byte}s this {@code Decoder} currently
	 * has access to. This does <em>NOT</em> include any
	 * {@link #pendingInput() pending input}.
	 *
	 * @return the number of input {@code byte}s this {@code Decoder} currently
	 * has access to.
	 */
	int inputRemaining();

	/**
	 * Returns the number of additional input {@code byte}s required to fully
	 * resolve the code point currently being decoded.
	 *
	 * @return the number of additional input {@code byte}s required to fully
	 * resolve the code point currently being decoded.
	 */
	int needsInput();

	/**
	 * Returns the number of input {@code byte}s held by this {@code Decoder} as
	 * part of the code point currently being decoded. These {@code byte}s are
	 * considered to be part of the value returned by
	 * {@link #bytesConsumed() bytesConsumed()}, either until they are
	 * {@link #dropPending() dropped} or until they are resolved in a subsequent
	 * {@code decode()} call.
	 *
	 * @return the number of input {@code byte}s held by this {@code Decoder} as
	 * part of the code point currently being decoded.
	 */
	int pendingInput();

	/**
	 * Returns the number of output {@code char}s held by this {@code Decoder}
	 * as part of the code point currently being decoded.
	 * <p>
	 * Specifically, returns {@code 1} if the low surrogate of a <em>valid</em>
	 * supplementary code point is currently being held by this {@code Decoder}
	 * until more output can be produced in a subsequent {@code decode()} call,
	 * returning {@code 0} in all other cases.
	 *
	 * @return the number of output {@code char}s held by this {@code Decoder}
	 * as part of the code point currently being decoded.
	 */
	int pendingOutput();

	/**
	 * Returns the number of <em>valid</em> {@code char}s produced since the
	 * last {@link #reset() reset}. {@code char}s that are part of malformed or
	 * unmappable code points are not counted this way.
	 *
	 * @return the number of <em>valid</em> {@code char}s produced since the
	 * last {@code reset}.
	 */
	long charsProduced();

	/**
	 * Returns the number of input {@code byte}s consumed since the last
	 * {@link #reset() reset}. Any pending input is part of this count,
	 * {@link #dropPending() dropping} it will be reflected in the value
	 * returned by this method.
	 *
	 * @return the number of input {@code byte}s consumed since the last
	 * {@link #reset() reset}.
	 */
	long bytesConsumed();

	/**
	 * Returns the number of <em>valid</em> code points resolved since the last
	 * {@link #reset() reset}. Malformed or unmappable code points are not
	 * counted this way.
	 *
	 * @return the number of <em>valid</em> code points resolved since the last
	 * {@code reset}.
	 */
	long codePointsResolved();

	/**
	 * Causes this {@code Decoder} to discard its current input. Pending input
	 * is <em>unaffacted</em> by this operation.
	 *
	 * @return this {@code Decoder}.
	 */
	Decoder dropInput();

	/**
	 * Causes this {@code Decoder} to discard its pending input or output (only
	 * one of which can be held at a time).
	 * <p>
	 * If any pending input is discarded, the value returned by
	 * {@link #bytesConsumed() bytesConsumed()} will be decremented by the
	 * number of currently pending input {@code byte}s.
	 *
	 * @return this {@code Decoder}.
	 */
	Decoder dropPending();

	/**
	 * Fully resets this {@code Decoder} to its initial state.
	 *
	 * @return this {@code Decoder}.
	 */
	Decoder reset();

	/**
	 * Returns {@code true} if this {@code Decoder} has any input or output
	 * pending.
	 *
	 * @return {@code true} if this {@code Decoder} has any input or output
	 * pending.
	 */
	boolean hasPending();
}
