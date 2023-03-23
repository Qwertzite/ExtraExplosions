package qwertzite.extraexplosions.util.math;

import java.util.Random;

import net.minecraft.world.phys.Vec3;

public class EeMath {
	public static final float PI = (float) Math.PI;
	public static final float RAD2DEG = (float) (180.0d / Math.PI);
	public static final float DEG2RAD = (float) (Math.PI / 180.0f);
	public static final double SQRT_2 = Math.sqrt(2.0d);
	
	public static Vec3 sum(Vec3 v1, Vec3 v2, Vec3 v3) {
		return new Vec3(
				v1.x() + v2.x() + v3.x(),
				v1.y() + v2.y() + v3.y(),
				v1.z() + v2.z() + v3.z());
	}
	
	public static Vec3 average(Vec3 v1, Vec3 v2) {
		return new Vec3(
				(v1.x() + v2.x()) / 2.0,
				(v1.y() + v2.y()) / 2.0,
				(v1.z() + v2.z()) / 2.0);
	}
	
	/**
	 * 
	 * @param v0
	 * @param scale
	 * @param v1
	 * @return {@code v0*scale + v1}
	 */
	public static Vec3 multiplyAdd(Vec3 v0, double scale, Vec3 v1) {
		return new Vec3(
				v0.x()*scale + v1.x(),
				v0.y()*scale + v1.y(),
				v0.z()*scale + v1.z());
	}
	
	public static Vec3 multiplyAdd(
			Vec3 v0, Vec3 v1, Vec3 v2, double scale, Vec3 v3) {
		return new Vec3(
				v0.x()*scale + v1.x()*scale + v2.x()*scale + v3.x(),
				v0.y()*scale + v1.y()*scale + v2.y()*scale + v3.y(),
				v0.z()*scale + v1.z()*scale + v2.z()*scale + v3.z());
	}
	
	public static double pow(double base, int exponent) {
		assert(exponent > 0);
		if (exponent == 0) return 1;
		if (exponent == 1) return base;
		if (base == 1) return 1;
		if (base == 0) return 0; // 0^n == delta(n), 
		double res = 1;
		for (int i = 31; i >= 0; i--) { // 31: number of bits of integer.
			res *= res;
			if ((exponent & (1 << i)) != 0) { res *= base; }
		}
		return res;
	}
	
	public static float pow(float base, int exponent) {
		assert(exponent > 0);
		if (exponent == 0) return 1;
		if (exponent == 1) return base;
		if (base == 1) return 1;
		if (base == 0) return 0; // 0^n == delta(n), 
		var res = 1;
		for (int i = 31; i >= 0; i--) { // 31: number of bits of integer.
			res *= res;
			if ((exponent & (1 << i)) != 0) { res *= base; }
		}
		return res;
	}
	
	// Fisher–Yates shuffle
	public static void shuffle(float[] array, Random rand) {
		if (array.length <= 1) { return; }
		for (int i = array.length - 1; i > 0; i--) {
			int index = rand.nextInt(i + 1);
			
			var tmp = array[index]; // swap
			array[index] = array[i];
			array[i] = tmp;
		}
	}
	
	// Fisher–Yates shuffle
	public static void shuffle(double[] array, Random rand) {
		if (array.length <= 1) { return; }
		for (int i = array.length - 1; i > 0; i--) {
			int index = rand.nextInt(i + 1);
			
			var tmp = array[index]; // swap
			array[index] = array[i];
			array[i] = tmp;
		}
	}
}
