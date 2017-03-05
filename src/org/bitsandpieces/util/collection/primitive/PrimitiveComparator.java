/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.primitive;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 *
 * @author Jan Kebernik
 */
public interface PrimitiveComparator<T> extends Comparator<T> {

	@FunctionalInterface
	public static interface BooleanComparator extends PrimitiveComparator<Boolean> {

		@Override
		default int compare(Boolean x, Boolean y) {
			return compareBoolean(x, y);
		}

		int compareBoolean(boolean x, boolean y);

		@Override
		default BooleanComparator reversed() {
			BooleanComparator base = this;
			return new BooleanComparator() {
				@Override
				public int compareBoolean(boolean x, boolean y) {
					return base.compareBoolean(y, x);
				}

				@Override
				public BooleanComparator reversed() {
					return base;
				}
			};
		}

		@Override
		default BooleanComparator thenComparing(Comparator<? super Boolean> other) {
			if (other instanceof BooleanComparator) {
				return thenComparing((BooleanComparator) other);
			}
			Objects.requireNonNull(other);
			return thenComparing((BooleanComparator) other::compare);
		}

		default BooleanComparator thenComparing(BooleanComparator other) {
			Objects.requireNonNull(other);
			return (BooleanComparator & Serializable) (c1, c2) -> {
				int res = compareBoolean(c1, c2);
				return (res != 0) ? res : other.compareBoolean(c1, c2);
			};
		}
	}

	@FunctionalInterface
	public static interface ByteComparator extends PrimitiveComparator<Byte> {

		@Override
		default int compare(Byte x, Byte y) {
			return compareByte(x, y);
		}

		int compareByte(byte x, byte y);

		@Override
		default ByteComparator reversed() {
			ByteComparator base = this;
			return new ByteComparator() {
				@Override
				public int compareByte(byte x, byte y) {
					return base.compareByte(y, x);
				}

				@Override
				public ByteComparator reversed() {
					return base;
				}
			};
		}

		@Override
		default ByteComparator thenComparing(Comparator<? super Byte> other) {
			if (other instanceof ByteComparator) {
				return thenComparing((ByteComparator) other);
			}
			Objects.requireNonNull(other);
			return thenComparing((ByteComparator) other::compare);
		}

		default ByteComparator thenComparing(ByteComparator other) {
			Objects.requireNonNull(other);
			return (ByteComparator & Serializable) (c1, c2) -> {
				int res = compareByte(c1, c2);
				return (res != 0) ? res : other.compareByte(c1, c2);
			};
		}
	}

	@FunctionalInterface
	public static interface CharComparator extends PrimitiveComparator<Character> {

		@Override
		default int compare(Character x, Character y) {
			return compareChar(x, y);
		}

		int compareChar(char x, char y);

		@Override
		default CharComparator reversed() {
			CharComparator base = this;
			return new CharComparator() {
				@Override
				public int compareChar(char x, char y) {
					return base.compareChar(y, x);
				}

				@Override
				public CharComparator reversed() {
					return base;
				}
			};
		}

		@Override
		default CharComparator thenComparing(Comparator<? super Character> other) {
			if (other instanceof CharComparator) {
				return thenComparing((CharComparator) other);
			}
			Objects.requireNonNull(other);
			return thenComparing((CharComparator) other::compare);
		}

		default CharComparator thenComparing(CharComparator other) {
			Objects.requireNonNull(other);
			return (CharComparator & Serializable) (c1, c2) -> {
				int res = compareChar(c1, c2);
				return (res != 0) ? res : other.compareChar(c1, c2);
			};
		}
	}

	@FunctionalInterface
	public static interface ShortComparator extends PrimitiveComparator<Short> {

		@Override
		default int compare(Short x, Short y) {
			return compareShort(x, y);
		}

		int compareShort(short x, short y);

		@Override
		default ShortComparator reversed() {
			ShortComparator base = this;
			return new ShortComparator() {
				@Override
				public int compareShort(short x, short y) {
					return base.compareShort(y, x);
				}

				@Override
				public ShortComparator reversed() {
					return base;
				}
			};
		}

		@Override
		default ShortComparator thenComparing(Comparator<? super Short> other) {
			if (other instanceof ShortComparator) {
				return thenComparing((ShortComparator) other);
			}
			Objects.requireNonNull(other);
			return thenComparing((ShortComparator) other::compare);
		}

		default ShortComparator thenComparing(ShortComparator other) {
			Objects.requireNonNull(other);
			return (ShortComparator & Serializable) (c1, c2) -> {
				int res = compareShort(c1, c2);
				return (res != 0) ? res : other.compareShort(c1, c2);
			};
		}
	}

	@FunctionalInterface
	public static interface IntComparator extends PrimitiveComparator<Integer> {

		@Override
		default int compare(Integer x, Integer y) {
			return compareInt(x, y);
		}

		int compareInt(int x, int y);

		@Override
		default IntComparator reversed() {
			IntComparator base = this;
			return new IntComparator() {
				@Override
				public int compareInt(int x, int y) {
					return base.compareInt(y, x);
				}

				@Override
				public IntComparator reversed() {
					return base;
				}
			};
		}

		@Override
		default IntComparator thenComparing(Comparator<? super Integer> other) {
			if (other instanceof IntComparator) {
				return thenComparing((IntComparator) other);
			}
			Objects.requireNonNull(other);
			return thenComparing((IntComparator) other::compare);
		}

		default IntComparator thenComparing(IntComparator other) {
			Objects.requireNonNull(other);
			return (IntComparator & Serializable) (c1, c2) -> {
				int res = compareInt(c1, c2);
				return (res != 0) ? res : other.compareInt(c1, c2);
			};
		}
	}

	@FunctionalInterface
	public static interface LongComparator extends PrimitiveComparator<Long> {

		@Override
		default int compare(Long x, Long y) {
			return compareLong(x, y);
		}

		int compareLong(long x, long y);

		@Override
		default LongComparator reversed() {
			LongComparator base = this;
			return new LongComparator() {
				@Override
				public int compareLong(long x, long y) {
					return base.compareLong(y, x);
				}

				@Override
				public LongComparator reversed() {
					return base;
				}
			};
		}

		@Override
		default LongComparator thenComparing(Comparator<? super Long> other) {
			if (other instanceof LongComparator) {
				return thenComparing((LongComparator) other);
			}
			Objects.requireNonNull(other);
			return thenComparing((LongComparator) other::compare);
		}

		default LongComparator thenComparing(LongComparator other) {
			Objects.requireNonNull(other);
			return (LongComparator & Serializable) (c1, c2) -> {
				int res = compareLong(c1, c2);
				return (res != 0) ? res : other.compareLong(c1, c2);
			};
		}
	}

	@FunctionalInterface
	public static interface FloatComparator extends PrimitiveComparator<Float> {

		@Override
		default int compare(Float x, Float y) {
			return compareFloat(x, y);
		}

		int compareFloat(float x, float y);

		@Override
		default FloatComparator reversed() {
			FloatComparator base = this;
			return new FloatComparator() {
				@Override
				public int compareFloat(float x, float y) {
					return base.compareFloat(y, x);
				}

				@Override
				public FloatComparator reversed() {
					return base;
				}
			};
		}

		@Override
		default FloatComparator thenComparing(Comparator<? super Float> other) {
			if (other instanceof FloatComparator) {
				return thenComparing((FloatComparator) other);
			}
			Objects.requireNonNull(other);
			return thenComparing((FloatComparator) other::compare);
		}

		default FloatComparator thenComparing(FloatComparator other) {
			Objects.requireNonNull(other);
			return (FloatComparator & Serializable) (c1, c2) -> {
				int res = compareFloat(c1, c2);
				return (res != 0) ? res : other.compareFloat(c1, c2);
			};
		}
	}

	@FunctionalInterface
	public static interface DoubleComparator extends PrimitiveComparator<Double> {

		@Override
		default int compare(Double x, Double y) {
			return compareDouble(x, y);
		}

		int compareDouble(double x, double y);

		@Override
		default DoubleComparator reversed() {
			DoubleComparator base = this;
			return new DoubleComparator() {
				@Override
				public int compareDouble(double x, double y) {
					return base.compareDouble(y, x);
				}

				@Override
				public DoubleComparator reversed() {
					return base;
				}
			};
		}

		@Override
		default DoubleComparator thenComparing(Comparator<? super Double> other) {
			if (other instanceof DoubleComparator) {
				return thenComparing((DoubleComparator) other);
			}
			Objects.requireNonNull(other);
			return thenComparing((DoubleComparator) other::compare);
		}

		default DoubleComparator thenComparing(DoubleComparator other) {
			Objects.requireNonNull(other);
			return (DoubleComparator & Serializable) (c1, c2) -> {
				int res = compareDouble(c1, c2);
				return (res != 0) ? res : other.compareDouble(c1, c2);
			};
		}
	}
}
