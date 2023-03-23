package qwertzite.extraexplosions.exp.barostrain;

import qwertzite.extraexplosions.exmath.RayTrigonal;
import qwertzite.extraexplosions.util.math.EeMath;

/**
 * 
 * @author owner
 * @date 2023/03/02
 * 
 * @param trigonal
 * @param intencity
 * @param divStep
 * @param initial weather this ray is included in the initial ray set. internal hit test will be conducted when true.
 */
public record PressureRay(RayTrigonal trigonal, double intencity, double division, double travelledDistance, int divStep, boolean initial) {

	public PressureRay(RayTrigonal trigonal, double intencity, double division) {
		this(trigonal, intencity, division, 0.0d, 0, true);
	}
	
	/**
	 * computers pressure at "distFromFrom" away from "from".
	 * From からdistFromFrom　離れた位置における圧力を計算する
	 * @param distFromFrom
	 * @return
	 */
	public double computePressureAt(double distFromFrom) {
		final double P = Math.sqrt(intencity);
		final double Q = 0.5d*P + 0.5d;
		final double x1 = 2*Q;
		final double x2 = (2 + EeMath.SQRT_2) * Q;
		final double y1 = P*Math.exp(-2)*4*Q*Q;
		final double y2 = P*Math.exp(-2-EeMath.SQRT_2) * (6+4*EeMath.SQRT_2) * Q * Q;
		double res = (y2 - y1)/(x2 - x1)*(this.travelledDistance + distFromFrom - x1) + y1;
		if (res < 0) res = 0.0d;
		else res *= this.division;
		return res;
	}
}
