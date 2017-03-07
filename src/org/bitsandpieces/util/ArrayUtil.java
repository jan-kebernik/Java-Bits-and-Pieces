/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.BooleanComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.ByteComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.CharComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.DoubleComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.FloatComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.IntComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.LongComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.ShortComparator;

/**
 * Convenience methods.
 *
 * @author Jan Kebernik
 */
public class ArrayUtil {

	private ArrayUtil() {
	}

	private static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
		if (fromIndex > toIndex) {
			throw new IllegalArgumentException(
					"fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
		}
		if (fromIndex < 0) {
			throw new ArrayIndexOutOfBoundsException(fromIndex);
		}
		if (toIndex > arrayLength) {
			throw new ArrayIndexOutOfBoundsException(toIndex);
		}
	}

	private static boolean[] csort(boolean[] a, int fromIndex, int toIndex) {
		int z = fromIndex;
		for (int i = fromIndex; i < toIndex; i++) {
			if (!a[i]) {
				z++;
			}
		}
		for (int i = fromIndex; i < z; i++) {
			a[i] = false;
		}
		for (int i = z; i < toIndex; i++) {
			a[i] = true;
		}
		return a;
	}

	public static boolean[] sort(boolean[] a) {
		return csort(a, 0, a.length);
	}

	public static boolean[] sort(boolean[] a, int fromIndex, int toIndex) {
		return csort(a, fromIndex, toIndex);
	}

	public static boolean[] sort(boolean[] a, Comparator<? super Boolean> c) {
		if (c instanceof BooleanComparator) {
			return sort(a, (BooleanComparator) c);
		}
		if (c != null) {
			return sort(a, (BooleanComparator) c::compare);
		}
		return sort(a);
	}

	public static boolean[] sort(boolean[] a, int fromIndex, int toIndex, Comparator<? super Boolean> c) {
		if (c instanceof BooleanComparator) {
			return sort(a, fromIndex, toIndex, (BooleanComparator) c);
		}
		if (c != null) {
			return sort(a, fromIndex, toIndex, (BooleanComparator) c::compare);
		}
		return sort(a, fromIndex, toIndex);
	}

	public static boolean[] sort(boolean[] a, BooleanComparator c) {
		if (c == null) {
			return sort(a);
		}
		PrimitiveTimSort.sort(a, 0, a.length, c, null, 0, 0);
		return a;
	}

	public static boolean[] sort(boolean[] a, int fromIndex, int toIndex, BooleanComparator c) {
		if (c == null) {
			return sort(a, fromIndex, toIndex);
		}
		rangeCheck(a.length, fromIndex, toIndex);
		PrimitiveTimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
		return a;
	}

	public static byte[] sort(byte[] a) {
		Arrays.sort(a);
		return a;
	}

	public static byte[] sort(byte[] a, int fromIndex, int toIndex) {
		Arrays.sort(a, fromIndex, toIndex);
		return a;
	}

	public static byte[] sort(byte[] a, Comparator<? super Byte> c) {
		if (c instanceof ByteComparator) {
			return sort(a, (ByteComparator) c);
		}
		if (c != null) {
			return sort(a, (ByteComparator) c::compare);
		}
		return sort(a);
	}

	public static byte[] sort(byte[] a, int fromIndex, int toIndex, Comparator<? super Byte> c) {
		if (c instanceof ByteComparator) {
			return sort(a, fromIndex, toIndex, (ByteComparator) c);
		}
		if (c != null) {
			return sort(a, fromIndex, toIndex, (ByteComparator) c::compare);
		}
		return sort(a, fromIndex, toIndex);
	}

	public static byte[] sort(byte[] a, ByteComparator c) {
		if (c == null) {
			return sort(a);
		}
		PrimitiveTimSort.sort(a, 0, a.length, c, null, 0, 0);
		return a;
	}

	public static byte[] sort(byte[] a, int fromIndex, int toIndex, ByteComparator c) {
		if (c == null) {
			return sort(a, fromIndex, toIndex);
		}
		rangeCheck(a.length, fromIndex, toIndex);
		PrimitiveTimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
		return a;
	}

	public static char[] sort(char[] a) {
		Arrays.sort(a);
		return a;
	}

	public static char[] sort(char[] a, int fromIndex, int toIndex) {
		Arrays.sort(a, fromIndex, toIndex);
		return a;
	}

	public static char[] sort(char[] a, Comparator<? super Character> c) {
		if (c instanceof CharComparator) {
			return sort(a, (CharComparator) c);
		}
		if (c != null) {
			return sort(a, (CharComparator) c::compare);
		}
		return sort(a);
	}

	public static char[] sort(char[] a, int fromIndex, int toIndex, Comparator<? super Character> c) {
		if (c instanceof CharComparator) {
			return sort(a, fromIndex, toIndex, (CharComparator) c);
		}
		if (c != null) {
			return sort(a, fromIndex, toIndex, (CharComparator) c::compare);
		}
		return sort(a, fromIndex, toIndex);
	}

	public static char[] sort(char[] a, CharComparator c) {
		if (c == null) {
			return sort(a);
		}
		PrimitiveTimSort.sort(a, 0, a.length, c, null, 0, 0);
		return a;
	}

	public static char[] sort(char[] a, int fromIndex, int toIndex, CharComparator c) {
		if (c == null) {
			return sort(a, fromIndex, toIndex);
		}
		rangeCheck(a.length, fromIndex, toIndex);
		PrimitiveTimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
		return a;
	}

	public static short[] sort(short[] a) {
		Arrays.sort(a);
		return a;
	}

	public static short[] sort(short[] a, int fromIndex, int toIndex) {
		Arrays.sort(a, fromIndex, toIndex);
		return a;
	}

	public static short[] sort(short[] a, Comparator<? super Short> c) {
		if (c instanceof ShortComparator) {
			return sort(a, (ShortComparator) c);
		}
		if (c != null) {
			return sort(a, (ShortComparator) c::compare);
		}
		return sort(a);
	}

	public static short[] sort(short[] a, int fromIndex, int toIndex, Comparator<? super Short> c) {
		if (c instanceof ShortComparator) {
			return sort(a, fromIndex, toIndex, (ShortComparator) c);
		}
		if (c != null) {
			return sort(a, fromIndex, toIndex, (ShortComparator) c::compare);
		}
		return sort(a, fromIndex, toIndex);
	}

	public static short[] sort(short[] a, ShortComparator c) {
		if (c == null) {
			return sort(a);
		}
		PrimitiveTimSort.sort(a, 0, a.length, c, null, 0, 0);
		return a;
	}

	public static short[] sort(short[] a, int fromIndex, int toIndex, ShortComparator c) {
		if (c == null) {
			return sort(a, fromIndex, toIndex);
		}
		rangeCheck(a.length, fromIndex, toIndex);
		PrimitiveTimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
		return a;
	}

	public static int[] sort(int[] a) {
		Arrays.sort(a);
		return a;
	}

	public static int[] sort(int[] a, int fromIndex, int toIndex) {
		Arrays.sort(a, fromIndex, toIndex);
		return a;
	}

	public static int[] sort(int[] a, Comparator<? super Integer> c) {
		if (c instanceof IntComparator) {
			return sort(a, (IntComparator) c);
		}
		if (c != null) {
			return sort(a, (IntComparator) c::compare);
		}
		return sort(a);
	}

	public static int[] sort(int[] a, int fromIndex, int toIndex, Comparator<? super Integer> c) {
		if (c instanceof IntComparator) {
			return sort(a, fromIndex, toIndex, (IntComparator) c);
		}
		if (c != null) {
			return sort(a, fromIndex, toIndex, (IntComparator) c::compare);
		}
		return sort(a, fromIndex, toIndex);
	}

	public static int[] sort(int[] a, IntComparator c) {
		if (c == null) {
			return sort(a);
		}
		PrimitiveTimSort.sort(a, 0, a.length, c, null, 0, 0);
		return a;
	}

	public static int[] sort(int[] a, int fromIndex, int toIndex, IntComparator c) {
		if (c == null) {
			return sort(a, fromIndex, toIndex);
		}
		rangeCheck(a.length, fromIndex, toIndex);
		PrimitiveTimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
		return a;
	}

	public static float[] sort(float[] a) {
		Arrays.sort(a);
		return a;
	}

	public static float[] sort(float[] a, int fromIndex, int toIndex) {
		Arrays.sort(a, fromIndex, toIndex);
		return a;
	}

	public static float[] sort(float[] a, Comparator<? super Float> c) {
		if (c instanceof FloatComparator) {
			return sort(a, (FloatComparator) c);
		}
		if (c != null) {
			return sort(a, (FloatComparator) c::compare);
		}
		return sort(a);
	}

	public static float[] sort(float[] a, int fromIndex, int toIndex, Comparator<? super Float> c) {
		if (c instanceof FloatComparator) {
			return sort(a, fromIndex, toIndex, (FloatComparator) c);
		}
		if (c != null) {
			return sort(a, fromIndex, toIndex, (FloatComparator) c::compare);
		}
		return sort(a, fromIndex, toIndex);
	}

	public static float[] sort(float[] a, FloatComparator c) {
		if (c == null) {
			return sort(a);
		}
		PrimitiveTimSort.sort(a, 0, a.length, c, null, 0, 0);
		return a;
	}

	public static float[] sort(float[] a, int fromIndex, int toIndex, FloatComparator c) {
		if (c == null) {
			return sort(a, fromIndex, toIndex);
		}
		rangeCheck(a.length, fromIndex, toIndex);
		PrimitiveTimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
		return a;
	}

	public static long[] sort(long[] a) {
		Arrays.sort(a);
		return a;
	}

	public static long[] sort(long[] a, int fromIndex, int toIndex) {
		Arrays.sort(a, fromIndex, toIndex);
		return a;
	}

	public static long[] sort(long[] a, Comparator<? super Long> c) {
		if (c instanceof LongComparator) {
			return sort(a, (LongComparator) c);
		}
		if (c != null) {
			return sort(a, (LongComparator) c::compare);
		}
		return sort(a);
	}

	public static long[] sort(long[] a, int fromIndex, int toIndex, Comparator<? super Long> c) {
		if (c instanceof LongComparator) {
			return sort(a, fromIndex, toIndex, (LongComparator) c);
		}
		if (c != null) {
			return sort(a, fromIndex, toIndex, (LongComparator) c::compare);
		}
		return sort(a, fromIndex, toIndex);
	}

	public static long[] sort(long[] a, LongComparator c) {
		if (c == null) {
			return sort(a);
		}
		PrimitiveTimSort.sort(a, 0, a.length, c, null, 0, 0);
		return a;
	}

	public static long[] sort(long[] a, int fromIndex, int toIndex, LongComparator c) {
		if (c == null) {
			return sort(a, fromIndex, toIndex);
		}
		rangeCheck(a.length, fromIndex, toIndex);
		PrimitiveTimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
		return a;
	}

	public static double[] sort(double[] a) {
		Arrays.sort(a);
		return a;
	}

	public static double[] sort(double[] a, int fromIndex, int toIndex) {
		Arrays.sort(a, fromIndex, toIndex);
		return a;
	}

	public static double[] sort(double[] a, Comparator<? super Double> c) {
		if (c instanceof DoubleComparator) {
			return sort(a, (DoubleComparator) c);
		}
		if (c != null) {
			return sort(a, (DoubleComparator) c::compare);
		}
		return sort(a);
	}

	public static double[] sort(double[] a, int fromIndex, int toIndex, Comparator<? super Double> c) {
		if (c instanceof DoubleComparator) {
			return sort(a, fromIndex, toIndex, (DoubleComparator) c);
		}
		if (c != null) {
			return sort(a, fromIndex, toIndex, (DoubleComparator) c::compare);
		}
		return sort(a, fromIndex, toIndex);
	}

	public static double[] sort(double[] a, DoubleComparator c) {
		if (c == null) {
			return sort(a);
		}
		PrimitiveTimSort.sort(a, 0, a.length, c, null, 0, 0);
		return a;
	}

	public static double[] sort(double[] a, int fromIndex, int toIndex, DoubleComparator c) {
		if (c == null) {
			return sort(a, fromIndex, toIndex);
		}
		rangeCheck(a.length, fromIndex, toIndex);
		PrimitiveTimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
		return a;
	}

	public static <T> T[] sort(T[] a) {
		Arrays.sort(a);
		return a;
	}

	public static <T> T[] sort(T[] a, int fromIndex, int toIndex) {
		Arrays.sort(a, fromIndex, toIndex);
		return a;
	}

	public static <T> T[] sort(T[] a, Comparator<? super T> c) {
		Arrays.sort(a, c);
		return a;
	}

	public static <T> T[] sort(T[] a, int fromIndex, int toIndex, Comparator<? super T> c) {
		Arrays.sort(a, fromIndex, toIndex, c);
		return a;
	}

	public static boolean[] fill(boolean[] a, boolean val) {
		Arrays.fill(a, val);
		return a;
	}

	public static boolean[] fill(boolean[] a, int fromIndex, int toIndex, boolean val) {
		Arrays.fill(a, fromIndex, toIndex, val);
		return a;
	}

	public static byte[] fill(byte[] a, byte val) {
		Arrays.fill(a, val);
		return a;
	}

	public static byte[] fill(byte[] a, int fromIndex, int toIndex, byte val) {
		Arrays.fill(a, fromIndex, toIndex, val);
		return a;
	}

	// first arguement to fillFunction is index, second current value
	public static byte[] fill(byte[] a, IntBinaryOperator fillFunction) {
		Objects.requireNonNull(fillFunction);
		return _fill(a, 0, a.length, fillFunction);
	}

	// first arguement to fillFunction is index, second current value
	public static byte[] fill(byte[] a, int fromIndex, int toIndex, IntBinaryOperator fillFunction) {
		Objects.requireNonNull(fillFunction);
		rangeCheck(a.length, fromIndex, toIndex);
		return _fill(a, fromIndex, toIndex, fillFunction);
	}

	private static byte[] _fill(byte[] a, int fromIndex, int toIndex, IntBinaryOperator fillFunction) {
		for (; fromIndex < toIndex; fromIndex++) {
			a[fromIndex] = (byte) fillFunction.applyAsInt(fromIndex, a[fromIndex]);
		}
		return a;
	}

	public static char[] fill(char[] a, char val) {
		Arrays.fill(a, val);
		return a;
	}

	public static char[] fill(char[] a, int fromIndex, int toIndex, char val) {
		Arrays.fill(a, fromIndex, toIndex, val);
		return a;
	}

	// first arguement to fillFunction is index, second current value
	public static char[] fill(char[] a, IntBinaryOperator fillFunction) {
		Objects.requireNonNull(fillFunction);
		return _fill(a, 0, a.length, fillFunction);
	}

	// first arguement to fillFunction is index, second current value
	public static char[] fill(char[] a, int fromIndex, int toIndex, IntBinaryOperator fillFunction) {
		Objects.requireNonNull(fillFunction);
		rangeCheck(a.length, fromIndex, toIndex);
		return _fill(a, fromIndex, toIndex, fillFunction);
	}

	private static char[] _fill(char[] a, int fromIndex, int toIndex, IntBinaryOperator fillFunction) {
		for (; fromIndex < toIndex; fromIndex++) {
			a[fromIndex] = (char) fillFunction.applyAsInt(fromIndex, a[fromIndex]);
		}
		return a;
	}

	public static short[] fill(short[] a, short val) {
		Arrays.fill(a, val);
		return a;
	}

	public static short[] fill(short[] a, int fromIndex, int toIndex, short val) {
		Arrays.fill(a, fromIndex, toIndex, val);
		return a;
	}

	// first arguement to fillFunction is index, second current value
	public static short[] fill(short[] a, IntBinaryOperator fillFunction) {
		Objects.requireNonNull(fillFunction);
		return _fill(a, 0, a.length, fillFunction);
	}

	// first arguement to fillFunction is index, second current value
	public static short[] fill(short[] a, int fromIndex, int toIndex, IntBinaryOperator fillFunction) {
		Objects.requireNonNull(fillFunction);
		rangeCheck(a.length, fromIndex, toIndex);
		return _fill(a, fromIndex, toIndex, fillFunction);
	}

	private static short[] _fill(short[] a, int fromIndex, int toIndex, IntBinaryOperator fillFunction) {
		for (; fromIndex < toIndex; fromIndex++) {
			a[fromIndex] = (short) fillFunction.applyAsInt(fromIndex, a[fromIndex]);
		}
		return a;
	}

	public static int[] fill(int[] a, int val) {
		Arrays.fill(a, val);
		return a;
	}

	public static int[] fill(int[] a, int fromIndex, int toIndex, int val) {
		Arrays.fill(a, fromIndex, toIndex, val);
		return a;
	}

	// first arguement to fillFunction is index, second current value
	public static int[] fill(int[] a, IntBinaryOperator fillFunction) {
		Objects.requireNonNull(fillFunction);
		return _fill(a, 0, a.length, fillFunction);
	}

	// first arguement to fillFunction is index, second current value
	public static int[] fill(int[] a, int fromIndex, int toIndex, IntBinaryOperator fillFunction) {
		Objects.requireNonNull(fillFunction);
		rangeCheck(a.length, fromIndex, toIndex);
		return _fill(a, fromIndex, toIndex, fillFunction);
	}

	private static int[] _fill(int[] a, int fromIndex, int toIndex, IntBinaryOperator fillFunction) {
		for (; fromIndex < toIndex; fromIndex++) {
			a[fromIndex] = fillFunction.applyAsInt(fromIndex, a[fromIndex]);
		}
		return a;
	}

	public static float[] fill(float[] a, float val) {
		Arrays.fill(a, val);
		return a;
	}

	public static float[] fill(float[] a, int fromIndex, int toIndex, float val) {
		Arrays.fill(a, fromIndex, toIndex, val);
		return a;
	}

	public static long[] fill(long[] a, long val) {
		Arrays.fill(a, val);
		return a;
	}

	public static long[] fill(long[] a, int fromIndex, int toIndex, long val) {
		Arrays.fill(a, fromIndex, toIndex, val);
		return a;
	}

	// first arguement to fillFunction is index, second current value
	public static long[] fill(long[] a, LongBinaryOperator fillFunction) {
		Objects.requireNonNull(fillFunction);
		return _fill(a, 0, a.length, fillFunction);
	}

	// first arguement to fillFunction is index, second current value
	public static long[] fill(long[] a, int fromIndex, int toIndex, LongBinaryOperator fillFunction) {
		Objects.requireNonNull(fillFunction);
		rangeCheck(a.length, fromIndex, toIndex);
		return _fill(a, fromIndex, toIndex, fillFunction);
	}

	private static long[] _fill(long[] a, int fromIndex, int toIndex, LongBinaryOperator fillFunction) {
		for (; fromIndex < toIndex; fromIndex++) {
			a[fromIndex] = fillFunction.applyAsLong(fromIndex, a[fromIndex]);
		}
		return a;
	}

	public static double[] fill(double[] a, double val) {
		Arrays.fill(a, val);
		return a;
	}

	public static double[] fill(double[] a, int fromIndex, int toIndex, double val) {
		Arrays.fill(a, fromIndex, toIndex, val);
		return a;
	}

	public static <T> T[] fill(T[] a, T val) {
		Arrays.fill(a, val);
		return a;
	}

	public static <T> T[] fill(T[] a, int fromIndex, int toIndex, T val) {
		Arrays.fill(a, fromIndex, toIndex, val);
		return a;
	}

	public static String toString(byte[] a) {
		return _toString(a, 0, a.length, Formats.DEC);
	}

	public static String toString(byte[] a, Format f) {
		Objects.requireNonNull(f);
		return _toString(a, 0, a.length, f);
	}

	public static String toString(byte[] a, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex, Formats.DEC);
	}

	public static String toString(byte[] a, int fromIndex, int toIndex, Format f) {
		Objects.requireNonNull(f);
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex, f);
	}

	private static String _toString(byte[] a, int fromIndex, int toIndex, Format f) {
		if (fromIndex == toIndex) {
			return "[]";
		}
		StringMaker sm = new StringMaker((toIndex - fromIndex) * 3);
		sm.append('[');
		Format f0 = f.suffix(f.suffix() == null ? ", " : f.suffix().concat(", "));
		for (int i = fromIndex; i < toIndex - 1; i++) {
			sm.append(a[i], f0);
		}
		sm.append(a[toIndex - 1], f);
		return sm.append(']').toString();
	}

	public static String toString(char[] a) {
		return _toString(a, 0, a.length);
	}

	public static String toString(char[] a, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex);
	}

	private static String _toString(char[] a, int fromIndex, int toIndex) {
		if (fromIndex == toIndex) {
			return "[]";
		}
		StringMaker sm = new StringMaker((toIndex - fromIndex) * 3);
		sm.append('[').append(a[fromIndex]);
		for (int i = fromIndex + 1; i < toIndex; i++) {
			sm.append(", ").append(a[i]);
		}
		return sm.append(']').toString();
	}

	public static String toString(char[] a, Format f) {
		Objects.requireNonNull(f);
		return _toString(a, 0, a.length, f);
	}

	public static String toString(char[] a, int fromIndex, int toIndex, Format f) {
		Objects.requireNonNull(f);
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex, f);
	}

	private static String _toString(char[] a, int fromIndex, int toIndex, Format f) {
		if (fromIndex == toIndex) {
			return "[]";
		}
		StringMaker sm = new StringMaker((toIndex - fromIndex) * 3);
		sm.append('[');
		Format f0 = f.suffix(f.suffix() == null ? ", " : f.suffix().concat(", "));
		for (int i = fromIndex; i < toIndex - 1; i++) {
			sm.append(a[i], f0);
		}
		sm.append(a[toIndex - 1], f);
		return sm.append(']').toString();
	}

	public static String toString(short[] a) {
		return _toString(a, 0, a.length, Formats.DEC);
	}

	public static String toString(short[] a, Format f) {
		Objects.requireNonNull(f);
		return _toString(a, 0, a.length, f);
	}

	public static String toString(short[] a, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex, Formats.DEC);
	}

	public static String toString(short[] a, int fromIndex, int toIndex, Format f) {
		Objects.requireNonNull(f);
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex, f);
	}

	private static String _toString(short[] a, int fromIndex, int toIndex, Format f) {
		if (fromIndex == toIndex) {
			return "[]";
		}
		StringMaker sm = new StringMaker((toIndex - fromIndex) * 3);
		sm.append('[');
		Format f0 = f.suffix(f.suffix() == null ? ", " : f.suffix().concat(", "));
		for (int i = fromIndex; i < toIndex - 1; i++) {
			sm.append(a[i], f0);
		}
		sm.append(a[toIndex - 1], f);
		return sm.append(']').toString();
	}

	public static String toString(int[] a) {
		return _toString(a, 0, a.length, Formats.DEC);
	}

	public static String toString(int[] a, Format f) {
		Objects.requireNonNull(f);
		return _toString(a, 0, a.length, f);
	}

	public static String toString(int[] a, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex, Formats.DEC);
	}

	public static String toString(int[] a, int fromIndex, int toIndex, Format f) {
		Objects.requireNonNull(f);
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex, f);
	}

	private static String _toString(int[] a, int fromIndex, int toIndex, Format f) {
		if (fromIndex == toIndex) {
			return "[]";
		}
		StringMaker sm = new StringMaker((toIndex - fromIndex) * 3);
		sm.append('[');
		Format f0 = f.suffix(f.suffix() == null ? ", " : f.suffix().concat(", "));
		for (int i = fromIndex; i < toIndex - 1; i++) {
			sm.append(a[i], f0);
		}
		sm.append(a[toIndex - 1], f);
		return sm.append(']').toString();
	}

	public static String toString(long[] a) {
		return _toString(a, 0, a.length, Formats.DEC);
	}

	public static String toString(long[] a, Format f) {
		Objects.requireNonNull(f);
		return _toString(a, 0, a.length, f);
	}

	public static String toString(long[] a, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex, Formats.DEC);
	}

	public static String toString(long[] a, int fromIndex, int toIndex, Format f) {
		Objects.requireNonNull(f);
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex, f);
	}

	private static String _toString(long[] a, int fromIndex, int toIndex, Format f) {
		if (fromIndex == toIndex) {
			return "[]";
		}
		StringMaker sm = new StringMaker((toIndex - fromIndex) * 3);
		sm.append('[');
		Format f0 = f.suffix(f.suffix() == null ? ", " : f.suffix().concat(", "));
		for (int i = fromIndex; i < toIndex - 1; i++) {
			sm.append(a[i], f0);
		}
		sm.append(a[toIndex - 1], f);
		return sm.append(']').toString();
	}

	public static String toString(float[] a) {
		return _toString(a, 0, a.length);
	}

	public static String toString(float[] a, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex);
	}

	private static String _toString(float[] a, int fromIndex, int toIndex) {
		if (fromIndex == toIndex) {
			return "[]";
		}
		// StringBuilder able to use FloatingDecimal under the hood
		StringBuilder sm = new StringBuilder(a.length * 3);
		sm.append('[').append(a[fromIndex]);
		for (int i = fromIndex + 1; i < toIndex; i++) {
			sm.append(", ").append(a[i]);
		}
		return sm.append(']').toString();
	}

	public static String toString(double[] a) {
		return _toString(a, 0, a.length);
	}

	public static String toString(double[] a, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		return _toString(a, fromIndex, toIndex);
	}

	private static String _toString(double[] a, int fromIndex, int toIndex) {
		if (fromIndex == toIndex) {
			return "[]";
		}
		// StringBuilder able to use FloatingDecimal under the hood
		StringBuilder sm = new StringBuilder(a.length * 3);
		sm.append('[').append(a[fromIndex]);
		for (int i = fromIndex + 1; i < toIndex; i++) {
			sm.append(", ").append(a[i]);
		}
		return sm.append(']').toString();
	}
}
