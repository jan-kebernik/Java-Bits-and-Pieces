/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

import java.util.Arrays;
import java.util.Comparator;
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

	public static char[] fill(char[] a, char val) {
		Arrays.fill(a, val);
		return a;
	}

	public static char[] fill(char[] a, int fromIndex, int toIndex, char val) {
		Arrays.fill(a, fromIndex, toIndex, val);
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

	public static int[] fill(int[] a, int val) {
		Arrays.fill(a, val);
		return a;
	}

	public static int[] fill(int[] a, int fromIndex, int toIndex, int val) {
		Arrays.fill(a, fromIndex, toIndex, val);
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
}
