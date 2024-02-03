package qwertzite.extraexplosions.exp.barostrain;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Represents a group of blocks which is connected and not separated by elastically deforming blocks.
 * 
 * @author Qwertzite
 * @date 2023/12/29
 */
public class BlockCluster {
	
	private boolean isFixed;
	
	public boolean transmitAll;
	public double pressureXNeg;
	public double pressureXPos;
	public double pressureYNeg;
	public double pressureYPos;
	public double pressureZNeg;
	public double pressureZPos;
	
	public double inertiaX;
	public double inertiaY;
	public double inertiaZ;
	
	public BlockCluster() {}
	
	public void setResult(GroupResult result) {
		this.isFixed = result.fixed;
		this.transmitAll = !result.resistive; // transmit all pressure when blast resistance is zero.
		this.pressureXNeg = result.extForceXNeg;
		this.pressureXPos = result.extForceXPos;
		this.pressureYNeg = result.extForceYNeg;
		this.pressureYPos = result.extForceYPos;
		this.pressureZNeg = result.extForceZNeg;
		this.pressureZPos = result.extForceZPos;
		this.inertiaX = result.inertia[0];
		this.inertiaY = result.inertia[1];
		this.inertiaZ = result.inertia[2];
	}
	public boolean isFixed() { return this.isFixed; }
	
	/**
	 * 
	 * @param dir the direction of the force applied by pressure.
	 * @return
	 */
	public double transmittance(Direction dir) {
		if (this.isFixed) return 0.0d;
		return this.transmitAll ? 1.0d : switch (dir) {
		case EAST:  yield pressureXPos == 0.0d ? 0.0d : Mth.clamp(inertiaX / pressureXPos, 0.0d, 1.0d); // +x
		case WEST:  yield pressureXNeg == 0.0d ? 0.0d : Mth.clamp(inertiaX / pressureXNeg, 0.0d, 1.0d); // -x
		case UP:    yield pressureYPos == 0.0d ? 0.0d : Mth.clamp(inertiaY / pressureYPos, 0.0d, 1.0d); // +y
		case DOWN:  yield pressureYNeg == 0.0d ? 0.0d : Mth.clamp(inertiaY / pressureYNeg, 0.0d, 1.0d); // -y
		case SOUTH: yield pressureZPos == 0.0d ? 0.0d : Mth.clamp(inertiaZ / pressureZPos, 0.0d, 1.0d); // +z
		case NORTH: yield pressureZNeg == 0.0d ? 0.0d : Mth.clamp(inertiaZ / pressureZNeg, 0.0d, 1.0d); // -z
		default: assert(false); yield 0.0d;
		};
	}
	
	
	public static class GroupResult {
		private boolean fixed = false;
		private boolean resistive = false; // can reflect blast. (i.e. has non-zero mass)
		private double[] inertia = new double[3];
		private int depth;
		private double extForceXPos;
		private double extForceXNeg;
		private double extForceYPos;
		private double extForceYNeg;
		private double extForceZPos;
		private double extForceZNeg;
		
		public GroupResult(boolean fixed, boolean resistive, double[] inertia,
				double xn, double xp, double yn, double yp, double zn, double zp) {
			this.fixed = fixed;
			this.inertia = inertia;
			this.resistive = resistive;
			this.extForceXNeg = xn;
			this.extForceXPos = xp;
			this.extForceYNeg = yn;
			this.extForceYPos = yp;
			this.extForceZNeg = zn;
			this.extForceZPos = zp;
		}
		
		public static GroupResult empty() {
			return new GroupResult(false, false, new double[3], 0, 0, 0, 0, 0, 0);
		}
		
		public void setFixed(boolean fixed) {
			this.fixed = fixed;
		}
		
		public GroupResult add(GroupResult other) {
			this.fixed |= other.fixed;
			this.resistive |= other.resistive;
			var inertia = other.inertia;
			this.inertia[0] += inertia[0];
			this.inertia[1] += inertia[1];
			this.inertia[2] += inertia[2];
			this.depth = Math.max(this.depth, other.depth);
			this.extForceXNeg += other.extForceXNeg;
			this.extForceXPos += other.extForceXPos;
			this.extForceYNeg += other.extForceYNeg;
			this.extForceYPos += other.extForceYPos;
			this.extForceZNeg += other.extForceZNeg;
			this.extForceZPos += other.extForceZPos;
			return this;
		}
		
		@Override
		public String toString() {
			return "fix=" + this.fixed + ",in=" + this.inertia; 
		}
	}
}
