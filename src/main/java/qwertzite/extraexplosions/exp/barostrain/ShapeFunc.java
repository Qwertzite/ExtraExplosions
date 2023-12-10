package qwertzite.extraexplosions.exp.barostrain;

import net.minecraft.core.Vec3i;
import qwertzite.extraexplosions.util.math.EeMath;

public class ShapeFunc {
	
	private static final double pp = (1.0d + 1.0d / Math.sqrt(3));
	private static final double nn = (1.0d + 1.0d / Math.sqrt(3));
	
	private static final double N3 = pp * pp * pp / 8.0d;
	private static final double N2 = pp * pp * nn / 8.0d;
	private static final double N1 = pp * nn * nn / 8.0d;
	private static final double N0 = nn * nn * nn / 8.0d;
	
	private static final double N2d = pp * pp / 8.0d;
	private static final double N1d = pp * nn / 8.0d;
	private static final double N0d = nn * nn / 8.0d;
	
	public static final double NA = 03 * (N2d*N2d + 2*N1d*N1d + N0d*N0d);
	private static final double NBij = (N2d*N2d + 2*N2d*N1d + 2*N1d*N1d + 2*N1d*N0d + N0d*N0d) / 2.0d;
	public static final double NBii = N2d*N2d + 2*N1d*N1d + N0d*N0d;
	public static final double NC = N3*N3 +3*N2*N2 + 3*N1*N1 + N0*N0;
	
	public static double nbij(Vec3i offset, int i, int j) {
		return  NBij
				* (EeMath.getXi(offset, i) < 0 ? 1 : -1)
				* (EeMath.getXi(offset, j) < 0 ? 1 : -1);
	}
	
	public static double partial(ElemVertex vertex, int j, IntPoint intPt) {
		var ret = 1.0d / 8;
		for (int i = 0; i < 3; i++) {
			if (i == j) { ret *= vertex.getSign(i); }
			else { ret *= 1 + vertex.getSign(j) * intPt.getXi_i(i); }
		}
		return ret;
	}
	
	public static double value(ElemVertex vertex, IntPoint intPt) {
		var ret = 1.0d / 8;
		ret *= 1 + vertex.getSign(0) * intPt.getXi_i(0);
		ret *= 1 + vertex.getSign(1) * intPt.getXi_i(1);
		ret *= 1 + vertex.getSign(2) * intPt.getXi_i(2);
		return ret;
	}
	
}
