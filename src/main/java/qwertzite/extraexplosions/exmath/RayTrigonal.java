package qwertzite.extraexplosions.exmath;

import java.util.stream.Stream;

import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import qwertzite.extraexplosions.util.math.EeMath;

/**
 * 
 * @author Qwertzite
 * @date 2023/02/12
 * 
 * @param origin Centre of the pseudo-sphere.
 * @param v1 relative position of pseudo-sphere vertex.
 * @param v2 relative position of pseudo-sphere vertex.
 * @param v3 relative position of pseudo-sphere vertex.
 * @param from ABSOLUTE position of starting point of ray.
 * @param to ABSOLUTE position of end point of ray.
 */
public record RayTrigonal(Vec3 origin, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 from, Vec3 to) {
	
	private static final RayTrigonal[] ORIGIN;
	static {
		double phi = (1 + Math.sqrt(5))/2;
		Vec3[] vgx = new Vec3[] { new Vec3( 0, 1, phi).normalize(), new Vec3( 0,-1, phi).normalize(), new Vec3( 0,-1,-phi).normalize(), new Vec3( 0, 1,-phi).normalize(), };
		Vec3[] vgy = new Vec3[] { new Vec3( phi, 0, 1).normalize(), new Vec3( phi, 0,-1).normalize(), new Vec3(-phi, 0,-1).normalize(), new Vec3(-phi, 0, 1).normalize(), };
		Vec3[] vgz = new Vec3[] { new Vec3( 1, phi, 0).normalize(), new Vec3(-1, phi, 0).normalize(), new Vec3(-1,-phi, 0).normalize(), new Vec3( 1,-phi, 0).normalize(), };
		
		ORIGIN = new RayTrigonal[] {
				new RayTrigonal(Vec3.ZERO, vgx[0], vgz[0], vgz[1]),
				new RayTrigonal(Vec3.ZERO, vgx[1], vgz[2], vgz[3]),
				new RayTrigonal(Vec3.ZERO, vgx[2], vgz[3], vgz[2]),
				new RayTrigonal(Vec3.ZERO, vgx[3], vgz[1], vgz[0]),
				
				new RayTrigonal(Vec3.ZERO, vgy[0], vgx[0], vgx[1]),
				new RayTrigonal(Vec3.ZERO, vgy[1], vgx[2], vgx[3]),
				new RayTrigonal(Vec3.ZERO, vgy[2], vgx[3], vgx[2]),
				new RayTrigonal(Vec3.ZERO, vgy[3], vgx[1], vgx[0]),
				
				new RayTrigonal(Vec3.ZERO, vgz[0], vgy[0], vgy[1]),
				new RayTrigonal(Vec3.ZERO, vgz[1], vgy[2], vgy[3]),
				new RayTrigonal(Vec3.ZERO, vgz[2], vgy[3], vgy[2]),
				new RayTrigonal(Vec3.ZERO, vgz[3], vgy[1], vgy[0]),
				
				new RayTrigonal(Vec3.ZERO, vgx[0], vgy[0], vgz[0]),
				new RayTrigonal(Vec3.ZERO, vgx[1], vgy[0], vgz[3]),
				new RayTrigonal(Vec3.ZERO, vgx[2], vgy[1], vgz[3]),
				new RayTrigonal(Vec3.ZERO, vgx[3], vgy[1], vgz[0]),
				
				new RayTrigonal(Vec3.ZERO, vgx[0], vgy[3], vgz[1]),
				new RayTrigonal(Vec3.ZERO, vgx[1], vgy[3], vgz[2]),
				new RayTrigonal(Vec3.ZERO, vgx[2], vgy[2], vgz[2]),
				new RayTrigonal(Vec3.ZERO, vgx[3], vgy[2], vgz[1]),
		};
	}
	
	public RayTrigonal(Vec3 origin, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 from) {
		this(origin, v1, v2, v3, from, EeMath.multiplyAdd(v1, v2, v3, 1.0/3, origin));
	}
	
	public RayTrigonal(Vec3 origin, Vec3 v1, Vec3 v2, Vec3 v3) {
		this(origin, v1, v2, v3, origin, EeMath.multiplyAdd(v1, v2, v3, 1.0/3, origin));
	}
	
	public static RayTrigonal[] createInitialSphere(Vec3 centre, double radius, int division, RandomSource rand) {
		
		float yaw  = rand.nextFloat()*2*EeMath.PI;
		float pitch = (float) Math.asin(rand.nextFloat()*2 - 1);
		float roll = rand.nextFloat()*2*EeMath.PI;
		
//		var origin = ORIGIN;
		var origin = Stream.of(ORIGIN)
				.map(o -> new RayTrigonal(o.origin(),
						o.v1().zRot(roll).xRot(pitch).yRot(yaw),
						o.v2().zRot(roll).xRot(pitch).yRot(yaw),
						o.v3().zRot(roll).xRot(pitch).yRot(yaw)))
				.toArray(i -> new RayTrigonal[i]);
		
		RayTrigonal[] result = origin;
		for (int i = 0; i < division; i++) {
			int len = result.length;
			var newResult = new RayTrigonal[len*4];
			for (int n = 0; n < len; n++) {
				var nn = n*4;
				var next = result[n].divide();
				newResult[nn+0] = next[0];
				newResult[nn+1] = next[1];
				newResult[nn+2] = next[2];
				newResult[nn+3] = next[3];
			}
			result = newResult;
		}
		
		for (int i = 0; i < result.length; i++) {
			result[i] = result[i].scaleAndTranslate(radius, centre);
		}
		return result;
	}
	
	/**
	 * This method is intended to be called to create initial sphere.<br>
	 * Starting point of ray tracing will be set to {@code "vec"}.
	 * @param radius
	 * @param vec absolute coordinate of new origin.
	 * @return
	 */
	private RayTrigonal scaleAndTranslate(double radius, Vec3 vec) {
		double scale = radius / this.v1.length();
		return new RayTrigonal(vec, v1.scale(scale), v2.scale(scale), v3.scale(scale));
	}
	
	public RayTrigonal[] divide() {
		var v1 = this.v1.add(this.v1); // relative position to origin
		var v2 = this.v2.add(this.v2);
		var v3 = this.v3.add(this.v3);
		double rad = v1.length();
		
		var w1 = this.v1.add(this.v2); // relative position to origin
		var w2 = this.v2.add(this.v3);
		var w3 = this.v3.add(this.v1);
		w1 = w1.scale(rad / w1.length());
		w2 = w2.scale(rad / w2.length());
		w3 = w3.scale(rad / w3.length());
		
		return new RayTrigonal[] {
				new RayTrigonal(this.origin, w1, w2, w3, this.to),
				new RayTrigonal(this.origin, v1, w1, w3, this.to),
				new RayTrigonal(this.origin, v2, w2, w1, this.to),
				new RayTrigonal(this.origin, v3, w3, w2, this.to),
		};
	}
	
	public RayTrigonal setFromVec(Vec3 from) {
		return new RayTrigonal(this.origin, this.v1, this.v2, this.v3, from, this.to);
	}
	
	public RayTrigonal inverted(Axis plane, Vec3 from) {
		// origin -> from　を中心に反転
		// v1, v2, v3 反転
		// from: from
		// to: from を中心に反転
		return new RayTrigonal(
				EeMath.invertAxis(this.origin(), plane, from),
				EeMath.invertAxis(this.v1(), plane),
				EeMath.invertAxis(this.v2(), plane),
				EeMath.invertAxis(this.v3(), plane),
				from,
				EeMath.invertAxis(this.to(), plane, from));
	}
}
